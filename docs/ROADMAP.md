# Planning Roadmap & Status

> Last updated: 2026-07-26 (US-7 merged; US-8 up next)
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**US-7 (View portfolio) is done, first story in Milestone #11 (Portfolio & Reporting).** Backend added `TradeService.getHoldings`/`GET /trades/holdings` (list), reusing the market-value/unrealized-P&L calc factored into a shared `toHoldingResponse` helper. Frontend diverged from the original plan of a separate `/portfolio` page — see [ADR 0028](./adr/0028-portfolio-view-no-combined-endpoint.md): holdings display directly on the dashboard below the cash-balance summary instead, with a `computed()` total portfolio value (cash + sum of holdings' market value). `docs/architecture/api-contract.md`/`frontend-architecture.md`/`openapi.yaml` updated to match. Merged via PR [#25](https://github.com/Efren707/toptrader/pull/25), 2026-07-26.

**Next step:** US-8 (View transaction history), next story in Milestone #11. Needs a transaction list endpoint/service and a place to display it (likely the dashboard's reserved right column, once the two-column layout is actually built out). US-9 (profit/loss) follows once US-8 lands.

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed. Revisit once AWS deployment (post Milestone #11) is in place.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #9 (Market Data Integration)** — ✅ done. US-4 Look up a stock quote (PR [#18](https://github.com/Efren707/toptrader/pull/18) backend, [#19](https://github.com/Efren707/toptrader/pull/19) frontend).
- **Milestone #10 (Trading Core)** — ✅ done. US-5 Buy shares (PR [#21](https://github.com/Efren707/toptrader/pull/21)), US-6 Sell shares (PR [#23](https://github.com/Efren707/toptrader/pull/23)). Also found and fixed a pre-existing CSRF gap affecting every authenticated mutating endpoint — see [ADR 0026](./adr/0026-csrf-spa-token-handshake.md).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
