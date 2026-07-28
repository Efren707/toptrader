# 0031 - US-11 navbar consistency: shared layout shell route instead of per-page `<app-navbar/>`

- Status: Accepted
- Date: 2026-07-28
- Revises: ADR 0029's guidance that "any future authenticated page should render `<app-navbar/>` rather than rebuilding header markup"

## Context

US-11 (issue #34) was filed because the stock details page rendered no navbar at all - `StockDetails` was never updated to follow ADR 0029's pattern when it was built, so it had no way back to the dashboard, no search, no account menu. The same ADR's pattern (each page imports `Navbar` and renders `<app-navbar/>` inside its own `.page` wrapper) was also silently duplicating identical markup and an identical `.page` CSS rule (`max-width: 78rem; margin: 0 auto; padding: 0 1.5rem 4rem;`) across `dashboard.css`, `transactions.css`, `performance.css`, and `stock-details.css` - nothing enforced that a new page would remember to add either.

Separately, the navbar's own spacing was uneven: `.search-form` stopped growing at `max-width: 28rem`, and nothing pushed `.account-menu-wrap` to the right edge, leaving a dead gap between the search box and the account icon on wide viewports.

## Options considered

### How to guarantee every authenticated page has a navbar

- **Keep the per-page `<app-navbar/>` convention, just fix `StockDetails`** - cheapest, matches ADR 0029, but is exactly the pattern that already produced this bug once and has no mechanism to stop it recurring on the next new page.
- **Shared layout shell route** (chosen) - a new `Layout` component (`shared/layout/`) owns `.page`, `<app-navbar/>`, and a `<router-outlet/>`; `app.routes.ts` nests `dashboard`, `stocks/:ticker`, `transactions`, and `performance` as `children` of a parent route that loads `Layout`. A page can no longer be routed to without the navbar - it's structurally impossible to forget, not just a convention to remember. `authGuard` also moves from each of the four child routes to the one parent route, since Angular blocks child activation when a parent's `canActivate` fails.

### Where the `.page` CSS rule lives

- Left duplicated per-page (status quo) - rejected, same reasoning as above.
- **Moved into `layout.css`, deleted from the four feature stylesheets** (chosen) - one definition, matching the one place it's now used in markup.

### Navbar spacing fix

- `margin-left: auto` on `.account-menu-wrap` - keeps the logo and search box's `gap: 2rem` fixed and only stretches the space before the account icon.
- **`justify-content: space-between` on `.navbar`** (chosen) - simpler one-line change; guarantees the first child (wordmark) sits at the container's start edge and the last child (`.account-menu-wrap`) sits at its end edge regardless of viewport width, with `.search-form`'s leftover space (once it hits its `28rem` cap) distributed evenly between the two internal gaps. Matches the "evenly-spaced" framing of the issue title more literally than pinning just one gap.

## Decision

- New `Layout` component (`shared/layout/layout.ts`/`.html`/`.css`): renders `<app-navbar/>` + `<main class="content"><router-outlet/></main>` inside `.page`.
- `app.routes.ts`: `dashboard`, `stocks/:ticker`, `transactions`, `performance` become `children` of a new parent route (`path: ''`, `loadComponent` → `Layout`, `canActivate: [authGuard]`); the guard is removed from each child.
- `Dashboard`, `Transactions`, `Performance`, `StockDetails`: templates lose the `<div class="page"><app-navbar/><main class="content">...</main></div>` wrapper (now just the inner content, rendered into the layout's outlet); `.ts` files lose their `Navbar` import; `.css` files lose the duplicated `.page` rule. `StockDetails` additionally drops its own `← Back to dashboard` link and the now-unused `RouterLink` import, since the navbar wordmark already links to `/dashboard` (ADR from US-10).
- `navbar.css`: `.navbar` gains `justify-content: space-between`.
- New test coverage: `layout.spec.ts` (using `RouterTestingHarness`) asserts a routed page renders both `app-navbar` and the outlet's content - a regression test for the exact bug this ADR fixes. `navbar.spec.ts`, previously just a "should create" placeholder, now covers the account-dropdown toggle, ticker search (success and 404), navigating to a search result's stock page, and all three account-menu actions (transactions, performance, logout) including that each closes the dropdown.

## Consequences

- Any future authenticated route added as a child of the layout route automatically gets the navbar and `.page` sizing - no per-page opt-in step to forget. A page that must NOT show the navbar (there are none today) would need its own top-level route outside this parent, same as `login`/`register` already are.
- ADR 0029's "any future authenticated page should render `<app-navbar/>`" is superseded by this structural guarantee - new pages don't need that reminder anymore.
- `authGuard` is now asserted once per navigation into the authenticated section rather than once per page component, which is both fewer places to keep in sync and the more standard Angular pattern for a guarded section of an app.
