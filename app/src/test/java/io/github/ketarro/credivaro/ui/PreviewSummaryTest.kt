package io.github.ketarro.credivaro.ui

import io.github.ketarro.credivaro.model.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class PreviewSummaryTest {
    private fun record(code: String, amount: Long?) = Obligation(UUID.randomUUID(), "Test",
        currency = Currency.getInstance(code), bankZone = ZoneId.of("UTC"), terms = ObligationTerms.Loan(),
        balance = amount?.let { BalanceSnapshot(LocalDate.of(2026, 1, 1), Money(code, it)) })

    @Test fun groupsCurrenciesAndCountsUnknown() {
        val result = summarizePreview(listOf(record("RUB", 108000), record("USD", 2000), record("RUB", 220200), record("EUR", null)))
        assertEquals(Money("RUB", 328200), result.totals["RUB"])
        assertEquals(Money("USD", 2000), result.totals["USD"])
        assertFalse(result.totals.containsKey("EUR"))
        assertEquals(1, result.unknown)
    }
    @Test fun zeroIsKnown() {
        val result = summarizePreview(listOf(record("RUB", 0)))
        assertEquals(Money("RUB", 0), result.totals["RUB"])
        assertEquals(0, result.unknown)
    }
    @Test fun overflowDoesNotEraseOtherCurrencies() {
        val result = summarizePreview(listOf(record("RUB", Long.MAX_VALUE), record("RUB", 1), record("USD", 1)))
        assertTrue(result.totals.containsKey("RUB"))
        assertNull(result.totals["RUB"])
        assertEquals(Money("USD", 1), result.totals["USD"])
    }
    @Test fun parsesOptionalDate() {
        assertNull(parsePreviewDate(""))
        assertEquals(LocalDate.of(2028, 2, 29), parsePreviewDate("2028-02-29"))
    }
    @Test(expected = java.time.DateTimeException::class)
    fun rejectsImpossibleDate() { parsePreviewDate("2027-02-29") }
}
