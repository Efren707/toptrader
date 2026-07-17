# 0003 - Use Finnhub for market data

- Status: Accepted
- Date: 2026-07-16

## Context

TopTrader needs a source of stock quote data for US-4 (quote lookup), US-5/US-6 (buy/sell execution price), and US-7 (portfolio valuation). As a low-traffic, non-commercial solo demo project, the deciding factors are free-tier usability during development/demo (not just production traffic), redistribution terms permissive enough to display quotes to logged-in users, and simple REST integration from a Spring Boot backend.

## Options considered

- **Finnhub** - 60 requests/min free, no stated daily cap, claims real-time US quotes on the free tier, plain REST. Redistribution clause bars sharing data with third parties without approval, which reads as standard anti-scraping/anti-reselling language rather than a bar on displaying quotes to an app's own users — worth a support check before going live, but not a blocker for a demo.
- **Twelve Data** - 8 requests/min, 800/day, delayed 1-15 min. Ships an explicit market-status field (pre-market/regular/post-market), which would have solved the market-hours/stale-price open question directly. Free tier explicitly bars commercial use (paper risk only here, since the project generates no revenue). Rate limit is tight enough to be annoying during active development/testing.
- **Alpha Vantage** - 25 requests/**day** hard cap. Unusable even for solo dev/testing traffic; rejected outright.
- **Polygon.io (rebranded "Massive")** - free tier is end-of-day data only, 5 req/min, and provider is mid-rebrand with inconsistent documentation. Rejected as a weak, higher-risk fit.
- **IEX Cloud** - shut down August 2024, all keys deactivated. No viable free successor under that brand. Rejected as dead.

## Decision

Use Finnhub as the market data provider.

## Consequences

- Rate limit (60/min) is comfortable for solo development, testing, and demo/recruiter traffic; unlikely to be hit outside of a bug causing a request loop.
- Finnhub does not provide a market-open/closed status field. The market-hours/stale-price behavior question (tracked in `docs/ROADMAP.md` Phase 2 and affecting US-4/5/6 acceptance criteria) will need to be resolved by computing market-open/closed ourselves from a trading calendar (regular US market hours + holidays), not by trusting a field from the API response. This is a follow-up decision, not resolved by this ADR.
- The free-tier "real-time" claim for US quotes should be spot-checked empirically once a key is provisioned, since third-party sources disagreed with Finnhub's own docs on this point.
- Redistribution ToS language should be re-read if the project ever moves beyond a personal demo (e.g. if traffic or visibility grows) — not a concern at current scope.
