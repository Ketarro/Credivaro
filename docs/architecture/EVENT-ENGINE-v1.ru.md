# Движок финансовых событий и уведомлений v1.0

## 1. Разделение
EventEngine is a pure deterministic Kotlin component. Input: rule revisions, manual overrides, bank zone, reference date, generation window. Output: desired event instances and validation diagnostics. No Android calls or network. Calendar and notification delivery are separate; a financial event exists even if no reminder is enabled.

EventType: REQUIRED_PAYMENT, STATEMENT_ISSUED, BILLING_CYCLE_START, GRACE_END, CASH_LIMIT_RESET, INTEREST_ACCRUAL, SERVICE_FEE, CUSTOM.
MonetaryRole: MANDATORY, ALTERNATIVE_TARGET, INFORMATIONAL, NON_MONETARY. Mandatory amount can be unknown. Grace target and minimum amount share alternativeGroupKey when they refer to same statement, only minimum is mandatory unless user supplies other actual obligation. Do not assume grace covers all operations.

## 2. Recurrence grammar
Recurrence: OneOff(date), Monthly(day, missingDay=CLAMP|SKIP), LastDayOfMonth, Weekly(anchorDate, intervalWeeks, weekdays), EveryNDays(anchorDate, interval), ExplicitDates(list). Validate interval > 0, day 1..31, bounded lists. Rule revision has effective interval [from,to), stable series identity, bankZone, cutoff optional, amount and type. Monthly day 31 in February uses explicit policy; last day handles leap year. Business-day adjustment only NONE or explicit user-supplied date override in MVP; no guessed holiday calendar. Custom event uses same recurrence grammar.

OccurrenceKey: stableSeriesId + canonical nominal occurrence slot (monthly year-month, weekly/daily anchor-based nominal date, explicit entry UUID). Not derived from adjusted dueDate, not from amount or revision. Revision edits may change recurrence family: create new series if slots cannot be mapped unambiguously; retire old future occurrences. Manual exception keyed by slot, survives regeneration; manual financial history preserved.

## 3. Reconciliation
Generate default 12 months ahead (configurable in design; bounded), include next required date and unpaid past instances, preserve history. Iterate nominal slots, apply missing-day policy and explicit override, then filter by effective dates. Snapshot calculation input in one transaction; validate before mutation.
- Upsert desired unconfirmed future events by occurrenceKey.
- Keep past/factual/linked/overridden occurrences; do not replace payment records.
- On rule change retire future unlinked obsolete instances and cancel jobs; flag conflicts for user review if linked or manually overridden.
- Recompute planned remaining amount and status using active payment links; no double count.
- Write scheduler_outbox entries atomically with changes; scheduler processes after commit.
- On retry same keys produce same rows; no duplicate events/jobs. On rolling horizon extension run on app start and periodic best-effort work.

Example: rule monthly 31 CLAMP, Feb 2027 => Feb 28; Feb 2028 => Feb 29. Change Feb 2028 due date to March 1 as explicit override => same Feb slot; not duplicate March slot.

## 4. Time semantics
Bank deadline LocalDate in bank zone; if cutoff unknown, possible overdue after end of that bank day, not bank-confirmed. User reminder local time policy FOLLOW_DEVICE or FIXED_ZONE; date derived from event's nominal due date minus offset in calendar days, then resolved in selected reminder zone. UI shows bank zone separately. DST gap: shift forward by transition gap; overlap: use earlier offset consistently. Time change/reboot triggers reschedule. Clock injected for all logic. Quiet hours may cross midnight; policy defer to end or explicitly allow urgent alerts. Never silently move banking deadline.

## 5. Reminder scheduling
Rule resolution: explicit event override > obligation+type > obligation > global+type > global default. A more specific rule set replaces less-specific set, including explicit disabled set. Multiple distinct offsets/times within effective set. GenerationVersion invalidates stale jobs. Job states: PENDING, SCHEDULED, CLAIMED, DELIVERED, CANCELED, MISSED, BLOCKED; transitions conditional on active generation.
Android adapter: WorkManager for reconciliation/recovery and approximate delivery when acceptable; AlarmManager for user-justified exact reminders only after capability/permission checks. Check current Android policies during implementation; no assumption of exact delivery. A durable DB record is source of truth, OS registration is a recoverable projection.
Schedule bounded near-term horizon (initial proposal 30 days), next alert beyond horizon via periodic/app-start replenishment. Outbox action coalescing by job and version; use stable work/alarm identity. Alarm payload contains only job UUID/version, no financial details. Before delivery, reload current job, check event status/rules/version/permission, atomically claim with recovery lease. Stable notification ID replaces duplicate display on retries; exact-once delivery cannot be guaranteed across process death. Reconcile CLAIMED after lease and avoid unnecessary repeat alerts.
Snooze creates explicit replacement occurrence token, not arbitrary duplicate. Acknowledge nonmonetary event; monetary action opens amount/date confirmation. Cancel all redundant alerts after recorded completion; partial payment updates remaining amount and keeps relevant alerts. Notification actions use immutable PendingIntent where applicable; validate IDs and current state.

## 6. Recovery and privacy
On app start, boot (after unlock when needed), time/zone change, permission changes and periodic maintenance, compare desired schedule with stored intents; re-register missing/stale jobs. Android force-stop, battery restrictions and denial may prevent delivery; show diagnostics, no claim of guarantee. Missed reminder policy: one summarized catch-up after unlock if enabled, never flood past alerts. No exact alarm permission requested unless feature warrants it. Lock-screen default generic text, no lender, amount or balance. Deep link requires app unlock when protection enabled. No financial information in worker input or logs.

## 7. Test matrix
Recurrence: leap/non-leap, 29/30/31, skip/clamp, interval anchors, window edges, effective dates, manual override, invalid rule.
Reconciliation: repeated runs, editing future schedule, linked payment, archive/delete, crash after commit, duplicate command, stale generation.
Time: DST gap/overlap, travel, manual clock change, quiet hours across midnight, unknown cutoff.
Platform: permission denied/revoked, reboot, process death, force-stop, snooze, partial/full payment, notification privacy, repeated receiver, missed horizon.
Tests use fake clock and synthetic values; real-device results required before release.
