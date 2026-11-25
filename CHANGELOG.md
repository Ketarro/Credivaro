# Changelog

## [Unreleased] — v0.1.0-alpha
No published release.

### Build 17 preparation (versionCode unchanged)
- 2026-11-25 — R05.03: baseline CI [45541750692](https://github.com/Ketarro/Credivaro/actions/runs/45541750692) SUCCESS (debug/schema guard, API26/36 storage, signing and API26/28/29/30/36 compatibility; artifact 14990286480 is Build16, not Build17). Added isolated transactional explicit override edit/move/cancel/clear with optimistic snapshot, canonical slot regeneration, future 12-month bound, linked/historical/retired protection, atomic override and occurrence update; no balance changes. Expanded encrypted fixture test for stale edits, invalid past move, cancel/link rejection, clear, linked rejection and reopen. New CI pending; no local Android build. Outbox, correction lifecycle, parity/recovery, independent review and production activation remain open. No versionCode bump, 17/68.
- 2026-11-25 — CI [45540911782](https://github.com/Ketarro/Credivaro/actions/runs/45540911782) on df6966c: debug Check and build PASS, roadmap PASS, API26 and API36 storage jobs PASS; debug failed at schema guard because candidate v4 export untracked. Extracted actual compiler export from debug job 138958911349; identityHash 7a286fb7d862b063c029a85b65101087, all five preexisting v3 entities structurally identical, five added event entities. Existing schema files unchanged by this commit. Guard intact, rerun required. Production DB stays v3/versionCode16; no distribution; full recovery, independent review, override/outbox and correction lifecycle open. 17/68.
- 2026-11-25 — CI [45539272598](https://github.com/Ketarro/Credivaro/actions/runs/45539272598) FAIL on 68928ba. ROADMAP consistency PASS. Debug and API36 failed :core:database:compileDebugAndroidTestKotlin: EventMigrationCandidateTest lines 32/42 used Closeable.use on RoomDatabase which does not implement that receiver. Fixed explicit try/finally close for both handles; assertions unchanged. API26 failed dependency resolution of KSP 2.3.6 before compilation; retry unchanged dependencies (same KSP resolved in debug/API36), underlying repository/network cause unconfirmed. No instrumentation execution or signing; schema guard not reached. New CI required, do not interrupt it. Production DB/versionCode unchanged. 17/68, no acceptance. Compiler-generated v4 export still needs tracking.
- 2026-11-25 — R05.03/R03.06: added separate Room v4 compiler/fixture target EventMigrationCandidateDatabase, five typed entities and explicit additive migration 3→4; adjusted DDL to named unique indexes matching Room. Instrumentation test builds populated actual v3 using existing Room implementation and same synthetic key, migrates, checks prior obligation/payment/zero, integrity/FK, event replay/link and reopen. Production StorageDatabase remains v3; versionCode remains 16. New Room export must be captured from compiler (schema guard may fail until tracked); guard unchanged. Fresh Room schema lacks SQL CHECK constraints present in migrated candidate, so repository enforcement/parity needs review before activation. CI 45539025297 in progress at inspection; earlier CI cancelled by subsequent pushes. No verified new APK. 17/68. Next: capture compiler schema, validate migration/recovery and parity, finish override/outbox and only then activate v4.
- 2026-11-25 — R05.03: isolated transactional event repository candidate added (append/replay rules, generation/reconciliation, soft retirement, explicit same-owner/same-currency mandatory-payment links; corrected payments rejected, protected conflicts stop writes). Synthetic encrypted repository test covers replay, overlap/conflicting revision rejection, linking and reopen. No production Room registration, no outbox or manual override editing yet, no versionCode bump. CI 45538427301 was in progress; 45538015908 cancelled by subsequent push. Tests for this commit pending. Roadmap remains 17/68. Next: actual v3 fixture, Room mapping, migration, correction-aware link lifecycle, override editing and outbox.
- 2026-11-25 — R05.03 / Build17 preparation: added versioned typed EventCodec for all recurrence shapes and event payloads, exact minor units as decimal strings, canonical UUID/date decoding, unknown version rejection; Android instrumentation roundtrip and invalid-payload tests added. Previous candidate CI 45538015908 in progress at inspection; new tests not yet verified. No Room activation or versionCode change. Next: transactional repository, same-owner payment links, actual v3 fixture and migration schema. 17/68 unchanged.
- 2026-11-25: Build 16 baseline c5aaad22 — CI [push](https://github.com/Ketarro/Credivaro/actions/runs/45536421242) SUCCESS, debug/storage API26/36, persistent signing, compatibility/upgrade API26/28/29/30/36, final signed artifact 14987124149. Physical acceptance, independent review and populated recovery remain open. Build 17 preparation: feat/build17-event-storage, R05.03, additive candidate DDL and isolated encrypted fixture test; NOT wired to Room, versionCode remains 16, no new APK authorized. Next: typed codec/DAO, transactional reconciliation and link validation, actual v3 fixture and compiler-exported v4 schema, migration/recovery evidence and independent review. No checklist closure.

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
