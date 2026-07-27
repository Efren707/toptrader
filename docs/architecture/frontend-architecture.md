# Frontend Architecture

> Angular structure, state management, styling, and testing approach for the TopTrader SPA. Builds on the API contract (`api-contract.md`, `openapi.yaml`) and the session-cookie auth model (ADR 0004, ADR 0007).

## Project structure

Standalone components (no `NgModule`s) — Angular's modern default, less boilerplate for an app this size.

```
frontend/src/app/
  core/
    services/        auth.service.ts, trade.service.ts (buy/sell + holdings + transactions, US-5-US-8), quote.service.ts
    guards/          auth.guard.ts, guest.guard.ts   (functional CanActivateFn)
    interceptors/    error.interceptor.ts
  features/
    auth/            login/, register/
    dashboard/       account summary, cash balance, holdings list (US-3, US-7)
    stock-details/   quote lookup + buy/sell, with confirm step (US-4/US-5/US-6)
    transactions/    transaction history list (US-8)
  shared/
    navbar/          shared header (wordmark, stock search, account menu) - used by dashboard and transactions (ADR 0029)
    ui/              reusable presentational components (card, button, input)
    trade-form/       buy/sell confirm form, used from stock-details
  app.routes.ts
  app.config.ts
```

There's no separate `portfolio/` feature or route: holdings ended up living on the dashboard instead of a dedicated page, since splitting them out added a navigation hop without a second consumer to justify it (ADR 0028). US-8 (transaction history) *did* get its own routed page rather than folding into the dashboard - see ADR 0029, which also covers the `Navbar` extraction and its account-menu dropdown as the nav path to it.

## Routing

| Path | Component | Guard |
|---|---|---|
| `/register` | `Register` | `guestGuard` (redirect to `/dashboard` if already authenticated) |
| `/login` | `Login` | `guestGuard` |
| `/dashboard` | `Dashboard` | `authGuard` — account summary, cash balance, holdings list (US-3, US-7) |
| `/stocks/:ticker` | `StockDetails` | `authGuard` — quote lookup + buy/sell (US-4/US-5/US-6) |
| `/transactions` | `Transactions` | `authGuard` — transaction history, most recent first (US-8) |
| `''`, `'**'` | redirect to `/login` | — |

## State management

Native Angular **signals inside plain injectable services** — no external state library (NgRx rejected as unnecessary boilerplate for ~4-5 pieces of state: current user, portfolio, transactions, in-flight quote).

- `AuthService` — `signal<UserSummary | null>` for the current user; calls `GET /auth/session` on app init (via an `app.config.ts` initializer) to restore session state across a page refresh (US-2); exposes `register()`, `login()`. No `logout()` yet — deferred, see ADR 0029.
- `TradeService` — stateless wrapper around `POST /trades/buy`, `POST /trades/sell`, `GET /trades/holdings/{ticker}`, `GET /trades/holdings`, and `GET /trades/transactions`; no persistent signal of its own. Holdings and transactions aren't centralized in shared services — `Dashboard` owns a local `holdings` signal populated from `getHoldings()` in `ngOnInit`, and `Transactions` owns a local `transactions` signal populated from `getTransactions()` the same way. Cash balance is read directly off `AuthService.currentUser()`. Each has exactly one consumer, so dedicated `PortfolioService`/`TransactionsService` weren't justified; revisit if a second consumer shows up for either.
- `QuoteService` — stateless wrapper around `GET /quotes/{ticker}`, called on-demand from the trade feature (no persistent signal needed).

## HTTP layer

`provideHttpClient(withXsrfConfiguration(...), withFetch())` — the standalone-API equivalent of the `HttpClientXsrfModule` referenced in ADR 0007 (same CSRF cookie+header mechanism: reads the `XSRF-TOKEN` cookie, sends it back as `X-XSRF-TOKEN`), plus `withCredentials: true` so the session cookie is sent on every request.

A functional `errorInterceptor` unwraps the RFC 7807 `ProblemDetail` body (ADR 0012) from failed responses into a consistent shape the UI can render (e.g. a toast/inline error), so no component has to know about the raw HTTP error format.

## Styling

**Tailwind CSS** — utility-first, full design control, smaller runtime footprint than a component library. Trade-off accepted: since Tailwind ships no pre-built components, interactive widgets (the buy/sell confirm dialog in particular, per US-5/US-6's explicit confirmation step) must be hand-built with correct keyboard navigation and focus trapping to meet the accessibility NFR — flagged here as a concrete implementation task for whoever builds `shared/confirm-dialog`, not a gap to discover later.

## Browser support & responsive layout

Angular CLI's default `browserslist` config (current + previous major version of Chrome, Firefox, Edge, Safari) is used as-is — matches `nfr.md`'s "modern evergreen browsers" target with no custom tuning needed. Layout is responsive via Tailwind's breakpoint utilities (`sm:`/`md:`/`lg:`), targeting both desktop and mobile web viewports per `nfr.md` — no separate mobile app or mobile-specific routes.

## Testing

**Vitest**, via Angular CLI's built-in `@angular/build:unit-test` builder — superseded the original Jest plan (this doc previously called for Jest as "the commonly-adopted Karma replacement," but flagged verifying against the actual CLI version at implementation time; Angular CLI 22 now ships Vitest as its own native default builder, so no third-party test runner integration is needed at all).

## Carried forward from prior ADRs

- CORS origin, CSRF cookie+header pairing: ADR 0007.
- All errors as RFC 7807 `application/problem+json`: ADR 0012.
- Security headers (CSP, HSTS, etc.) are applied at the CloudFront layer, not in Angular code: `security-architecture.md`.
