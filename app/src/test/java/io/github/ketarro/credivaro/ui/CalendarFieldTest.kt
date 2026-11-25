package io.github.ketarro.credivaro.ui

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class CalendarFieldTest {
    @Test fun utcRoundTripAndLeapDays() {
        listOf("1900-01-01","1969-12-31","1970-01-01","2024-02-29","2026-03-29","2026-10-25","2200-12-31").forEach {
            val day = LocalDate.parse(it)
            assertEquals(day, pickerDate(pickerMillis(day)))
        }
        assertEquals(LocalDate.of(1969, 12, 31), pickerDate(-1))
    }
    @Test fun actualPaymentBounds() {
        val start = LocalDate.of(2026, 1, 1)
        val today = LocalDate.of(2026, 11, 25)
        assertFalse(dateAllowed(start.minusDays(1), start, today))
        assertTrue(dateAllowed(start, start, today))
        assertTrue(dateAllowed(today, start, today))
        assertFalse(dateAllowed(today.plusDays(1), start, today))
        assertTrue(dateAllowed(today.plusYears(1), null, null))
    }
}
