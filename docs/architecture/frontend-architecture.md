# Frontend Architecture

> Angular structure, state management, styling, and testing approach for the TopTrader SPA. Builds on the API contract (`api-contract.md`, `openapi.yaml`) and the session-cookie auth model (ADR 0004, ADR 0007).

## Project structure

Standalone components (no `NgModule`s) — Angular's modern default, less boilerplate for an app this size.

```
frontend/src/app/
  core/
    services/        auth.service.ts, portfolio.service.ts, transactions.service.ts, quote.service.ts
    guards/          auth.guard.ts        (functional CanActivateFn)
    interceptors/    error.interceptor.ts
  features/
    auth/            login/, register/
    portfolio/       (US-7, US-9)
    trade/           (US-4 quote lookup + US-5/US-6 buy/sell, with confirm step)
    transactions/    (US-8)
  shared/            reusable presentational components (e.g. confirm-dialog)
  app.routes.ts
  app.config.ts
```

## Routing

| Path | Component | Guard |
|---|---|---|
| `/login` | `LoginComponent` | none (redirect to `/portfolio` if already authenticated) |
| `/register` | `RegisterComponent` | none |
| `/portfolio` | `PortfolioComponent` | `authGuard` |
| `/trade` | `TradeComponent` | `authGuard` |
| `/transactions` | `TransactionsComponent` | `authGuard` |
| `''` | redirect to `/portfolio` | — |

## State management

Native Angular **signals inside plain injectable services** — no external state library (NgRx rejected as unnecessary boilerplate for ~4-5 pieces of state: current user, portfolio, transactions, in-flight quote).

- `AuthService` — `signal<UserSummary | null>` for the current user; calls `GET /auth/session` on app init (via an `app.config.ts` initializer) to restore session state across a page refresh (US-2); exposes `register()`, `login()`, `logout()`.
- `PortfolioService` — `signal<Portfolio | null>`, `refresh()` calling `GET /portfolio`; re-called after every buy/sell.
- `TransactionsService` — `signal<Transaction[]>`, `refresh()` calling `GET /transactions`.
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
