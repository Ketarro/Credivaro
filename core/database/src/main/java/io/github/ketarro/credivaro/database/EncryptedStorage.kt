package io.github.ketarro.credivaro.database

import android.content.Context
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
    override fun close() {
        try { db.close() } finally { password.fill(0) }
    }

    companion object {
        internal fun build(context: Context, name: String, password: ByteArray): StorageDatabase {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context, StorageDatabase::class.java, name)
                .openHelperFactory(PreserveFileFactory(password))
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .build()
        }
        suspend fun open(context: Context, name: String = "credivaro.db", clock: Clock = Clock.systemUTC()): EncryptedStorage =
            withContext(Dispatchers.IO) {
                val app = context.applicationContext
                val password = DatabaseKey.loadOrCreate(app, name)
                var db: StorageDatabase? = null
                try {
                    val opened = build(app, name, password)
                    db = opened
                    opened.openHelper.writableDatabase
                    EncryptedStorage(opened, password, clock)
                } catch (e: Throwable) {
                    try { db?.close() } finally { password.fill(0) }
                    throw e
                }
            }
    }
}
