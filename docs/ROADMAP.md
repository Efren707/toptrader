# Planning Roadmap & Status

> Last updated: 2026-07-23 (US-5 backend done, frontend pending)
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**US-5 (Buy shares) backend is done**, on branch `feature/us-5-buy-shares`, not yet merged. Done so far: Flyway migration `V2__create_trading_tables.sql` (`holdings` — unique on `(user_id, ticker)`; `transactions` — `side` CHECK-constrained to `BUY`/`SELL`, composite index on `(user_id, executed_at)` for the future US-8 history query); JPA entities `Holding`/`Transaction`; `HoldingRepository`/`TransactionRepository`; and `TradeService.buyStock`/`TradeController` (`POST /trades/buy`). Behavior: quantity validated before any external call; price fetched live from `QuoteService`; the user row is locked (`UserRepository.findByIdForUpdate`, `PESSIMISTIC_WRITE`) and cash sufficiency re-checked against that locked balance; the holding is upserted (new row, or weighted-average cost basis recalculated on top of an existing one); the transaction is inserted — all in one `@Transactional` method, so it's all-or-nothing. Response is `TradeResult` (transaction + updated cash balance + holding), matching `openapi.yaml`. Covered by `TradeServiceTest` (10 cases: new-holding success, existing-holding avg-cost recalculation, insufficient cash with no partial state change, zero/negative/null quantity rejected before any external call, unknown-ticker propagation, missing-user edge case, single quote fetch used as the execution price). `sellStock`/US-6 is explicitly out of scope for this story.

**Next step:** Frontend buy-shares flow (Angular) — ticker/quantity entry, the explicit confirmation step required by the US-5 acceptance criteria ("Buy N TICKER @ $price — Confirm?"), and wiring to `POST /trades/buy`. US-5 isn't done until this lands.

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed. Revisit once AWS deployment (post Milestone #11) is in place.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #9 (Market Data Integration)** — ✅ done. US-4 Look up a stock quote (PR [#18](https://github.com/Efren707/toptrader/pull/18) backend, [#19](https://github.com/Efren707/toptrader/pull/19) frontend).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
