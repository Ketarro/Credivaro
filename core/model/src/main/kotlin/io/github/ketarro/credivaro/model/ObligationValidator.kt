package io.github.ketarro.credivaro.model

import java.time.LocalDate

enum class ValidationCode {
    REQUIRED, CURRENCY_MISMATCH, NEGATIVE_AMOUNT, NON_POSITIVE_COUNT,
    INVALID_DATE_ORDER, FUTURE_SNAPSHOT, DUPLICATE_ID, DOWN_PAYMENT_EXCEEDS_AMOUNT,
    UNSUPPORTED_CURRENCY, NO_KNOWN_BALANCE
}

data class ValidationIssue(val field: String, val code: ValidationCode)

/** Pure validation; UI maps stable field/code pairs to localized messages. */
object ObligationValidator {
    fun validate(value: Obligation, bankToday: LocalDate): List<ValidationIssue> = buildList {
        fun issue(field: String, code: ValidationCode) { add(ValidationIssue(field, code)) }
        fun amount(field: String, money: Money?) {
            if (money == null) return
            if (money.currency != value.currency.currencyCode) issue(field, ValidationCode.CURRENCY_MISMATCH)
            if (money.minorUnits < 0) issue(field, ValidationCode.NEGATIVE_AMOUNT)
        }
        fun count(field: String, n: Int?) {
            if (n != null && n <= 0) issue(field, ValidationCode.NON_POSITIVE_COUNT)
        }
        fun ordered(field: String, start: LocalDate?, end: LocalDate?) {
            if (start != null && end != null && end.isBefore(start)) issue(field, ValidationCode.INVALID_DATE_ORDER)
        }

        fun schedule(frequency: PaymentFrequency?, anchor: LocalDate?, monthly: MonthlyDay?) {
            // Legacy v1 payloads may have only a monthly rule; preserve without inventing dates.
            if (frequency == null) return
            if (anchor == null) issue("terms.firstDueDate", ValidationCode.REQUIRED)
            if (frequency == PaymentFrequency.MONTHLY && monthly == null) issue("terms.paymentDay", ValidationCode.REQUIRED)
            if (frequency != PaymentFrequency.MONTHLY && monthly != null) issue("terms.paymentDay", ValidationCode.INVALID_DATE_ORDER)
        }
        if (value.title.isBlank()) issue("title", ValidationCode.REQUIRED)
        if (value.currency.defaultFractionDigits !in 0..3) issue("currency", ValidationCode.UNSUPPORTED_CURRENCY)
        value.balance?.let {
            amount("balance.totalDebt", it.totalDebt)
            amount("balance.principal", it.principal)
            amount("balance.availableCredit", it.availableCredit)
            ordered("balance.bankAsOf", value.openedOn, it.bankAsOf)
            if (it.bankAsOf.isAfter(bankToday)) issue("balance.bankAsOf", ValidationCode.FUTURE_SNAPSHOT)
            if (it.totalDebt == null && it.principal == null && it.availableCredit == null)
                issue("balance", ValidationCode.NO_KNOWN_BALANCE)
        }
        when (val t = value.terms) {
            is ObligationTerms.Loan -> {
                amount("terms.originalPrincipal", t.originalPrincipal)
                amount("terms.scheduledPayment", t.scheduledPayment)
                count("terms.termMonths", t.termMonths)
                ordered("terms.firstDueDate", value.openedOn, t.firstDueDate)
                schedule(t.paymentFrequency, t.firstDueDate, t.paymentDay)
            }
            is ObligationTerms.CreditCard -> {
                amount("terms.creditLimit", t.creditLimit)
                amount("terms.minimumPayment", t.minimumPayment)
                amount("terms.cashFeeFreeLimit", t.cashFeeFreeLimit)
                ordered("terms.minimumPaymentDueDate", value.openedOn, t.minimumPaymentDueDate)
                val ids = mutableSetOf<java.util.UUID>()
                t.graceGroups.forEachIndexed { index, g ->
                    val path = "terms.graceGroups[$index]"
                    if (!ids.add(g.id)) issue("$path.id", ValidationCode.DUPLICATE_ID)
                    if (g.category.isBlank()) issue("$path.category", ValidationCode.REQUIRED)
                    amount("$path.amountToPreserveGrace", g.amountToPreserveGrace)
                    ordered("$path.deadline", g.statementDate, g.deadline)
                }
            }
            is ObligationTerms.Installment -> {
                amount("terms.originalAmount", t.originalAmount)
                amount("terms.downPayment", t.downPayment)
                amount("terms.installmentAmount", t.installmentAmount)
                amount("terms.fees", t.fees)
                count("terms.installmentCount", t.installmentCount)
                ordered("terms.firstDueDate", value.openedOn, t.firstDueDate)
                schedule(t.paymentFrequency, t.firstDueDate, t.paymentDay)
                val original = t.originalAmount
                val down = t.downPayment
                if (original != null && down != null && original.currency == down.currency &&
                    down.minorUnits > original.minorUnits) issue("terms.downPayment", ValidationCode.DOWN_PAYMENT_EXCEEDS_AMOUNT)
            }
            is ObligationTerms.Other -> Unit
        }
    }
}
