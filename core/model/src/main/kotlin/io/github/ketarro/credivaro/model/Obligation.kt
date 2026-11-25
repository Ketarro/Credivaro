package io.github.ketarro.credivaro.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency
import java.util.UUID

enum class ObligationStatus { ACTIVE, CLOSED, ARCHIVED }
enum class ObligationType { LOAN, CREDIT_CARD, INSTALLMENT, OTHER }
enum class PaymentScheme { ANNUITY, DIFFERENTIATED, CUSTOM }
enum class MissingDayPolicy { CLAMP, SKIP }

/** Monthly recurrence with explicit policy for shorter months. */
data class MonthlyDay(val day: Int, val missingDayPolicy: MissingDayPolicy) {
    init { require(day in 1..31) }
}

/** Percent, not a fractional multiplier. Unknown rates are represented by null. */
data class AnnualRate(val percent: BigDecimal) {
    init { require(percent.signum() >= 0) }
}

/** Terms are manual facts or inputs, not inferred bank policies. */
sealed interface ObligationTerms {
    val type: ObligationType

    data class Loan(
        val originalPrincipal: Money? = null,
        val annualRate: AnnualRate? = null,
        val termMonths: Int? = null,
        val paymentScheme: PaymentScheme? = null,
        val scheduledPayment: Money? = null,
        val firstDueDate: LocalDate? = null,
        val paymentDay: MonthlyDay? = null
    ) : ObligationTerms {
        override val type = ObligationType.LOAN
    }

    data class CreditCard(
        val creditLimit: Money? = null,
        val annualRate: AnnualRate? = null,
        val statementDay: MonthlyDay? = null,
        val minimumPayment: Money? = null,
        val minimumPaymentDueDate: LocalDate? = null,
        val cashFeeFreeLimit: Money? = null,
        val cashResetDay: MonthlyDay? = null,
        val graceGroups: List<GraceGroup> = emptyList()
    ) : ObligationTerms {
        override val type = ObligationType.CREDIT_CARD
    }

    data class Installment(
        val originalAmount: Money? = null,
        val downPayment: Money? = null,
        val installmentCount: Int? = null,
        val installmentAmount: Money? = null,
        val firstDueDate: LocalDate? = null,
        val annualRate: AnnualRate? = null,
        val fees: Money? = null
    ) : ObligationTerms {
        override val type = ObligationType.INSTALLMENT
    }

    data class Other(val description: String? = null) : ObligationTerms {
        override val type = ObligationType.OTHER
    }
}

/** Each statement/category may have a distinct grace deadline. */
data class GraceGroup(
    val id: UUID,
    val category: String,
    val statementDate: LocalDate? = null,
    val deadline: LocalDate? = null,
    val amountToPreserveGrace: Money? = null
)

/** A user-entered bank reconciliation, not a live bank integration. */
data class BalanceSnapshot(
    val bankAsOf: LocalDate,
    val totalDebt: Money? = null,
    val principal: Money? = null,
    val availableCredit: Money? = null
)

data class Obligation(
    val id: UUID,
    val title: String,
    val lender: String? = null,
    val currency: Currency,
    val bankZone: ZoneId,
    val openedOn: LocalDate? = null,
    val status: ObligationStatus = ObligationStatus.ACTIVE,
    val terms: ObligationTerms,
    val balance: BalanceSnapshot? = null,
    val notes: String? = null
) {
    val type: ObligationType get() = terms.type
}
