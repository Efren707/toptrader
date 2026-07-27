# Planning Roadmap & Status

> Last updated: 2026-07-27 (US-8 implemented, PR pending)
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**US-8 (View transaction history) is implemented (backend + frontend), second story in Milestone #11 (Portfolio & Reporting).** Backend added `TradeService.getTransactions`/`GET /trades/transactions` (list, most recent first), reusing the existing `Transaction`/`TransactionResponse` plumbing already built for buy/sell and extracting a shared `toTransactionResponse` helper. Frontend gave transaction history its own routed page (`/transactions`) rather than folding it into the dashboard as previously expected — see [ADR 0029](./adr/0029-transaction-history-page-and-shared-navbar.md), which also covers extracting the dashboard's header into a shared `Navbar` component with an account-menu dropdown (the nav path to the new page). Logout was intentionally left out of that menu — `AuthService` has no `logout()` wired yet and it needs its own story. `docs/architecture/api-contract.md`/`frontend-architecture.md`/`openapi.yaml` and ADR 0028 updated to match.

**Next step:** open a PR for this US-8 work and get it reviewed/merged, then US-9 (profit/loss), the last story in Milestone #11. A dedicated logout story (wiring `AuthService.logout()` + verifying `POST /auth/logout` behaves correctly for a SPA) is now on the backlog too, surfaced while building US-8's account menu.

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed. Revisit once AWS deployment (post Milestone #11) is in place.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #9 (Market Data Integration)** — ✅ done. US-4 Look up a stock quote (PR [#18](https://github.com/Efren707/toptrader/pull/18) backend, [#19](https://github.com/Efren707/toptrader/pull/19) frontend).
- **Milestone #10 (Trading Core)** — ✅ done. US-5 Buy shares (PR [#21](https://github.com/Efren707/toptrader/pull/21)), US-6 Sell shares (PR [#23](https://github.com/Efren707/toptrader/pull/23)). Also found and fixed a pre-existing CSRF gap affecting every authenticated mutating endpoint — see [ADR 0026](./adr/0026-csrf-spa-token-handshake.md).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
