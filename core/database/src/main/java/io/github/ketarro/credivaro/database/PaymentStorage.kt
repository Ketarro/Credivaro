package io.github.ketarro.credivaro.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.ketarro.credivaro.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency
import java.util.UUID

@Entity(tableName = "payment",
    foreignKeys = [ForeignKey(entity = ObligationRow::class, parentColumns = ["id"],
        childColumns = ["obligationId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["operationId"], unique = true), Index(value = ["obligationId", "bankDate"])])
internal data class PaymentRow(
    @PrimaryKey val id: String, val operationId: String, val obligationId: String,
    val bankDate: String, val currency: String, val scale: Int, val amount: Long,
    val principal: Long, val interest: Long, val fees: Long, val unallocated: Long,
    val notes: String?, val recordedAt: Long
)

@Dao
internal interface PaymentDao {
    @Query("SELECT * FROM payment WHERE obligationId=:id ORDER BY bankDate DESC, recordedAt DESC, id")
    fun observeFor(id: String): Flow<List<PaymentRow>>

    @Query("SELECT * FROM payment WHERE operationId=:id")
    suspend fun findOperation(id: String): PaymentRow?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(row: PaymentRow)
}

/** Additive migration: existing obligations, snapshots and key material are untouched. */
internal val PAYMENT_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `payment` (`id` TEXT NOT NULL, `operationId` TEXT NOT NULL, `obligationId` TEXT NOT NULL, `bankDate` TEXT NOT NULL, `currency` TEXT NOT NULL, `scale` INTEGER NOT NULL, `amount` INTEGER NOT NULL, `principal` INTEGER NOT NULL, `interest` INTEGER NOT NULL, `fees` INTEGER NOT NULL, `unallocated` INTEGER NOT NULL, `notes` TEXT, `recordedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`obligationId`) REFERENCES `obligation`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_payment_operationId` ON `payment` (`operationId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_obligationId_bankDate` ON `payment` (`obligationId`, `bankDate`)")
    }
}

enum class PaymentRecordResult { INSERTED, ALREADY_RECORDED }

/** Journal of actual payments; no automatic bank transfer or balance mutation. */
class PaymentRepository internal constructor(private val db: StorageDatabase, private val clock: Clock) {
    private val dao = db.paymentDao()

    fun observeFor(obligationId: UUID): Flow<List<RecordedPayment>> =
        dao.observeFor(obligationId.toString()).map { rows -> rows.map { it.toDomain() } }

    suspend fun record(value: RecordedPayment): PaymentRecordResult = db.withTransaction {
        val existing = dao.findOperation(value.operationId.toString())
        if (existing != null) {
            require(existing.toDomain() == value) { "Operation ID already used for a different payment" }
            return@withTransaction PaymentRecordResult.ALREADY_RECORDED
        }
        val owner = requireNotNull(db.dao().find(value.obligationId.toString())) { "Unknown obligation" }.row
        val currency = Currency.getInstance(owner.currency)
        require(currency.defaultFractionDigits == owner.scale && owner.scale in 0..3) { "Invalid stored currency scale" }
        value.validateFor(UUID.fromString(owner.id), currency)
        require(!value.bankDate.isAfter(LocalDate.now(clock.withZone(ZoneId.of(owner.bankZone))))) {
            "Actual payment cannot be dated in the future"
        }
        require(owner.openedOn == null || !value.bankDate.isBefore(LocalDate.parse(owner.openedOn))) {
            "Payment predates obligation"
        }
        val a = value.allocation
        dao.insert(PaymentRow(value.id.toString(), value.operationId.toString(), owner.id,
            value.bankDate.toString(), owner.currency, owner.scale, value.amount.minorUnits,
            a.principal.minorUnits, a.interest.minorUnits, a.fees.minorUnits,
            a.unallocated.minorUnits, value.notes, clock.millis()))
        PaymentRecordResult.INSERTED
    }

    private fun PaymentRow.toDomain(): RecordedPayment {
        require(Currency.getInstance(currency).defaultFractionDigits == scale && scale in 0..3) {
            "Stored payment currency scale differs"
        }
        fun money(n: Long) = Money(currency, n)
        return RecordedPayment(UUID.fromString(id), UUID.fromString(operationId), UUID.fromString(obligationId),
            LocalDate.parse(bankDate), money(amount),
            PaymentAllocation(money(principal), money(interest), money(fees), money(unallocated)), notes)
    }
}
