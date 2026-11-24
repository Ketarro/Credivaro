package io.github.ketarro.credivaro.database

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import androidx.room.withTransaction
import io.github.ketarro.credivaro.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Currency
import java.util.UUID

/** Debug-only synthetic checks. No caller-provided database name or production repository. */
object StorageSelfCheck {
    val ids: List<String> = listOf("DB01", "DB02", "DB03", "DB04", "DB05", "DB06")
    data class Result(val id: String, val passed: Boolean, val elapsedMs: Long, val errorType: String? = null)
    private class ExpectedRollback : RuntimeException()
    private val date = LocalDate.of(2026, 1, 1)
    private val clock = Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun record(terms: ObligationTerms = ObligationTerms.Loan(), amount: Long? = 100L) = Obligation(
        UUID.randomUUID(), "SYNTHETIC_SELF_CHECK", "Test lender", Currency.getInstance("RUB"),
        ZoneOffset.UTC, date.minusDays(1), terms = terms,
        balance = amount?.let { BalanceSnapshot(date, Money("RUB", it)) }, notes = "Synthetic"
    )

    suspend fun run(context: Context, onResult: (Result) -> Unit) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        check((app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
        for (id in ids) {
            val started = SystemClock.elapsedRealtime()
            // Every case has an internally generated, independent database and wrapping key.
            val name = "selfcheck-" + UUID.randomUUID() + ".db"
            val keyFile = DatabaseKey.envelope(app, name)
            val database = app.getDatabasePath(name)
            // Refuse even a highly improbable collision; never clean up an existing database.
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (listOf("", "-wal", "-shm", "-journal").any { File(database.path + it).exists() } ||
                listOf("", ".bak", ".new").any { File(keyFile.path + it).exists() } ||
                store.containsAlias(DatabaseKey.alias(name))) {
                onResult(Result(id, false, SystemClock.elapsedRealtime() - started, "NameCollision"))
                continue
            }
            var failure: String? = null
            try {
                when (id) {
                    "DB01" -> {
                        val records = listOf(
                            record(ObligationTerms.Loan(termMonths = 12)),
                            record(ObligationTerms.CreditCard(creditLimit = Money("RUB", 10000))),
                            record(ObligationTerms.Installment(installmentCount = 4), 0),
                            record(ObligationTerms.Other("Synthetic"), null)
                        )
                        EncryptedStorage.open(app, name, clock).use { store -> records.forEach { store.repository.save(it) } }
                        EncryptedStorage.open(app, name, clock).use { store ->
                            records.forEach { check(store.repository.find(it.id) == it) }
                        }
                    }
                    "DB02" -> {
                        val password = DatabaseKey.loadOrCreate(app, name)
                        val db = EncryptedStorage.build(app, name, password)
                        try {
                            val repo = ObligationRepository(db, clock)
                            val initial = record()
                            repo.save(initial)
                            val edited = initial.copy(terms = ObligationTerms.Loan(termMonths = 3),
                                balance = BalanceSnapshot(date, Money("RUB", 70)))
                            repo.save(edited)
                            check(db.dao().termsCount(initial.id.toString()) == 2)
                            check(db.dao().snapshotCount(initial.id.toString()) == 2)
                            var rolledBack = false
                            try {
                                db.withTransaction {
                                    repo.save(edited.copy(title = "Synthetic rollback"))
                                    check(repo.find(initial.id)?.title == "Synthetic rollback")
                                    throw ExpectedRollback()
                                }
                            } catch (_: ExpectedRollback) { rolledBack = true }
                            check(rolledBack && repo.find(initial.id) == edited)
                        } finally { try { db.close() } finally { password.fill(0) } }
                    }
                    "DB03" -> {
                        val value = record()
                        EncryptedStorage.open(app, name, clock).use {
                            it.repository.save(value)
                            check(it.repository.delete(value.id))
                        }
                        EncryptedStorage.open(app, name, clock).use { check(it.repository.find(value.id) == null) }
                    }
                    "DB04" -> {
                        val value = record()
                        EncryptedStorage.open(app, name, clock).use {
                            it.repository.save(value)
                            var rejected = false
                            try { it.repository.save(value.copy(title = " ")) }
                            catch (_: IllegalArgumentException) { rejected = true }
                            check(rejected && it.repository.find(value.id) == value)
                        }
                    }
                    "DB05" -> {
                        val value = record()
                        EncryptedStorage.open(app, name, clock).use { it.repository.save(value) }
                        val bytes = database.readBytes()
                        check(bytes.size >= 16)
                        check(!bytes.take(16).toByteArray().contentEquals("SQLite format 3\u0000".toByteArray()))
                        check(!bytes.toString(Charsets.ISO_8859_1).contains(value.title))
                        val wrong = ByteArray(44) { 'X'.code.toByte() }
                        val db = EncryptedStorage.build(app, name, wrong)
                        try {
                            var rejected = false
                            try { db.openHelper.writableDatabase }
                            catch (_: Exception) { rejected = true }
                            check(rejected)
                        } finally { try { db.close() } finally { wrong.fill(0) } }
                        check(database.readBytes().contentEquals(bytes))
                        EncryptedStorage.open(app, name, clock).use { check(it.repository.find(value.id) == value) }
                    }
                    "DB06" -> {
                        val value = record()
                        EncryptedStorage.open(app, name, clock).use { it.repository.save(value) }
                        val before = database.readBytes()
                        val original = keyFile.readBytes()
                        // Damage only the ephemeral test envelope, never the production key.
                        keyFile.writeBytes(original.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() })
                        var rejected = false
                        try { EncryptedStorage.open(app, name, clock).close() }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { rejected = true }
                        check(rejected && database.readBytes().contentEquals(before))
                        keyFile.writeBytes(original)
                        EncryptedStorage.open(app, name, clock).use { check(it.repository.find(value.id) == value) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = e.javaClass.simpleName
            } catch (e: LinkageError) {
                failure = e.javaClass.simpleName
            } finally {
                withContext(NonCancellable) {
                    // Exact names only; no directory scanning or broad key deletion.
                    try {
                        app.deleteDatabase(name)
                        listOf("", ".bak", ".new").forEach { suffix ->
                            val file = File(keyFile.path + suffix)
                            check(!file.exists() || file.delete())
                        }
                        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(DatabaseKey.alias(name)) }
                        check(listOf("", "-wal", "-shm", "-journal").none { File(database.path + it).exists() })
                    } catch (_: Exception) {
                        failure = "CleanupFailed"
                    }
                }
            }
            onResult(Result(id, failure == null, SystemClock.elapsedRealtime() - started, failure))
        }
    }
}
