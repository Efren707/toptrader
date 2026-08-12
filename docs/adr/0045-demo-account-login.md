# 0045 - Demo account: passwordless login endpoint, CSRF/rate-limit treatment, and read-only enforcement

- Status: Accepted
- Date: 2026-08-11

## Context

The [Demo Account & Showcase Readiness milestone](https://github.com/Efren707/toptrader/milestone/15) (tracked in `docs/tasks/in-progress/demo-account.md`) adds a one-click "Try Demo" login so recruiters/reviewers can explore the live app without registering. The original plan (from the `aws-infrastructure-implementation.md` cutover section this work was split out of) was README-documented credentials for reviewers to type in manually; that was revisited as unlikely to get noticed by someone skimming the live site, in favor of a button that logs a visitor in directly.

That shifts the design away from every existing auth flow in the app: `LoginService` and `RegistrationService` both end in an authenticated session, but both start from either submitted credentials (login) or a newly created account (register). A demo login starts from neither — there's no credential to check and no new account to create, since the demo account is a single, pre-existing, shared row.

Making that account trivially reachable by a button (not just by someone who read a README) also means it needs a restriction a normal account doesn't: it can't be allowed to trade, or any visitor could permanently degrade the curated seed data (mixed gain/loss/flat holdings, coherent transaction history — see `docs/tasks/in-progress/demo-account.md`'s "Decided now" section) for every visitor after them.

## Options considered

**How the endpoint authenticates.** 
- *Route through `AuthenticationManager`* with a fixed, known password - rejected. This still requires a real password to exist and be checked, which contradicts the goal of the demo account never having exposed or checkable credentials, and would route through the same `UserDetailsService`/password-hash comparison as a real login for no benefit.
- *Look the user up directly and establish a session* (chosen) - `DemoLoginService` fetches the seeded demo `User` by a fixed email via `UserRepository.findByEmail(...)` and hands it straight to `SessionEstablisher` (the shared component extracted from `LoginService`/`RegistrationService` in this same milestone). No password field, no `AuthenticationManager` call, no interaction with `LoginService`'s failed-attempt/lockout tracking at all - the two flows share nothing except the session-establishment step.
- If the demo row isn't found (e.g. the `V6` seed migration hasn't run in some environment), the service throws a `500` rather than silently doing nothing - a missing demo account is a deployment defect, not a normal/expected outcome worth a quiet no-op.

**CSRF exemption.** Extends the precedent set by [ADR 0022](./0022-csrf-bootstrap-exemption.md) (`/auth/register`, `/auth/login`) and continued by [ADR 0036](./0036-password-reset-flow.md)/[ADR 0037](./0037-email-verification-at-signup.md): any endpoint reachable before a session/CSRF token exists needs to be exempted (`ignoringRequestMatchers`) and `permitAll()`'d. `/auth/demo-login` is an even simpler case than those - it doesn't even carry a request body, let alone a credential submission, so there's nothing for CSRF protection to meaningfully cover on this specific request.

**Rate limiting.** New `RateLimitGroup.DEMO_LOGIN`, client-IP keyed, 5 requests/hour - identical shape to `RateLimitGroup.REGISTER` (see [ADR 0034](./0034-api-rate-limiting.md)). Chosen over reusing `REGISTER`'s bucket, since demo login and registration are unrelated actions that shouldn't share a quota, and over a higher threshold, since there's no legitimate reason for one visitor to need more than a handful of demo logins per hour.

**Read-only enforcement, and why here instead of a reset job.**
- *Scheduled reset job* (e.g. nightly restore of seed data) - rejected. No scheduling infrastructure exists anywhere in this codebase today, and building one solely for this would be a large addition for a small problem.
- *Per-visitor demo account (create-on-demand)* - rejected. Defeats the purpose of curated, coherent seed data (mixed gain/loss/flat holdings telling "a small coherent story," per the task doc) - a fresh account would just be an empty $500 balance, which is the exact outcome this milestone is trying to avoid.
- *Read-only enforcement* (chosen) - since every visitor shares the same account, the simplest fix is removing the only way the shared state can change: trading. Enforced server-side as a guard clause in `TradeService.buyStock`/`sellStock` checking `user.isDemo()` (tracked as this milestone's section 2, not yet implemented as of this ADR), with a matching frontend affordance (section 5) so the restriction is visible before a visitor tries and fails. This sidesteps reset jobs, shared-mutable-state races, and new infrastructure entirely - the seed data simply never changes after the `V6` migration inserts it.

## Decision (summary)

- `POST /auth/demo-login` takes no request body. `DemoLoginService` fetches the seeded demo user by a fixed `DEMO_EMAIL` constant and calls the shared `SessionEstablisher` directly - no password, no `AuthenticationManager`, no interaction with `LoginService`'s lockout tracking. A missing demo row is a `500`, not a silent no-op.
- `/auth/demo-login` is added to `SecurityConfig`'s CSRF `ignoringRequestMatchers` and `permitAll()`, extending the ADR 0022 precedent.
- New `RateLimitGroup.DEMO_LOGIN`: client-IP keyed, 5/hour, matching `REGISTER`'s shape and separate from its bucket.
- The demo account cannot trade. Enforced server-side in `TradeService` (guard clause on `user.isDemo()`, rejecting with `403` before any mutation) as the actual security boundary; the frontend disables trade controls for the demo user as UX polish, not enforcement.
- `User.isDemo` (boolean column, added via `V5`) and `UserSummary.isDemo` (frontend-facing) key both the login flow and the read-only checks off the same flag.

## Consequences

- The demo account is permanently frozen exactly as seeded (`V6` migration, this milestone's section 3) - no drift, no reset job, no per-visitor state to reconcile. Its cost is that the read-only restriction is a real product constraint a normal account doesn't have, which is why it's enforced server-side rather than left as a frontend-only nicety.
- Because there's no password and no `AuthenticationManager` involvement, `DemoLoginService` is a much smaller surface than `LoginService` - it also means brute-force lockout protection is structurally irrelevant here (there's nothing to brute-force), not just unimplemented.
- If the demo account ever needs to be reset or reseeded (e.g. after enough sessions to make the transaction history stale, though nothing currently ages it out), that's a manual Flyway migration, same as any other data fix - no tooling exists for it today, consistent with this project's lack of scheduling infrastructure noted above.
