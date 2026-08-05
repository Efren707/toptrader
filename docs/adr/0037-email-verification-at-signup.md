# 0037 - Email verification at signup: token design, gating, and delivery

- Status: Accepted
- Date: 2026-08-04

## Context

The pre-production security checklist (see `docs/ROADMAP.md`) flags email verification at signup as the remaining half of the item split out by [ADR 0036](./0036-password-reset-flow.md) — password reset was built first, email verification was called out there as "a related but separate follow-up." `docs/requirements/user-stories.md` lists it as explicitly out of MVP scope, but it's now being picked up as part of the pre-production checklist.

The app still has no real email-delivery infrastructure in prod (`EmailSender` interface + `LogEmailSender` exist from ADR 0036, but only the log-stub is active, non-prod only — no SES/domain setup yet).

## Options considered

**Gating.**
- *Hard gate* - block login until verified. Rejected: with no working `EmailSender` in prod yet, this would fully lock out prod registrations until SES is wired up during AWS deployment sequencing — a much bigger blast radius than password reset's prod gap, since it would block the core signup flow rather than just a recovery path.
- *Soft gate* - allow login, restrict trading until verified. Rejected for this pass as more than the current need requires; revisit once prod email delivery actually exists.
- *No gate* (chosen) - registration still auto-logs-in as it does today (`RegistrationService`); verification only records state (`User.emailVerifiedAt`) and sends the email. Nothing in the app currently checks verified status. A gate (soft or hard) can be layered on later once real email delivery exists in prod, without redesigning the token/record layer.

**Token storage.**
- *Column(s) on `User`* - rejected for the same reason ADR 0036 rejected it for password reset: only represents one outstanding token, no clean way to invalidate-and-reissue on resend, no audit trail.
- *New `email_verification_tokens` table* (chosen) - `id`, `user_id` (FK), `token_hash`, `expires_at`, `used_at`, `created_at`. Same shape as `password_reset_tokens` (Flyway `V3`); new migration `V4__create_email_verification_tokens_table.sql`. Server-generated 32-byte `SecureRandom` token, URL-safe base64-encoded, only the SHA-256 hash persisted — same reasoning as ADR 0036 (the token is already high-entropy random data, so slow/salted password hashing has no benefit here). 30-minute TTL, matching the reset-token precedent.

**Verified-state representation.** New nullable `email_verified_at TIMESTAMP` column on `User` (null = unverified). Chosen over a plain boolean so the record also captures *when* verification happened, at negligible extra cost. Existing rows are backfilled to "verified" (`now()`) in the `V4` migration itself, so current users aren't retroactively flagged unverified by a feature that didn't exist when they registered — matters for future reporting/gating even though no gate exists today.

**Delivery.** Reuses the `EmailSender` interface and `LogEmailSender` (`@Profile("!prod")`) from ADR 0036 unchanged — no new transport code. Registration calls `EmailSender.send(...)` with the verification link after the user is created, in the same request, without delaying or blocking the existing auto-login/session-establishment. In prod, with no working sender bean, the verification email simply won't be deliverable until SES is wired up during deployment sequencing — same tracked, non-silent gap pattern as ADR 0036's password-reset prod posture. Since there's no gate, this doesn't block anyone from using the app; it only delays when verification can actually complete for prod users.

**Resend endpoint.** Included in this pass (unlike a leaner "registration-email-only" alternative that was considered and rejected) so a user whose link expired or got lost isn't stuck without a self-service path. `POST /auth/resend-verification` issues a new token and invalidates prior outstanding ones for that user (mirrors single-use/one-live-token intent of the reset flow, even though multiple historical rows can exist in the table).

**Transport/framework wiring.** `/auth/verify-email` and `/auth/resend-verification` are unauthenticated-friendly (a verification link is clicked before any session exists in the general case, and resend needs to work for a just-registered-but-not-yet-verified user), so they get the same treatment as `/auth/forgot-password`/`reset-password`: added to CSRF `ignoringRequestMatchers` and `permitAll()`. New `RateLimitGroup.EMAIL_VERIFICATION` (client-IP keyed, 5/hour), covering both endpoints, mirrors `RateLimitGroup.FORGOT_PASSWORD`.

## Decision (summary)

- No login gate. Verification only records state; registration behavior (auto-login) is unchanged.
- New `email_verification_tokens` table (Flyway `V4`, same shape as `password_reset_tokens`): `id`, `user_id`, `token_hash` (SHA-256 of a 32-byte `SecureRandom` token), `expires_at` (30 min TTL), `used_at`, `created_at`.
- `User` gains nullable `email_verified_at TIMESTAMP`; existing rows backfilled to verified in the same migration.
- Registration sends a verification email (via the existing `EmailSender`/`LogEmailSender`) alongside its existing auto-login, without blocking it.
- `POST /auth/verify-email` (consume token) and `POST /auth/resend-verification` (issue a new token, invalidate prior outstanding ones) — both unauthenticated-friendly, added to CSRF exemption + `permitAll()`, covered by new `RateLimitGroup.EMAIL_VERIFICATION` (client-IP keyed, 5/hour).
- Frontend: register success flow adds a "verify your email" notice alongside the existing redirect to `/dashboard`; new `verify-email` route/component handles the token link (success/expired/invalid states) with a resend action for the failure cases.

## Consequences

- Prod email verification is not actually deliverable end-to-end until a real `EmailSender` (SES-backed) implementation is built as part of AWS deployment sequencing — same tracked-not-silent gap as ADR 0036's password reset. Since there's no gate, this doesn't block prod usage, only delays when verification itself can complete.
- No login-path or route-guard changes are needed for this pass, since there's no gate to enforce. Layering a soft or hard gate on later is a separate, smaller follow-up once prod email delivery exists — it would reuse `email_verified_at` as-is.
- Existing users are backfilled as verified in the `V4` migration, so this change has no effect on anyone who registered before it shipped.
- Single-use enforcement (`used_at`) and the 30-minute TTL mean an expired/lost link just needs a resend request; no manual cleanup job for expired/used rows exists yet, same accepted trade-off as ADR 0036.
