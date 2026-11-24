package io.github.ketarro.credivaro.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "obligation")
internal data class ObligationRow(
    @PrimaryKey val id: String,
    val title: String, val lender: String?, val currency: String, val scale: Int,
    val bankZone: String, val openedOn: String?, val status: String, val notes: String?,
    val currentTermsRevision: Long, val currentSnapshotRevision: Long?
)

@Entity(tableName = "terms_revision", primaryKeys = ["obligationId", "revision"],
    foreignKeys = [ForeignKey(entity = ObligationRow::class, parentColumns = ["id"],
        childColumns = ["obligationId"], onDelete = ForeignKey.CASCADE)])
internal data class TermsRow(
    val obligationId: String, val revision: Long, val recordedAt: Long,
    val type: String, val payload: String
)

@Entity(tableName = "balance_snapshot", primaryKeys = ["obligationId", "revision"],
    foreignKeys = [ForeignKey(entity = ObligationRow::class, parentColumns = ["id"],
        childColumns = ["obligationId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["obligationId", "bankAsOf"])])
internal data class SnapshotRow(
    val obligationId: String, val revision: Long, val recordedAt: Long,
    val bankAsOf: String, val totalDebt: Long?, val principal: Long?, val availableCredit: Long?
)

internal data class StoredRecord(
    @Embedded val row: ObligationRow,
    val termsType: String?, val termsPayload: String?,
    val snapshotDate: String?, val totalDebt: Long?, val principal: Long?, val availableCredit: Long?
)

@Dao
internal interface StorageDao {
    @Query("""SELECT o.*, t.type AS termsType, t.payload AS termsPayload,
        s.bankAsOf AS snapshotDate, s.totalDebt, s.principal, s.availableCredit
        FROM obligation o
        LEFT JOIN terms_revision t ON t.obligationId=o.id AND t.revision=o.currentTermsRevision
        LEFT JOIN balance_snapshot s ON s.obligationId=o.id AND s.revision=o.currentSnapshotRevision
        ORDER BY o.title, o.id""")
    fun observe(): Flow<List<StoredRecord>>

    @Query("""SELECT o.*, t.type AS termsType, t.payload AS termsPayload,
        s.bankAsOf AS snapshotDate, s.totalDebt, s.principal, s.availableCredit
        FROM obligation o
        LEFT JOIN terms_revision t ON t.obligationId=o.id AND t.revision=o.currentTermsRevision
        LEFT JOIN balance_snapshot s ON s.obligationId=o.id AND s.revision=o.currentSnapshotRevision
        WHERE o.id=:id""")
    suspend fun find(id: String): StoredRecord?

    @Insert suspend fun insert(row: ObligationRow)
    @Update suspend fun update(row: ObligationRow): Int
    @Insert suspend fun insertTerms(row: TermsRow)
    @Insert suspend fun insertSnapshot(row: SnapshotRow)
    @Query("SELECT MAX(revision) FROM balance_snapshot WHERE obligationId=:id")
    suspend fun lastSnapshotRevision(id: String): Long?
    @Query("SELECT COUNT(*) FROM terms_revision WHERE obligationId=:id")
    suspend fun termsCount(id: String): Int
    @Query("SELECT COUNT(*) FROM balance_snapshot WHERE obligationId=:id")
    suspend fun snapshotCount(id: String): Int
    @Query("DELETE FROM obligation WHERE id=:id")
    suspend fun delete(id: String): Int
}

@Database(entities = [ObligationRow::class, TermsRow::class, SnapshotRow::class, PaymentRow::class],
    version = 2, exportSchema = true)
internal abstract class StorageDatabase : RoomDatabase() {
    abstract fun dao(): StorageDao
    abstract fun paymentDao(): PaymentDao
}
