# Planning Roadmap & Status

> Last updated: 2026-07-26 (US-7 done, Milestone #11 in progress; US-8 up next)
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**US-7 (View portfolio) is done, first story in Milestone #11 (Portfolio & Reporting).** Backend added `TradeService.getHoldings`/`GET /trades/holdings` (list, reuses the same market-value/unrealized-P&L calc as `getHolding`, factored into a shared `toHoldingResponse` helper). Frontend ended up diverging from the original plan of a separate `/portfolio` page (see `docs/architecture/api-contract.md` and `frontend-architecture.md`, both updated, and [ADR 0028](./adr/0028-portfolio-view-no-combined-endpoint.md) for why): holdings now display directly on the dashboard, below the existing username/cash-balance summary, in one column — the planned two-column layout (right column reserved for future US-8/US-9 widgets) is deferred until there's real content to put there. No unified `/portfolio` endpoint was needed — cash balance already came from `GET /auth/session`, so the dashboard just combines that with the new holdings list client-side. The now-unused standalone `features/portfolio` page/route was removed. Dashboard also shows total portfolio value (cash + sum of holdings' market value) as a `computed()` signal, closing the US-7 acceptance criterion for that figure.

**Next step:** US-8 (View transaction history) — needs a transaction list endpoint/service and a place to display it (likely the dashboard's reserved right column, per the above). US-9 (profit/loss) follows once US-8 lands.

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed. Revisit once AWS deployment (post Milestone #11) is in place.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #9 (Market Data Integration)** — ✅ done. US-4 Look up a stock quote (PR [#18](https://github.com/Efren707/toptrader/pull/18) backend, [#19](https://github.com/Efren707/toptrader/pull/19) frontend).
- **Milestone #10 (Trading Core)** — ✅ done. US-5 Buy shares (PR [#21](https://github.com/Efren707/toptrader/pull/21)), US-6 Sell shares (PR [#23](https://github.com/Efren707/toptrader/pull/23)). Also found and fixed a pre-existing CSRF gap affecting every authenticated mutating endpoint — see [ADR 0026](./adr/0026-csrf-spa-token-handshake.md).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
