# 0029 - US-8 transaction history: separate routed page, plus a shared Navbar with an account menu

- Status: Accepted
- Date: 2026-07-27
- Revises: ADR 0028's expectation that "the dashboard's reserved second column is where US-8 is expected to land next"

## Context

ADR 0028 (US-7) predicted transaction history (US-8) would land in a second column added to the dashboard, once that two-column layout existed. When US-8 was actually built, no such column was ever added - `frontend-architecture.md`'s original plan (predating ADR 0028) of a dedicated `transactions/` feature and route turned out to be what got built instead. Giving transaction history its own page also meant the dashboard's navbar (wordmark + stock search), which previously only had one page to live on, needed to be reachable from a second page too, and needed a way to actually navigate to `/transactions` from anywhere in the app.

## Options considered

### Where transaction history lives

- **Dashboard's reserved second column** (ADR 0028's prediction) - keeps everything on one route, but the two-column layout was never built, and a transaction list doesn't share state or a natural visual grouping with the holdings table it would sit next to.
- **Separate `/transactions` route + component** (chosen) - matches `frontend-architecture.md`'s original plan, gives the list room to grow (pagination, filtering) without competing for space with holdings, and keeps `Dashboard`'s template from growing further.

### How to reach it

- **Ad hoc link added to the dashboard only** - cheapest, but would need to be re-added by hand to every future authenticated page individually.
- **Extract the header into a shared `Navbar` component with an account-menu dropdown** (chosen) - one component (`shared/navbar/`) now owns the wordmark, stock search, and a right-aligned account menu; both `Dashboard` and `Transactions` render `<app-navbar/>` instead of duplicating header markup. The dropdown (button + outside-click-to-close panel, same pattern already used for the search results dropdown) gives the app a single, reusable spot for account-level actions generally, not just this one link.

### Account menu scope

- **Include Logout now, since it's a natural fit for the same menu** - considered, but deferred. `AuthService` has no `logout()` method yet, and while Spring Security's default logout handling exists server-side (`SecurityConfig`'s `.logout(Customizer.withDefaults())`), nothing has verified it behaves correctly for a SPA (clearing the session cookie, an appropriate response body/status for `fetch`, whether the frontend needs to redirect after). Wiring it deserves its own story rather than being folded in as a side effect of this one.

## Decision

- New shared `Navbar` component (`shared/navbar/`): wordmark, the stock ticker search (moved out of `Dashboard`), and an account-menu button on the right. The dropdown currently has one item, "Transaction history", navigating to `/transactions`.
- New `/transactions` route, guarded by `authGuard`, backed by `Transactions` (`features/transactions/`). Data comes from `TradeService.getTransactions()` (`GET /trades/transactions`), following the same single-consumer reasoning ADR 0028 used for holdings - no dedicated `TransactionsService`.
- Logout is explicitly out of scope here - not in the account menu, no `AuthService.logout()` yet. Tracked as a follow-up story.
- `frontend-architecture.md` updated: project structure, routing table, and state-management notes now reflect `Navbar`, the corrected route list, and the folded-in `getTransactions()`.

## Consequences

- ADR 0028's "reserved second column" note no longer describes anything planned; the dashboard stays single-column. Revisit only if some future feature actually wants a second column.
- Any future authenticated page (e.g. US-9 profit/loss) should render `<app-navbar/>` rather than rebuilding header markup - the account menu is the natural place to add further account-scoped links as they show up.
- Logout is a known, explicitly deferred gap: no `AuthService.logout()`, no UI entry point. Whoever picks up that story needs to confirm the backend's default Spring Security logout behavior actually suits an SPA (response shape/status) before wiring the frontend to it.
