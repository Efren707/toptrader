# 0021 - Market open/closed status: hardcoded NYSE hours + static holiday list

- Status: Accepted
- Date: 2026-07-17

## Context

Finnhub (ADR 0003) has no market-open/closed status field, so quote freshness and the "as-of timestamp"/stale-price behavior required by US-4/5/6's acceptance criteria (`docs/requirements/acceptance-criteria.md`) needs the app to compute market-open/closed itself. This was flagged as an open item in ADR 0003 and carried through Phases 2-4 unresolved. It now blocks the Market Data Integration milestone (US-4), the first milestone after Auth & Account Foundation.

## Options considered

- **Hardcoded NYSE regular hours + a static holiday list** - compute Mon-Fri 9:30am-4:00pm ET in app code, plus a small checked-in data file (or constant list) of US market holidays and early-close days, manually updated once a year.
- **A trading-calendar library** - pull in a Java dependency to compute market hours/holidays automatically. No well-established, actively-maintained option was identified in the Java ecosystem comparable to e.g. Python's `pandas_market_calendars`; would add a dependency for a fairly narrow need.

## Decision

Hardcode regular NYSE hours (9:30am-4:00pm America/New_York, Monday-Friday) in application code, plus a small checked-in list of US market holidays and early-close days. All quotes and trades are US equities, so a single exchange calendar is sufficient — no per-ticker calendar logic needed.

## Consequences

- Zero new dependencies, zero added cost — consistent with this project's cost-first bias.
- The holiday list needs manual upkeep once a year (added to as new years' holidays are published); an accepted trade-off for a solo, low-traffic project.
- Does not account for exchange-specific trading halts or ad-hoc closures (e.g. weather, national days of mourning) — acceptable for a simulator's threat/accuracy model, not a real trading system.
- Implementation detail (where in the codebase this lives, exact data format for the holiday list) is left for when US-4 is actually built, not decided here.
