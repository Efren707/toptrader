# Planning Roadmap & Status

> Last updated: 2026-07-22
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**US-4 (Look up a stock quote) is done.** Backend: Finnhub client wiring (ADR 0003, `MarketDataConfig`/`FinnhubTokenInterceptor`/`FinnhubClient`) plus `Quote` (DTO matching the `openapi.yaml` schema), `QuoteService` (normalizes ticker to uppercase before calling Finnhub, detects unknown tickers via price `== 0` or a missing profile name since Finnhub returns 200s rather than 404s for bad symbols, maps provider call failures to 502), and a thin `QuoteController` exposing `GET /quotes/{ticker}` behind the existing `anyRequest().authenticated()` rule. Frontend: quote lookup lives on the dashboard (not a standalone route, per US-5/6/7/8 not needing it elsewhere) — a navbar search box (`QuoteService.getQuote`, native input with left search icon + right clear icon, Enter-to-submit only, no visible button) with a results dropdown showing the matched ticker/company/price or a "No stocks found" state, closing and clearing on an outside click. Merged via PR [#18](https://github.com/Efren707/toptrader/pull/18) (backend) and [#19](https://github.com/Efren707/toptrader/pull/19) (frontend + ticker-normalization fix), 2026-07-22/23. This closes out Milestone #9 (Market Data Integration).

**Next step:** start US-5 (Buy shares), the first story in the Trading Core build-order group (Milestone #10) — buys execute at the price `QuoteService` fetches at request time, never a client-supplied price (see `openapi.yaml`'s `/trades/buy` description). Mentor-mode coding collaboration continues per `CLAUDE.md`.

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed. Revisit once AWS deployment (post Milestone #11) is in place.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #9 (Market Data Integration)** — ✅ done. US-4 Look up a stock quote (PR [#18](https://github.com/Efren707/toptrader/pull/18) backend, [#19](https://github.com/Efren707/toptrader/pull/19) frontend).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
