package io.github.ketarro.credivaro.model

import org.junit.Assert.*
import org.junit.Test
import java.time.*
import java.util.UUID

class FinancialEventEngineTest {
    private val series = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val owner = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val revision = UUID.fromString("00000000-0000-0000-0000-000000000003")
    private fun d(s: String) = LocalDate.parse(s)
    private fun rule(r: Recurrence, start: String = "2026-01-01", end: String? = null) =
        EventRule(series, revision, owner, EventType.REQUIRED_PAYMENT, MonetaryRole.MANDATORY,
            r, ZoneOffset.UTC, d(start), end?.let(::d), "RUB", Money("RUB", 100_00))
    private fun window(a: String, b: String) = EventWindow(d(a), d(b))
    private fun generate(r: EventRule, w: EventWindow, o: List<EventOverride> = emptyList()) =
        FinancialEventEngine.generate(listOf(r), o, w)
    private fun dates(r: EventRule, w: EventWindow) = generate(r, w).events.map { it.dueDate.toString() }
    private fun monthly(policy: MissingDayPolicy, day: Int = 31) = Recurrence.Monthly(MonthlyDay(day, policy))
    private fun clock(day: String) = Clock.fixed(d(day).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    @Test fun clampNonLeapAndLeap() {
        val r = rule(monthly(MissingDayPolicy.CLAMP))
        assertEquals(listOf("2027-01-31","2027-02-28","2027-03-31"), dates(r, window("2027-01-01","2027-04-01")))
        assertEquals(listOf("2028-02-29"), dates(r, window("2028-02-01","2028-03-01")))
    }
    @Test fun skipDoesNotDrift() {
        assertEquals(listOf("2027-01-31","2027-03-31"), dates(rule(monthly(MissingDayPolicy.SKIP)), window("2027-01-01","2027-04-01")))
    }
    @Test fun twentyNineAndThirty() {
        assertEquals(listOf("2027-02-28"), dates(rule(monthly(MissingDayPolicy.CLAMP,29)),window("2027-02-01","2027-03-01")))
        assertTrue(dates(rule(monthly(MissingDayPolicy.SKIP,30)),window("2028-02-01","2028-03-01")).isEmpty())
    }
    @Test fun lastDayAndYearChange() {
        assertEquals(listOf("2026-12-31","2027-01-31","2027-02-28"), dates(rule(Recurrence.LastDayOfMonth),window("2026-12-01","2027-03-01")))
    }
    @Test fun oneOffAndHalfOpenWindow() {
        val r = rule(Recurrence.OneOff(d("2026-12-01")))
        assertTrue(dates(r,window("2026-11-01","2026-12-01")).isEmpty())
        assertEquals(listOf("2026-12-01"), dates(r,window("2026-12-01","2027-01-01")))
    }
    @Test fun weeklyAnchorAndWeekdays() {
        val r = rule(Recurrence.Weekly(d("2026-11-25"),2,setOf(DayOfWeek.MONDAY,DayOfWeek.WEDNESDAY)))
        assertEquals(listOf("2026-11-25","2026-12-07","2026-12-09"),dates(r,window("2026-11-23","2026-12-10")))
    }
    @Test fun everyDaysAnchoredAcrossMonth() {
        val r = rule(Recurrence.EveryNDays(d("2026-11-28"),3))
        assertEquals(listOf("2026-12-01","2026-12-04","2026-12-07"),dates(r,window("2026-12-01","2026-12-08")))
    }
    @Test fun explicitDistinctEntriesOnSameDay() {
        val a = ExplicitDate(UUID.randomUUID(),d("2026-12-01"))
        val b = ExplicitDate(UUID.randomUUID(),a.date)
        val events = generate(rule(Recurrence.ExplicitDates(listOf(a,b))),window("2026-12-01","2027-01-01")).events
        assertEquals(2,events.size)
        assertEquals(2,events.map { it.key.id }.distinct().size)
    }
    @Test fun stableMonthlyIdentityAndIdempotence() {
        val w = window("2027-02-01","2027-04-01")
        val r = rule(monthly(MissingDayPolicy.CLAMP))
        val original = generate(r,w)
        assertEquals(original,generate(r,w))
        val revised = r.copy(revisionId=UUID.randomUUID(), amount=Money("RUB",20),recurrence=monthly(MissingDayPolicy.CLAMP,15))
        assertEquals(original.events.map { it.key.id },generate(revised,w).events.map { it.key.id })
    }
    @Test fun manualMoveAcrossWindowRetainsSlot() {
        val r = rule(monthly(MissingDayPolicy.CLAMP))
        val feb = generate(r,window("2028-02-01","2028-03-01")).events.single()
        val override = EventOverride.Move(feb.key,d("2028-03-01"))
        val result = generate(r,window("2028-03-01","2028-04-01"),listOf(override))
        assertEquals(listOf("2028-03-01","2028-03-31"),result.events.map { it.dueDate.toString() })
        assertEquals(feb.key,result.events.first().key)
        assertTrue(result.events.first().overridden)
        assertTrue(generate(r,window("2028-02-01","2028-03-01"),listOf(override)).events.isEmpty())
    }
    @Test fun manualCancelDoesNotDisappear() {
        val r=rule(Recurrence.OneOff(d("2026-12-01")))
        val w=window("2026-12-01","2027-01-01")
        val event=generate(r,w).events.single()
        assertTrue(generate(r,w,listOf(EventOverride.Cancel(event.key))).events.single().cancelled)
    }
    @Test fun unknownOverrideReported() {
        val key=OccurrenceKey(series,"m:2027-02")
        val r=rule(monthly(MissingDayPolicy.SKIP))
        assertEquals(setOf(key),generate(r,window("2027-02-01","2027-03-01"),listOf(EventOverride.Move(key,d("2027-02-28")))).unresolvedOverrides)
    }
    @Test fun revisionIntervalsAndNoDuplicates() {
        val w=window("2027-01-01","2027-04-01")
        val old=rule(monthly(MissingDayPolicy.CLAMP),end="2027-02-01")
        val next=old.copy(revisionId=UUID.randomUUID(),effectiveFrom=d("2027-02-01"),effectiveUntil=null,recurrence=monthly(MissingDayPolicy.CLAMP,15))
        val result=FinancialEventEngine.generate(listOf(next,old),emptyList(),w)
        assertEquals(listOf("2027-01-31","2027-02-15","2027-03-15"),result.events.map { it.dueDate.toString() })
        assertEquals(result,FinancialEventEngine.generate(listOf(old,next),emptyList(),w))
    }
    @Test(expected=IllegalArgumentException::class) fun overlappingRevisionsRejected() {
        val r=rule(Recurrence.OneOff(d("2026-12-01")))
        FinancialEventEngine.generate(listOf(r,r.copy(revisionId=UUID.randomUUID())),emptyList(),window("2026-12-01","2027-01-01"))
    }
    @Test(expected=IllegalArgumentException::class) fun ambiguousMonthlyRevisionRejected() {
        val a=rule(monthly(MissingDayPolicy.CLAMP,5),end="2026-12-15")
        val b=a.copy(revisionId=UUID.randomUUID(),effectiveFrom=d("2026-12-15"),effectiveUntil=null,recurrence=monthly(MissingDayPolicy.CLAMP,25))
        FinancialEventEngine.generate(listOf(a,b),emptyList(),window("2026-12-01","2027-01-01"))
    }
    @Test(expected=IllegalArgumentException::class) fun invalidIntervalRejected() { Recurrence.EveryNDays(d("2026-12-01"),0) }
    @Test(expected=IllegalArgumentException::class) fun overlongWindowRejected() { window("2026-01-01","2027-01-02") }
    @Test fun bankZoneWindowNotDeviceZone() {
        val c=Clock.fixed(Instant.parse("2026-12-01T00:30:00Z"),ZoneOffset.UTC)
        assertEquals(d("2026-11-30"),EventWindow.nextYear(c,ZoneId.of("America/Los_Angeles")).from)
        assertEquals(d("2026-12-01"),EventWindow.nextYear(c,ZoneId.of("Europe/Moscow")).from)
    }
    @Test fun unknownMoneyAndNonMonetaryRemainDistinct() {
        val r=rule(Recurrence.OneOff(d("2026-12-01"))).copy(amount=null)
        val w=window("2026-12-01","2027-01-01")
        assertNull(generate(r,w).events.single().amount)
        val info=r.copy(type=EventType.CASH_LIMIT_RESET,monetaryRole=MonetaryRole.NON_MONETARY)
        assertEquals(MonetaryRole.NON_MONETARY,generate(info,w).events.single().monetaryRole)
        assertNull(generate(info,w).events.single().amount)
    }
    @Test fun reconciliationPreservesHistoryAndRetiresFuture() {
        val r=rule(monthly(MissingDayPolicy.CLAMP))
        val w=window("2026-11-01","2027-03-01")
        val old=generate(r,w).events
        val existing=old.mapIndexed { i,e -> ExistingEvent(e,hasPaymentLink=i==1) }
        val desired=generate(r.copy(revisionId=UUID.randomUUID(),recurrence=monthly(MissingDayPolicy.CLAMP,15)),w)
        val result=EventReconciler.reconcile(existing,desired,w,clock("2026-12-01"))
        assertEquals(old.take(2),result.keep)
        assertEquals(2,result.upsert.size)
        assertEquals(setOf(old[1].key),result.conflicts)
        val retired=EventReconciler.reconcile(existing,FinancialEventEngine.Generation(emptyList(),emptySet()),w,clock("2026-12-01"))
        assertEquals(old.take(2),retired.keep)
        assertEquals(old.drop(2).map { it.key }.toSet(),retired.retire)
    }
    @Test fun unchangedReconciliationHasNoWrites() {
        val w=window("2026-12-01","2027-01-01")
        val generated=generate(rule(monthly(MissingDayPolicy.CLAMP)),w)
        val result=EventReconciler.reconcile(generated.events.map { ExistingEvent(it) },generated,w,clock("2026-12-01"))
        assertEquals(generated.events,result.keep)
        assertTrue(result.upsert.isEmpty() && result.retire.isEmpty() && result.conflicts.isEmpty())
    }
    @Test fun priorManualOverrideAndFutureOutsideWindowKept() {
        val w=window("2026-12-01","2027-01-01")
        val r=rule(monthly(MissingDayPolicy.CLAMP))
        val dec=generate(r,w).events.single().copy(overridden=true)
        val feb=generate(r,window("2027-02-01","2027-03-01")).events.single()
        val result=EventReconciler.reconcile(listOf(ExistingEvent(dec),ExistingEvent(feb)),
            FinancialEventEngine.Generation(emptyList(),emptySet()),w,clock("2026-12-01"))
        assertEquals(listOf(dec,feb),result.keep)
        assertEquals(setOf(dec.key),result.conflicts)
        assertTrue(result.retire.isEmpty())
    }
}
