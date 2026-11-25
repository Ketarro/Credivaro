package io.github.ketarro.credivaro.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ketarro.credivaro.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EventStorageCandidateTest {
    @Test fun materializeReplayLinkAndReopen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "event-repository-test-" + UUID.randomUUID() + ".db"
        val password = ByteArray(32).apply { java.security.SecureRandom().nextBytes(this) }
        val owner = UUID.randomUUID()
        val other = UUID.randomUUID()
        val payment = UUID.randomUUID()
        val wrongPayment = UUID.randomUUID()
        val corrected = UUID.randomUUID()
        val date = LocalDate.of(2028,2,29)
        val clock = Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
        val window = EventWindow(date,date.plusMonths(2))
        val rule = EventRule(UUID.randomUUID(), UUID.randomUUID(), owner, EventType.REQUIRED_PAYMENT,
            MonetaryRole.MANDATORY, Recurrence.Monthly(MonthlyDay(31,MissingDayPolicy.CLAMP),date),
            ZoneOffset.UTC,date,currency="RUB",amount=Money("RUB",100))
        System.loadLibrary("sqlcipher")
        val callback = object : SupportSQLiteOpenHelper.Callback(3) {
            override fun onConfigure(db: SupportSQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE obligation(id TEXT NOT NULL PRIMARY KEY,currency TEXT NOT NULL,bankZone TEXT NOT NULL)")
                db.execSQL("CREATE TABLE payment(id TEXT NOT NULL PRIMARY KEY,obligationId TEXT NOT NULL,currency TEXT NOT NULL)")
                db.execSQL("CREATE TABLE payment_correction(targetPaymentId TEXT NOT NULL PRIMARY KEY)")
                for (o in listOf(owner,other)) db.execSQL("INSERT INTO obligation VALUES (?,'RUB','Z')",arrayOf(o.toString()))
                for ((p,o) in listOf(payment to owner,wrongPayment to other,corrected to owner))
                    db.execSQL("INSERT INTO payment VALUES (?,?,'RUB')",arrayOf(p.toString(),o.toString()))
                db.execSQL("INSERT INTO payment_correction VALUES (?)",arrayOf(corrected.toString()))
                EventStorageSchemaCandidate.create(db)
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("Unexpected upgrade")
        }
        fun open() = PreserveFileFactory(password).create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(callback).build())
        fun count(db: SupportSQLiteDatabase, table: String): Int =
            db.query("SELECT COUNT(*) FROM " + table).use { check(it.moveToFirst()); it.getInt(0) }
        try {
            open().use { helper ->
                val db = helper.writableDatabase
                val store = EventStorageCandidate(db,clock)
                store.appendRule(rule,window)
                store.appendRule(rule,window)
                assertEquals(1,count(db,"event_rule_revision"))
                assertTrue(runCatching { store.appendRule(rule.copy(amount=Money("RUB",200)),window) }.isFailure)
                assertTrue(runCatching { store.appendRule(rule.copy(revisionId=UUID.randomUUID()),window) }.isFailure)
                val first = store.reconcile(owner,window)
                assertEquals(2,first.upsert.size)
                assertTrue(store.reconcile(owner,window).upsert.isEmpty())
                val id = first.upsert.first().key.id
                assertTrue(runCatching { store.linkPayment(id,wrongPayment) }.isFailure)
                assertTrue(runCatching { store.linkPayment(id,corrected) }.isFailure)
                val original = first.upsert.last()
                val moved = store.editOverride(original, EventOverride.Move(original.key, date.plusMonths(1).plusDays(1)))
                assertEquals(original.key, moved.key)
                assertEquals(date.plusMonths(1).plusDays(1), moved.dueDate)
                assertTrue(moved.overridden)
                assertTrue(runCatching { store.editOverride(original, null) }.isFailure)
                assertTrue(runCatching { store.editOverride(moved, EventOverride.Move(moved.key,date.minusDays(1))) }.isFailure)
                val cancelled = store.editOverride(moved, EventOverride.Cancel(moved.key))
                assertTrue(cancelled.cancelled)
                assertTrue(runCatching { store.linkPayment(cancelled.key.id,payment) }.isFailure)
                assertEquals(original, store.editOverride(cancelled, null))
                assertEquals(0,count(db,"event_override"))
                assertTrue(store.reconcile(owner,window).upsert.isEmpty())
                store.linkPayment(id,payment)
                store.linkPayment(id,payment)
                assertTrue(runCatching { store.editOverride(first.upsert.first(), EventOverride.Cancel(first.upsert.first().key)) }.isFailure)
                store.editOverride(original, EventOverride.Move(original.key,date.plusMonths(1).plusDays(1)))
                assertEquals(1,count(db,"event_payment_link"))
            }
            open().use { helper ->
                val db = helper.writableDatabase
                assertEquals(2,count(db,"event_occurrence"))
                assertEquals(1,count(db,"event_payment_link"))
                assertEquals(3,count(db,"payment"))
                assertEquals(1,count(db,"event_override"))
                assertTrue(EventStorageCandidate(db,clock).reconcile(owner,window).upsert.isEmpty())
                db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            }
        } finally {
            password.fill(0)
            context.deleteDatabase(name)
        }
    }
}
