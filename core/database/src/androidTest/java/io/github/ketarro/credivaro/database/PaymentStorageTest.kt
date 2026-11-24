package io.github.ketarro.credivaro.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ketarro.credivaro.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore
import java.time.*
import java.util.Currency
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PaymentStorageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "payment-test-" + UUID.randomUUID() + ".db"
    private val today = LocalDate.of(2026, 1, 1)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private fun rub(n: Long) = Money("RUB", n)
    private fun payment() = RecordedPayment(UUID.randomUUID(), UUID.randomUUID(), id, today, rub(100))

    @After fun cleanup() {
        context.deleteDatabase(name)
        val envelope = DatabaseKey.envelope(context, name)
        listOf("", ".bak", ".new").forEach { File(envelope.path + it).delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(DatabaseKey.alias(name)) }
    }

    @Test fun recordReplayConflictAndReopen() = runBlocking {
        val obligation = Obligation(id, "Synthetic", currency = Currency.getInstance("RUB"),
            bankZone = ZoneOffset.UTC, openedOn = today.minusDays(1), terms = ObligationTerms.Other(),
            balance = BalanceSnapshot(today, rub(500)))
        val p = payment()
        EncryptedStorage.open(context, name, clock).use { store ->
            store.repository.save(obligation)
            assertEquals(PaymentRecordResult.INSERTED, store.payments.record(p))
            assertEquals(PaymentRecordResult.ALREADY_RECORDED, store.payments.record(p))
            assertTrue(runCatching { store.payments.record(p.copy(notes = "different")) }.isFailure)
            assertTrue(runCatching { store.payments.record(p.copy(operationId = UUID.randomUUID())) }.isFailure)
            assertTrue(runCatching { store.payments.record(payment().copy(obligationId = UUID.randomUUID())) }.isFailure)
            assertTrue(runCatching { store.payments.record(payment().copy(bankDate = today.plusDays(1))) }.isFailure)
            assertTrue(runCatching { store.payments.record(payment().copy(bankDate = today.minusDays(2))) }.isFailure)
            val usd = Money("USD", 100)
            assertTrue(runCatching { store.payments.record(payment().copy(amount = usd,
                allocation = PaymentAllocation.unknown(usd))) }.isFailure)
            assertEquals(listOf(p), store.payments.observeFor(id).first())
            assertEquals(obligation, store.repository.find(id))
        }
        EncryptedStorage.open(context, name, clock).use { store ->
            assertEquals(listOf(p), store.payments.observeFor(id).first())
            assertEquals(obligation, store.repository.find(id))
            assertTrue(store.repository.delete(id))
            assertTrue(store.payments.observeFor(id).first().isEmpty())
        }
    }

    /** SQLCipher fixture uses the actual exported v1 DDL and identity hash, not the old APK. */
    @Test fun migrateEncryptedV1Fixture() = runBlocking {
        val password = DatabaseKey.loadOrCreate(context, name)
        System.loadLibrary("sqlcipher")
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onConfigure(db: SupportSQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `obligation` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `lender` TEXT, `currency` TEXT NOT NULL, `scale` INTEGER NOT NULL, `bankZone` TEXT NOT NULL, `openedOn` TEXT, `status` TEXT NOT NULL, `notes` TEXT, `currentTermsRevision` INTEGER NOT NULL, `currentSnapshotRevision` INTEGER, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `terms_revision` (`obligationId` TEXT NOT NULL, `revision` INTEGER NOT NULL, `recordedAt` INTEGER NOT NULL, `type` TEXT NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`obligationId`, `revision`), FOREIGN KEY(`obligationId`) REFERENCES `obligation`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE TABLE IF NOT EXISTS `balance_snapshot` (`obligationId` TEXT NOT NULL, `revision` INTEGER NOT NULL, `recordedAt` INTEGER NOT NULL, `bankAsOf` TEXT NOT NULL, `totalDebt` INTEGER, `principal` INTEGER, `availableCredit` INTEGER, PRIMARY KEY(`obligationId`, `revision`), FOREIGN KEY(`obligationId`) REFERENCES `obligation`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_balance_snapshot_obligationId_bankAsOf` ON `balance_snapshot` (`obligationId`, `bankAsOf`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '640c14a555749a50028946c4e260e7e9')")
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                error("Legacy fixture must not upgrade itself")
            }
        }
        val config = SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(callback).build()
        val helper = PreserveFileFactory(password).create(config)
        try {
            val db = helper.writableDatabase
            db.execSQL("INSERT INTO obligation VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                arrayOf<Any?>(id.toString(), "Synthetic legacy", null, "RUB", 2, "Z", "2025-12-31", "ACTIVE", null, 1L, 1L))
            db.execSQL("INSERT INTO terms_revision VALUES(?,?,?,?,?)",
                arrayOf<Any?>(id.toString(), 1L, 1L, "OTHER", TermsCodec.encode(ObligationTerms.Other("fixture"))))
            db.execSQL("INSERT INTO balance_snapshot VALUES(?,?,?,?,?,?,?)",
                arrayOf<Any?>(id.toString(), 1L, 1L, today.toString(), 500L, null, null))
        } finally { helper.close(); password.fill(0) }
        val envelope = DatabaseKey.envelope(context, name).readBytes()
        val p = payment()
        EncryptedStorage.open(context, name, clock).use { store ->
            val value = checkNotNull(store.repository.find(id))
            assertEquals("Synthetic legacy", value.title)
            assertEquals(ObligationTerms.Other("fixture"), value.terms)
            assertEquals(BalanceSnapshot(today, rub(500)), value.balance)
            assertTrue(store.payments.observeFor(id).first().isEmpty())
            assertEquals(PaymentRecordResult.INSERTED, store.payments.record(p))
            assertEquals(value, store.repository.find(id))
        }
        assertArrayEquals(envelope, DatabaseKey.envelope(context, name).readBytes())
        EncryptedStorage.open(context, name, clock).use { store ->
            assertEquals(listOf(p), store.payments.observeFor(id).first())
        }
    }
}
