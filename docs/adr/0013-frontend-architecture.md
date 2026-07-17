# 0013 - Frontend architecture: standalone components, signals-based state, Tailwind CSS, Jest

- Status: Accepted
- Date: 2026-07-16

## Context

Phase 3 needed a concrete Angular architecture: component style, state management, styling approach, and testing framework, on top of the session-cookie auth model (ADR 0004, ADR 0007) and the API contract (ADR 0012). Full structure (folders, routing, HTTP layer) is in `docs/architecture/frontend-architecture.md`. Four sub-decisions here have real alternatives and are recorded as this ADR.

## Options considered

### Component style

- **Standalone components** - Angular's modern default (since v15+), no `NgModule` boilerplate.
- **NgModule-based** - the older pattern (`AppModule` + feature modules). Fully supported, but no advantage for an app this size — added structural ceremony for no functional gain.

### State management

- **Signals + plain injectable services** - native Angular signals (`AuthService`, `PortfolioService`, `TransactionsService`, `QuoteService`), no external library. Matches the app's actual scope: ~4-5 pieces of state, no complex cross-cutting derived state.
- **NgRx** - full Redux-style store (actions, reducers, effects, selectors). Powerful for large apps with complex shared state; meaningful boilerplate overhead not justified here.

### Styling / component library

- **Tailwind CSS** - utility-first, full design control, smaller runtime footprint than a component library, framework-agnostic skill. No pre-built components — interactive widgets (notably the buy/sell confirm dialog required by US-5/US-6) must be hand-built with correct keyboard nav/focus trapping to meet the accessibility NFR.
- **Angular Material** - free/OSS, first-party, pre-built accessible components (dialogs, tables, forms) — faster to a working accessible UI, but a more generic "Material Design" look and a real dependency/bundle-size cost.
- **Plain CSS/SCSS** - no dependency at all, same hand-built-accessibility burden as Tailwind without the utility-class productivity boost.

### Testing framework

- **Jest** - the commonly-adopted replacement for Angular's deprecated Karma runner; faster, more familiar API.
- **Jasmine + Karma** - Angular's classic default, still functional today but on a deprecation path.

## Decision

- **Standalone components**, no `NgModule`s.
- **Signals-based state** in plain injectable services, no NgRx.
- **Tailwind CSS** for styling.
- **Jest** for testing.

## Consequences

- Choosing Tailwind over Angular Material means the buy/sell confirmation dialog (and any other interactive widget) is a hand-built implementation task that must be explicitly checked against the accessibility NFR (keyboard navigation, focus trapping, ARIA attributes) — not something inherited for free from a component library. Flagged for whoever implements `shared/confirm-dialog`.
- Signals-based services keep state management simple now; if the app's shared/derived state grows meaningfully more complex post-MVP (e.g. real-time price streaming, multi-entity filtering), NgRx or a similar library may need revisiting — not expected within MVP scope.
- Jest's exact integration with the Angular CLI should be verified against the actual CLI version in use at implementation time, since Angular's testing tooling defaults have been shifting; this ADR records the *intent* (move off Karma), not a pinned CLI/Jest version.
- No change needed to ADR 0007's CSRF mechanism — the standalone `provideHttpClient(withXsrfConfiguration(...))` API achieves the same cookie+header pairing as the `HttpClientXsrfModule` reference in that ADR, just via the non-NgModule API surface (detailed in `frontend-architecture.md`).
