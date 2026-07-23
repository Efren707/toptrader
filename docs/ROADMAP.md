# Planning Roadmap & Status

> Last updated: 2026-07-22 (US-5 in progress)
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**US-5 (Buy shares) is in progress**, on branch `feature/us-5-buy-shares`. Done so far: Flyway migration `V2__create_trading_tables.sql` (`holdings` — unique on `(user_id, ticker)`, FK to `users`; `transactions` — `side` CHECK-constrained to `BUY`/`SELL`, composite index on `(user_id, executed_at)` for the future US-8 history query), plus JPA entities in the new `com.toptrader.backend.trading` package: `Holding` (`@ManyToOne User`, `BigDecimal averageCostBasis`, `@UpdateTimestamp updatedAt`) and `Transaction` (`@ManyToOne User`, nested `Transaction.Side` enum mapped via `@Enumerated(EnumType.STRING)` for the BUY/SELL column, `@CreationTimestamp executedAt`, no setters — it's an immutable audit-log row per `data-model.md`).

**Next step:** `HoldingRepository` and `TransactionRepository` (Spring Data JPA interfaces), then `TradeService`/`TradeController` implementing `POST /trades/buy` per `openapi.yaml` — price always re-fetched server-side via `QuoteService` at execution time, cash sufficiency re-checked at confirmation, all mutations (cash balance, holding, transaction insert) in one DB transaction. Mentor-mode coding collaboration continues per `CLAUDE.md`.

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed. Revisit once AWS deployment (post Milestone #11) is in place.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #9 (Market Data Integration)** — ✅ done. US-4 Look up a stock quote (PR [#18](https://github.com/Efren707/toptrader/pull/18) backend, [#19](https://github.com/Efren707/toptrader/pull/19) frontend).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
