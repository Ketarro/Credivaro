package io.github.ketarro.credivaro.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Isolated compiler/fixture target. Never use with the application database name. */
@Database(entities = [ObligationRow::class, TermsRow::class, SnapshotRow::class,
    PaymentRow::class, PaymentCorrectionRow::class, EventSeriesRow::class,
    EventRuleRevisionRow::class, EventOccurrenceRow::class, EventOverrideRow::class,
    EventPaymentLinkRow::class], version = 4, exportSchema = true)
internal abstract class EventMigrationCandidateDatabase : RoomDatabase() {
    abstract fun dao(): StorageDao
    abstract fun paymentDao(): PaymentDao
}

/** Additive DDL matching Room index metadata. CHECK constraints require repository validation
 * for fresh Room-created tables; the candidate migration also retains SQL CHECK constraints.
 */
internal val EVENT_MIGRATION_CANDIDATE_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        EventStorageSchemaCandidate.create(db)
    }
}
