package io.github.ketarro.credivaro.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ketarro.credivaro.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EventCodecTest {
    @Test fun allRecurrencesAndExactAmountsRoundTrip() {
        val d = LocalDate.of(2028, 2, 29)
        val recurrences = listOf(Recurrence.OneOff(d), Recurrence.Monthly(MonthlyDay(31, MissingDayPolicy.CLAMP), d),
            Recurrence.Monthly(MonthlyDay(31, MissingDayPolicy.SKIP)), Recurrence.LastDayOfMonth,
            Recurrence.Weekly(d, 2, setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY)),
            Recurrence.EveryNDays(d, 3), Recurrence.ExplicitDates(listOf(ExplicitDate(UUID.randomUUID(), d))))
        for (r in recurrences) for (amount in listOf(null, Money("RUB", 0), Money("RUB", Long.MAX_VALUE))) {
            val rule = EventRule(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), EventType.REQUIRED_PAYMENT,
                MonetaryRole.MANDATORY, r, ZoneId.of("Europe/Moscow"), d, d.plusMonths(3), "RUB", amount)
            assertEquals(rule, EventCodec.decodeRule(EventCodec.encodeRule(rule)))
            val window = EventWindow(d, d.plusMonths(3))
            FinancialEventEngine.generate(listOf(rule), emptyList(), window).events.forEach {
                assertEquals(it, EventCodec.decodeEvent(EventCodec.encodeEvent(it)))
            }
        }
    }
    @Test fun rejectsUnknownVersionAndNumericAmount() {
        val d = LocalDate.of(2028, 2, 29)
        val r = EventRule(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), EventType.REQUIRED_PAYMENT,
            MonetaryRole.MANDATORY, Recurrence.OneOff(d), ZoneOffset.UTC, d, currency = "RUB", amount = Money("RUB", 100))
        val j = org.json.JSONObject(EventCodec.encodeRule(r))
        assertTrue(runCatching { EventCodec.decodeRule(j.put("v", 2).toString()) }.isFailure)
        assertTrue(runCatching { EventCodec.decodeRule(j.put("v", 1).put("amount", 100).toString()) }.isFailure)
    }
}
