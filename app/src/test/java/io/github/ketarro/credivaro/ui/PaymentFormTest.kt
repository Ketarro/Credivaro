package io.github.ketarro.credivaro.ui

import io.github.ketarro.credivaro.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Currency
import java.util.UUID

class PaymentFormTest {
    private val today = LocalDate.of(2026, 1, 1)
    private val owner = Obligation(UUID.randomUUID(), "Synthetic", currency = Currency.getInstance("RUB"),
        bankZone = ZoneOffset.UTC, openedOn = today.minusDays(2), terms = ObligationTerms.Loan())
    private fun make(amount: String = "10,25", principal: String = "", interest: String = "",
        fees: String = "", date: String = today.toString(), obligation: Obligation = owner) =
        buildPayment(obligation, UUID.randomUUID(), UUID.randomUUID(), date, amount, principal, interest, fees, " test ", today)
    @Test fun unknownAllocation() { assertEquals(Money("RUB", 1025), make().allocation.unallocated) }
    @Test fun partialAllocation() {
        val p = make(principal = "2", interest = "1", fees = "0.25")
        assertEquals(Money("RUB", 700), p.allocation.unallocated)
        assertEquals("test", p.notes)
    }
    @Test fun fullAllocation() { assertEquals(Money("RUB", 0), make(principal = "10.25").allocation.unallocated) }
    @Test fun overpaymentAllowedWithoutBalanceMutation() {
        val o = owner.copy(balance = BalanceSnapshot(today, Money("RUB", 1)))
        assertEquals(Money("RUB", 1025), make(obligation = o).amount)
        assertEquals(Money("RUB", 1), o.balance?.totalDebt)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsZero() { make(amount = "0") }
    @Test(expected = IllegalArgumentException::class) fun rejectsOverAllocation() { make(principal = "11") }
    @Test(expected = IllegalArgumentException::class) fun rejectsFuture() { make(date = today.plusDays(1).toString()) }
    @Test(expected = IllegalArgumentException::class) fun rejectsBeforeOpening() { make(date = today.minusDays(3).toString()) }
    @Test(expected = IllegalArgumentException::class) fun rejectsPrecision() { make(amount = "1.001") }
    @Test(expected = ArithmeticException::class) fun rejectsOverflow() { make(amount = "999999999999999999999999") }
}
