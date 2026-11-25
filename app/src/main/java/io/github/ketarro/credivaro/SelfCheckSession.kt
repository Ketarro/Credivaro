package io.github.ketarro.credivaro

import android.content.Context
import android.os.Build
import android.os.SystemClock
import io.github.ketarro.credivaro.database.StorageSelfCheck
import io.github.ketarro.credivaro.model.*
import io.github.ketarro.credivaro.ui.buildPayment
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
    val ids = listOf("FIN01", "FIN02", "FIN03", "FIN04", "FIN05", "FIN06", "FIN07", "FIN08", "FIN09", "FIN10") + StorageSelfCheck.ids
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
                val today = LocalDate.of(2026, 1, 1)
                val owner = Obligation(UUID.randomUUID(), "Synthetic", currency = Currency.getInstance("RUB"),
                    bankZone = ZoneOffset.UTC, openedOn = today.minusDays(1), terms = ObligationTerms.Other(),
                    balance = BalanceSnapshot(today, Money("RUB", 100)))
                fun form(amount: String = "10,25", principal: String = "", interest: String = "",
                    fees: String = "", date: String = today.toString()) =
                    buildPayment(owner, UUID.randomUUID(), UUID.randomUUID(), date, amount, principal, interest, fees, " synthetic ", today)
                checkCase("FIN05") {
                    check(parsePreviewMoney("1", Currency.getInstance("JPY")) == Money("JPY", 1))
                    check(rejected { parsePreviewMoney("1.1", Currency.getInstance("JPY")) })
                    check(parsePreviewMoney("1.234", Currency.getInstance("KWD")) == Money("KWD", 1234))
                    check(runCatching { Money("RUB", Long.MAX_VALUE) + Money("RUB", 1) }.exceptionOrNull() is ArithmeticException)
                    check(rejected { Money("RUB", 1) + Money("USD", 1) })
                }
                checkCase("FIN06") {
                    val p = form()
                    check(p.amount == Money("RUB", 1025) && p.allocation.unallocated == p.amount)
                    check(p.allocation.principal == Money("RUB", 0))
                }
                checkCase("FIN07") {
                    val p = form(principal = "2", interest = "1", fees = "0.25")
                    check(p.allocation.unallocated == Money("RUB", 700))
                    check(form(principal = "10.25").allocation.unallocated == Money("RUB", 0))
                    check(p.notes == "synthetic" && owner.balance?.totalDebt == Money("RUB", 100))
                }
                checkCase("FIN08") {
                    check(rejected { form(amount = "0") })
                    check(rejected { form(amount = "-1") })
                    check(rejected { form(principal = "11") })
                    check(rejected { form(amount = "1.001") })
                    check(runCatching { form(amount = "999999999999999999999999") }.exceptionOrNull() is ArithmeticException)
                }
                checkCase("FIN09") {
                    check(rejected { form(date = today.plusDays(1).toString()) })
                    check(rejected { form(date = today.minusDays(2).toString()) })
                    check(runCatching { form(date = "2026-02-30") }.exceptionOrNull() is java.time.DateTimeException)
                }
                checkCase("FIN10") {
                    val random = kotlin.random.Random(42)
                    repeat(1000) {
                        val a = random.nextLong(1, 1000000)
                        val b = random.nextLong(0, a)
                        val c = random.nextLong(0, a - b + 1)
                        val alloc = PaymentAllocation(Money("RUB", b), Money("RUB", c), Money("RUB", 0), Money("RUB", a - b - c))
                        val p = RecordedPayment(UUID.randomUUID(), UUID.randomUUID(), owner.id, today, Money("RUB", a), alloc)
                        check(p.allocation.total == p.amount)
                        check(rejected { p.copy(amount = Money("RUB", a + 1)) })
                    }
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
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}; ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
        appendLine("Page size: " + runCatching { android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE) }.getOrNull() + " bytes")
        appendLine("Self-check: ${report.results.count { it.passed }}/${ids.size} PASS; complete=${report.complete}")
        report.results.forEach {
            appendLine("${it.id} ${if (it.passed) "PASS" else "FAIL"} ${it.ms} ms" +
                (it.errorType?.let { type -> " ($type)" } ?: ""))
        }
        appendLine("Total: ${report.results.sumOf { it.ms }} ms; failed=${report.results.count { !it.passed }}; missing=${ids.filterNot { id -> report.results.any { it.id == id } }.joinToString()}")
        appendLine("Synthetic isolated data only. Not a signed upgrade, actual UI, reboot, disk-full or complete security test.")
    }
}
