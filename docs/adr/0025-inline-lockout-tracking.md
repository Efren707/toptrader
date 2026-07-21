# 0025 - Brute-force lockout tracking: inline in LoginService, not auth event listeners

- Status: Accepted
- Date: 2026-07-20

## Context

ADR 0004 decided on a DB-tracked failed-attempt counter (`failedAttempts`/`lockedUntil` on `users`) for brute-force protection, and specifically named the implementation mechanism: Spring Security's built-in `AuthenticationFailureBadCredentialsEvent`/`AuthenticationSuccessEvent` listeners. Implementing US-2 (Log in) surfaced a real choice between that literal approach and handling the counter inline in `LoginService`, the service that already owns the `authenticate()` call and holds the request/response needed to establish the session.

## Options considered

- **`ApplicationListener` on the Spring Security auth events** - matches ADR 0004's wording exactly; decouples bookkeeping from the login request flow; would fire for any future code path authenticating through the same `AuthenticationManager`. But the event carries only the submitted `Authentication`, not a loaded `User`, so the listener would need its own `UserRepository` lookup duplicating work `DaoAuthenticationProvider` already did, in a separate transactional context. It also splits into two listener classes in practice, since an already-locked account fails via `LockedException`/`AuthenticationFailureLockedEvent` during Spring Security's pre-auth checks, not `AuthenticationFailureBadCredentialsEvent` - the two failure modes need to be handled differently (see Decision) and a single listener type can't see both.
- **Inline in `LoginService`** - the failure path (catch block around `authenticate()`) and success path already run in this class with direct access to request context. Consistent with `RegistrationService`'s existing style in this codebase (validation, persistence, and session logic handled directly in one service, not via cross-cutting framework hooks). Straightforward to unit/integration-test against one method rather than relying on event-publishing plumbing. Trade-off: diverges from ADR 0004's literal mechanism, and ties the lockout logic to this one login path - a future second authentication entry point using the same `AuthenticationManager` would not automatically get the same protection and would need its own equivalent logic.

## Decision

**Track failed attempts and lockout inline in `LoginService`, not via Spring Security auth event listeners.** `LoginService.recordFailedAttempt(email)` runs from the `catch` block around `authenticationManager.authenticate(...)`: it loads the user by the submitted email (a lookup miss is a no-op, preserving the no-enumeration guarantee - failure is still DB-write-only, never reflected in the response), skips incrementing if the account is already locked (so repeated attempts against a locked account can't keep pushing `lockedUntil` forward - ADR 0004's "fixed window, not permanent lockout"), and otherwise increments `failedAttempts` and sets `lockedUntil` once the count reaches the threshold (5 attempts -> 15-minute lockout, per ADR 0004's own example figures). `LoginService.resetFailedAttempts(user)` runs on successful login and unconditionally zeroes both fields.

This refines, not reverses, ADR 0004 - the DB-tracked-counter decision and the columns it specified are unchanged; only the wiring mechanism differs from what ADR 0004 originally named.

## Consequences

- No new listener classes; the entire lockout behavior is readable in one file (`LoginService`) alongside the authentication call it reacts to.
- Directly testable via `AuthControllerLoginTest` (repeated bad-password requests, then a correct one, asserting the 401/lockout/success transitions) without needing to exercise Spring's event bus in tests.
- If a second authentication entry point is ever added that also goes through `AuthenticationManager`, it will need its own explicit call into the same lockout logic (or a shared extraction) - this isn't automatic the way a listener-based approach would have been. Not a concern for MVP scope, which has exactly one login path.
- `LoginService` now depends on `UserRepository` in addition to `AuthenticationManager`, to load the user on the failure path where `authenticate()` doesn't provide one.
