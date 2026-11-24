# Agent and contributor instructions

## Scope
Read [HANDOFF.md](HANDOFF.md) and [ROADMAP.md](ROADMAP.md) before every development or review session; verify branch HEAD, PR, current CI and evidence. Select the stable roadmap item ID before implementation. ROADMAP.md in the active development branch is the canonical alpha execution plan; legacy roadmap in PR #1 is historical. Do not start implementation beyond the agreed scope. Keep main buildable once Android implementation begins; use focused branches and PRs. Preserve user changes and existing license. Never force-push or merge without approval.

## Versioning and release approval
The first planned application version is v0.1.0-alpha. Follow docs/VERSIONING.md. Iterate pre-release versions based on maintainer feedback; no automatic promotion to beta, RC or stable, no release tags or publication without explicit approval. Documentation revisions such as v1.0 are not application versions. Do not mark an alpha as released without actual build evidence and approval.

## Architecture
Kotlin / Compose / Material 3 are the intended stack. Keep pure financial rules separate from Android UI, persistence and scheduling. Start with necessary modules only; document architecture decisions. Use repository abstractions and explicit data migrations.

## Financial correctness
Use integer minor units or exact decimal arithmetic, never binary floating point for money. Store currency and rounding policy. Keep planned, actual, estimated and bank-confirmed data distinct. Support partial and extra payments, missing terms, configurable dates, end-of-month rules and rate changes. Do not invent lender rules or guarantee grace periods. Test calculations with synthetic cases and disclose assumptions.

## Privacy and security
Local-first. No compulsory login, telemetry, network calls or bank credentials by default. No real customer data, PINs, card numbers, tokens, keystores or personal statements in source, fixtures, logs or screenshots. Use Android Keystore for supported key protection; verify encryption and backup choices. Never send financial data off device without explicit opt-in.

## UI and notifications
Use Signal Frame brand colors, accessible contrast and scalable text; do not rely on color alone. Localize strings (RU/EN). Reminders must be configurable, handle time zones/reboots/permission denial and avoid duplicates. Marking a payment in the app is not a bank transfer.

## Quality gates
Add tests for financial rules, migrations, reminder scheduling and key UI flows. Run available build/lint/test tasks; report actual results and blockers. Do not claim an APK or passed checks without verification. Update docs and changelog with relevant changes. After each substantial change or test, update ROADMAP.md, HANDOFF.md and test evidence together, recompute counts from checkboxes and run `python3 scripts/check-roadmap.py`. Never check off an unverified item; reopen it on regression. Report progress and blockers. Scope changes require maintainer agreement. All mandatory roadmap items must be complete before the first published v0.1.0-alpha; obtain final maintainer confirmation, then publish exactly that alpha, not a stable release. Record actual tag/URL/SHA/versionCode after publication.

## AI_FRAMEWORK_V1 — mandatory entry and readiness gate
Read [.ai-framework/ENTRYPOINT.md](.ai-framework/ENTRYPOINT.md) before work, and follow its linked rules and project.json source map together with all existing instructions above. For current integration use INIT-001 / PROJECT_ZERO existing-project gap audit; read [checkpoint](.ai-framework/checkpoints/INIT-001.md). `project.json` active means configured, not approved. `project-zero.json` is draft with approval=null. Do not start or resume ordinary DEVELOP, REFACTOR, code-changing UX, or RELEASE until explicit approval of the concrete plan, `python3 .ai-framework/tools/verify.py --proposal` before approval, then `python3 .ai-framework/tools/verify.py --ready` passes and the task's actual Definition of Ready/permissions are checked. Do not bypass the gate or infer prior approval. Safe AUDIT, PLAN, DOCS and non-risky TEST may proceed in authorized scope; no migration of user data, publication, infrastructure change, main/merge, or risky action without required authority. One task ID, one checkpoint under `.ai-framework/checkpoints/`; use existing ROADMAP and HANDOFF, not a second roadmap. Existing format: manually verify priorities, dependencies, owner, criteria, checks, blockers and next step; the existing roadmap script checks counts, not factual readiness. Read applicable pinned PR documents as proposals until reconciled. No other agent is launched or granted access by these instructions.
