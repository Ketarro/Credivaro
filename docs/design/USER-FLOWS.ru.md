# Пользовательские сценарии и UX-критерии

Status: proposed. Synthetic examples only. These checks supplement financial/architecture acceptance in PRs #2 and #3; no test run has occurred.

| ID | Scenario | Flow and expected behavior |
|---|---|---|
| UX-01 | First launch | Open dashboard without account/network request; empty state explains manual tracking and add action. No fake obligations. |
| UX-02 | Add loan | Type -> name/currency -> known balance and date -> optional terms -> review -> save; list and detail show saved data. Back preserves draft; cancel confirms if changed. |
| UX-03 | Add card | Independent minimum, statement, grace deadline, cash-limit fields; optional unknowns; no credit limit conflated with debt. |
| UX-04 | Add installment | Explicit payment count/date or manual schedule; zero interest not assumed. |
| UX-05 | Edit terms | Explain effective date and preserve historical facts. Invalid input retained for correction. |
| UX-06 | Partial payment | Record 400 against required 1000; show 600 remaining, status partial; no second debit. No bank transfer. |
| UX-07 | Unknown allocation | Record payment without distribution; do not invent principal reduction. Offer reconciliation. |
| UX-08 | Balance reconciliation | Add dated bank snapshot; keep old snapshot; estimate remains separately labeled. |
| UX-09 | Card grace event | Show minimum and target for grace in same group; not summed as two mandatory amounts. |
| UX-10 | Monthly schedule | Enter day 31; choose clamp/skip; preview upcoming dates before saving. |
| UX-11 | Event reminders | Set offsets 7/3/1 and time; preview dates; disable or override without duplicates. |
| UX-12 | Notification denied | Calendar works; diagnosis and link to system settings; no repetitive permission prompts. |
| UX-13 | Notification click | Navigate to existing event after protection check; canceled/deleted item yields safe fallback. |
| UX-14 | Archive / delete | Archive retains history and disables future events; delete is explicit, separate, confirmed action. |
| UX-15 | Different currencies | Separate totals and visible currency codes; no conversion without explicit rates. |
| UX-16 | Privacy | Mask values on dashboard, generic notification; no secret data in screenshots/preview fixture. |
| UX-17 | Accessibility | TalkBack reads title, source, amount and status logically; large font and keyboard do not block actions. |
| UX-18 | Localization/theme | RU/EN, light/dark/system; formatted money/date; no clipping of long translations. |
| UX-19 | Process death and duplicate submit | Durable saved state, form recovery where appropriate; operation IDs prevent duplicate writes. |
| UX-20 | Alpha feedback | About shows actual version and limitations; issue reporting guidance never requests real financial data. |

## First usability review
Maintainer manually checks initial navigation, adding/editing synthetic obligation, source labels, dark/light, large text, restart persistence and errors. Gather concrete changes rather than assuming interface accepted. Repeat with each alpha iteration. Use real data only after verified storage protection; test reminders on devices before reliance on deadlines.
