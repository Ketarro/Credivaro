package io.github.ketarro.credivaro.model

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency
import java.util.UUID

class ObligationValidatorTest {
    private val today = LocalDate.of(2026, 11, 24)
    private fun obligation(terms: ObligationTerms) = Obligation(
        UUID.randomUUID(), "Synthetic obligation", currency = Currency.getInstance("RUB"),
        bankZone = ZoneId.of("Europe/Moscow"), openedOn = today.minusMonths(1), terms = terms
    )
    private fun validate(o: Obligation) = ObligationValidator.validate(o, today)
    private fun rub(n: Long) = Money("RUB", n)

    @Test fun validatesExplicitFrequencyWithoutInventingLegacyDates() {
        assertTrue(validate(obligation(ObligationTerms.Loan(paymentDay = MonthlyDay(31, MissingDayPolicy.CLAMP)))).isEmpty())
        assertTrue(ValidationIssue("terms.firstDueDate", ValidationCode.REQUIRED) in
            validate(obligation(ObligationTerms.Loan(paymentFrequency = PaymentFrequency.WEEKLY))))
        assertTrue(ValidationIssue("terms.paymentDay", ValidationCode.REQUIRED) in
            validate(obligation(ObligationTerms.Installment(firstDueDate = today, paymentFrequency = PaymentFrequency.MONTHLY))))
        assertTrue(validate(obligation(ObligationTerms.Installment(firstDueDate = today,
            paymentFrequency = PaymentFrequency.MONTHLY, paymentDay = MonthlyDay(31, MissingDayPolicy.SKIP)))).isEmpty())
        assertTrue(validate(obligation(ObligationTerms.Loan(firstDueDate = today, paymentFrequency = PaymentFrequency.EVERY_TWO_WEEKS))).isEmpty())
    }
    @Test fun allowsUnknownLoanTerms() {
        assertTrue(validate(obligation(ObligationTerms.Loan())).isEmpty())
    }
    @Test fun identifiesCardType() {
        assertEquals(ObligationType.CREDIT_CARD, obligation(ObligationTerms.CreditCard()).type)
    }
    @Test fun rejectsBlankTitle() {
        assertTrue(ValidationIssue("title", ValidationCode.REQUIRED) in
            validate(obligation(ObligationTerms.Other()).copy(title = " ")))
    }
    @Test fun rejectsMixedCurrency() {
        assertTrue(ValidationIssue("terms.creditLimit", ValidationCode.CURRENCY_MISMATCH) in
            validate(obligation(ObligationTerms.CreditCard(creditLimit = Money("USD", 100)))))
    }
    @Test fun rejectsNegativePayment() {
        assertTrue(ValidationIssue("terms.scheduledPayment", ValidationCode.NEGATIVE_AMOUNT) in
            validate(obligation(ObligationTerms.Loan(scheduledPayment = rub(-1)))))
    }
    @Test fun rejectsNonPositiveTerm() {
        assertTrue(ValidationIssue("terms.termMonths", ValidationCode.NON_POSITIVE_COUNT) in
            validate(obligation(ObligationTerms.Loan(termMonths = 0))))
    }
    @Test fun rejectsFutureSnapshot() {
        val o = obligation(ObligationTerms.Loan()).copy(balance = BalanceSnapshot(today.plusDays(1), rub(100)))
        assertTrue(ValidationIssue("balance.bankAsOf", ValidationCode.FUTURE_SNAPSHOT) in validate(o))
    }
    @Test fun zeroBalanceIsKnown() {
        assertTrue(validate(obligation(ObligationTerms.Loan()).copy(balance = BalanceSnapshot(today, rub(0)))).isEmpty())
    }
    @Test fun rejectsEmptySnapshot() {
        assertTrue(ValidationIssue("balance", ValidationCode.NO_KNOWN_BALANCE) in
            validate(obligation(ObligationTerms.Loan()).copy(balance = BalanceSnapshot(today))))
    }
    @Test fun independentCardAmountsAreAllowed() {
        val t = ObligationTerms.CreditCard(creditLimit = rub(100000), minimumPayment = rub(1000),
            cashFeeFreeLimit = rub(5000), graceGroups = listOf(GraceGroup(UUID.randomUUID(),
                "Purchases", today, today.plusDays(20), rub(10000))))
        assertTrue(validate(obligation(t)).isEmpty())
    }
    @Test fun rejectsReversedGraceDates() {
        val g = GraceGroup(UUID.randomUUID(), "Purchases", today, today.minusDays(1))
        assertTrue(ValidationIssue("terms.graceGroups[0].deadline", ValidationCode.INVALID_DATE_ORDER) in
            validate(obligation(ObligationTerms.CreditCard(graceGroups = listOf(g)))))
    }
    @Test fun rejectsDuplicateGraceGroups() {
        val g = GraceGroup(UUID.randomUUID(), "Purchases")
        assertTrue(ValidationIssue("terms.graceGroups[1].id", ValidationCode.DUPLICATE_ID) in
            validate(obligation(ObligationTerms.CreditCard(graceGroups = listOf(g, g)))))
    }
    @Test fun rejectsExcessDownPayment() {
        assertTrue(ValidationIssue("terms.downPayment", ValidationCode.DOWN_PAYMENT_EXCEEDS_AMOUNT) in
            validate(obligation(ObligationTerms.Installment(originalAmount = rub(100), downPayment = rub(101)))))
    }
    @Test fun installmentRateCanBeUnknown() {
        assertTrue(validate(obligation(ObligationTerms.Installment(installmentCount = 3))).isEmpty())
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidMonthlyDay() { MonthlyDay(32, MissingDayPolicy.CLAMP) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeAnnualRate() { AnnualRate(BigDecimal("-1")) }
}
