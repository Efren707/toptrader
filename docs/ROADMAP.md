# Planning Roadmap & Status

> Last updated: 2026-07-21
> This file tracks *where we are* — a lean, current-state view. Full narrative detail for completed phases/milestones lives in [docs/planning-history.md](./planning-history.md). For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each milestone below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**US-3 (Receive starting virtual cash) is done.** Audit against `acceptance-criteria.md` confirmed the $500 grant itself was already correct (exactly once, at registration, never re-applied on login) — the gap was that the balance was never surfaced back to the user. Closed by adding `cashBalance` to the `UserSummary` contract (`openapi.yaml`, backend record, frontend interface) and rendering it on `Dashboard`, plus `jsonPath("$.cashBalance")` assertions added to the existing register/login integration tests. Merged via PR [#16](https://github.com/Efren707/toptrader/pull/16), 2026-07-21. This closes out Milestone #8 (Auth & Account Foundation). **Next step:** start US-4 (Look up a stock quote), the first story in the Market Data Integration build-order group (Milestone #9) — will need the Finnhub client wiring per ADR 0003. Mentor-mode coding collaboration continues per `CLAUDE.md`.

## Deferred until deploy

- **Demo/showcase readiness** (originally Phase 5) — mechanism and content are already decided, see `docs/guides/demo-showcase-readiness-outline.md`. The remaining work needs a live URL to demo/screenshot, so it's blocked until the app is actually deployed. Revisit once AWS deployment (post Milestone #11) is in place.

## Completed milestones & phases

- **Phases 0–6 (Planning)** — ✅ done. Vision/requirements, research spikes, architecture docs, CI/CD & environment strategy, user-facing doc outlines (except the deferred item above), MVP scope freeze, GO decision (2026-07-17). Full detail: [planning-history.md](./planning-history.md).
- **Milestone #8 (Auth & Account Foundation)** — ✅ done. US-1 Register (PR [#11](https://github.com/Efren707/toptrader/pull/11) backend, [#12](https://github.com/Efren707/toptrader/pull/12) frontend), US-2 Log in (PR [#14](https://github.com/Efren707/toptrader/pull/14)), US-3 Starting cash balance (PR [#16](https://github.com/Efren707/toptrader/pull/16)). Full detail: [planning-history.md](./planning-history.md).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
