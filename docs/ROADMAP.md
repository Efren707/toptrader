# Planning Roadmap & Status

> Last updated: 2026-07-28 (Milestone #12 UI/UX Polish Pass fully scoped: US-10 through US-14; US-10 done; issues #34/#35/#37/#39 open)
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**US-10 (Navigate between auth pages, and log out) is done, first story in Milestone #12 (UI/UX Polish Pass).** Login and register now cross-link via `<a routerLink>` (matching the pattern already used elsewhere, rather than a `router.navigate()` click handler), and the shared `Navbar` wordmark is now a link to `/dashboard`. Backend: `SecurityConfig.java`'s `.logout(...)` replaces Spring Security's default (a redirecting `/logout` handler) with an explicit `logoutUrl("/auth/logout")`, `deleteCookies("SESSION")`, and a `logoutSuccessHandler` that returns a bare `204` — matching `docs/architecture/openapi.yaml`'s documented contract. `/auth/logout` stays CSRF-protected (unlike `/auth/register`/`/auth/login`, exempted per ADR 0022) since it's only ever called while authenticated, consistent with the buy/sell CSRF handshake in ADR 0026. Frontend: `AuthService.logout()` POSTs to the endpoint and clears `currentUser`; `Navbar.onLogoutClick()` calls it, closes the account menu, and redirects to `/login` on success — after which `authGuard` sends any protected-route hit back to `/login`, same as an expired session.

Every MVP user story (US-1–US-9) is implemented; before deploying we're doing a UI/UX polish pass. AWS deployment (already architected — ADR 0005/0006/0014/0016/0017, not yet executed) stays blocked until this pass is done.

A manual test pass of the running app was scoped into **Milestone #12 (UI/UX Polish Pass)** — 5 stories:

1. **US-10 — Navigate between auth pages, and log out** — ✅ done, issue [#31](https://github.com/Efren707/toptrader/issues/31). Login/register cross-links, navbar logo → dashboard, logout wiring (frontend `AuthService.logout()` + backend `POST /auth/logout` fix). **Next step: implement US-11.**
2. **US-11 — Consistent, evenly-spaced navbar** — scoped, issue [#34](https://github.com/Efren707/toptrader/issues/34) open. Navbar is missing entirely on the stock details page; `.search-form`'s `max-width: 28rem` plus no `margin-left: auto` on `.account-menu-wrap` leaves a dead gap instead of filling the page width evenly.
3. **US-12 — Correct search result row layout and click behavior** — scoped, issue [#35](https://github.com/Efren707/toptrader/issues/35) open. Price dropped from the row (ticker left / company name right only); result button was missing `type="button"`, so it defaulted to `type="submit"` and re-triggered the search form against the already-cleared ticker field, flashing a "Required" error on click; layout rules targeted the unused `<li>` wrapper instead of the button.
4. **US-13 — Dashboard holdings as a list with day change, linking to stock details** — scoped, issue [#37](https://github.com/Efren707/toptrader/issues/37) open. Finnhub's `/quote` response already includes `dp` (percent change), silently discarded by `FinnhubQuoteResponse` (only `c`/`t` declared, unknown properties ignored); threading it through `Quote`/`QuoteService`/`HoldingResponse` powers the daily % change shown per holding. Touches backend + frontend, like US-10.
5. **US-14 — Redesign the stock details page (layout, position stats, trade form)** — scoped, issue [#39](https://github.com/Efren707/toptrader/issues/39) open. `TradeForm` already has a confirm step (satisfies US-5/US-6) but it's a bare quantity-only prompt, and the page renders two separate Buy/Sell forms instead of one with a toggle. Frontend-only — all needed data is already available client-side.

Deferred out of this pass to a future backlog: general market news feed (dashboard + stock details — needs a new news data source), a profile page for editing/deleting an account, and a possible single-page auth redesign (login/register combined with client-side toggle instead of separate routes, black-background landing treatment, password-strength checkmark on the password field — floated 2026-07-28, not yet scoped).

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #9 (Market Data Integration)** — ✅ done. US-4 Look up a stock quote (PR [#18](https://github.com/Efren707/toptrader/pull/18) backend, [#19](https://github.com/Efren707/toptrader/pull/19) frontend).
- **Milestone #10 (Trading Core)** — ✅ done. US-5 Buy shares (PR [#21](https://github.com/Efren707/toptrader/pull/21)), US-6 Sell shares (PR [#23](https://github.com/Efren707/toptrader/pull/23)). Also found and fixed a pre-existing CSRF gap affecting every authenticated mutating endpoint — see [ADR 0026](./adr/0026-csrf-spa-token-handshake.md).
- **Milestone #11 (Portfolio & Reporting)** — ✅ done. US-7 View portfolio (PR [#25](https://github.com/Efren707/toptrader/pull/25)), US-8 View transaction history (PR [#27](https://github.com/Efren707/toptrader/pull/27)), US-9 View profit/loss (PR [#29](https://github.com/Efren707/toptrader/pull/29)).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
