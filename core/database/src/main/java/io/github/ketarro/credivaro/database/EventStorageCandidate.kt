package io.github.ketarro.credivaro.database

import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.ketarro.credivaro.model.*
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

internal data class EventPaymentLinkState(
    val paymentId: UUID,
    val status: PaymentHistoryStatus,
    val replacementId: UUID?
)

/** Isolated candidate. Never open or migrate the production database through this class.
 * Caller owns the database connection; all operations are synchronous and must run off main.
 */
internal class EventStorageCandidate(private val db: SupportSQLiteDatabase, private val clock: Clock) {
    private fun <T> atomic(block: () -> T): T {
        check(!db.inTransaction()) { "Candidate requires its own transaction" }
        db.beginTransaction()
        try {
            val result = block()
            db.setTransactionSuccessful()
            return result
        } finally { db.endTransaction() }
    }

    private fun rules(owner: UUID): List<EventRule> =
        db.query("SELECT r.payload FROM event_rule_revision r JOIN event_series s ON s.id=r.seriesId WHERE s.obligationId=?", arrayOf(owner.toString())).use { c ->
            buildList { while (c.moveToNext()) add(EventCodec.decodeRule(c.getString(0))) }
        }

    private fun overrides(owner: UUID): List<EventOverride> =
        db.query("SELECT x.seriesId,x.slot,x.kind,x.movedDate FROM event_override x JOIN event_series s ON s.id=x.seriesId WHERE s.obligationId=?", arrayOf(owner.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val key = OccurrenceKey(UUID.fromString(c.getString(0)), c.getString(1))
                    add(when (c.getString(2)) {
                        "MOVE" -> EventOverride.Move(key, LocalDate.parse(c.getString(3)))
                        "CANCEL" -> EventOverride.Cancel(key)
                        else -> error("Unknown override")
                    })
                }
            }
        }

    /** Append only. Existing revision IDs may be replayed only with identical data. */
    fun appendRule(rule: EventRule, window: EventWindow) = atomic {
        val owner = rule.obligationId.toString()
        db.query("SELECT currency,bankZone FROM obligation WHERE id=?", arrayOf(owner)).use { c ->
            require(c.moveToFirst() && c.getString(0) == rule.currency && c.getString(1) == rule.bankZone.id)
        }
        val before = rules(rule.obligationId)
        val same = before.find { it.revisionId == rule.revisionId }
        if (same != null) {
            require(same == rule) { "Revision ID conflict" }
            return@atomic
        }
        // The generator validates family, overlapping revisions and ambiguous slots.
        FinancialEventEngine.generate(before + rule, overrides(rule.obligationId), window)
        val series = rule.seriesId.toString()
        db.execSQL("INSERT OR IGNORE INTO event_series(id,obligationId,type,recurrenceFamily) VALUES (?,?,?,?)",
            arrayOf(series, owner, rule.type.name, EventCodec.family(rule.recurrence)))
        db.query("SELECT obligationId,type,recurrenceFamily FROM event_series WHERE id=?", arrayOf(series)).use { c ->
            require(c.moveToFirst() && c.getString(0) == owner && c.getString(1) == rule.type.name &&
                c.getString(2) == EventCodec.family(rule.recurrence))
        }
        db.execSQL("INSERT INTO event_rule_revision(id,seriesId,effectiveFrom,effectiveUntil,payload) VALUES (?,?,?,?,?)",
            arrayOf(rule.revisionId.toString(), series, rule.effectiveFrom.toString(),
                rule.effectiveUntil?.toString(), EventCodec.encodeRule(rule)))
    }

    /** Explicit edit of a materialized future slot, with optimistic snapshot validation.
     * Null clears the exception. Linked or historical slots require separate review.
     * Candidate only: notification outbox must be added before production activation.
     */
    fun editOverride(expected: FinancialEvent, replacement: EventOverride?): FinancialEvent = atomic {
        val key = expected.key
        require(replacement == null || replacement.key == key)
        val current = db.query("""SELECT o.payload,o.retired,s.obligationId,
            EXISTS(SELECT 1 FROM event_payment_link l WHERE l.occurrenceId=o.id)
            FROM event_occurrence o JOIN event_series s ON s.id=o.seriesId WHERE o.id=?""",
            arrayOf(key.id.toString())).use { c ->
            require(c.moveToFirst() && c.getInt(1) == 0 && c.getInt(3) == 0) { "Missing, retired or linked event" }
            EventCodec.decodeEvent(c.getString(0)).also {
                require(it == expected && it.obligationId.toString() == c.getString(2)) { "Stale event snapshot" }
            }
        }
        val today = LocalDate.now(clock.withZone(current.bankZone))
        require(!current.nominalDate.isBefore(today) && !current.dueDate.isBefore(today)) { "Historical event" }
        val targetDate = (replacement as? EventOverride.Move)?.bankDate ?: current.nominalDate
        require(!targetDate.isBefore(today) && targetDate.isBefore(today.plusMonths(12))) { "Outside editable horizon" }
        val allRules = rules(current.obligationId)
        val previous = overrides(current.obligationId)
        val proposed = previous.filterNot { it.key == key } + listOfNotNull(replacement)
        val generation = FinancialEventEngine.generate(allRules, proposed, EventWindow(targetDate, targetDate.plusDays(1)))
        require(generation.unresolvedOverrides.isEmpty()) { "Unresolved override" }
        val updated = generation.events.singleOrNull { it.key == key }
            ?: error("Nominal slot no longer exists")
        require(updated.revisionId == current.revisionId && updated.nominalDate == current.nominalDate &&
            updated.copy(dueDate = current.dueDate, overridden = current.overridden, cancelled = current.cancelled) == current) {
            "Rule changed; reconcile before editing"
        }
        val args = arrayOf(key.seriesId.toString(), key.slot)
        db.execSQL("DELETE FROM event_override WHERE seriesId=? AND slot=?", args)
        if (replacement != null) {
            db.execSQL("INSERT INTO event_override(seriesId,slot,kind,movedDate) VALUES (?,?,?,?)",
                arrayOf(key.seriesId.toString(), key.slot,
                    if (replacement is EventOverride.Move) "MOVE" else "CANCEL",
                    (replacement as? EventOverride.Move)?.bankDate?.toString()))
        }
        db.execSQL("UPDATE event_occurrence SET dueDate=?,payload=? WHERE id=?",
            arrayOf(updated.dueDate.toString(), EventCodec.encodeEvent(updated), key.id.toString()))
        updated
    }

    /** Explicit association only. No inference, allocation or balance mutation. */
    fun linkPayment(eventId: UUID, paymentId: UUID) = atomic {
        val e = eventId.toString()
        val p = paymentId.toString()
        db.query("""SELECT o.payload,o.retired,s.obligationId,p.obligationId,p.currency
            FROM event_occurrence o JOIN event_series s ON s.id=o.seriesId
            JOIN payment p ON p.id=? WHERE o.id=?""", arrayOf(p,e)).use { c ->
            require(c.moveToFirst()) { "Unknown event or payment" }
            val event = EventCodec.decodeEvent(c.getString(0))
            require(c.getInt(1) == 0 && !event.cancelled && event.key.id == eventId &&
                event.obligationId.toString() == c.getString(2) && c.getString(2) == c.getString(3) &&
                event.currency == c.getString(4) && event.monetaryRole == MonetaryRole.MANDATORY &&
                event.type == EventType.REQUIRED_PAYMENT) { "Incompatible payment link" }
        }
        db.query("SELECT 1 FROM payment_correction WHERE targetPaymentId=?", arrayOf(p)).use {
            require(!it.moveToFirst()) { "Corrected payment cannot be newly linked" }
        }
        db.execSQL("INSERT OR IGNORE INTO event_payment_link(occurrenceId,paymentId) VALUES (?,?)", arrayOf(e,p))
    }

    /** Audit links remain immutable after a correction. Only uncorrected explicitly linked
     * payment facts are effective; a replacement never inherits a link implicitly.
     * A corrected historical link still protects its occurrence from destructive edits.
     */
    fun paymentLinks(eventId: UUID): List<EventPaymentLinkState> = atomic {
        db.query("SELECT 1 FROM event_occurrence WHERE id=?", arrayOf(eventId.toString())).use {
            require(it.moveToFirst()) { "Unknown event" }
        }
        db.query("""SELECT l.paymentId,c.targetPaymentId,c.replacementId
            FROM event_payment_link l LEFT JOIN payment_correction c ON c.targetPaymentId=l.paymentId
            WHERE l.occurrenceId=? ORDER BY l.paymentId""", arrayOf(eventId.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val replacement = if (c.isNull(2)) null else UUID.fromString(c.getString(2))
                    add(EventPaymentLinkState(UUID.fromString(c.getString(0)),
                        when {
                            c.isNull(1) -> PaymentHistoryStatus.ACTIVE
                            replacement == null -> PaymentHistoryStatus.CANCELLED
                            else -> PaymentHistoryStatus.REPLACED
                        }, replacement))
                }
            }
        }
    }

    /** Materialize active events and retire stale future slots without deleting history. */
    fun reconcile(owner: UUID, window: EventWindow): EventReconciliation = atomic {
        val generated = FinancialEventEngine.generate(rules(owner), overrides(owner), window)
        val existing = db.query("""SELECT o.id,o.payload,EXISTS(SELECT 1 FROM event_payment_link l WHERE l.occurrenceId=o.id)
            FROM event_occurrence o JOIN event_series s ON s.id=o.seriesId
            WHERE s.obligationId=? AND o.retired=0""", arrayOf(owner.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val event = EventCodec.decodeEvent(c.getString(1))
                    check(event.key.id.toString() == c.getString(0) && event.obligationId == owner)
                    add(ExistingEvent(event, hasPaymentLink = c.getInt(2) != 0))
                }
            }
        }
        val plan = EventReconciler.reconcile(existing, generated, window, clock)
        // No partial writes when a protected event or override requires review.
        if (plan.conflicts.isNotEmpty()) return@atomic plan
        for (event in plan.upsert) {
            val id = event.key.id.toString()
            val args = arrayOf<Any?>(event.revisionId.toString(), event.nominalDate.toString(),
                event.dueDate.toString(), EventCodec.encodeEvent(event), id)
            db.execSQL("UPDATE event_occurrence SET revisionId=?,nominalDate=?,dueDate=?,payload=?,retired=0 WHERE id=?", args)
            db.execSQL("""INSERT INTO event_occurrence(id,seriesId,revisionId,slot,nominalDate,dueDate,payload,retired)
                SELECT ?,?,?,?,?,?,?,0 WHERE NOT EXISTS(SELECT 1 FROM event_occurrence WHERE id=?)""",
                arrayOf(id,event.key.seriesId.toString(),event.revisionId.toString(),event.key.slot,
                    event.nominalDate.toString(),event.dueDate.toString(),EventCodec.encodeEvent(event),id))
        }
        plan.retire.forEach {
            db.execSQL("UPDATE event_occurrence SET retired=1 WHERE id=?", arrayOf(it.id.toString()))
        }
        plan
    }
}
