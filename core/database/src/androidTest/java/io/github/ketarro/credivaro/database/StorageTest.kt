package io.github.ketarro.credivaro.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.withTransaction
import io.github.ketarro.credivaro.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal
import java.security.KeyStore
import java.time.*
import java.util.Currency
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StorageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "test-" + UUID.randomUUID() + ".db"
    private val today = LocalDate.of(2026, 1, 1)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private fun rub(n: Long) = Money("RUB", n)
    private fun record(terms: ObligationTerms = ObligationTerms.Loan()) = Obligation(
        UUID.randomUUID(), "SYNTHETIC_TEST_MARKER_0123456789", "Test lender", Currency.getInstance("RUB"),
        ZoneOffset.UTC, today.minusDays(1), terms = terms,
        balance = BalanceSnapshot(today, rub(100), rub(50)), notes = "Synthetic")

    @After fun cleanup() {
        context.deleteDatabase(name)
        val envelope = DatabaseKey.envelope(context, name)
        listOf("", ".bak", ".new").forEach { File(envelope.path + it).delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(DatabaseKey.alias(name)) }
    }

    @Test fun roundTripAllTypesAndReopen() = runBlocking {
        val day = MonthlyDay(31, MissingDayPolicy.CLAMP)
        val rate = AnnualRate(BigDecimal("12.345"))
        val records = listOf(
            record(ObligationTerms.Loan(rub(10000), rate, 12, PaymentScheme.ANNUITY, rub(999), today, day, PaymentFrequency.MONTHLY)),
            record(ObligationTerms.CreditCard(rub(5000), rate, day, rub(0), today, rub(300), day,
                listOf(GraceGroup(UUID.randomUUID(), "Purchase", today, today.plusDays(30), rub(123))))),
            record(ObligationTerms.Installment(rub(900), rub(100), 4, rub(200), today, rate, rub(0), PaymentFrequency.MONTHLY, day)),
            record(ObligationTerms.Other("manual")).copy(balance = null)
        )
        EncryptedStorage.open(context, name, clock).use { store -> records.forEach { store.repository.save(it) } }
        EncryptedStorage.open(context, name, clock).use { store ->
            assertEquals(records.toSet(), store.repository.observeAll().first().toSet())
        }
    }

    @Test fun frequencyPayloadAndLegacyCompatibility() = runBlocking {
        val old = TermsCodec.decode("LOAN", """{"v":1,"paymentDay":{"day":31,"policy":"CLAMP"}}""", "RUB") as ObligationTerms.Loan
        assertNull(old.paymentFrequency)
        assertEquals(MonthlyDay(31, MissingDayPolicy.CLAMP), old.paymentDay)
        for (frequency in PaymentFrequency.entries) {
            val original = record(ObligationTerms.Loan(firstDueDate = today, paymentFrequency = frequency,
                paymentDay = if (frequency == PaymentFrequency.MONTHLY) MonthlyDay(31, MissingDayPolicy.SKIP) else null))
            EncryptedStorage.open(context, name, clock).use { it.repository.save(original) }
            EncryptedStorage.open(context, name, clock).use { assertEquals(original, it.repository.find(original.id)) }
        }
    }

    @Test fun historyAndRollback() = runBlocking {
        val password = DatabaseKey.loadOrCreate(context, name)
        val db = EncryptedStorage.build(context, name, password)
        try {
            val repo = ObligationRepository(db, clock)
            val original = record()
            repo.save(original)
            repo.save(original.copy(title = "Updated"))
            val revised = original.copy(terms = ObligationTerms.Loan(termMonths = 3), balance = BalanceSnapshot(today, rub(0)))
            repo.save(revised)
            repo.save(revised.copy(balance = null))
            repo.save(revised)
            assertEquals(2, db.dao().termsCount(original.id.toString()))
            assertEquals(3, db.dao().snapshotCount(original.id.toString()))
            assertEquals(revised, repo.find(original.id))
            try {
                db.withTransaction {
                    repo.save(revised.copy(title = "Rolled back"))
                    error("synthetic rollback")
                }
                fail("Transaction should fail")
            } catch (_: IllegalStateException) { }
            assertEquals(revised, repo.find(original.id))
            assertTrue(repo.delete(original.id))
            assertEquals(0, db.dao().termsCount(original.id.toString()))
            assertEquals(0, db.dao().snapshotCount(original.id.toString()))
        } finally { db.close(); password.fill(0) }
    }

    @Test fun rejectsWrongKeyAndPlaintextWithoutDeleting() = runBlocking {
        val original = record()
        EncryptedStorage.open(context, name, clock).use { it.repository.save(original) }
        val file = context.getDatabasePath(name)
        val bytes = file.readBytes()
        assertFalse(bytes.take(16).toByteArray().contentEquals("SQLite format 3\u0000".toByteArray()))
        assertFalse(bytes.toString(Charsets.ISO_8859_1).contains(original.title))
        val wrong = ByteArray(44) { 'X'.code.toByte() }
        withContext(Dispatchers.IO) {
            val db = EncryptedStorage.build(context, name, wrong)
            try {
                val failure = runCatching { db.openHelper.writableDatabase }.exceptionOrNull()
                assertNotNull("Wrong key should fail", failure)
            } finally { db.close(); wrong.fill(0) }
        }
        assertTrue(file.exists())
        assertArrayEquals(bytes, file.readBytes())
        // Try the platform SQLite reader on a disposable copy, not the original file.
        val copy = File(context.cacheDir, name + ".plain-test")
        try {
            copy.writeBytes(bytes)
            val failed = runCatching {
                android.database.sqlite.SQLiteDatabase.openDatabase(copy.path, null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY).use { plain ->
                    plain.rawQuery("SELECT title FROM obligation", null).use { it.moveToFirst() }
                }
            }.isFailure
            assertTrue(failed)
        } finally { copy.delete() }
        EncryptedStorage.open(context, name, clock).use { assertEquals(original, it.repository.find(original.id)) }
    }

    @Test fun lostKeystoreKeyDoesNotResetDatabase() = runBlocking {
        EncryptedStorage.open(context, name, clock).use { it.repository.save(record()) }
        val db = context.getDatabasePath(name)
        val before = db.readBytes()
        val envelope = DatabaseKey.envelope(context, name)
        val wrapped = envelope.readBytes()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(DatabaseKey.alias(name)) }
        assertTrue(runCatching { EncryptedStorage.open(context, name, clock).close() }.isFailure)
        assertArrayEquals(before, db.readBytes())
        assertArrayEquals(wrapped, envelope.readBytes())
    }

    @Test fun missingOrTamperedEnvelopeFailsClosed() = runBlocking {
        EncryptedStorage.open(context, name, clock).use { it.repository.save(record()) }
        val db = context.getDatabasePath(name)
        val before = db.readBytes()
        val envelope = DatabaseKey.envelope(context, name)
        val original = envelope.readBytes()
        envelope.writeBytes(original.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() })
        assertTrue(runCatching { EncryptedStorage.open(context, name, clock).close() }.isFailure)
        assertArrayEquals(before, db.readBytes())
        assertTrue(envelope.delete())
        assertTrue(runCatching { EncryptedStorage.open(context, name, clock).close() }.isFailure)
        assertArrayEquals(before, db.readBytes())
        envelope.writeBytes(original)
        EncryptedStorage.open(context, name, clock).use { assertEquals(1, it.repository.observeAll().first().size) }
    }
}
