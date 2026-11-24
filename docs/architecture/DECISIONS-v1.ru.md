# Архитектурные решения и порядок реализации

Status: proposed, pending review.

| ADR | Decision | Rationale / tradeoff |
|---|---|---|
| 001 | Native Kotlin + Compose + Material 3 | Android focus and consistent brand; pin compatible versions at bootstrap |
| 002 | Local-first Room + repository ports | offline and testable; encryption requires separate verified implementation |
| 003 | Pure Kotlin financial and event engines | deterministic JVM tests; no Android dependency |
| 004 | Integer minor units, decimal rates | exact money; currency/rounding explicitly tracked |
| 005 | Append-only snapshots and payment corrections | audit history; added storage and query complexity |
| 006 | Rule + occurrence + reminder job separation | flexible bank conditions and reproducible calendar |
| 007 | Durable scheduling outbox | recoverable OS side effects; at-least-once reconciliation |
| 008 | Incremental module extraction | avoid premature Gradle complexity |
| 009 | No compulsory account/network in MVP | privacy and offline; manual entry and reconciliation |
| 010 | RU/EN, accessibility and semantic colors | accessible consistent UX |
| 011 | Bank-confirmed data separate from estimates | avoid misleading accuracy, show incomplete inputs |
| 012 | Explicitly defer bank integrations and AI import | risk and scope control |

## Implementation order
1. Approve requirements and data/event/security design; choose supported SDKs, dependency versions, DB encryption and backup policy. Confirm full branding assets availability.
2. Bootstrap Android/Gradle, theme, CI with debug assembly, lint/unit tests. No invented build status.
3. Domain vertical slice: money, obligations, snapshots, validation, tests; Room schema v1 and repository with migrations strategy.
4. UI vertical slice: list, editor, detail, dashboard; offline persistence and RU/EN.
5. Payments and reconciliation with explicit allocation, correction and calendar links.
6. Event engine: recurrence, exceptions, horizon, generation and tests; calendar UI.
7. Reminder outbox, scheduling, permissions and diagnostics; test on devices.
8. Security controls, storage and backup gates, accessibility and regression tests. No real data release before storage design and verification.
9. MVP release after acceptance; advanced analytics and strategies as subsequent phase.

## Open decisions
- minSdk, targetSdk, compileSdk, AGP, Kotlin, JDK, Gradle, CI runner: verify current official compatibility and distribution rules during bootstrap.
- DB encryption library and migration/key recovery policy; app lock vs background scheduler metadata.
- Calendar horizon, reminder horizon and missed-alert policy (defaults proposed, not final).
- initial currency list/metadata version and optional lender rules; manual values preferred.
- applicationId proposal io.github.ketarro.credivaro; verify ownership and store uniqueness before public distribution.
- detailed screen flows/wireframes, branding import, legal clearance, release process.
- contribution policies, issue labels and branch protection require maintainer approval.

## Review checklist
No cycles in dependencies; immutable past facts; valid foreign keys and currency constraints; deterministic calendar; durable scheduling; privacy threat model; no invented bank logic; tests mapped to requirements; no premature claim of production readiness.
