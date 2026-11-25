package io.github.ketarro.credivaro

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-local debug diagnostics; fixed stage names and durations only, no financial data. */
internal object StartupDiagnostics {
    private val start = SystemClock.elapsedRealtime()
    private val mutable = MutableStateFlow<List<String>>(emptyList())
    val events = mutable.asStateFlow()

    @Synchronized
    fun mark(stage: String, durationMs: Long? = null) {
        if (!BuildConfig.DEBUG) return
        val line = "+${SystemClock.elapsedRealtime() - start} ms  $stage" +
            (durationMs?.let { "  ($it ms)" } ?: "")
        mutable.value = (mutable.value + line).takeLast(100)
        Log.d("CredivaroStartup", line)
    }
}
