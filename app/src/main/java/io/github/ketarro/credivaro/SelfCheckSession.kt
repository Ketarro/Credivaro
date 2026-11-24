package io.github.ketarro.credivaro

import android.content.Context
import android.os.Build
import android.os.SystemClock
import io.github.ketarro.credivaro.database.StorageSelfCheck
import io.github.ketarro.credivaro.model.*
import io.github.ketarro.credivaro.ui.parsePreviewDate
import io.github.ketarro.credivaro.ui.parsePreviewMoney
import io.github.ketarro.credivaro.ui.summarizePreview
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Currency
import java.util.UUID

internal data class CheckResult(val id: String, val passed: Boolean, val ms: Long, val errorType: String? = null)
internal data class CheckReport(val running: Boolean = false, val results: List<CheckResult> = emptyList(),
    val aborted: Boolean = false) {
    val complete: Boolean get() = !running && !aborted && results.size == SelfCheckSession.ids.size
}

internal object SelfCheckSession {
    val ids = listOf("FIN01", "FIN02", "FIN03", "FIN04") + StorageSelfCheck.ids
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutable = MutableStateFlow(CheckReport())
    val state = mutable.asStateFlow()

    @Synchronized
    fun start(context: Context) {
        if (!BuildConfig.DEBUG || mutable.value.running) return
        mutable.value = CheckReport(running = true)
        val app = context.applicationContext
        scope.launch {
            val results = mutableListOf<CheckResult>()
            fun publish(result: CheckResult) {
                results.add(result)
                mutable.value = CheckReport(running = true, results = results.toList())
            }
            var aborted = false
            try {
                val usd = Currency.getInstance("USD")
                fun rejected(block: () -> Unit): Boolean =
                    try { block(); false } catch (_: IllegalArgumentException) { true }
                fun checkCase(id: String, block: () -> Unit) {
                    val start = SystemClock.elapsedRealtime()
                    val failure = try { block(); null } catch (e: Exception) { e.javaClass.simpleName }
                    publish(CheckResult(id, failure == null, SystemClock.elapsedRealtime() - start, failure))
                }
                checkCase("FIN01") {
                    check(parsePreviewMoney("12.34", usd) == Money("USD", 1234))
                    check(parsePreviewMoney("12,34", usd) == Money("USD", 1234))
                    check(rejected { parsePreviewMoney("-100", usd) })
                    check(rejected { parsePreviewMoney("abc", usd) })
                    check(rejected { parsePreviewMoney("12.345", usd) })
                }
                checkCase("FIN02") {
                    check(parsePreviewMoney("", usd) == null)
                    check(parsePreviewMoney("0", usd) == Money("USD", 0))
                }
                checkCase("FIN03") {
                    check(parsePreviewDate("2026-02-28") == LocalDate.of(2026, 2, 28))
                    check(parsePreviewDate("") == null)
                    var invalid = false
                    try { parsePreviewDate("2026-02-30") } catch (_: java.time.DateTimeException) { invalid = true }
                    check(invalid)
                }
                checkCase("FIN04") {
                    fun record(code: String, amount: Long?) = Obligation(UUID.randomUUID(), "Synthetic",
                        currency = Currency.getInstance(code), bankZone = ZoneOffset.UTC,
                        terms = ObligationTerms.Loan(), balance = amount?.let {
                            BalanceSnapshot(LocalDate.of(2026, 1, 1), Money(code, it))
                        })
                    val summary = summarizePreview(listOf(record("RUB", 1000), record("RUB", 2000),
                        record("USD", 20), record("USD", 0), record("RUB", null)))
                    check(summary.totals == mapOf("RUB" to Money("RUB", 3000), "USD" to Money("USD", 20)))
                    check(summary.unknown == 1)
                }
                StorageSelfCheck.run(app) { publish(CheckResult(it.id, it.passed, it.elapsedMs, it.errorType)) }
            } catch (e: CancellationException) {
                aborted = true
                throw e
            } catch (_: Exception) {
                aborted = true
            } catch (_: LinkageError) {
                aborted = true
            } finally {
                mutable.value = CheckReport(results = results.toList(), aborted = aborted || results.size != ids.size)
            }
        }
    }

    fun reportText(report: CheckReport): String = buildString {
        appendLine("Credivaro ${BuildConfig.VERSION_NAME} build ${BuildConfig.VERSION_CODE}")
        appendLine("Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
        appendLine("Self-check: ${report.results.count { it.passed }}/${ids.size} PASS; complete=${report.complete}")
        report.results.forEach {
            appendLine("${it.id} ${if (it.passed) "PASS" else "FAIL"} ${it.ms} ms" +
                (it.errorType?.let { type -> " ($type)" } ?: ""))
        }
        appendLine("Synthetic isolated data only. UI/reboot/update manual checks not covered.")
    }
}
