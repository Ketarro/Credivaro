package io.github.ketarro.credivaro.model

import java.time.LocalDate
import java.util.Currency
import java.util.UUID

/** Unallocated funds are explicitly unknown, not automatically principal. */
data class PaymentAllocation(
    val principal: Money,
    val interest: Money,
    val fees: Money,
    val unallocated: Money
) {
    init {
        require(listOf(principal, interest, fees, unallocated).all {
            it.currency == principal.currency && it.minorUnits >= 0
        }) { "Allocation currency or amount invalid" }
    }
    val total: Money get() = principal + interest + fees + unallocated

    companion object {
        fun unknown(amount: Money): PaymentAllocation {
            require(amount.minorUnits > 0)
            val zero = Money(amount.currency, 0)
            return PaymentAllocation(zero, zero, zero, amount)
        }
    }
}

/** A recorded fact, not a bank transfer or a mutation of a bank snapshot. */
data class RecordedPayment(
    val id: UUID,
    val operationId: UUID,
    val obligationId: UUID,
    val bankDate: LocalDate,
    val amount: Money,
    val allocation: PaymentAllocation = PaymentAllocation.unknown(amount),
    val notes: String? = null
) {
    init {
        require(amount.minorUnits > 0) { "Payment must be positive" }
        require(allocation.total == amount) { "Allocation must equal payment" }
    }

    /** Persistence must additionally enforce IDs, uniqueness and obligation existence. */
    fun validateFor(obligationId: UUID, currency: Currency) {
        require(this.obligationId == obligationId) { "Obligation mismatch" }
        require(amount.currency == currency.currencyCode) { "Currency mismatch" }
    }
}
