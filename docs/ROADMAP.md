# Planning Roadmap & Status

> Last updated: 2026-07-24 (US-5 done end-to-end, not yet merged)
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**US-5 (Buy shares) is done end-to-end**, on branch `feature/us-5-buy-shares`, not yet merged.

Backend: Flyway migration `V2__create_trading_tables.sql` (`holdings` — unique on `(user_id, ticker)`; `transactions` — `side` CHECK-constrained to `BUY`/`SELL`, composite index on `(user_id, executed_at)` for the future US-8 history query); JPA entities `Holding`/`Transaction`; `HoldingRepository`/`TransactionRepository`; and `TradeService.buyStock`/`TradeController` (`POST /trades/buy`). Behavior: quantity validated before any external call; price fetched live from `QuoteService`; the user row is locked (`UserRepository.findByIdForUpdate`, `PESSIMISTIC_WRITE`) and cash sufficiency re-checked against that locked balance; the holding is upserted (new row, or weighted-average cost basis recalculated on top of an existing one); the transaction is inserted — all in one `@Transactional` method, so it's all-or-nothing. Response is `TradeResult` (transaction + updated cash balance + holding), matching `openapi.yaml`. Covered by `TradeServiceTest` (10 cases). `sellStock`/US-6 is explicitly out of scope for this story.

Frontend: `TradeForm` (shared component, `frontend/src/app/shared/trade-form/`) — quantity entry, then an explicit confirm step ("Buy N TICKER @ $price — Confirm?" per the US-5 acceptance criteria, no single-click execution), then the actual `POST /trades/buy` call only on confirm; handles cancel, in-flight disabling, server errors, and a post-trade summary/reset. Wired into `StockDetails` (`/stocks/:ticker`), reachable from the dashboard's quote search results. Covered by `trade-form.spec.ts` (9 cases) — the first real frontend test file in the repo; every other `.spec.ts` is still the CLI-generated stub.

Along the way, found and fixed a pre-existing gap that had silently made CSRF protection non-functional for *every* authenticated mutating endpoint (not just trades) since ADR 0007/0022: the CSRF cookie was never actually issued, Angular's built-in XSRF interceptor never fires for this app's cross-origin API calls, and the default token handler expected a masked value no standard SPA client sends. Fixed via `CsrfCookieFilter`, a custom frontend `xsrfInterceptor`, and switching to `csrf.spa()` — see [ADR 0026](./adr/0026-csrf-spa-token-handshake.md) for the full root-cause writeup.

**Next step:** Open a PR and merge `feature/us-5-buy-shares` to `main`. After that, US-6 (Sell shares) is next — the second half of Milestone #10 "Trading Core" per ADR 0020 — and can reuse `TradeForm`/`TradeService` (already `side`-aware) rather than building a parallel sell flow.

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed. Revisit once AWS deployment (post Milestone #11) is in place.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #9 (Market Data Integration)** — ✅ done. US-4 Look up a stock quote (PR [#18](https://github.com/Efren707/toptrader/pull/18) backend, [#19](https://github.com/Efren707/toptrader/pull/19) frontend).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
