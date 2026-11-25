package io.github.ketarro.credivaro.database

import androidx.room.withTransaction
import io.github.ketarro.credivaro.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency
import java.util.UUID

/** No Android UI dependency. Caller handles failures and must not report false save success. */
class ObligationRepository internal constructor(private val db: StorageDatabase, private val clock: Clock) {
    private val dao = db.dao()
    fun observeAll(): Flow<List<Obligation>> = dao.observe().map { rows -> rows.map { it.toDomain() } }
    suspend fun find(id: UUID): Obligation? = dao.find(id.toString())?.toDomain()

    suspend fun save(value: Obligation) = db.withTransaction {
        require(ObligationValidator.validate(value, LocalDate.now(clock.withZone(value.bankZone))).isEmpty()) {
            "Invalid obligation"
        }
        val id = value.id.toString()
        val previous = dao.find(id)
        val old = previous?.toDomain()
        require(old == null || (old.currency == value.currency && old.type == value.type)) {
            "Changing currency or type requires a new obligation"
        }
        val termsChanged = old == null || old.terms != value.terms
        val snapshotChanged = old?.balance != value.balance
        val termsRev = if (termsChanged) Math.addExact(previous?.row?.currentTermsRevision ?: 0L, 1L)
            else checkNotNull(previous).row.currentTermsRevision
        val snapshotRev = when {
            value.balance == null -> null
            snapshotChanged -> Math.addExact(dao.lastSnapshotRevision(id) ?: 0L, 1L)
            else -> checkNotNull(previous).row.currentSnapshotRevision
        }
        val row = ObligationRow(id, value.title, value.lender, value.currency.currencyCode,
            value.currency.defaultFractionDigits, value.bankZone.id, value.openedOn?.toString(),
            value.status.name, value.notes, termsRev, snapshotRev)
        if (previous == null) dao.insert(row) else check(dao.update(row) == 1)
        val now = clock.millis()
        if (termsChanged) dao.insertTerms(TermsRow(id, termsRev, now, value.type.name, TermsCodec.encode(value.terms)))
        if (snapshotChanged && value.balance != null) {
            val b = checkNotNull(value.balance)
            dao.insertSnapshot(SnapshotRow(id, checkNotNull(snapshotRev), now, b.bankAsOf.toString(),
                b.totalDebt?.minorUnits, b.principal?.minorUnits, b.availableCredit?.minorUnits))
        }
        Unit
    }

    /** Explicit hard delete only; UI confirmation and archival policy are separate. */
    suspend fun delete(id: UUID): Boolean = db.withTransaction { dao.delete(id.toString()) == 1 }

    private fun StoredRecord.toDomain(): Obligation {
        val currency = Currency.getInstance(row.currency)
        require(currency.defaultFractionDigits == row.scale) { "Stored currency scale differs" }
        fun money(n: Long?) = n?.let { Money(row.currency, it) }
        val snapshot = if (row.currentSnapshotRevision == null) null else BalanceSnapshot(
            LocalDate.parse(checkNotNull(snapshotDate)), money(totalDebt), money(principal), money(availableCredit))
        return Obligation(UUID.fromString(row.id), row.title, row.lender, currency, ZoneId.of(row.bankZone),
            row.openedOn?.let(LocalDate::parse), ObligationStatus.valueOf(row.status),
            TermsCodec.decode(checkNotNull(termsType), checkNotNull(termsPayload), row.currency), snapshot, row.notes)
    }
}
