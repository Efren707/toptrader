# 0030 - US-9 profit/loss: client-side computation on a new /performance page

- Status: Accepted
- Date: 2026-07-27
- Resolves: ADR 0028's open question ("US-9 still needs to decide its own data source when it's tackled")

## Context

ADR 0028 deliberately left US-9 (view profit/loss) undecided: no combined `GET /portfolio` endpoint exists, and the dashboard's `portfolioBalance` (cash + sum of holdings' `marketValue`) is already computed entirely client-side from data fetched via `GET /auth/session` and `GET /trades/holdings`. The acceptance criteria for US-9 require overall P&L (current total portfolio value − $500 starting balance) shown as both a dollar amount and a percentage, consistent with the portfolio view.

## Options considered

### Data source

- **Pure client-side (chosen)** - compute P&L in the frontend from values already available: `portfolioBalance` (existing pattern from `dashboard.ts`) minus a `$500` starting-balance constant. No backend changes, no new network calls. Trade-off: the `$500` starting balance (currently only defined server-side as `RegistrationService.STARTING_CASH_BALANCE`) has to be duplicated as a frontend constant, since no endpoint exposes it.
- **Add a field to an existing response** - e.g. `startingCashBalance` on `UserSummary`/`GET /auth/session`, avoiding the duplicated constant at the cost of a small backend change.
- **Dedicated `GET /trades/pnl` endpoint** - keeps the calculation and the $500 constant in one place server-side, at the cost of a new endpoint and an extra round trip for a value already derivable from data the frontend has loaded.

Client-side was picked to stay consistent with ADR 0028's precedent (avoid new backend surface when the frontend already has what it needs) and because the duplicated constant is a single literal, not meaningfully divergent logic.

### Where it's displayed

- **On the dashboard, next to `portfolioBalance`** - cheapest, no new route, but the dashboard was already trimmed back to single-column in ADR 0029 and keeps growing in scope.
- **New dedicated route (chosen)** - a `/performance` page, following the same precedent ADR 0029 set for US-8 (transaction history got its own `/transactions` route rather than folding into the dashboard). Reached via the shared `Navbar`'s account menu, alongside "Transaction history".

## Decision

- P&L is computed entirely client-side: `total portfolio value (cash + sum of holdings' marketValue) − $500`, shown as both a dollar amount and a percentage of the $500 starting balance. The `$500` is a frontend constant (mirroring the backend's `RegistrationService.STARTING_CASH_BALANCE`), not fetched from any endpoint.
- No backend changes for US-9. No new endpoint, no new DTO field.
- New `/performance` route, guarded by `authGuard`, reachable from the `Navbar` account menu (added as a second item next to "Transaction history"). No dedicated Angular service — same single-consumer reasoning ADR 0028/0029 used for holdings and transactions; the page computes its total from `AuthService.currentUser()` and `TradeService.getHoldings()` directly.

## Consequences

- The `$500` starting balance now exists in two places (`RegistrationService.STARTING_CASH_BALANCE` server-side, a frontend constant for US-9). If it's ever made configurable per-user or changed, both need updating — acceptable for MVP given it's a fixed constant today, but worth revisiting if that changes.
- Once Milestone #11 (Portfolio & Reporting) closes with US-9, the `Navbar` account menu has two entries (Transaction history, Performance); any future account-scoped page should follow the same pattern.
- `docs/architecture/frontend-architecture.md` (routing table) should be updated to include `/performance`.
