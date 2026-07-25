# Planning Roadmap & Status

> Last updated: 2026-07-24 (US-5 merged; US-6 up next)
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**US-5 (Buy shares) is done.** Backend: `TradeService.buyStock`/`TradeController` (`POST /trades/buy`) — price fetched live from `QuoteService` at request time (never client-supplied), user row locked (`PESSIMISTIC_WRITE`) and cash re-checked against that locked balance, `Holding` upserted with weighted-average cost basis on repeat buys, all in one all-or-nothing `@Transactional` method. `TradeServiceTest` (10 cases). Frontend: shared `TradeForm` (`shared/trade-form/`) — quantity entry, then an explicit "Buy N TICKER @ $price — Confirm?" step per the acceptance criteria (no single-click execution), then the actual API call only on confirm; handles cancel, in-flight disabling, server errors, and a post-trade summary/reset. Wired into a new `/stocks/:ticker` route (`StockDetails`), reachable from the dashboard's quote search. `trade-form.spec.ts` (9 cases) is the first real frontend test file in the repo — every other `.spec.ts` is still the CLI-generated stub, worth using this one as the template going forward.

Along the way, found and fixed a pre-existing gap that had silently broken CSRF protection for *every* authenticated mutating endpoint (not just trades) since ADR 0007/0022 — see [ADR 0026](./adr/0026-csrf-spa-token-handshake.md) for the full root-cause writeup (cookie never issued, Angular's built-in XSRF interceptor skipping this app's cross-origin calls, and a default token handler expecting a masked value no SPA client sends).

Merged via PR [#21](https://github.com/Efren707/toptrader/pull/21), 2026-07-24.

**Next step:** US-6 (Sell shares), the second story in Milestone #10 (Trading Core). Can reuse `TradeForm`/`TradeService` on the frontend (already `side`-aware) rather than building a parallel sell flow — needs a `TradeService.sellStock` backend counterpart (reject if quantity exceeds the held amount, decrease/remove the holding, credit cash) plus a way to actually reach a sell action in the UI (currently `TradeForm` is only ever invoked in `BUY` mode, from `StockDetails` — there's no entry point yet for selling an existing holding). Once US-6 lands, Milestone #10 closes and Milestone #11 (Portfolio & Reporting: US-7/8/9) starts.

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed. Revisit once AWS deployment (post Milestone #11) is in place.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #9 (Market Data Integration)** — ✅ done. US-4 Look up a stock quote (PR [#18](https://github.com/Efren707/toptrader/pull/18) backend, [#19](https://github.com/Efren707/toptrader/pull/19) frontend).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
