# Planning Roadmap & Status

> Last updated: 2026-08-05 (restructured tracking to lean on GitHub Issues/Milestones for hardening work, trimmed duplication out of this file and `pre-deployment-checklist.md` — see Current focus)
> This file tracks *where we are* — a lean, current-state view, pointers only. **How work is tracked:** every unit of work (MVP user story or pre-deployment hardening item) is a GitHub Issue on a [Milestone](https://github.com/Efren707/toptrader/milestones), closed by a PR; the PR holds full implementation detail, an ADR (`docs/adr/`) holds the "why" for notable decisions. Locally, [docs/planning-history.md](./planning-history.md) is the frozen narrative archive for completed phases/milestones, and [docs/pre-deployment-checklist.md](./pre-deployment-checklist.md) is the living checklist (done items + evidence links, remaining items → their Issues). Requirements detail lives in `docs/requirements/`.

## Current focus

Every MVP user story (US-1–US-9) plus the UI/UX polish pass (Milestone #12) is done — see Completed milestones below. AWS deployment is architected (ADR 0005/0006/0014/0016/0017) but not started; before sequencing it, the **[Pre-Deployment Hardening milestone](https://github.com/Efren707/toptrader/milestone/13)** is being worked through item by item (not deferred) — see [docs/pre-deployment-checklist.md](./pre-deployment-checklist.md) for the full list and evidence on closed items.

Since the last update: password reset (PR [#89](https://github.com/Efren707/toptrader/pull/89), [ADR 0036](./adr/0036-password-reset-flow.md)) and email verification at signup (PR [#95](https://github.com/Efren707/toptrader/pull/95), [ADR 0037](./adr/0037-email-verification-at-signup.md)) both merged, and the dev workflow itself got formalized into `CONTRIBUTING.md`/`CLAUDE.md` (PR [#91](https://github.com/Efren707/toptrader/pull/91), merge-commit-only now enforced structurally). Full detail for all three: `docs/pre-deployment-checklist.md`'s Done section.

[#97](https://github.com/Efren707/toptrader/issues/97) backend logging framework is implemented (PR open, pending merge) — [ADR 0038](./adr/0038-backend-logging-framework.md): Slf4j logging via SLF4J's fluent `addKeyValue` API wired into `LoginService`, `RegistrationService`, `PasswordResetService`, `EmailVerificationService`, `TradeService` (INFO on success, WARN on security-relevant failures), a catch-all handler in `GlobalExceptionHandler` that logs unhandled exceptions with a correlation id, and structured JSON output in prod via Spring Boot's native `logging.structured.format.file=logstash` support (no new dependency). #97 was narrowed from its original scope mid-session: the CloudWatch agent + `StatusCheckFailed`→SNS alarm pieces (ADR 0008) need an actual EC2 instance to attach to, which doesn't exist yet, so that half moved to [#102](https://github.com/Efren707/toptrader/issues/102), blocked on EC2 provisioning.

**Next up:** [#98](https://github.com/Efren707/toptrader/issues/98) rollback strategy — needs its own dedicated design session.

Also this session: task/milestone tracking got restructured so hardening work follows the same Issue → Milestone → PR flow the MVP user stories always used, instead of living only as prose here and in `pre-deployment-checklist.md` — new [Pre-Deployment Hardening milestone](https://github.com/Efren707/toptrader/milestone/13), new `hardening` issue template, milestones #8-12 closed out (were done but never closed).

Coding collaboration mode applies throughout (user implements, Claude guides/reviews) — expect a guided-review round per file.

Two small things surfaced while verifying CSP against the real build, noted here so they don't get lost (not blocking, not yet actioned):
- `frontend/src/environments/environment.ts` has a placeholder prod `apiUrl` (`https://api.toptrader.example`) that doesn't match the real domain used elsewhere in the docs (`https://api.toptrader.com`) — needs reconciling once the domain is actually registered.
- When the frontend's initial session check (`checkSession()` in the app initializer) fails outright (network error, wrong/unreachable API origin), the app renders a blank page instead of falling back to a logged-out view — worth a look before going live, separate from the CSP work.
- GitHub's Dependabot flagged 1 high-severity vulnerability on `main` (`https://github.com/Efren707/toptrader/security/dependabot/17`) — user decision (2026-08-05): put off for now, not yet actioned.

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
