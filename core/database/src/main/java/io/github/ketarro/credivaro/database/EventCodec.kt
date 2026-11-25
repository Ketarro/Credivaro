package io.github.ketarro.credivaro.database

import io.github.ketarro.credivaro.model.*
import org.json.JSONObject
import org.json.JSONArray
import java.time.*
import java.util.UUID

/** Versioned exact-value codec for the isolated event storage candidate. */
internal object EventCodec {
    private fun JSONObject.optional(k: String): String? = if (isNull(k)) null else getString(k)
    private fun JSONObject.uuid(k: String): UUID = getString(k).let { raw ->
        UUID.fromString(raw).also { require(it.toString() == raw) }
    }
    private fun JSONObject.date(k: String): LocalDate = getString(k).let { raw ->
        LocalDate.parse(raw).also { require(it.toString() == raw) }
    }
    private fun JSONObject.version() { require(getInt("v") == 1) }
    private fun recurrence(r: Recurrence): JSONObject = JSONObject().apply {
        when (r) {
            is Recurrence.OneOff -> { put("kind", "once"); put("date", r.date.toString()) }
            is Recurrence.Monthly -> {
                put("kind", "monthly"); put("day", r.day.day); put("policy", r.day.missingDayPolicy.name)
                put("first", r.firstKnownDate?.toString())
            }
            Recurrence.LastDayOfMonth -> put("kind", "last")
            is Recurrence.Weekly -> {
                put("kind", "weekly"); put("anchor", r.anchor.toString()); put("interval", r.intervalWeeks)
                put("days", JSONArray(r.weekdays.sortedBy { it.value }.map { it.name }))
            }
            is Recurrence.EveryNDays -> {
                put("kind", "interval"); put("anchor", r.anchor.toString()); put("interval", r.intervalDays)
            }
            is Recurrence.ExplicitDates -> {
                put("kind", "explicit"); put("entries", JSONArray().apply {
                    r.entries.forEach { put(JSONObject().put("id", it.id.toString()).put("date", it.date.toString())) }
                })
            }
        }
    }
    private fun recurrence(j: JSONObject): Recurrence = when (j.getString("kind")) {
        "once" -> Recurrence.OneOff(j.date("date"))
        "monthly" -> Recurrence.Monthly(MonthlyDay(j.getInt("day"), MissingDayPolicy.valueOf(j.getString("policy"))),
            if (j.isNull("first")) null else j.date("first"))
        "last" -> Recurrence.LastDayOfMonth
        "weekly" -> Recurrence.Weekly(j.date("anchor"), j.getInt("interval"), j.getJSONArray("days").let { a ->
            require(a.length() in 1..7)
            List(a.length()) { DayOfWeek.valueOf(a.getString(it)) }.also { require(it.distinct().size == it.size) }.toSet()
        })
        "interval" -> Recurrence.EveryNDays(j.date("anchor"), j.getInt("interval"))
        "explicit" -> Recurrence.ExplicitDates(j.getJSONArray("entries").let { a ->
            require(a.length() in 1..3660)
            List(a.length()) { a.getJSONObject(it).let { e -> ExplicitDate(e.uuid("id"), e.date("date")) } }
        })
        else -> error("Unknown recurrence")
    }
    fun family(r: Recurrence): String = when (r) {
        is Recurrence.OneOff -> "once"
        is Recurrence.Monthly, Recurrence.LastDayOfMonth -> "month"
        is Recurrence.Weekly, is Recurrence.EveryNDays -> "day"
        is Recurrence.ExplicitDates -> "explicit"
    }
    private fun JSONObject.amount(currency: String): Money? = optional("amount")?.let { raw ->
        val n = raw.toLong()
        require(n >= 0 && n.toString() == raw && get("amount") is String)
        Money(currency, n)
    }
    fun encodeRule(r: EventRule): String = JSONObject().apply {
        put("v", 1); put("series", r.seriesId.toString()); put("revision", r.revisionId.toString())
        put("owner", r.obligationId.toString()); put("type", r.type.name); put("role", r.monetaryRole.name)
        put("recurrence", recurrence(r.recurrence)); put("zone", r.bankZone.id)
        put("from", r.effectiveFrom.toString()); put("until", r.effectiveUntil?.toString())
        put("currency", r.currency); put("amount", r.amount?.minorUnits?.toString())
        put("group", r.alternativeGroupKey?.toString())
    }.toString()
    fun decodeRule(payload: String): EventRule = JSONObject(payload).let { j ->
        j.version()
        val currency = j.getString("currency")
        EventRule(j.uuid("series"), j.uuid("revision"), j.uuid("owner"), EventType.valueOf(j.getString("type")),
            MonetaryRole.valueOf(j.getString("role")), recurrence(j.getJSONObject("recurrence")),
            ZoneId.of(j.getString("zone")), j.date("from"), if (j.isNull("until")) null else j.date("until"),
            currency, j.amount(currency), if (j.isNull("group")) null else j.uuid("group"))
    }
    fun encodeEvent(e: FinancialEvent): String = JSONObject().apply {
        put("v", 1); put("series", e.key.seriesId.toString()); put("slot", e.key.slot)
        put("revision", e.revisionId.toString()); put("owner", e.obligationId.toString())
        put("type", e.type.name); put("role", e.monetaryRole.name); put("zone", e.bankZone.id)
        put("nominal", e.nominalDate.toString()); put("due", e.dueDate.toString())
        put("currency", e.currency); put("amount", e.amount?.minorUnits?.toString())
        put("group", e.alternativeGroupKey?.toString()); put("overridden", e.overridden); put("cancelled", e.cancelled)
    }.toString()
    fun decodeEvent(payload: String): FinancialEvent = JSONObject(payload).let { j ->
        j.version()
        val currency = j.getString("currency")
        FinancialEvent(OccurrenceKey(j.uuid("series"), j.getString("slot")), j.uuid("revision"), j.uuid("owner"),
            EventType.valueOf(j.getString("type")), MonetaryRole.valueOf(j.getString("role")),
            ZoneId.of(j.getString("zone")), j.date("nominal"), j.date("due"), currency, j.amount(currency),
            if (j.isNull("group")) null else j.uuid("group"), j.getBoolean("overridden"), j.getBoolean("cancelled"))
    }
}
