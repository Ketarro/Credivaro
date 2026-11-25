package io.github.ketarro.credivaro.ui

import io.github.ketarro.credivaro.model.Money
import java.math.BigDecimal
import java.util.Currency

/** Preview input accepts a decimal separator but no grouping or implicit rounding. */
fun parsePreviewMoney(raw: String, currency: Currency): Money? {
    if (raw.isBlank()) return null
    val value = raw.trim().replace(',', '.')
    require(value.matches(Regex("[0-9]+(\\.[0-9]+)?")))
    val decimal = BigDecimal(value)
    require(decimal.scale() <= currency.defaultFractionDigits)
    return Money(currency.currencyCode, decimal.movePointRight(currency.defaultFractionDigits).longValueExact())
}

fun parsePreviewRate(raw: String): BigDecimal? {
    if (raw.isBlank()) return null
    val value = raw.trim().replace(',', '.')
    require(value.length <= 32 && value.matches(Regex("[0-9]+(\\.[0-9]+)?")))
    return BigDecimal(value)
}
