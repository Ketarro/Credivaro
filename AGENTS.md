# Agent and contributor instructions

## Scope
Read the project brief and roadmap before changes. Do not start implementation beyond the agreed scope. Keep main releasable; use focused branches and PRs. Preserve user changes and existing license. Never force-push or merge without approval.

## Architecture
Kotlin / Compose / Material 3 are the intended stack. Keep pure financial rules separate from Android UI, persistence and scheduling. Start with necessary modules only; document architecture decisions. Use repository abstractions and explicit data migrations.

## Financial correctness
Use integer minor units or exact decimal arithmetic, never binary floating point for money. Store currency and rounding policy. Keep planned, actual, estimated and bank-confirmed data distinct. Support partial and extra payments, missing terms, configurable dates, end-of-month rules and rate changes. Do not invent lender rules or guarantee grace periods. Test calculations with synthetic cases and disclose assumptions.

## Privacy and security
Local-first. No compulsory login, telemetry, network calls or bank credentials by default. No real customer data, PINs, card numbers, tokens, keystores or personal statements in source, fixtures, logs or screenshots. Use Android Keystore for supported key protection; verify encryption and backup choices. Never send financial data off device without explicit opt-in.

## UI and notifications
Use Signal Frame brand colors, accessible contrast and scalable text; do not rely on color alone. Localize strings (RU/EN). Reminders must be configurable, handle time zones/reboots/permission denial and avoid duplicates. Marking a payment in the app is not a bank transfer.

## Quality gates
Add tests for financial rules, migrations, reminder scheduling and key UI flows. Run available build/lint/test tasks; report actual results and blockers. Do not claim an APK or passed checks without verification. Update docs and changelog with relevant changes.
