package io.github.ketarro.credivaro.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EventStorageSchemaCandidateTest {
    @Test fun syntheticEncryptedFixtureConstraintsAndRollback() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "event-schema-test-" + UUID.randomUUID() + ".db"
        val password = ByteArray(32).apply { java.security.SecureRandom().nextBytes(this) }
        System.loadLibrary("sqlcipher")
        val callback = object : SupportSQLiteOpenHelper.Callback(3) {
            override fun onConfigure(db: SupportSQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE obligation(id TEXT NOT NULL PRIMARY KEY)")
                db.execSQL("CREATE TABLE payment(id TEXT NOT NULL PRIMARY KEY)")
                db.execSQL("INSERT INTO obligation VALUES ('o')")
                db.execSQL("INSERT INTO payment VALUES ('p')")
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
                error("No upgrade in isolated candidate test")
        }
        fun open() = PreserveFileFactory(password).create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(callback).build())
        fun count(db: SupportSQLiteDatabase, sql: String): Int =
            db.query(sql).use { check(it.moveToFirst()); it.getInt(0) }
        try {
            open().use { helper ->
                val db = helper.writableDatabase
                assertTrue(runCatching { EventStorageSchemaCandidate.create(db) }.isFailure)
                db.beginTransaction()
                try { EventStorageSchemaCandidate.create(db) } finally { db.endTransaction() }
                assertEquals(0, count(db, "SELECT COUNT(*) FROM sqlite_master WHERE name='event_series'"))
                assertEquals(1, count(db, "SELECT COUNT(*) FROM obligation"))
                db.beginTransaction()
                try {
                    EventStorageSchemaCandidate.create(db)
                    db.execSQL("INSERT INTO event_series VALUES ('s','o','CUSTOM','month')")
                    db.execSQL("INSERT INTO event_series VALUES ('s2','o','CUSTOM','month')")
                    db.execSQL("INSERT INTO event_rule_revision VALUES ('r','s','2026-01-01',NULL,'synthetic')")
                    db.execSQL("INSERT INTO event_occurrence VALUES ('e','s','r','m:2026-01','2026-01-01','2026-01-02','synthetic',0)")
                    assertTrue(runCatching { db.execSQL("INSERT INTO event_occurrence VALUES ('duplicate','s','r','m:2026-01','2026-01-01','2026-01-02','synthetic',0)") }.isFailure)
                    assertTrue(runCatching { db.execSQL("INSERT INTO event_occurrence VALUES ('wrong','s2','r','m:2026-01','2026-01-01','2026-01-02','synthetic',0)") }.isFailure)
                    assertTrue(runCatching { db.execSQL("INSERT INTO event_override VALUES ('s','m:2026-01','MOVE',NULL)") }.isFailure)
                    db.execSQL("INSERT INTO event_override VALUES ('s','m:2026-01','MOVE','2026-01-02')")
                    db.execSQL("INSERT INTO event_payment_link VALUES ('e','p')")
                    assertTrue(runCatching { db.execSQL("INSERT INTO event_payment_link VALUES ('e','missing')") }.isFailure)
                    db.setTransactionSuccessful()
                } finally { db.endTransaction() }
            }
            open().use { helper ->
                val db = helper.writableDatabase
                assertEquals(1, count(db, "SELECT COUNT(*) FROM event_occurrence"))
                assertEquals(1, count(db, "SELECT COUNT(*) FROM event_override"))
                assertEquals(1, count(db, "SELECT COUNT(*) FROM event_payment_link"))
                assertEquals(1, count(db, "SELECT COUNT(*) FROM payment"))
                db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            }
        } finally {
            password.fill(0)
            context.deleteDatabase(name)
        }
    }
}
