package io.github.ketarro.credivaro.database

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Candidate additive schema for R05.03. Not registered with Room yet.
 * Run only inside a transaction on a synthetic v3 fixture until Room mapping,
 * compiler-exported schema and recovery/review gates are satisfied.
 */
internal object EventStorageSchemaCandidate {
    internal val statements = listOf(
        """CREATE TABLE event_series (
            id TEXT NOT NULL PRIMARY KEY, obligationId TEXT NOT NULL,
            type TEXT NOT NULL, recurrenceFamily TEXT NOT NULL,
            UNIQUE(id, obligationId),
            FOREIGN KEY(obligationId) REFERENCES obligation(id) ON DELETE CASCADE
        )""",
        """CREATE INDEX event_series_owner ON event_series(obligationId)""",
        """CREATE TABLE event_rule_revision (
            id TEXT NOT NULL PRIMARY KEY, seriesId TEXT NOT NULL,
            effectiveFrom TEXT NOT NULL, effectiveUntil TEXT,
            payload TEXT NOT NULL, UNIQUE(id, seriesId),
            CHECK(effectiveUntil IS NULL OR effectiveUntil > effectiveFrom),
            FOREIGN KEY(seriesId) REFERENCES event_series(id) ON DELETE CASCADE
        )""",
        """CREATE INDEX event_revision_series ON event_rule_revision(seriesId, effectiveFrom)""",
        """CREATE TABLE event_occurrence (
            id TEXT NOT NULL PRIMARY KEY, seriesId TEXT NOT NULL, revisionId TEXT NOT NULL,
            slot TEXT NOT NULL, nominalDate TEXT NOT NULL, dueDate TEXT NOT NULL,
            payload TEXT NOT NULL, retired INTEGER NOT NULL CHECK(retired IN (0,1)),
            UNIQUE(seriesId, slot), UNIQUE(id, seriesId),
            FOREIGN KEY(seriesId) REFERENCES event_series(id) ON DELETE CASCADE,
            FOREIGN KEY(revisionId, seriesId) REFERENCES event_rule_revision(id, seriesId) ON DELETE NO ACTION
        )""",
        """CREATE INDEX event_occurrence_revision ON event_occurrence(revisionId, seriesId)""",
        """CREATE INDEX event_occurrence_due ON event_occurrence(dueDate)""",
        """CREATE TABLE event_override (
            seriesId TEXT NOT NULL, slot TEXT NOT NULL,
            kind TEXT NOT NULL, movedDate TEXT,
            PRIMARY KEY(seriesId, slot),
            CHECK((kind = 'MOVE' AND movedDate IS NOT NULL) OR
                  (kind = 'CANCEL' AND movedDate IS NULL)),
            FOREIGN KEY(seriesId) REFERENCES event_series(id) ON DELETE CASCADE
        )""",
        """CREATE TABLE event_payment_link (
            occurrenceId TEXT NOT NULL, paymentId TEXT NOT NULL,
            PRIMARY KEY(occurrenceId, paymentId),
            FOREIGN KEY(occurrenceId) REFERENCES event_occurrence(id) ON DELETE CASCADE,
            FOREIGN KEY(paymentId) REFERENCES payment(id) ON DELETE CASCADE
        )""",
        """CREATE INDEX event_link_payment ON event_payment_link(paymentId)"""
    )

    fun create(db: SupportSQLiteDatabase) {
        check(db.inTransaction()) { "Candidate event schema requires a transaction" }
        statements.forEach { db.execSQL(it) }
    }
}
