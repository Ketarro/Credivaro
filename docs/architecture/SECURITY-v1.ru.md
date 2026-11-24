# Модель угроз и безопасность v1.0

Design proposal; no security feature has been implemented or verified. Financial records are sensitive. Local-first, least privilege, no mandatory account or network, no advertising/telemetry in MVP.

## Threat model
| Threat | Mitigation / design decision |
|---|---|
| Lost unlocked phone / shared access | Optional app lock using platform authentication, re-lock timeout, safe task previews; no false promise of protection on compromised devices |
| Extracted database / backups | Design authenticated encryption at rest; evaluate vetted Room-compatible encrypted DB implementation; platform Keystore protects wrapping key, not DB by itself |
| Malicious notification observer | Generic lock-screen notification and default private payload, no financial IDs or amounts beyond opaque job ID |
| Logs/crash reports/screenshots | No lender labels, amounts, notes or raw SQL in logs; synthetic fixtures only; optional screen capture protection |
| Malicious import / altered backup | No import in MVP; future versioned authenticated format, size limits, schema validation, transactional restore |
| Dependency/supply chain | Pin versions, review updates, licenses, vulnerability checks; CI no secrets on untrusted PRs |
| Exported Android components | Minimize exported activities/receivers/providers, explicit intent handling and parameter validation |
| Rooted device or malicious OS | Out of guaranteed protection; document limitations |

## Storage and keys
Choose encryption implementation before storing real user data. Proposed random per-install DB key, wrapped with Android Keystore-backed authenticated encryption (AES-GCM with unique nonce and versioned envelope). Only wrapped key and nonce persisted; no plaintext key in source, DataStore, backup, logs. Evaluate provider compatibility, key rotation, corruption and invalidation recovery. Keep DB key availability and app-lock policy separate: scheduled jobs should use minimal generic metadata, or defer protected reads until unlock; no silent weakening of lock. Decide architecture with threat tests, not custom crypto. Keystore hardware backing not guaranteed. Never treat ordinary Room as encrypted.
Exclude financial DB, keys, settings with sensitive values and scheduling payloads from OS cloud/device-transfer backups using appropriate Android backup rules until secure restore exists. Confirm version-specific behavior in tests. Restore must never destructively reset DB on key error; display recovery options and explicit deletion confirmation.

## Authentication
Prefer BiometricPrompt with device credential if supported; user opt-in, configure timeout, cold start lock, background behavior, process death. An independent app PIN is a separate feature requiring salted slow KDF, throttling and recovery model; do not implement naive PIN or store it in plaintext. Notification deep links respect lock; app lock does not mean bank authentication. Optional screenshot protection, task-switcher masking. Accessibility must remain usable.

## Privacy and permissions
MVP no INTERNET permission unless a reviewed feature requires it. Notification permissions requested in context; exact alarms only where eligible and justified. No contacts/SMS/bank credentials/card number/CVV. No external analytics or cloud sync. Clear user explanation of local data, notification limits, deletion and no bank execution. User-generated export/backup later must be explicit and authenticated. Minimal public demo data only.

## Acceptance gates
- Threat-model review and dependency choice approved before implementation.
- Test encryption/key lifecycle, permission denial, backup exclusions, unlock, restore failure, process death and app upgrades.
- Inspect logs, crash stack context, screenshots and exported components for leakage.
- No financial data in CI artifacts; signing keys kept out of repo.
- Document residual risks and supported Android versions. No security guarantees before test evidence.
