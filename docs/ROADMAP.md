# Planning Roadmap & Status

> Last updated: 2026-07-22
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**US-4 (Look up a stock quote) is done end-to-end, PR about to open.** Backend: Finnhub client wiring (ADR 0003, `MarketDataConfig`/`FinnhubTokenInterceptor`/`FinnhubClient`) plus `Quote` (DTO matching the `openapi.yaml` schema), `QuoteService` (normalizes ticker to uppercase before calling Finnhub, detects unknown tickers via price `== 0` or a missing profile name since Finnhub returns 200s rather than 404s for bad symbols, maps provider call failures to 502), and a thin `QuoteController` exposing `GET /quotes/{ticker}` behind the existing `anyRequest().authenticated()` rule. Frontend: quote lookup lives on the dashboard (not a standalone route, per US-5/6/7/8 not needing it elsewhere) — a navbar search box (`QuoteService.getQuote`, native input with left search icon + right clear icon, Enter-to-submit only, no visible button) with a results dropdown showing the matched ticker/company/price or a "No stocks found" state, closing and clearing on an outside click.

Test coverage: `QuoteControllerTest` (happy path, both not-found branches, both 502 branches, 401 when unauthenticated) with `FinnhubClient` mocked via `@MockitoBean`. Also fixed a pre-existing break: `MarketDataConfig`'s required `toptrader.finnhub.api-key` property had no test value, so every `@SpringBootTest` was failing context startup; added a dummy key to `src/test/resources/application.properties`. Full suite (15 tests), `spotless:check`, and `ng lint`/`ng build` all pass.

A real Finnhub key is still needed for manual/local verification against the live API (drop it into the gitignored `application-local.yml`, per ADR 0009) and eventually for production — neither blocks this PR.

**Next step:** open the PR for `feature/us-4-quote-lookup` (closes issue [#4](https://github.com/Efren707/toptrader/issues/4)), then move to the next story in Milestone #10 (Trading Core: US-5 Buy shares).

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed. Revisit once AWS deployment (post Milestone #11) is in place.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
