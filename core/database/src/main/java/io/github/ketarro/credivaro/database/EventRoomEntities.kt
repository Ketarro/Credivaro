package io.github.ketarro.credivaro.database

import androidx.room.*

@Entity(tableName = "event_series",
    foreignKeys = [ForeignKey(entity = ObligationRow::class, parentColumns = ["id"], childColumns = ["obligationId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["obligationId"], name = "event_series_owner"), Index(value = ["id", "obligationId"], unique = true)])
internal data class EventSeriesRow(@PrimaryKey val id: String, val obligationId: String,
    val type: String, val recurrenceFamily: String)

@Entity(tableName = "event_rule_revision",
    foreignKeys = [ForeignKey(entity = EventSeriesRow::class, parentColumns = ["id"], childColumns = ["seriesId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["seriesId", "effectiveFrom"], name = "event_revision_series"), Index(value = ["id", "seriesId"], unique = true)])
internal data class EventRuleRevisionRow(@PrimaryKey val id: String, val seriesId: String,
    val effectiveFrom: String, val effectiveUntil: String?, val payload: String)

@Entity(tableName = "event_occurrence",
    foreignKeys = [
        ForeignKey(entity = EventSeriesRow::class, parentColumns = ["id"], childColumns = ["seriesId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = EventRuleRevisionRow::class, parentColumns = ["id", "seriesId"], childColumns = ["revisionId", "seriesId"])
    ],
    indices = [Index(value = ["seriesId", "slot"], unique = true), Index(value = ["id", "seriesId"], unique = true),
        Index(value = ["revisionId", "seriesId"], name = "event_occurrence_revision"), Index(value = ["dueDate"], name = "event_occurrence_due")])
internal data class EventOccurrenceRow(@PrimaryKey val id: String, val seriesId: String,
    val revisionId: String, val slot: String, val nominalDate: String, val dueDate: String,
    val payload: String, val retired: Boolean)

@Entity(tableName = "event_override", primaryKeys = ["seriesId", "slot"],
    foreignKeys = [ForeignKey(entity = EventSeriesRow::class, parentColumns = ["id"], childColumns = ["seriesId"], onDelete = ForeignKey.CASCADE)])
internal data class EventOverrideRow(val seriesId: String, val slot: String, val kind: String, val movedDate: String?)

@Entity(tableName = "event_payment_link", primaryKeys = ["occurrenceId", "paymentId"],
    foreignKeys = [
        ForeignKey(entity = EventOccurrenceRow::class, parentColumns = ["id"], childColumns = ["occurrenceId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PaymentRow::class, parentColumns = ["id"], childColumns = ["paymentId"], onDelete = ForeignKey.CASCADE)
    ], indices = [Index(value = ["paymentId"], name = "event_link_payment")])
internal data class EventPaymentLinkRow(val occurrenceId: String, val paymentId: String)
