# Planning Roadmap & Status

> Last updated: 2026-07-27 (US-9 implemented, PR pending)
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**US-9 (View profit/loss) is implemented (frontend only), last story in Milestone #11 (Portfolio & Reporting).** Per [ADR 0030](./adr/0030-profit-loss-client-side-performance-page.md), P&L is computed entirely client-side (portfolio value − $500 starting balance, shown as both $ and %) — no backend changes. New `Performance` component/route (`/performance`, `authGuard`), reachable from the `Navbar` account menu alongside "Transaction history". `docs/architecture/frontend-architecture.md`'s routing table updated to match. `performance.spec.ts` covers positive/negative P&L rendering and the holdings-fetch error state, following the `TradeForm` spec's `HttpTestingController` pattern rather than the bare smoke test used by `Dashboard`/`Transactions`.

**Next step:** get the US-9 PR reviewed and merged — once it lands, Milestone #11 closes. A dedicated logout story (wiring `AuthService.logout()` + verifying `POST /auth/logout` behaves correctly for a SPA) is on the backlog, surfaced while building US-8's account menu.

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed. Revisit once AWS deployment (post Milestone #11) is in place.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #9 (Market Data Integration)** — ✅ done. US-4 Look up a stock quote (PR [#18](https://github.com/Efren707/toptrader/pull/18) backend, [#19](https://github.com/Efren707/toptrader/pull/19) frontend).
- **Milestone #10 (Trading Core)** — ✅ done. US-5 Buy shares (PR [#21](https://github.com/Efren707/toptrader/pull/21)), US-6 Sell shares (PR [#23](https://github.com/Efren707/toptrader/pull/23)). Also found and fixed a pre-existing CSRF gap affecting every authenticated mutating endpoint — see [ADR 0026](./adr/0026-csrf-spa-token-handshake.md).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
