# Planning Roadmap & Status

> Last updated: 2026-07-21
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**US-4 (Look up a stock quote) is in progress.** Finnhub client wiring (ADR 0003) is done, built step-by-step in mentor mode: `toptrader.finnhub.base-url` default in `application.properties`; `MarketDataConfig`'s `RestClient` bean, with a `FinnhubTokenInterceptor` appending Finnhub's required `token` query param to every outgoing request and a required (no-default) `toptrader.finnhub.api-key` so a missing key fails fast at startup instead of silently calling with a placeholder; raw response records `FinnhubQuoteResponse`/`FinnhubCompanyProfileResponse` (both `@JsonIgnoreProperties(ignoreUnknown = true)`, since each Finnhub payload has more fields than we consume); and `FinnhubClient`, with `fetchQuote`/`fetchProfile` methods calling `/quote` and `/stock/profile2` respectively. Work is on branch `feature/us-4-quote-lookup`, not yet PR'd. **Next step:** `QuoteService` — call both `FinnhubClient` methods, detect the "ticker not found" case (Finnhub returns `c: 0` / an empty profile body rather than a clean error or 404), assemble the `Quote` DTO, and translate failures into the 404/"unknown ticker"/502/"provider unreachable" responses `openapi.yaml`'s `/quotes/{ticker}` contract already specifies — then wire up `QuoteController`. This is the first story in Milestone #9 (Market Data Integration). Mentor-mode coding collaboration continues per `CLAUDE.md`.

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed. Revisit once AWS deployment (post Milestone #11) is in place.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
