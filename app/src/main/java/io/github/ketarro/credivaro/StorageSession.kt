package io.github.ketarro.credivaro

import android.content.Context
import android.os.SystemClock
import io.github.ketarro.credivaro.database.EncryptedStorage
import io.github.ketarro.credivaro.database.ObligationRepository
import io.github.ketarro.credivaro.model.Obligation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect

/** One process owner of the connection AND its observable state. No activity-owned re-open. */
internal object StorageSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutable = MutableStateFlow<StorageState>(StorageState.Loading)
    val state = mutable.asStateFlow()
    private var storage: EncryptedStorage? = null
    private var job: Job? = null

    @Synchronized
    fun start(context: Context) {
        if (job?.isActive == true) return
        if (mutable.value is StorageState.Ready) return
        val app = context.applicationContext
        mutable.value = StorageState.Loading
        job = scope.launch {
            val started = SystemClock.elapsedRealtime()
            StartupDiagnostics.mark("storage.start")
            try {
                val repository = (storage ?: EncryptedStorage.open(app, onStage = { stage, ms ->
                    StartupDiagnostics.mark(stage, ms)
                }).also { storage = it }).repository
                val queryStart = SystemClock.elapsedRealtime()
                var first = true
                repository.observeAll().collect {
                    mutable.value = StorageState.Ready(repository, it)
                    if (first) {
                        first = false
                        StartupDiagnostics.mark("storage.firstQuery", SystemClock.elapsedRealtime() - queryStart)
                        StartupDiagnostics.mark("storage.ready", SystemClock.elapsedRealtime() - started)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                StartupDiagnostics.mark("storage.failed", SystemClock.elapsedRealtime() - started)
                mutable.value = StorageState.Failed
            } catch (_: LinkageError) {
                StartupDiagnostics.mark("storage.failed", SystemClock.elapsedRealtime() - started)
                mutable.value = StorageState.Failed
            }
        }
    }
}

internal sealed interface StorageState {
    data object Loading : StorageState
    data object Failed : StorageState
    data class Ready(val repository: ObligationRepository, val records: List<Obligation>) : StorageState
}
