package io.github.ketarro.credivaro.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test fun addsExactMinorUnits() {
        assertEquals(Money("RUB", 1200), Money("RUB", 1000) + Money("RUB", 200))
    }

    @Test fun subtractsExactMinorUnits() {
        assertEquals(Money("RUB", 600), Money("RUB", 1000) - Money("RUB", 400))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMixedCurrencies() { Money("RUB", 100) + Money("USD", 100) }

    @Test(expected = ArithmeticException::class)
    fun rejectsOverflow() { Money("RUB", Long.MAX_VALUE) + Money("RUB", 1) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMalformedCurrency() { Money("rub", 1) }
}
