package io.github.ketarro.credivaro.database

import android.content.Context
import android.os.SystemClock
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.DatabaseErrorHandler
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.time.Clock

/** Explicit handler: unlike the default SQLCipher helper, never delete on corruption. */
internal class PreserveFileFactory(private val password: ByteArray) : SupportSQLiteOpenHelper.Factory {
    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val callback = configuration.callback
        return object : SQLiteOpenHelper(configuration.context, configuration.name, password, null,
            callback.version, 0, DatabaseErrorHandler { _, error -> throw error }, null, false) {
            override fun onConfigure(db: SQLiteDatabase) = callback.onConfigure(db)
            override fun onCreate(db: SQLiteDatabase) = callback.onCreate(db)
            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) =
                callback.onUpgrade(db, oldVersion, newVersion)
            override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) =
                callback.onDowngrade(db, oldVersion, newVersion)
            override fun onOpen(db: SQLiteDatabase) = callback.onOpen(db)
        }
    }
}

/** Prototype; do not wire into the production app until security gates pass.
 * One owner per database, no concurrent close/use; close on a background thread.
 */
class EncryptedStorage private constructor(
    private val db: StorageDatabase, private val password: ByteArray, clock: Clock
) : Closeable {
    val repository = ObligationRepository(db, clock)
    val payments = PaymentRepository(db, clock)
    override fun close() {
        try { db.close() } finally { password.fill(0) }
    }

    companion object {
        internal fun build(context: Context, name: String, password: ByteArray): StorageDatabase {
            System.loadLibrary("sqlcipher")
            return buildRoom(context, name, password)
        }

        private fun buildRoom(context: Context, name: String, password: ByteArray): StorageDatabase =
            Room.databaseBuilder(context, StorageDatabase::class.java, name)
                .openHelperFactory(PreserveFileFactory(password))
                .addMigrations(PAYMENT_MIGRATION_1_2)
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .build()

        private inline fun <T> timed(stage: String, onStage: (String, Long) -> Unit, block: () -> T): T {
            val start = SystemClock.elapsedRealtime()
            try { return block() } finally { onStage(stage, SystemClock.elapsedRealtime() - start) }
        }

        suspend fun open(context: Context, name: String = "credivaro.db", clock: Clock = Clock.systemUTC(),
            onStage: (String, Long) -> Unit = { _, _ -> }): EncryptedStorage =
            withContext(Dispatchers.IO) {
                val app = context.applicationContext
                val password = timed("storage.keystoreEnvelope", onStage) { DatabaseKey.loadOrCreate(app, name) }
                var db: StorageDatabase? = null
                try {
                    timed("storage.nativeLoad", onStage) { System.loadLibrary("sqlcipher") }
                    val opened = timed("storage.roomBuilder", onStage) { buildRoom(app, name, password) }
                    db = opened
                    timed("storage.databaseOpen", onStage) { opened.openHelper.writableDatabase }
                    EncryptedStorage(opened, password, clock)
                } catch (e: Throwable) {
                    try { db?.close() } finally { password.fill(0) }
                    throw e
                }
            }
    }
}
