package io.github.ketarro.credivaro.model

import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

enum class EventType {
    REQUIRED_PAYMENT, STATEMENT_ISSUED, BILLING_CYCLE_START, GRACE_END,
    CASH_LIMIT_RESET, INTEREST_ACCRUAL, SERVICE_FEE, CUSTOM
}
enum class MonetaryRole { MANDATORY, ALTERNATIVE_TARGET, INFORMATIONAL, NON_MONETARY }

sealed interface Recurrence {
    data class OneOff(val date: LocalDate) : Recurrence
    data class Monthly(val day: MonthlyDay) : Recurrence
    data object LastDayOfMonth : Recurrence
    data class Weekly(val anchor: LocalDate, val intervalWeeks: Int, val weekdays: Set<DayOfWeek>) : Recurrence {
        init { require(intervalWeeks > 0 && weekdays.isNotEmpty()) }
    }
    data class EveryNDays(val anchor: LocalDate, val intervalDays: Int) : Recurrence {
        init { require(intervalDays > 0) }
    }
    data class ExplicitDates(val entries: List<ExplicitDate>) : Recurrence {
        init {
            require(entries.size in 1..3660)
            require(entries.map { it.id }.distinct().size == entries.size)
        }
    }
}
data class ExplicitDate(val id: UUID, val date: LocalDate)

/** A revision owns dates in [effectiveFrom, effectiveUntil), evaluated BEFORE manual date moves. */
data class EventRule(
    val seriesId: UUID,
    val revisionId: UUID,
    val obligationId: UUID,
    val type: EventType,
    val monetaryRole: MonetaryRole,
    val recurrence: Recurrence,
    val bankZone: ZoneId,
    val effectiveFrom: LocalDate,
    val effectiveUntil: LocalDate? = null,
    val currency: String,
    val amount: Money? = null,
    val alternativeGroupKey: UUID? = null
) {
    init {
        require(effectiveUntil == null || effectiveUntil.isAfter(effectiveFrom))
        require(currency.matches(Regex("[A-Z]{3}")))
        require(amount == null || (amount.currency == currency && amount.minorUnits >= 0))
        require(monetaryRole != MonetaryRole.NON_MONETARY || amount == null)
        require(monetaryRole != MonetaryRole.ALTERNATIVE_TARGET || alternativeGroupKey != null)
        if (type in setOf(EventType.STATEMENT_ISSUED, EventType.BILLING_CYCLE_START, EventType.CASH_LIMIT_RESET))
            require(monetaryRole == MonetaryRole.NON_MONETARY)
    }
    fun owns(date: LocalDate): Boolean = !date.isBefore(effectiveFrom) &&
        (effectiveUntil == null || date.isBefore(effectiveUntil))
}

/** Canonical nominal slot; independent of adjusted date, amount and rule revision. */
data class OccurrenceKey(val seriesId: UUID, val slot: String) {
    init { require(slot.length in 1..80) }
    val id: UUID get() = UUID.nameUUIDFromBytes(("credivaro:event:v1:" + seriesId + ":" + slot).toByteArray(StandardCharsets.UTF_8))
}
sealed interface EventOverride {
    val key: OccurrenceKey
    data class Move(override val key: OccurrenceKey, val bankDate: LocalDate) : EventOverride
    data class Cancel(override val key: OccurrenceKey) : EventOverride
}
data class FinancialEvent(
    val key: OccurrenceKey,
    val revisionId: UUID,
    val obligationId: UUID,
    val type: EventType,
    val monetaryRole: MonetaryRole,
    val bankZone: ZoneId,
    val nominalDate: LocalDate,
    val dueDate: LocalDate,
    val currency: String,
    val amount: Money?,
    val alternativeGroupKey: UUID?,
    val overridden: Boolean = false,
    val cancelled: Boolean = false
)

/** Local-date window, inclusive start and exclusive end; bank day, not UTC day. */
data class EventWindow(val from: LocalDate, val until: LocalDate) {
    init { require(until.isAfter(from) && !until.isAfter(from.plusMonths(12))) }
    fun contains(date: LocalDate): Boolean = !date.isBefore(from) && date.isBefore(until)
    companion object {
        fun nextYear(clock: Clock, bankZone: ZoneId): EventWindow {
            val today = LocalDate.now(clock.withZone(bankZone))
            return EventWindow(today, today.plusMonths(12))
        }
    }
}

private data class Slot(val key: String, val date: LocalDate)

object FinancialEventEngine {
    private fun family(recurrence: Recurrence): String = when (recurrence) {
        is Recurrence.OneOff -> "once"
        is Recurrence.Monthly, Recurrence.LastDayOfMonth -> "month"
        is Recurrence.Weekly, is Recurrence.EveryNDays -> "day"
        is Recurrence.ExplicitDates -> "explicit"
    }

    private fun monthlyDate(month: YearMonth, recurrence: Recurrence): LocalDate? = when (recurrence) {
        is Recurrence.Monthly -> {
            val day = recurrence.day
            if (day.day <= month.lengthOfMonth()) month.atDay(day.day)
            else if (day.missingDayPolicy == MissingDayPolicy.CLAMP) month.atEndOfMonth() else null
        }
        Recurrence.LastDayOfMonth -> month.atEndOfMonth()
        else -> error("Not monthly")
    }

    private fun isIntervalDate(date: LocalDate, recurrence: Recurrence): Boolean = when (recurrence) {
        is Recurrence.EveryNDays -> !date.isBefore(recurrence.anchor) &&
            ChronoUnit.DAYS.between(recurrence.anchor, date) % recurrence.intervalDays == 0L
        is Recurrence.Weekly -> {
            val firstWeek = recurrence.anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val dateWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            !date.isBefore(recurrence.anchor) && date.dayOfWeek in recurrence.weekdays &&
                ChronoUnit.WEEKS.between(firstWeek, dateWeek) % recurrence.intervalWeeks == 0L
        }
        else -> false
    }

    private fun slots(recurrence: Recurrence, window: EventWindow): List<Slot> = buildList {
        when (recurrence) {
            is Recurrence.OneOff -> if (window.contains(recurrence.date)) add(Slot("once", recurrence.date)) else Unit
            is Recurrence.Monthly, Recurrence.LastDayOfMonth -> {
                var month = YearMonth.from(window.from)
                val last = YearMonth.from(window.until.minusDays(1))
                while (month <= last) {
                    val date = monthlyDate(month, recurrence)
                    if (date != null && window.contains(date)) add(Slot("m:" + month, date))
                    month = month.plusMonths(1)
                }
            }
            is Recurrence.Weekly, is Recurrence.EveryNDays -> {
                var date = window.from
                while (date.isBefore(window.until)) {
                    if (isIntervalDate(date, recurrence)) add(Slot("d:" + date, date))
                    date = date.plusDays(1)
                }
            }
            is Recurrence.ExplicitDates -> recurrence.entries.forEach {
                if (window.contains(it.date)) add(Slot("e:" + it.id, it.date))
            }
        }
    }

    /** Resolve a manually moved/cancelled slot even when its nominal date is outside the window. */
    private fun resolve(recurrence: Recurrence, slot: String): Slot? {
        val date = when (recurrence) {
            is Recurrence.OneOff -> recurrence.date.takeIf { slot == "once" }
            is Recurrence.Monthly, Recurrence.LastDayOfMonth -> {
                if (!slot.startsWith("m:")) null else runCatching { YearMonth.parse(slot.removePrefix("m:")) }.getOrNull()
                    ?.takeIf { slot == "m:" + it }?.let { monthlyDate(it, recurrence) }
            }
            is Recurrence.Weekly, is Recurrence.EveryNDays -> {
                if (!slot.startsWith("d:")) null else runCatching { LocalDate.parse(slot.removePrefix("d:")) }.getOrNull()
                    ?.takeIf { slot == "d:" + it && isIntervalDate(it, recurrence) }
            }
            is Recurrence.ExplicitDates -> recurrence.entries.singleOrNull { slot == "e:" + it.id }?.date
        }
        return date?.let { Slot(slot, it) }
    }

    private fun validate(rules: List<EventRule>, overrides: List<EventOverride>) {
        require(rules.size <= 10_000 && overrides.size <= 10_000)
        require(rules.map { it.revisionId }.distinct().size == rules.size) { "Duplicate revision ID" }
        require(overrides.map { it.key }.distinct().size == overrides.size) { "Duplicate override" }
        rules.groupBy { it.seriesId }.values.forEach { group ->
            val first = group.first()
            require(group.all {
                it.obligationId == first.obligationId && it.type == first.type &&
                    family(it.recurrence) == family(first.recurrence)
            }) { "Incompatible series revisions; create a new series" }
            val sorted = group.sortedBy { it.effectiveFrom }
            sorted.zipWithNext().forEach { (a, b) ->
                require(a.effectiveUntil != null && !a.effectiveUntil.isAfter(b.effectiveFrom)) { "Overlapping revisions" }
            }
        }
    }

    data class Generation(val events: List<FinancialEvent>, val unresolvedOverrides: Set<OccurrenceKey>)

    fun generate(rules: List<EventRule>, overrides: List<EventOverride>, window: EventWindow): Generation {
        validate(rules, overrides)
        val overrideMap = overrides.associateBy { it.key }
        val bySeries = overrides.groupBy { it.key.seriesId }
        val resolved = mutableSetOf<OccurrenceKey>()
        val events = linkedMapOf<OccurrenceKey, FinancialEvent>()
        rules.forEach { rule ->
            val candidates = slots(rule.recurrence, window).associateBy { it.key }.toMutableMap()
            bySeries[rule.seriesId].orEmpty().forEach { exception ->
                resolve(rule.recurrence, exception.key.slot)?.let { candidates[it.key] = it }
            }
            candidates.values.filter { rule.owns(it.date) }.forEach { slot ->
                val key = OccurrenceKey(rule.seriesId, slot.key)
                val exception = overrideMap[key]
                if (exception != null) resolved.add(key)
                val due = (exception as? EventOverride.Move)?.bankDate ?: slot.date
                if (window.contains(due)) {
                    val event = FinancialEvent(key, rule.revisionId, rule.obligationId, rule.type, rule.monetaryRole,
                        rule.bankZone, slot.date, due, rule.currency, rule.amount, rule.alternativeGroupKey,
                        exception != null, exception is EventOverride.Cancel)
                    // Two revisions may own different dates in the same monthly slot. Never double-count.
                    require(events.put(key, event) == null) { "Ambiguous nominal slot across revisions" }
                }
            }
        }
        return Generation(events.values.sortedWith(compareBy<FinancialEvent> { it.dueDate }.thenBy { it.key.id.toString() }),
            overrideMap.keys - resolved)
    }
}

/** Only a plan is returned; caller must persist changes, linked history and outbox atomically. */
data class ExistingEvent(val event: FinancialEvent, val hasPaymentLink: Boolean = false, val factual: Boolean = false)
data class EventReconciliation(
    val keep: List<FinancialEvent>, val upsert: List<FinancialEvent>, val retire: Set<OccurrenceKey>,
    val conflicts: Set<OccurrenceKey>
)

object EventReconciler {
    fun reconcile(existing: List<ExistingEvent>, generated: FinancialEventEngine.Generation,
        window: EventWindow, clock: Clock): EventReconciliation {
        require(existing.map { it.event.key }.distinct().size == existing.size)
        require(generated.events.map { it.key }.distinct().size == generated.events.size)
        val desired = generated.events.associateBy { it.key }.toMutableMap()
        val keep = mutableListOf<FinancialEvent>()
        val upsert = mutableListOf<FinancialEvent>()
        val retire = mutableSetOf<OccurrenceKey>()
        val conflicts = generated.unresolvedOverrides.toMutableSet()
        existing.forEach { stored ->
            val old = stored.event
            val replacement = desired.remove(old.key)
            val today = LocalDate.now(clock.withZone(old.bankZone))
            val protected = stored.hasPaymentLink || stored.factual || old.overridden || old.dueDate.isBefore(today)
            if (protected) {
                keep.add(old)
                if ((stored.hasPaymentLink || stored.factual || old.overridden) &&
                    ((replacement != null && replacement != old) || (replacement == null && window.contains(old.dueDate))))
                    conflicts.add(old.key)
            } else if (replacement != null) {
                if (replacement == old) keep.add(old) else upsert.add(replacement)
            } else if (window.contains(old.dueDate)) {
                retire.add(old.key)
            } else {
                keep.add(old)
            }
        }
        upsert.addAll(desired.values)
        val order = compareBy<FinancialEvent> { it.dueDate }.thenBy { it.key.id.toString() }
        return EventReconciliation(keep.sortedWith(order), upsert.sortedWith(order), retire, conflicts)
    }
}
