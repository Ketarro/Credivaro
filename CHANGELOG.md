# Changelog

## [Unreleased] — v0.1.0-alpha
No published release.

- CI #70: repair stale scroll action on fixed Back button in discard test; assertions preserved.

### Build 16 event engine (versionCode 16)
- Pure deterministic recurrence, versioned nominal slots, manual overrides, bounded bank-date generation and non-mutating reconciliation plan. Map known payment terms without invented dates; all eight event types and monetary roles. 24 JUnit methods and six CAL self-checks (32 total). CI pending; no event storage, calendar UI, reminders or release.
### Build 15 forms (versionCode 15)
- Local lender picker, exact display grouping, inline errors, persistent form footer, saved explicit payment frequency/day policy for loans/installments; validation, legacy codec, encrypted roundtrip and UI tests. CI pending; events/reminders not generated, no release.
### Build 14 form ergonomics and persistent test signing (versionCode 14)
- Material calendar fields, bank-day shortcuts and bounds, localized currency labels, safe currency change, exact unallocated preview; calendar unit/UI regression.
- Owner authorized persistent alpha signing on owner-branch CI after tests; separate signing job and required signed upgrade matrix, final verified artifact only after success. CI pending; no release.
### Build 13 UX candidate (versionCode 13)
- Hybrid dashboard, fast actions, credit filters, detail tabs, compact forms, discard confirmation and append-only correction/cancellation UI; updated UI regressions. CI #65 SUCCESS (28/28 instrumentation); temporary debug APK available, physical acceptance and persistent signing pending. No release or schema changes.
### Build 12 diagnostic candidate (versionCode 12)
- Payment model, exact allocation, idempotent encrypted storage, additive Room v1→2→3, RU/EN payment entry/history.
- API26 IME/navigation regression fixed (#54→#55). Append-only correction repository; schema exports verified, CI #57 green. Correction UI, signed upgrade, recovery/review and manual acceptance pending.
- 26 one-button synthetic diagnostics, device report, 44-case manual checklist; CI #61 green after API26 retry; 26/26 diagnostics on each API. Clean recompilation requested; no signing or test criteria changes.
- CI #62 clean rebuild PASS; owner partial CLEAN device acceptance, M01/M04/M08/M09 PASS, UX feedback recorded; no code change, signed upgrade remains unverified.
### Build 11 stage 1
- Debug self-check 10 cases; CI #49/#50 green, physical self-check 10/10, manual acceptance deferred.
### Build 10
- Startup diagnostics and locale/theme fixes, CI #39/#40, owner 28/31.
### Build 9
- Encrypted persistent storage, signed update CI #31/#32.
### Build 8 and foundation
- Android preview, RU/EN, themes, Signal Frame; owner 21/21; initial project brief, roadmap, guidance, security policy and Russian/English descriptions.
### Documentation 2026-11-25
- Standalone rules, roadmap, handoff, task checkpoints, testing and change history. No source, schema, signing or CI change; no release.

- UX research and three proposed navigation concepts recorded in R09.06; no application changes or release.
