package io.github.ketarro.credivaro.database

import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.ketarro.credivaro.model.*
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

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
