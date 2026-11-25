package io.github.ketarro.credivaro

import io.github.ketarro.credivaro.model.*
import java.time.*
import java.util.UUID

/** No application database, user data, network, or actual notifications. */
internal object CalendarSelfCheck {
    val ids = (1..6).map { "CAL" + it.toString().padStart(2, '0') }
    private fun day(value: String) = LocalDate.parse(value)
    private fun window(from: String, until: String) = EventWindow(day(from), day(until))
    private fun rule(recurrence: Recurrence) = EventRule(
        UUID.fromString("00000000-0000-0000-0000-000000000011"),
        UUID.fromString("00000000-0000-0000-0000-000000000012"),
        UUID.fromString("00000000-0000-0000-0000-000000000013"),
        EventType.REQUIRED_PAYMENT, MonetaryRole.MANDATORY, recurrence, ZoneOffset.UTC,
        day("2026-01-01"), currency = "RUB", amount = Money("RUB", 100000))
    private fun generate(rule: EventRule, window: EventWindow, overrides: List<EventOverride> = emptyList()) =
        FinancialEventEngine.generate(listOf(rule), overrides, window)
    fun run(id: String) {
        val clamp = rule(Recurrence.Monthly(MonthlyDay(31, MissingDayPolicy.CLAMP)))
        val w = window("2027-01-01","2027-04-01")
        when (id) {
            "CAL01" -> {
                check(generate(clamp,w).events.map { it.dueDate } == listOf(day("2027-01-31"),day("2027-02-28"),day("2027-03-31")))
                check(generate(clamp,window("2028-02-01","2028-03-01")).events.single().dueDate == day("2028-02-29"))
                check(generate(clamp.copy(recurrence = Recurrence.LastDayOfMonth),w).events.map { it.dueDate } ==
                    generate(clamp,w).events.map { it.dueDate })
            }
            "CAL02" -> {
                val skip = clamp.copy(recurrence=Recurrence.Monthly(MonthlyDay(31,MissingDayPolicy.SKIP)))
                check(generate(skip,w).events.map { it.dueDate } == listOf(day("2027-01-31"),day("2027-03-31")))
                val r = rule(Recurrence.Weekly(day("2026-11-25"),2,setOf(DayOfWeek.WEDNESDAY)))
                check(generate(r,window("2026-11-25","2026-12-24")).events.map { it.dueDate } ==
                    listOf(day("2026-11-25"),day("2026-12-09"),day("2026-12-23")))
                check(knownPaymentRecurrence(ObligationTerms.Loan()) == null)
                check(knownPaymentRecurrence(ObligationTerms.CreditCard(minimumPaymentDueDate=day("2027-01-01"))) ==
                    Recurrence.OneOff(day("2027-01-01")))
            }
            "CAL03" -> {
                val feb = generate(clamp,window("2028-02-01","2028-03-01")).events.single()
                val move = EventOverride.Move(feb.key,day("2028-03-01"))
                val events = generate(clamp,window("2028-03-01","2028-04-01"),listOf(move)).events
                check(events.size == 2 && events.first().key.id == feb.key.id && events.first().dueDate == day("2028-03-01"))
                check(generate(clamp,window("2028-02-01","2028-03-01"),listOf(move)).events.isEmpty())
                check(generate(clamp,window("2028-02-01","2028-03-01"),listOf(EventOverride.Cancel(feb.key))).events.single().cancelled)
            }
            "CAL04" -> {
                val first = generate(clamp,w)
                check(first == generate(clamp,w))
                val edited = clamp.copy(revisionId=UUID.randomUUID(),recurrence=Recurrence.Monthly(MonthlyDay(15,MissingDayPolicy.CLAMP)))
                val next = generate(edited,w)
                check(first.events.map { it.key.id } == next.events.map { it.key.id })
                val clock = Clock.fixed(day("2027-02-01").atStartOfDay(ZoneOffset.UTC).toInstant(),ZoneOffset.UTC)
                val old = first.events.mapIndexed { i,e -> ExistingEvent(e,hasPaymentLink=i==1) }
                val plan = EventReconciler.reconcile(old,next,w,clock)
                check(plan.keep == first.events.take(2) && plan.upsert.size == 1 && plan.conflicts == setOf(first.events[1].key))
            }
            "CAL05" -> {
                val clock = Clock.fixed(Instant.parse("2026-12-01T00:30:00Z"),ZoneOffset.UTC)
                check(EventWindow.nextYear(clock,ZoneId.of("America/Los_Angeles")).from == day("2026-11-30"))
                check(EventWindow.nextYear(clock,ZoneId.of("Europe/Moscow")).from == day("2026-12-01"))
                check(runCatching { EventWindow(day("2026-01-01"),day("2027-01-02")) }.isFailure)
                check(runCatching { Recurrence.EveryNDays(day("2026-01-01"),0) }.isFailure)
            }
            "CAL06" -> {
                check(generate(clamp.copy(amount=null),w).events.all { it.amount == null })
                val info=clamp.copy(type=EventType.CASH_LIMIT_RESET,monetaryRole=MonetaryRole.NON_MONETARY,amount=null)
                check(generate(info,w).events.all { it.monetaryRole == MonetaryRole.NON_MONETARY && it.amount == null })
                check(runCatching { FinancialEventEngine.generate(listOf(clamp,clamp.copy(revisionId=UUID.randomUUID())),emptyList(),w) }.isFailure)
            }
            else -> error("Unknown calendar check")
        }
    }
}
