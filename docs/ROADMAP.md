# Planning Roadmap & Status

> Last updated: 2026-08-04 (password-reset flow implementation complete on `feature/password-reset-flow`, ADR 0036 — PR open for review, see Current focus)
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). The full pre-deployment security checklist (done items + evidence, remaining items) lives in [docs/pre-deployment-checklist.md](./pre-deployment-checklist.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

Every MVP user story (US-1–US-9) plus the UI/UX polish pass (Milestone #12) is done — see Completed milestones below. AWS deployment is architected (ADR 0005/0006/0014/0016/0017) but not started; before sequencing it, a **pre-production security checklist** is being worked through item by item (not deferred) — see [docs/pre-deployment-checklist.md](./pre-deployment-checklist.md) for the full list and evidence on closed items.

**Password-reset flow implementation is complete**, on branch `feature/password-reset-flow`, design decisions recorded in [ADR 0036](./adr/0036-password-reset-flow.md) (token storage/hashing/TTL, log-stub email sender scoped to non-prod, CSRF/rate-limit treatment, session invalidation on reset). Email verification remains a separate follow-up (tracked as its own item in [docs/pre-deployment-checklist.md](./pre-deployment-checklist.md)), not part of this branch.

Implementation sequence and progress:
- [x] Flyway migration `V3__create_password_reset_tokens_table.sql` — verified against real Postgres, full backend suite green
- [x] `PasswordResetToken` entity + `PasswordResetTokenRepository` (`com.toptrader.backend.auth`)
- [x] `com.toptrader.backend.email` package (kept separate from `auth` so it can serve future email use cases, e.g. transaction confirmations): `EmailSender` interface + `LogEmailSender` (`@Service @Profile("!prod")`) — full backend suite green, spotless clean
- [x] `PasswordResetService` (`com.toptrader.backend.auth`) — `resetRequest(email)` (enumeration-safe: generates/hashes the token before checking whether the user exists, only persists+emails when found) and `resetPassword(rawToken, newPassword)` (validates existence/unused/unexpired as one collapsed check, updates password hash, marks token used, expires the user's other active sessions).
- [x] Backend tests for `PasswordResetService` (`PasswordResetServiceTest`, pure Mockito unit test like `TradeServiceTest`) — 9 tests covering the 503-no-sender path, save+email-on-found, no-op-on-not-found, successful reset+mark-used, session expiry scoped to only the reset user, and the three collapsed-400 failure cases (not found/used/expired).
- [x] `AuthController` endpoints: `POST /auth/forgot-password`, `POST /auth/reset-password` — both delegate directly to `PasswordResetService`, return `204 No Content`
- [x] `SecurityConfig` wiring: CSRF exemption for both new paths (`ignoringRequestMatchers`), both added to `authorizeHttpRequests().permitAll()` (needed alongside the CSRF exemption since these are pre-session endpoints, not called out explicitly in the original plan but required for unauthenticated callers to reach them), new `RateLimitGroup.FORGOT_PASSWORD` (client-IP keyed, 5/hour, covers both paths, mirrors `REGISTER`'s shape)
- [x] Backend tests for the controller endpoints + `SecurityConfig` wiring — `AuthControllerPasswordResetTest`, 8 tests (enumeration-safe forgot-password behavior, all four reset-password token outcomes, both DTOs' field validation). Caught the new `RateLimitGroup.FORGOT_PASSWORD` bucket throttling the test suite itself (all 8 requests shared one client-IP key) — fixed the same way `RateLimitFilterTest` does, distinct `X-Forwarded-For` per test.
- [x] Frontend: `AuthService.forgotPassword`/`resetPassword` methods; `forgot-password`/`reset-password` standalone components (`frontend/src/app/features/auth/`, following `login`/`register` conventions); routes added to `app.routes.ts` behind `guestGuard` (kept guest-only after weighing the logged-in-with-a-stale-link edge case — accepted as out of scope for now, no other password-change path exists yet but this is rare). Forgot-password shows an inline enumeration-safe confirmation ("if that email is registered...") with the form left enabled, matching the backend's identical-response contract. Reset-password reads the token from the `?token=` query param (`ActivatedRoute` snapshot), shows an invalid-link state with no form when the token is missing, and on success shows an inline message + manual link to `/login` (no session gets created by this flow, so no auto-redirect). Also added a right-aligned "Forgot password?" link inline with the password hint on the login page.
- [x] Frontend tests — `forgot-password.spec.ts` (8 tests) and `reset-password.spec.ts` (8 tests, incl. the missing-token case), following `trade-form.spec.ts`'s pattern (real `HttpClient` + `HttpTestingController` against the actual `AuthService` methods, not mocked).
- [x] `SessionRegistry`/`HttpSessionEventPublisher` infra — `SecurityConfig` gained both beans plus `.sessionConcurrency(concurrency -> concurrency.sessionRegistry(sessionRegistry()).maximumSessions(-1))` (unlimited concurrent sessions, just tracked, no cap — nothing in ADR 0036 or the user stories calls for a single-session-per-user policy). `PasswordResetService.resetPassword` matches sessions by `User.getId()` via `sessionRegistry.getAllPrincipals()` rather than `UserPrincipal.equals()` (not overridden, so instance-based lookup would silently find nothing).
- [x] `pre-deployment-checklist.md` updated — password-reset item moved to Done with full evidence; email-verification split out as its own still-open item
- [x] Full suite verified green against real Postgres after a clean build: backend 75 tests (Flyway `V3` validated), frontend 53 tests / 15 files, lint clean
- [ ] PR [#89](https://github.com/Efren707/toptrader/pull/89) open for review, not yet merged

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
