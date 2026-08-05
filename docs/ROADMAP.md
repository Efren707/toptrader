# Planning Roadmap & Status

> Last updated: 2026-08-04 (email verification at signup started — ADR [0037](./adr/0037-email-verification-at-signup.md), branch `feature/email-verification-at-signup` — see Current focus)
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). The full pre-deployment security checklist (done items + evidence, remaining items) lives in [docs/pre-deployment-checklist.md](./pre-deployment-checklist.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

Every MVP user story (US-1–US-9) plus the UI/UX polish pass (Milestone #12) is done — see Completed milestones below. AWS deployment is architected (ADR 0005/0006/0014/0016/0017) but not started; before sequencing it, a **pre-production security checklist** is being worked through item by item (not deferred) — see [docs/pre-deployment-checklist.md](./pre-deployment-checklist.md) for the full list and evidence on closed items.

**Password reset is done and merged** (PR [#89](https://github.com/Efren707/toptrader/pull/89), design in [ADR 0036](./adr/0036-password-reset-flow.md)): token generation/hashing/TTL, forgot-password/reset-password endpoints and frontend, and session invalidation of a user's other active sessions on reset via a new `SessionRegistry`. Full evidence lives in `docs/pre-deployment-checklist.md`'s Done section.

**Dev workflow formalized and merged** (PR [#91](https://github.com/Efren707/toptrader/pull/91)): planning-before-coding, branching, commit cadence, testing-before-done (incl. manual UI smoke test), and full-suite-before-PR are now written up in `CONTRIBUTING.md` and `CLAUDE.md`'s "Feature workflow" section, rather than living only in conversation. Repo is also now configured to only allow merge-commit merges (squash/rebase disabled) so the documented merge strategy is enforced structurally, not just by habit. Process-only change, doesn't affect the checklist below.

**Next up on the still-open checklist (quickest-to-largest):**
1. **Email verification at signup — in progress**, on branch `feature/email-verification-at-signup`, design in [ADR 0037](./adr/0037-email-verification-at-signup.md) (no login gate; verification is informational only, mirrors ADR 0036's password-reset token pattern). Broken into 12 tracked steps:
   - [x] `V4` migration: `email_verification_tokens` table + `User.emailVerifiedAt` column
   - [x] `EmailVerificationToken` entity + repository
   - [x] `RegistrationService` sends a verification email on signup (non-blocking if no `EmailSender` bean, i.e. prod today)
   - [x] `EmailVerificationService.verifyEmail(token)`
   - [ ] `EmailVerificationService.resendVerification(email)` — **next up.** Decided: mirror `PasswordResetService.resetRequest`'s enumeration-safe shape (identical response whether the email exists or not); additionally skip issuing a token for already-verified users (no dead tokens), but the response must still look identical either way. Needs its own `Optional<EmailSender>` + `frontendOrigin` (unlike `verifyEmail`, this one builds a link) — generate the token the same way `RegistrationService.sendVerificationEmail` does.
   - [ ] `AuthController` verify/resend endpoints
   - [ ] `SecurityConfig` CSRF/`permitAll()` wiring for both new endpoints
   - [ ] `RateLimitGroup.EMAIL_VERIFICATION`
   - [ ] Backend tests for verify/resend flow
   - [ ] Frontend: register success verification notice
   - [ ] Frontend: verify-email route/component (success/expired/invalid + resend)
   - [ ] Frontend tests + manual browser smoke test, then full suite + PR
2. Logging and alerts in prod — zero logging framework exists yet; ADR 0008 plans the infra (logback → CloudWatch Logs, alarm → SNS email) but execution hasn't started.
3. Rollback strategy — the exception to "quickest-to-largest": the user wants an actual rollback plan instead of the current fix-forward posture, which needs its own dedicated session to design (likely revisiting ADR 0005/0006/0011/0017/0019).

Coding collaboration mode applies throughout (user implements, Claude guides/reviews) — expect a guided-review round per file.

Two small things surfaced while verifying CSP against the real build, noted here so they don't get lost (not blocking, not yet actioned):
- `frontend/src/environments/environment.ts` has a placeholder prod `apiUrl` (`https://api.toptrader.example`) that doesn't match the real domain used elsewhere in the docs (`https://api.toptrader.com`) — needs reconciling once the domain is actually registered.
- When the frontend's initial session check (`checkSession()` in the app initializer) fails outright (network error, wrong/unreachable API origin), the app renders a blank page instead of falling back to a logged-out view — worth a look before going live, separate from the CSP work.

Deferred out of the polish pass to a future backlog: general market news feed (dashboard + stock details — needs a new news data source), a profile page for editing/deleting an account, and a possible single-page auth redesign (login/register combined with client-side toggle instead of separate routes, black-background landing treatment, password-strength checkmark on the password field — floated 2026-07-28, not yet scoped).

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #9 (Market Data Integration)** — ✅ done. US-4 Look up a stock quote (PR [#18](https://github.com/Efren707/toptrader/pull/18) backend, [#19](https://github.com/Efren707/toptrader/pull/19) frontend).
- **Milestone #10 (Trading Core)** — ✅ done. US-5 Buy shares (PR [#21](https://github.com/Efren707/toptrader/pull/21)), US-6 Sell shares (PR [#23](https://github.com/Efren707/toptrader/pull/23)). Also found and fixed a pre-existing CSRF gap affecting every authenticated mutating endpoint — see [ADR 0026](./adr/0026-csrf-spa-token-handshake.md).
- **Milestone #11 (Portfolio & Reporting)** — ✅ done. US-7 View portfolio (PR [#25](https://github.com/Efren707/toptrader/pull/25)), US-8 View transaction history (PR [#27](https://github.com/Efren707/toptrader/pull/27)), US-9 View profit/loss (PR [#29](https://github.com/Efren707/toptrader/pull/29)).
- **Milestone #12 (UI/UX Polish Pass)** — ✅ done. US-10 Auth nav/logout (PR [#41](https://github.com/Efren707/toptrader/pull/41)), US-11 Consistent navbar (PR [#43](https://github.com/Efren707/toptrader/pull/43)), US-12 Search result row layout (PR [#45](https://github.com/Efren707/toptrader/pull/45)), US-13 Dashboard holdings list (PR [#47](https://github.com/Efren707/toptrader/pull/47)), US-14 Stock details redesign (PR [#49](https://github.com/Efren707/toptrader/pull/49)).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
