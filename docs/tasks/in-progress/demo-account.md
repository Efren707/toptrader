# Demo Account & Showcase Readiness

> Status: In progress as of 2026-08-11, tracked under the [Demo Account & Showcase Readiness milestone](https://github.com/Efren707/toptrader/milestone/15). Originally tracked from [docs/tasks/completed/aws-infrastructure-implementation.md](../completed/aws-infrastructure-implementation.md)'s cutover section (section 8), then split into its own issue (#134) once the app went live. #134 has since been closed and superseded by this doc's 6 sections/issues below — its original "credentials in README" item was explicitly dropped in favor of a one-click login button (section 1).

Working agreement applies as usual: one section at a time, check in before deciding anything not already settled below.

## Decided now

### Demo account seeding mechanism
A Flyway seed migration (versioned, e.g. `V6__seed_demo_account.sql`), using an idempotent guard (`INSERT ... ON CONFLICT DO NOTHING`) keyed on a fixed demo email. Runs automatically at startup like every other migration (ADR 0011) — no new mechanism, no new deploy step. Chosen over a backend startup seeder (extra demo-only code path) or a manual script (not reproducible/version-controlled, out of step with the rest of this project's process).

### Demo account content
- Fixed login (e.g. `demo@toptrader.dev`).
- 3 holdings with a mix of gain / loss / flat performance, so the portfolio view and overall P/L (US-7, US-9) look realistic rather than a flat, untouched $500.
- Matching buy (and at least one sell) transactions in the history (US-8) that produced those holdings — the seed data tells a small coherent story, not just raw rows.
- Remaining cash left below $500 (not the full starting balance), so it reads as an account that's actually been used.

### Access mechanism — one-click login, not README credentials
Originally planned as README-documented credentials for reviewers to type in manually. Revisited: recruiters skimming the live site are unlikely to notice a credentials callout, so instead the login page gets a **"Try Demo" button** that logs a visitor straight in via a new `POST /auth/demo-login` endpoint — no typing, no credentials ever exposed in the README or anywhere else.

### Read-only scope
Since the button makes the demo account trivially accessible to anyone (not just README readers), it needs a restriction a normal account doesn't: **the demo account cannot trade** (no buy/sell). It's a single account shared by every visitor — allowing trades would let any visitor permanently degrade the curated seed story (mixed gain/loss/flat, coherent transaction history) for every visitor after them, with no reset mechanism. Read-only sidesteps that entirely: no scheduled reset job, no shared-mutable-state concerns, no new scheduling infrastructure (none exists in this codebase today). Enforced server-side (`TradeService` guard clause, section 2) as the actual security boundary, with a matching frontend affordance (section 5) so the restriction is visible before a visitor tries and fails.

### Session-establishment mechanism
`LoginService` and `RegistrationService` each already have their own private, near-identical session-establishing helper. Rather than adding a third copy for the new demo-login path, both existing services are refactored to share one `SessionEstablisher` component — removes duplication that already existed, not just avoids adding to it.

## Sections

### 1. Backend — demo-login endpoint

- [x] `V5__add_is_demo_column_to_users.sql` migration + `User.isDemo` field
- [x] Shared `SessionEstablisher` extracted from `LoginService`/`RegistrationService`'s duplicated session-setup logic, reused by both plus the new service
- [x] `DemoLoginService` + `POST /auth/demo-login` on `AuthController` — no password required, never touches `LoginService`'s failed-attempt lockout tracking
- [x] `SecurityConfig` CSRF exemption + `permitAll` for the new endpoint, extending ADR 0022's precedent (no existing session for CSRF to ride on; here there isn't even a credential submission)
- [x] `RateLimitGroup.DEMO_LOGIN` — IP-keyed, 5/hour, matching `REGISTER`'s existing shape (ADR 0034)
- [x] `UserSummary.isDemo` field added (backend record), so the frontend can key UX off it
- [x] Backend tests: `AuthControllerDemoLoginTest`
- [x] New ADR covering this endpoint, its CSRF exemption, rate limiting, and the read-only enforcement decision (section 2) — [ADR 0045](../../adr/0045-demo-account-login.md)

GitHub Issue: [#139](https://github.com/Efren707/toptrader/issues/139)

### 2. Backend — read-only trading enforcement

- [ ] Guard clause in `TradeService.buyStock` and `sellStock` rejecting any trade where `user.isDemo()` (403), before any mutation happens
- [ ] `TradeServiceTest` cases for both methods confirming rejection and zero side effects (`verifyNoInteractions` on holding/transaction repos)

Depends on section 1's `is_demo` column. GitHub Issue: [#140](https://github.com/Efren707/toptrader/issues/140)

### 3. Seed migration

- [ ] `V6__seed_demo_account.sql` — idempotent (`ON CONFLICT DO NOTHING` on the fixed demo email), `is_demo = TRUE`, backdated transaction timestamps so the history reads as "already happened," not "seeded seconds ago"
- [ ] Demo email constant matches `DemoLoginService.DEMO_EMAIL` exactly — can't share a constant across SQL/Java, verified by hand

Depends on section 1's `is_demo` column. GitHub Issue: [#141](https://github.com/Efren707/toptrader/issues/141)

### 4. Frontend — "Try Demo" login button

- [ ] `AuthService.demoLogin()` + `isDemo` added to the frontend `UserSummary` interface
- [ ] Login page: new "Try Demo" button (secondary variant, own loading state so it doesn't cross-disable with the regular submit button), navigates to `/dashboard` on success, surfaces errors the same way the existing login form does
- [ ] `login.spec.ts` covers the new button's success/error paths

Depends on section 1's endpoint. GitHub Issue: [#142](https://github.com/Efren707/toptrader/issues/142)

### 5. Frontend — read-only trading UX

- [ ] `TradeForm` gets a new `readOnly` input (same pattern as its existing `hasHolding` input) — when true, disables the trade controls and shows a static "Demo account is read-only" note instead of a submit button
- [ ] Wired from the stock details page via `authService.currentUser()?.isDemo`
- [ ] `trade-form.spec.ts` covers the disabled state

This is UX polish, not the actual enforcement (that's section 2, server-side) — defense in depth. GitHub Issue: [#143](https://github.com/Efren707/toptrader/issues/143)

### 6. README & showcase readiness

- [ ] README screenshots/GIF of the core buy/sell flow — see the [README structure outline](../../guides/readme-structure-outline.md)'s screenshots section
- [ ] Live demo link callout / "Try Demo" mention on the README status line

Not blocked on sections 1-5, but makes most sense to capture screenshots once the Try Demo button exists. GitHub Issue: [#144](https://github.com/Efren707/toptrader/issues/144)
