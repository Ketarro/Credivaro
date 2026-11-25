package io.github.ketarro.credivaro.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.room.withTransaction
import io.github.ketarro.credivaro.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.time.*
import java.security.KeyStore
import java.util.Currency
import java.util.UUID

/** Called only with a fresh collision-checked synthetic database. */
internal object PaymentSelfCheck {
    val ids = listOf("DB07", "DB08", "PAY01", "PAY02", "PAY03", "PAY04", "PAY05", "MIG01", "MIG02", "MIG03")
    private val date = LocalDate.of(2026, 1, 1)
    private fun rub(n: Long) = Money("RUB", n)
    private fun owner() = Obligation(UUID.randomUUID(), "Synthetic payment", currency = Currency.getInstance("RUB"),
        bankZone = ZoneOffset.UTC, openedOn = date.minusDays(1), terms = ObligationTerms.Other("fixture"),
        balance = BalanceSnapshot(date, rub(500)))
    private fun payment(o: Obligation, amount: Long = 100) =
        RecordedPayment(UUID.randomUUID(), UUID.randomUUID(), o.id, date, rub(amount))
    private suspend fun rejected(block: suspend () -> Unit): Boolean =
        try { block(); false } catch (e: CancellationException) { throw e } catch (_: IllegalArgumentException) { true }
    private suspend fun openRejected(c: Context, name: String, clock: Clock): Boolean =
        try { EncryptedStorage.open(c, name, clock).close(); false }
        catch (e: CancellationException) { throw e } catch (_: Exception) { true }
    private class Rollback : RuntimeException()

    suspend fun runCase(id: String, c: Context, name: String, clock: Clock) {
        check(name.startsWith("selfcheck-") && name.endsWith(".db"))
        val o = owner()
        val p = payment(o)
        when (id) {
            "DB07", "DB08" -> {
                EncryptedStorage.open(c, name, clock).use { it.repository.save(o) }
                val db = c.getDatabasePath(name)
                val before = db.readBytes()
                val key = DatabaseKey.envelope(c, name)
                val envelope = key.readBytes()
                if (id == "DB07") check(key.delete()) else
                    KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(DatabaseKey.alias(name)) }
                check(openRejected(c, name, clock) && db.readBytes().contentEquals(before))
                if (id == "DB07") {
                    check(!key.exists())
                    key.writeBytes(envelope)
                    EncryptedStorage.open(c, name, clock).use { check(it.repository.find(o.id) == o) }
                } else {
                    check(key.readBytes().contentEquals(envelope))
                    check(!KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(DatabaseKey.alias(name)))
                }
            }
            "PAY01" -> {
                EncryptedStorage.open(c, name, clock).use { s ->
                    s.repository.save(o)
                    check(s.payments.record(p) == PaymentRecordResult.INSERTED)
                    check(s.payments.record(p) == PaymentRecordResult.ALREADY_RECORDED)
                    check(s.payments.observeFor(o.id).first() == listOf(p))
                    check(s.repository.find(o.id) == o)
                }
                EncryptedStorage.open(c, name, clock).use { s ->
                    check(s.payments.observeFor(o.id).first() == listOf(p) && s.repository.find(o.id) == o)
                }
            }
            "PAY02" -> EncryptedStorage.open(c, name, clock).use { s ->
                s.repository.save(o); s.payments.record(p)
                check(rejected { s.payments.record(p.copy(notes = "conflict")) })
                check(rejected { s.payments.record(payment(o).copy(bankDate = date.plusDays(1))) })
                check(rejected { s.payments.record(payment(o).copy(bankDate = date.minusDays(2))) })
                check(rejected { s.payments.record(payment(o).copy(obligationId = UUID.randomUUID())) })
                val usd = Money("USD", 100)
                check(rejected { s.payments.record(payment(o).copy(amount = usd, allocation = PaymentAllocation.unknown(usd))) })
                check(s.payments.observeFor(o.id).first() == listOf(p))
            }
            "PAY03" -> {
                val replacement = payment(o, 90)
                val change = PaymentCorrection(UUID.randomUUID(), o.id, p.id, "Synthetic correction", replacement)
                val cancel = PaymentCorrection(UUID.randomUUID(), o.id, replacement.id, "Synthetic cancellation")
                EncryptedStorage.open(c, name, clock).use { s ->
                    s.repository.save(o); s.payments.record(p)
                    check(s.payments.correct(change) == PaymentRecordResult.INSERTED)
                    check(s.payments.correct(change) == PaymentRecordResult.ALREADY_RECORDED)
                    check(rejected { s.payments.correct(change.copy(operationId = UUID.randomUUID())) })
                    check(s.payments.observeFor(o.id).first() == listOf(replacement))
                    check(s.payments.correct(cancel) == PaymentRecordResult.INSERTED)
                    check(s.payments.observeFor(o.id).first().isEmpty() && s.repository.find(o.id) == o)
                }
                EncryptedStorage.open(c, name, clock).use { s ->
                    val h = s.payments.observeHistoryFor(o.id).first().associateBy { it.payment.id }
                    check(h.size == 2 && h.getValue(p.id).payment == p)
                    check(h.getValue(p.id).status == PaymentHistoryStatus.REPLACED)
                    check(h.getValue(p.id).replacementId == replacement.id)
                    check(h.getValue(replacement.id).status == PaymentHistoryStatus.CANCELLED)
                    check(s.repository.delete(o.id))
                    check(s.payments.observeHistoryFor(o.id).first().isEmpty())
                }
            }
            "PAY04" -> EncryptedStorage.open(c, name, clock).use { s ->
                s.repository.save(o)
                val results = coroutineScope {
                    val a = async { s.payments.record(p) }; val b = async { s.payments.record(p) }
                    listOf(a.await(), b.await())
                }
                check(results.toSet() == setOf(PaymentRecordResult.INSERTED, PaymentRecordResult.ALREADY_RECORDED))
                check(s.payments.observeFor(o.id).first() == listOf(p))
            }
            "PAY05" -> {
                val password = DatabaseKey.loadOrCreate(c, name)
                val db = EncryptedStorage.build(c, name, password)
                try {
                    val repo = ObligationRepository(db, clock); val payments = PaymentRepository(db, clock)
                    repo.save(o); payments.record(p)
                    val correction = PaymentCorrection(UUID.randomUUID(), o.id, p.id, "Synthetic rollback", payment(o, 90))
                    var rolledBack = false
                    try { db.withTransaction { payments.correct(correction); throw Rollback() } }
                    catch (_: Rollback) { rolledBack = true }
                    check(rolledBack && payments.observeFor(o.id).first() == listOf(p))
                    check(payments.observeHistoryFor(o.id).first().single().status == PaymentHistoryStatus.ACTIVE)
                    check(payments.correct(correction) == PaymentRecordResult.INSERTED)
                } finally { try { db.close() } finally { password.fill(0) } }
                EncryptedStorage.open(c, name, clock).use { check(it.payments.observeHistoryFor(o.id).first().size == 2) }
            }
            "MIG01" -> migrate(c, name, clock, 1, false)
            "MIG02" -> migrate(c, name, clock, 2, false)
            "MIG03" -> migrate(c, name, clock, 2, true)
            else -> error("Unknown check")
        }
    }
    private suspend fun migrate(c: Context, name: String, clock: Clock, version: Int, rollback: Boolean) {
        val o = owner(); val p = payment(o); val other = UUID.randomUUID().toString()
        val password = DatabaseKey.loadOrCreate(c, name)
        System.loadLibrary("sqlcipher")
        val callback = object : SupportSQLiteOpenHelper.Callback(version) {
            override fun onConfigure(db: SupportSQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
            override fun onCreate(db: SupportSQLiteDatabase) {
                listOf(
                    "CREATE TABLE IF NOT EXISTS `obligation` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `lender` TEXT, `currency` TEXT NOT NULL, `scale` INTEGER NOT NULL, `bankZone` TEXT NOT NULL, `openedOn` TEXT, `status` TEXT NOT NULL, `notes` TEXT, `currentTermsRevision` INTEGER NOT NULL, `currentSnapshotRevision` INTEGER, PRIMARY KEY(`id`))",
                    "CREATE TABLE IF NOT EXISTS `terms_revision` (`obligationId` TEXT NOT NULL, `revision` INTEGER NOT NULL, `recordedAt` INTEGER NOT NULL, `type` TEXT NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`obligationId`, `revision`), FOREIGN KEY(`obligationId`) REFERENCES `obligation`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    "CREATE TABLE IF NOT EXISTS `balance_snapshot` (`obligationId` TEXT NOT NULL, `revision` INTEGER NOT NULL, `recordedAt` INTEGER NOT NULL, `bankAsOf` TEXT NOT NULL, `totalDebt` INTEGER, `principal` INTEGER, `availableCredit` INTEGER, PRIMARY KEY(`obligationId`, `revision`), FOREIGN KEY(`obligationId`) REFERENCES `obligation`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    "CREATE INDEX IF NOT EXISTS `index_balance_snapshot_obligationId_bankAsOf` ON `balance_snapshot` (`obligationId`, `bankAsOf`)"
                ).forEach { db.execSQL(it) }
                if (version == 2) PAYMENT_MIGRATION_1_2.migrate(db)
                db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, ?)",
                    arrayOf<Any>(if (version == 1) "640c14a555749a50028946c4e260e7e9" else "bfe82344d21281a6ea4d0aa3c5072a86"))
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) { error("Fixture must not upgrade itself") }
        }
        val helper = PreserveFileFactory(password).create(SupportSQLiteOpenHelper.Configuration.builder(c).name(name).callback(callback).build())
        try {
            val db = helper.writableDatabase
            val id = o.id.toString()
            db.execSQL("INSERT INTO obligation VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                arrayOf<Any?>(id, o.title, null, "RUB", 2, "Z", date.minusDays(1).toString(), "ACTIVE", null, 2L, 2L))
            for (r in 1L..2L) {
                db.execSQL("INSERT INTO terms_revision VALUES(?,?,?,?,?)", arrayOf<Any?>(id, r, r, "OTHER", TermsCodec.encode(o.terms)))
                db.execSQL("INSERT INTO balance_snapshot VALUES(?,?,?,?,?,?,?)", arrayOf<Any?>(id, r, r, date.toString(), if (r == 2L) 500L else 700L, null, null))
            }
            db.execSQL("INSERT INTO obligation VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                arrayOf<Any?>(other, "Synthetic zero USD", null, "USD", 2, "Z", null, "ACTIVE", null, 1L, 1L))
            db.execSQL("INSERT INTO terms_revision VALUES(?,?,?,?,?)", arrayOf<Any?>(other, 1L, 1L, "OTHER", TermsCodec.encode(ObligationTerms.Other())))
            db.execSQL("INSERT INTO balance_snapshot VALUES(?,?,?,?,?,?,?)", arrayOf<Any?>(other, 1L, 1L, date.toString(), 0L, null, null))
            if (version == 2) db.execSQL("INSERT INTO payment VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                arrayOf<Any?>(p.id.toString(), p.operationId.toString(), id, date.toString(), "RUB", 2, 100L, 0L, 0L, 0L, 100L, null, 1L))
            if (rollback) {
                db.beginTransaction()
                try { PAYMENT_MIGRATION_2_3.migrate(db); check(tableExists(db, "payment_correction")) }
                finally { db.endTransaction() } // No setTransactionSuccessful: rollback.
                check(!tableExists(db, "payment_correction") && db.version == 2)
                check(count(db, "payment") == 1 && count(db, "obligation") == 2)
            }
        } finally { try { helper.close() } finally { password.fill(0) } }
        val envelope = DatabaseKey.envelope(c, name).readBytes()
        EncryptedStorage.open(c, name, clock).use { s ->
            check(s.repository.find(o.id) == o)
            val zero = checkNotNull(s.repository.find(UUID.fromString(other)))
            check(zero.currency.currencyCode == "USD" && zero.balance?.totalDebt == Money("USD", 0))
            check(s.payments.observeFor(o.id).first() == if (version == 2) listOf(p) else emptyList())
            if (version == 1) check(s.payments.record(p) == PaymentRecordResult.INSERTED)
        }
        check(DatabaseKey.envelope(c, name).readBytes().contentEquals(envelope))
        EncryptedStorage.open(c, name, clock).use { s ->
            check(s.payments.observeFor(o.id).first() == listOf(p) && s.repository.find(o.id) == o)
        }
        val key = DatabaseKey.loadOrCreate(c, name)
        val db = EncryptedStorage.build(c, name, key)
        try {
            val sqlite = db.openHelper.writableDatabase
            check(sqlite.version == 3 && tableExists(sqlite, "payment_correction"))
            check(db.dao().termsCount(o.id.toString()) == 2 && db.dao().snapshotCount(o.id.toString()) == 2)
            sqlite.query("PRAGMA foreign_key_check").use { check(!it.moveToFirst()) }
            sqlite.query("PRAGMA integrity_check").use { check(it.moveToFirst() && it.getString(0) == "ok") }
        } finally { try { db.close() } finally { key.fill(0) } }
    }
    private fun tableExists(db: SupportSQLiteDatabase, name: String) =
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$name'").use { it.moveToFirst() }
    private fun count(db: SupportSQLiteDatabase, table: String) =
        db.query("SELECT COUNT(*) FROM $table").use { check(it.moveToFirst()); it.getInt(0) }
}
