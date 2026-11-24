# План UI/UX реализации в alpha

Initial planned app version v0.1.0-alpha, no APK or tag yet. Documentation versions are independent. Maintainer controls alpha progression, beta/RC if desired, and stable release; no automatic publication.

## First working vertical slice (proposal)
- Android bootstrap, real launcher, Signal Frame theme (light/dark), RU/EN, About with actual versionName.
- Navigation for implemented screens only.
- Empty dashboard, add/list/detail/edit obligation, typed product form with essential known/unknown data.
- Local persistence only after storage/security choice; tests with synthetic data; no fake bank sync.
- Unit tests for money/validation, UI state, persistence and basic navigation; build/lint/assembleDebug in CI once configured.

Initial alpha can be smaller than the full MVP. Do not represent missing functionality as implemented. If initial persistence is not encrypted, clearly mark build experimental and synthetic-data-only; do not release for real financial use.

## Following alpha iterations
1. Payments, allocations, balance reconciliation, history/corrections.
2. Event rules and real calendar, manual bank exceptions, per-currency summary.
3. Reminder scheduling, system permissions, recovery, privacy and device tests.
4. Protection, backup decisions, accessibility, usability refinement, regressions.
5. Analytics/forecast/strategies as separately scoped additions, possibly still in alpha.

Order may change after maintainer feedback. Release number chosen according to actual changes, not phase number. Do not create tags or GitHub Releases without explicit approval.

## UI readiness
- All visible controls work or are explicitly disabled with reason; no demo data in live state.
- Synthetic inputs survive rotation/restart once persistence is implemented.
- Known and unknown amounts, bank snapshots and estimates distinguished.
- No UI or notifications imply actual bank payment.
- RU/EN, contrast, text scale, TalkBack, keyboard, back behavior verified.
- Version and limitations documented; tests report actual results, not planned success.
- Owner reviews visual direction before locking design. Full MVP and stable release are separate decisions.
