package io.github.ketarro.credivaro.ui

import io.github.ketarro.credivaro.model.*
import java.time.LocalDate
import java.util.UUID

internal fun buildPayment(
    obligation: Obligation, id: UUID, operationId: UUID, date: String,
    amount: String, principal: String, interest: String, fees: String, notes: String,
    bankToday: LocalDate
): RecordedPayment {
    val c = obligation.currency
    val day = LocalDate.parse(date.trim())
    require(!day.isAfter(bankToday))
    obligation.openedOn?.let { require(!day.isBefore(it)) }
    val total = requireNotNull(parsePreviewMoney(amount, c))
    val zero = Money(c.currencyCode, 0)
    val p = parsePreviewMoney(principal, c) ?: zero
    val i = parsePreviewMoney(interest, c) ?: zero
    val f = parsePreviewMoney(fees, c) ?: zero
    return RecordedPayment(id, operationId, obligation.id, day, total,
        PaymentAllocation(p, i, f, total - (p + i + f)), notes.trim().takeIf { it.isNotEmpty() })
}
