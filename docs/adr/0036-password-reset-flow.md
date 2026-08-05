# 0036 - Password reset flow: token design, delivery, and session handling

- Status: Accepted
- Date: 2026-08-01

## Context

The pre-production security checklist (see `docs/ROADMAP.md`) flags password-reset as unimplemented: there is no self-service recovery path for a locked-out/forgetful real user once the app has real accounts on the public internet. `docs/requirements/user-stories.md` and `docs/architecture/security-architecture.md` both explicitly deferred this. Scope for this ADR is password reset only; email verification at signup is a related but separate follow-up.

The app has no email-sending infrastructure at all (no `spring-boot-starter-mail`, no SES, no SMTP config) and isn't deployed yet (no registered domain, no verified sender identity). That constrains how a reset link can actually be delivered today.

## Options considered

**Email delivery.**
- *Real AWS SES integration now* - matches the eventual "going live" shape, but requires AWS sender-identity verification and domain setup that hasn't happened yet (AWS deployment is architected per ADR 0005/0006/0014/0016/0017 but execution hasn't started). Would block this feature on unrelated deployment sequencing.
- *`EmailSender` interface with a log-only impl for now* (chosen) - the full request/token/validate/update flow is built and testable today; only the delivery transport is deferred. A `LogEmailSender` prints the reset link instead of emailing it. The real SES-backed implementation gets swapped in behind the same interface when AWS deployment is actually sequenced, with no flow code changes needed.

**Log-stub scope.** Logging a raw reset token is a secret-in-logs violation of ADR 0033's PII/logging policy if it happens in prod. `LogEmailSender` is therefore wired only for local/dev profiles. The prod profile has no working `EmailSender` bean; `POST /auth/forgot-password` returns a generic `503` ("password reset is temporarily unavailable") in prod today, identical for existing and non-existing emails so it doesn't weaken the enumeration-safe contract login already relies on (see `docs/architecture/security-architecture.md`, ADR 0025). This is a known, tracked gap - prod password reset is not functionally usable until the SES swap happens as part of deployment work - not a silent one.

**Token storage.**
- *Columns on `User`* (`resetTokenHash`/`resetTokenExpiry`) - simplest, mirrors how `failedAttempts`/`lockedUntil` already live on `User`, but only represents one outstanding token per user and keeps no history.
- *New `password_reset_tokens` table* (chosen) - `id`, `user_id` (FK), `token_hash`, `expires_at`, `used_at`, `created_at`. Lets a new reset request coexist with/invalidate a prior outstanding token cleanly and gives single-use enforcement (`used_at`) and an audit trail without overloading `User`. New Flyway migration `V3__create_password_reset_tokens_table.sql`.

**Token generation and hashing.** The emailed token is a server-generated 32-byte `SecureRandom` value, URL-safe base64-encoded. Only its SHA-256 hash is persisted (`token_hash`), so a DB read alone can't produce a usable token. SHA-256 (not Argon2/bcrypt) is appropriate here because the token is already high-entropy random data, not a low-entropy user-chosen secret - the slow, salted hashing that defends passwords against offline guessing has no equivalent benefit here and would just add cost. Expiry is 30 minutes, matching this app's existing session-timeout scale (`server.servlet.session.timeout=30m`).

**Transport/framework wiring.** `/auth/forgot-password` and `/auth/reset-password` are unauthenticated, like `/auth/register` and `/auth/login`, so they get the same treatment: added to the CSRF `ignoringRequestMatchers` list (ADR 0022's existing exemption for pre-session endpoints), and a new `RateLimitGroup.FORGOT_PASSWORD` (per ADR 0034's pattern) keyed by client IP, capacity 5 per hour, covering both endpoints - this bounds both email-spam and reset-token-guessing risk the same way `REGISTER` bounds account-creation spam.

**Session handling on reset.** A successful reset invalidates the user's other active sessions - if a password was reset because it leaked or was forgotten, an existing session (potentially an attacker's) shouldn't survive it. No `SessionRegistry` exists in `SecurityConfig` today; this adds one, registers `HttpSessionEventPublisher`, and wires `sessionManagement().sessionRegistry(...)` so `PasswordResetService` can expire all of a user's sessions via `SessionInformation.expireNow()` after updating the password hash.

## Decision (summary)

- New `password_reset_tokens` table (Flyway `V3`): `id`, `user_id`, `token_hash` (SHA-256 of a 32-byte `SecureRandom` token), `expires_at` (30 min TTL), `used_at`, `created_at`.
- `EmailSender` interface; `LogEmailSender` active in local/dev profiles only. No working sender in prod yet - `/auth/forgot-password` returns `503` in prod, identical response regardless of whether the email exists.
- `/auth/forgot-password` (request a reset) and `/auth/reset-password` (consume token + set new password) added to `SecurityConfig`'s CSRF exemption list and to a new `RateLimitGroup.FORGOT_PASSWORD` (client-IP keyed, 5/hour, covers both paths).
- Successful reset marks the token used, updates `passwordHash`, and expires all of the user's other active sessions via a newly-added `SessionRegistry`/`HttpSessionEventPublisher`.

## Consequences

- Prod password reset is not actually usable end-to-end until a real `EmailSender` (SES-backed) implementation is built as part of AWS deployment sequencing. Tracked explicitly here and in `docs/ROADMAP.md`, not silently shipped as if complete.
- The new `SessionRegistry` infra is reusable beyond this feature (e.g. a future "log out all devices" account action) but is being introduced now, scoped to this need, rather than speculatively built out further.
- Email verification at signup (the other half of the original checklist item) is out of scope for this ADR and remains a separate follow-up.
- Single-use enforcement (`used_at`) and the 30-minute TTL mean a user who lets a reset link go stale simply requests a new one; no manual cleanup job for expired/used rows exists yet - acceptable at current scale, worth revisiting if the table grows unbounded.
