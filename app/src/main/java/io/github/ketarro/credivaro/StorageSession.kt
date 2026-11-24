package io.github.ketarro.credivaro

import android.content.Context
import io.github.ketarro.credivaro.database.EncryptedStorage
import io.github.ketarro.credivaro.database.ObligationRepository
import io.github.ketarro.credivaro.model.Obligation
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Single process owner. Keep the connection across activity recreation and locale/theme changes. */
internal object StorageSession {
    private val lock = Mutex()
    private var storage: EncryptedStorage? = null

    suspend fun repository(context: Context): ObligationRepository = lock.withLock {
        // Complete ownership transfer even if an activity is recreated during opening.
        withContext(NonCancellable) {
            (storage ?: EncryptedStorage.open(context.applicationContext).also { storage = it }).repository
        }
    }
}

internal sealed interface StorageState {
    data object Loading : StorageState
    data object Failed : StorageState
    data class Ready(val repository: ObligationRepository, val records: List<Obligation>) : StorageState
}
