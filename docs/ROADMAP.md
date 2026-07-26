# Planning Roadmap & Status

> Last updated: 2026-07-25 (US-6 done; US-7 up next)
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**US-6 (Sell shares) is done.** Backend: `TradeService.sellStock`/`TradeController` (`POST /trades/sell`) — same locked-row/live-quote/all-or-nothing `@Transactional` shape as `buyStock`, rejects a sell that exceeds the held quantity, decreases (or removes, if fully liquidated) the `Holding`, credits cash. Also added `TradeService.getHolding`/`GET /trades/holdings/{ticker}` so the frontend can tell whether the user holds a given ticker at all — returns `Optional<HoldingResponse>` mapped to 200/404, a convention now established for query-style methods (as opposed to `buyStock`/`sellStock`, which keep throwing on every failure since they never have a legitimate empty-but-not-an-error outcome). Full rationale in [ADR 0027](./adr/0027-holdings-lookup-endpoint.md). `TradeServiceTest` expanded to cover `sellStock` and `getHolding` (25 cases total).

Frontend: reused the existing `TradeForm`/`TradeService` (already `side`-aware from US-5) rather than a parallel sell flow. `StockDetails` now fetches the user's holding for the current ticker alongside the quote and only renders the Sell `TradeForm` when one exists — the Buy form is always shown, Sell is conditional.

Along the way, a find-replace slip temporarily swapped `buyStock`/`sellStock`'s `findByIdForUpdate` (the row lock that serializes concurrent trades against the same user) for a plain `findById` — caught via the test suite (every test stubbing the locked lookup started failing) and reverted; `getHolding` correctly kept the plain `findById` since it's read-only.

**Next step:** US-7 (View portfolio), the first story in Milestone #11 (Portfolio & Reporting). Needs a holdings *list* endpoint (the single-ticker `GET /trades/holdings/{ticker}` from US-6 doesn't cover this — see ADR 0027) plus a new frontend page/route to show ticker, quantity, average cost, current value, and cash balance. US-8 (transaction history) and US-9 (profit/loss) follow once US-7 lands.

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed. Revisit once AWS deployment (post Milestone #11) is in place.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #9 (Market Data Integration)** — ✅ done. US-4 Look up a stock quote (PR [#18](https://github.com/Efren707/toptrader/pull/18) backend, [#19](https://github.com/Efren707/toptrader/pull/19) frontend).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
