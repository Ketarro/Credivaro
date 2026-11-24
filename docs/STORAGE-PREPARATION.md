# Room and protected storage — Build 8 preparation
Status: proposed, not integrated. Keep the test workspace memory-only until storage verification. No real financial records. No production database file created by Build 8.

## First Room slice
Create :core:database with Room entities/DAO and schema export, :core:data repository with suspend transactional writes and observable Flow reads. Proposed schema v1:
- obligation: id UUID PK, title, lender nullable, currency code, bank zone, opened date nullable, status, notes nullable.
- terms_revision: id PK, obligationId FK, revision, effectiveFrom, type, validated versioned payload for type-specific fields, unique(obligationId, revision).
- balance_snapshot: id PK, obligationId FK, bankAsOf, recordedAt, nullable totalDebt/principal/availableCredit minor units, source, supersedesId nullable; index(obligationId, bankAsOf).
- Store amount as signed Long plus currency and fixed scale; rate as decimal text; no REAL monetary fields. Dates ISO, timestamps UTC. Unknown null, not zero. Foreign keys indexed. Payment/event tables deferred until their vertical slices.
- DAO: observe active obligations, load by UUID, insert/update atomic with revision and snapshot, explicit delete/archiving; do not overwrite past bank snapshots on reconciliation. Domain validation before commit; errors surface in UI. No fallbackToDestructiveMigration. Export schemas into source, test every migration, no database access on main thread.

## Encryption decision pending
Evaluate maintained Room-compatible encrypted SQLite provider (e.g. SQLCipher for Android, verify current versions, license, native ABI and Room integration before choosing). Generate a random per-install DB secret, wrap with Android Keystore AES-GCM using a unique nonce, store only wrapped envelope and version. Keystore does not encrypt Room on its own. Do not hardcode keys, reuse signing secret, log data, store plaintext passphrases or auto-delete on key failure. DB key lifecycle and app lock are separate. Backup disabled and sensitive domains excluded; verify platform behavior. Keep old workspace memory-only until encrypted persistence actually passes checks.

## Acceptance
1. Confirm supported Android/ABI, dependency and license compatibility; dependency pinning and schema review.
2. JVM mapping round trips (all types, optional/zero, dates/currency, rates), validation and error cases.
3. Room instrumented tests: transaction rollback, FK/unique constraints, reopen, migrations, snapshot history; inspect schema exports.
4. Verify on device encrypted file cannot be opened as plaintext SQLite; wrong key/tampering fails without replacing original DB; restart/reboot, key invalidation and storage error flows. No plaintext journal/temp/backup/log leakage. Test Android backup and transfer exclusions.
5. UI uses repository as source of truth; no sample data or misleading save success; loading/error/recovery states. Update signed alpha APK without deleting data; retain only synthetic data until security reviewed.
6. Only maintainer approves real-data usage or release; do not publish tag/release automatically.
