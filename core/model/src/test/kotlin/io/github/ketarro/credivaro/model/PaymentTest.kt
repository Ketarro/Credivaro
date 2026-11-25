package io.github.ketarro.credivaro.model

import java.time.LocalDate
import java.util.Currency
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentTest {
    private val obligation = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private fun rub(n: Long) = Money("RUB", n)
    private fun payment(amount: Money = rub(100), allocation: PaymentAllocation = PaymentAllocation.unknown(amount)) =
        RecordedPayment(UUID.randomUUID(), UUID.randomUUID(), obligation, LocalDate.of(2026, 1, 1), amount, allocation)

    @Test fun unknownAllocationDoesNotReducePrincipal() {
        val p = payment()
        assertEquals(rub(0), p.allocation.principal)
        assertEquals(rub(100), p.allocation.unallocated)
    }
    @Test fun exactSplit() {
        val a = PaymentAllocation(rub(70), rub(20), rub(5), rub(5))
        assertEquals(rub(100), payment(allocation = a).allocation.total)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsZeroPayment() { payment(rub(0)) }
    @Test(expected = IllegalArgumentException::class) fun rejectsNegativePayment() { payment(rub(-1)) }
    @Test(expected = IllegalArgumentException::class) fun rejectsNegativeAllocation() {
        PaymentAllocation(rub(-1), rub(0), rub(0), rub(101))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsMixedAllocationCurrency() {
        PaymentAllocation(rub(70), Money("USD", 20), rub(5), rub(5))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsAllocationMismatch() {
        payment(allocation = PaymentAllocation(rub(90), rub(0), rub(0), rub(0)))
    }
    @Test(expected = ArithmeticException::class) fun rejectsAllocationOverflow() {
        payment(rub(Long.MAX_VALUE), PaymentAllocation(rub(Long.MAX_VALUE), rub(1), rub(0), rub(0)))
    }
    @Test fun matchesObligationAndCurrency() { payment().validateFor(obligation, Currency.getInstance("RUB")) }
    @Test(expected = IllegalArgumentException::class) fun rejectsForeignCurrency() {
        payment().validateFor(obligation, Currency.getInstance("USD"))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsWrongObligation() {
        payment().validateFor(UUID(0, 2), Currency.getInstance("RUB"))
    }
}
