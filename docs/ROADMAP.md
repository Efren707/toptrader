# Planning Roadmap & Status

> Last updated: 2026-07-30 (Milestone #12 UI/UX Polish Pass complete — US-10–US-14 merged via PR #41/#43/#45/#47/#49; pre-production security review started, see Current focus)
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**Milestone #12 (UI/UX Polish Pass) is complete** — all 5 stories (US-10–US-14) merged, closing out the manual test pass of the running app. US-14 (redesign the stock details page, issue [#39](https://github.com/Efren707/toptrader/issues/39)) was the last: a 2-column layout with a position-stats block (equity, today's/total return, average cost basis, shares owned, portfolio diversity) and `TradeForm` collapsed into a single instance with an internal Buy/Sell toggle and a full Review Order confirm step, merged via PR [#49](https://github.com/Efren707/toptrader/pull/49).

Every MVP user story (US-1–US-9) plus this polish pass is now done. AWS deployment is already architected (ADR 0005/0006/0014/0016/0017) but execution hasn't started — before sequencing it, we paused (2026-07-30) to run a **pre-production security review**, since going live means real user accounts (emails/passwords) on the public internet, not just an internal demo. 8 of 9 checklist items are done; the user has confirmed all remaining items get implemented (not deferred). **Next session: the password-reset/email-verification flow — the last checklist item — before sequencing AWS deployment execution.**

Two small things surfaced while verifying CSP against the real build, noted here so they don't get lost (not blocking, not yet actioned):
- `frontend/src/environments/environment.ts` has a placeholder prod `apiUrl` (`https://api.toptrader.example`) that doesn't match the real domain used elsewhere in the docs (`https://api.toptrader.com`) — needs reconciling once the domain is actually registered.
- When the frontend's initial session check (`checkSession()` in the app initializer) fails outright (network error, wrong/unreachable API origin), the app renders a blank page instead of falling back to a logged-out view — worth a look before going live, separate from the CSP work.

### Pre-production security checklist

Compiled from a full audit (docs/ADRs vs. actual backend/frontend/CI code) on 2026-07-29.

Done:
- [x] `gitleaks` CI scanning + custom ruleset for this app's secret shapes, `.github/dependabot.yml` (maven/npm/github-actions) — PR [#53](https://github.com/Efren707/toptrader/pull/53), amends ADR 0007
- [x] Dependency vulnerabilities: `bcprov-jdk18on` → 1.84 (PR [#51](https://github.com/Efren707/toptrader/pull/51)), `fast-uri` → 3.1.4 (PR [#52](https://github.com/Efren707/toptrader/pull/52)), `esbuild`/`@hono/node-server` pinned + `brace-expansion`/`tar` audit-fixed (PR [#54](https://github.com/Efren707/toptrader/pull/54)) — frontend at 0 known vulnerabilities
- [x] Explicit `application-prod.properties` + explicit actuator lockdown — committed prod profile (`spring.profiles.active=prod`) with explicit CORS origin (no unsafe localhost fallback), stack-trace suppression, actuator health-only/no-details, `jpa.show-sql=false`; secrets/DB config stay env-var-only, never committed — see [ADR 0032](./adr/0032-prod-config-shape.md)
- [x] Session timeout/fixation — explicit `.sessionManagement(session -> session.sessionFixation().migrateSession())` in `SecurityConfig.java` (previously implicit default), explicit `server.servlet.session.timeout=30m`
- [x] Frontend 401/403 handling consistency — route-guard coverage was already fine (parent-level `authGuard` covers all protected children, `checkSession()` blocks bootstrap via `provideAppInitializer` so no race on refresh); the real gap was a mid-session 401 (e.g. after the new 30m timeout) going unhandled. New `session-expired.interceptor.ts` clears `currentUser` and redirects to `/login` on any 401 outside `/auth/login`, `/auth/register`, `/auth/session` — manually verified in-browser (session-cookie deletion → redirect; wrong password on `/login` still shows inline, no redirect loop)
- [x] Logging/PII guard — no logging framework exists yet so nothing is at risk today; recorded the binding policy for whenever logging is added (never log full bodies, secret-carrying DTOs must mask `toString()`, log identifiers not object graphs) — see [ADR 0033](./adr/0033-logging-pii-policy.md)

Done (continued):
- [x] CSP directives — finalized and empirically verified against the real production build output; required disabling Angular's critical-CSS inlining (`inlineCritical: false` in `frontend/angular.json`) to avoid needing any `'unsafe-inline'`/hash/nonce exceptions — see `docs/architecture/security-architecture.md`
- [x] General API rate limiting — implemented per [ADR 0034](./adr/0034-api-rate-limiting.md): `RateLimitGroup` (matchers/keying/thresholds enum) + `RateLimitFilter` (bucket4j-caffeine, wired into `SecurityConfig`'s filter chain after `CsrfCookieFilter`), `429` + RFC 7807 body + `Retry-After`. Along the way found and fixed a real regression the feature introduced: the filter ran before Spring Security's authorization check, so anonymous/`@WithMockUser` requests hit an unchecked cast in `authenticatedUserId()` and threw `ClassCastException` — all 6 `QuoteControllerTest` tests were failing. Fixed by treating a non-`UserPrincipal` context as "pass through, let the real auth check reject it" instead of crashing. New `RateLimitFilterTest` (3 tests: register threshold + response shape, quote per-user isolation, trade's shared buy/sell bucket) plus the full backend suite (50 tests) verified green after the fix.

Still open (ordered quickest-to-largest; each needs a decision: fix, or accept as a documented trade-off):
- [ ] Password-reset / email-verification flow — none exists; locked-out/forgetful real users have no self-service recovery

Full detail/evidence for each item: see the security review plan at the time it was written (`.claude/plans/before-moving-on-to-inherited-haven.md`, local to this machine, not repo-tracked).

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
