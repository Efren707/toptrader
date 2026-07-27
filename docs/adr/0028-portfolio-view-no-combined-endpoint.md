# 0028 - US-7 portfolio view: no combined endpoint, holdings folded into the dashboard

- Status: Accepted
- Date: 2026-07-26
- Supersedes: ADR 0012's "Portfolio + P&L" sub-decision (combined `GET /portfolio`); ADR 0013's `PortfolioService` mention under "State management"

## Context

ADR 0012 decided US-7 (and US-9) would be served by a single combined `GET /portfolio` response (cash, holdings, total value, P&L), and ADR 0013/`frontend-architecture.md` planned a matching dedicated `/portfolio` route, `PortfolioComponent`, and `PortfolioService`. When US-7 (view portfolio) was actually implemented, neither happened: no combined endpoint was built, and holdings ended up on the existing dashboard instead of a separate page. This ADR records why, since it reverses part of an earlier accepted decision rather than just filling in an unspecified detail.

## Options considered

### Backend endpoint shape

- **Combined `GET /portfolio`** (ADR 0012's original plan) - one round trip for cash + holdings + total + P&L, computed together server-side.
- **Split: reuse `GET /auth/session` (cash) + new `GET /trades/holdings` (list)** (chosen) - avoids a second source of truth for cash balance, which `GET /auth/session`'s `UserSummary.cashBalance` already exposes and the frontend already reads on every page via `AuthService.currentUser()`. `GET /trades/holdings/{ticker}` already existed for the single-ticker case (ADR 0027); adding a plain list endpoint next to it was a minimal, natural extension rather than standing up a new combined resource.

### Frontend page shape

- **Separate `/portfolio` route + `PortfolioComponent` + `PortfolioService`** (ADR 0013's original plan) - matches the original plan, but adds a navigation hop between the dashboard (where stock search already lives) and the holdings/cash view, and a `PortfolioService` would have exactly one consumer.
- **Fold holdings into the existing dashboard** (chosen) - cash balance was already displayed there (US-3); the holdings table now renders directly below it in a single column. The dashboard's layout reserves a second column for future US-8/US-9 widgets rather than justifying a second routed page now.

## Decision

- No unified `/portfolio` endpoint. US-7's data needs are covered by `GET /trades/holdings` (list, new), `GET /trades/holdings/{ticker}` (single, ADR 0027), and the existing `GET /auth/session` (cash balance).
- No separate `/portfolio` route, `PortfolioComponent`, or `PortfolioService`. Holdings render inline in `DashboardComponent`, backed by a component-local `holdings` signal populated via `TradeService.getHoldings()` in `ngOnInit` — not a shared injectable service, since there's only one consumer.
- `docs/architecture/api-contract.md`, `frontend-architecture.md`, and `openapi.yaml` were updated to match; this ADR is the historical record of why they no longer match ADR 0012/0013 on this point. The rest of ADR 0012 (server-fetched trade pricing, auto-login, RFC 7807 errors) and ADR 0013 (standalone components, signals-over-NgRx as a general pattern, Tailwind, testing) are unaffected and still stand.

## Consequences

- US-9 (profit/loss) still needs to decide its own data source when it's tackled - options include summing `GET /trades/holdings` client-side, adding a field to an existing response, or a small dedicated endpoint. Not decided here; the shape wasn't obvious from a bare holdings list alone.
- The dashboard's reserved second column is where US-8 (transaction history) is expected to land next. If that column's content grows enough to need significant independent state/logic, revisit whether it should split back out into its own routed feature - this ADR rejects that now for lack of a second consumer, not permanently.
- Anyone reading ADR 0012 or 0013 in isolation would see a `/portfolio` endpoint or `PortfolioService` that was never built; both now carry a pointer to this ADR at the relevant line.
