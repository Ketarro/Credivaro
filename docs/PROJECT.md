# Credivaro — project brief

Status: initial concept; detailed specification and architecture are pending review.

## Goal
Help people with multiple debts understand obligations, plan expenses, avoid missed payments and compare repayment scenarios. Independent of lenders; no loan issuance or automatic payment execution.

## Core users and flows
A user adds an obligation, enters its terms, checks upcoming events, records payments, reconciles balances with lender data, and reviews forecasts. Each product type has its own fields.

## Functional scope
- Loans: principal, currency, interest model, rates, dates, schedule, outstanding balance, fees, early repayment terms.
- Cards: credit limit, balance, statement cycle, minimum payment, grace-period rules, cash withdrawal limits/reset rules, fees and actual bank statement data.
- Installments: installments, dates, principal, fees and custom terms.
- Events: mandatory payments, statement dates, grace-period expiry, cycle starts, limit resets, interest/fees and custom recurrence.
- Reminders: multiple offsets, time of day, snooze, acknowledgment, privacy options, rescheduling and duplicate prevention.
- Analytics: totals per currency, planned cash outflows, historical payments, forecast vs confirmed data, repayment scenarios (avalanche, snowball, grace-period priority).

## Nonfunctional requirements
Offline-first; no required accounts; clear permissions; data minimization; protected local storage and backups; accessible light/dark UI; RU/EN localization. Treat notification delivery as best effort under Android restrictions and surface permission problems.

## MVP boundaries
Manual entry, obligation list, payments, dashboard, calendar and notifications. No automatic bank integrations, AI parsing, automated transfers, or guaranteed bank-specific calculations in the MVP.

## Acceptance and correctness
User can edit and close obligations; partial payments and corrections preserve history; calendar recalculates without duplicate events; forecast assumptions are visible; currency totals are not mixed without an explicit conversion; data persists across restarts. Detailed rules, acceptance tests, and threat model must be approved before implementation.
