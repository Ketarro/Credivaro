package io.github.ketarro.credivaro.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** A target may be corrected once; a replacement can itself be corrected later. */
@Entity(tableName = "payment_correction",
    foreignKeys = [
        ForeignKey(entity = PaymentRow::class, parentColumns = ["id"],
            childColumns = ["targetPaymentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PaymentRow::class, parentColumns = ["id"],
            childColumns = ["replacementId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["operationId"], unique = true), Index(value = ["replacementId"], unique = true)])
internal data class PaymentCorrectionRow(
    @PrimaryKey val targetPaymentId: String,
    val operationId: String,
    val replacementId: String?,
    val reason: String,
    val recordedAt: Long
)

internal data class PaymentHistoryRow(
    @Embedded val payment: PaymentRow,
    val correctionId: String?,
    val correctionReason: String?,
    val replacementId: String?
)

internal val PAYMENT_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `payment_correction` (`targetPaymentId` TEXT NOT NULL, `operationId` TEXT NOT NULL, `replacementId` TEXT, `reason` TEXT NOT NULL, `recordedAt` INTEGER NOT NULL, PRIMARY KEY(`targetPaymentId`), FOREIGN KEY(`targetPaymentId`) REFERENCES `payment`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`replacementId`) REFERENCES `payment`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_payment_correction_operationId` ON `payment_correction` (`operationId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_payment_correction_replacementId` ON `payment_correction` (`replacementId`)")
    }
}
