package io.github.ketarro.credivaro.ui

import io.github.ketarro.credivaro.model.Money
import io.github.ketarro.credivaro.model.Obligation
import java.time.LocalDate

data class PreviewSummary(val totals: Map<String, Money?>, val unknown: Int)

fun summarizePreview(records: List<Obligation>): PreviewSummary {
    val known = records.mapNotNull { it.balance?.totalDebt }
    val totals = known.groupBy { it.currency }.toSortedMap().mapValues { (code, values) ->
        try { values.fold(Money(code, 0)) { a, b -> a + b } }
        catch (_: ArithmeticException) { null }
    }
    return PreviewSummary(totals, records.size - known.size)
}

fun parsePreviewDate(raw: String): LocalDate? {
    if (raw.isBlank()) return null
    require(raw.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")))
    return LocalDate.parse(raw).also { require(it.year in 1900..9999) }
}
