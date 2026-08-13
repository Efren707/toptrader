# 0047 - Profile editing & account deletion: scope, gating, and cascade policy

- Status: Accepted
- Date: 2026-08-12

## Context

The remaining two undecided bullets from `docs/tasks/planning/user-profile-management.md`: which fields an "edit profile" page can change, and what happens to a user's data on "delete account." Research into the current codebase found:

- `User.java` has no setters for `username`/`email` — both are effectively immutable today.
- No edit-profile or delete-account endpoint exists anywhere; this is greenfield.
- Login (`UserDetailsServiceImpl`) looks up a user by **email**, so an editable email touches the login identity directly.
- `Holding` and `Transaction` both FK to `User`; `docs/architecture/data-model.md` documents `transactions` as "the immutable source-of-truth audit log the NFR requires."
- The demo account (`User.isDemo`, [ADR 0045](./0045-demo-account-login.md)) is a single account shared by every site visitor via the "Try Demo" button, already guarded against trading (`TradeService`) to keep its curated seed data stable for everyone after the current visitor.
- `PasswordResetService.resetPassword` already has a working pattern for invalidating a user's other active sessions after a credential change, via `SessionRegistry`.

## Options considered

### Editable fields
Username, email, password, and avatar (avatar's own ADR: [0046](./0046-profile-avatar-preset-picker.md)) are all in scope for a single edit-profile endpoint, reusing the same uniqueness checks (`existsByUsername`/`existsByEmail`) `RegistrationService` already applies.

### Email change: verification and timing
- *Defer the change until the new address is verified* (hold it in a separate `pending_email` column until confirmed) - rejected. Adds a second email-shaped column and more state to reason about, for a guarantee this app doesn't actually enforce anywhere else — [ADR 0037](./0037-email-verification-at-signup.md) already chose *no login gate* on verification at signup.
- *Update immediately, re-verify after the fact* (chosen) - the email column updates immediately (so login switches to the new address right away, matching how `UserDetailsServiceImpl` looks up by email), `email_verified_at` resets to `null`, and a new verification token is issued and emailed via the existing `EmailSender`/token pattern from ADR 0037 (same shape as its resend-verification flow). Consistent with ADR 0037's precedent of recording verification state without gating anything on it.

### Session invalidation
On any password change **or** email change, invalidate the user's other active sessions the same way `PasswordResetService.resetPassword` already does (iterate `SessionRegistry`, `session.expireNow()` for every session belonging to that user). Email change is included because it changes the login identity itself — same security rationale as a password change, even though the mechanism (`UserDetailsService` lookup by email) is different from what changed.

### Re-authentication (current-password re-entry)
Considered requiring the current password to be re-entered before email/password change or account deletion, as many apps do. Rejected for this pass: no other mutating endpoint in this app re-checks a password against the session (trade execution relies on session auth alone), and adding it only here would be an inconsistent one-off rather than an app-wide posture. Session auth alone gates these endpoints, same as everything else. Revisit uniformly later if warranted, not as a one-off exception.

### Demo account exclusion
Both the edit-profile and delete-account endpoints reject `isDemo=true` users with 403, mirroring the existing read-only-trading guard pattern from ADR 0045. Necessary because the demo account is a single row shared by every visitor via the "Try Demo" button — without this guard, any visitor could rename, re-email, or delete it, breaking the curated demo experience for everyone after them.

### Delete-account cascade
- *Hard cascade delete* (delete the user row and cascade-delete `holdings`/`transactions`) - rejected. Conflicts directly with `data-model.md`'s documented intent for `transactions` as an immutable audit log.
- *Soft-delete / anonymize* (mark the row deleted, scrub PII, keep historical rows) - rejected for this pass. A reasonable future evolution, but it widens every existing read path (every query touching `users` would need to account for deleted-but-present rows) for a need not yet demonstrated.
- *Block deletion while holdings or transactions exist* (chosen) - `DELETE /auth/me` returns `409 Conflict` if the user has any `holdings` or `transactions` rows, with a message directing them to liquidate first. A brand-new, never-traded account (still at the $500 starting balance, no transaction history) can delete freely. The user's own `password_reset_tokens`/`email_verification_tokens` rows are deleted as part of account deletion — they carry no audit significance, unlike `transactions`.

## Decision (summary)

- New endpoints on `AuthController`, session-authenticated (not CSRF-exempt, unlike the token-based auth endpoints): `PATCH /auth/me` (edit profile: username, email, password, avatar) and `DELETE /auth/me` (delete account).
- Username/email uniqueness re-checked the same way as registration.
- Email change updates immediately, resets `email_verified_at` to `null`, and re-sends a verification email via the existing ADR 0037 pattern.
- Password or email change invalidates the user's other active sessions via `SessionRegistry`, reusing `PasswordResetService`'s existing pattern.
- No current-password re-entry is required for any of these actions — session auth alone, consistent with the rest of the app.
- Both endpoints reject `isDemo=true` users with 403.
- `DELETE /auth/me` returns 409 if the user has any holdings or transactions; otherwise deletes the user row along with their own token rows.

## Consequences

- A user who has ever traded can never delete their account without first fully liquidating (edge case: an illiquid or delisted ticker could make full liquidation impossible — not handled by this pass, tracked as a known limitation rather than solved now).
- Not requiring re-authentication is a slightly weaker posture than many apps take for sensitive account actions; accepted as a deliberate trade-off to stay consistent with this app's existing all-session-auth model rather than special-casing just these two endpoints.
- Any future friends/leaderboard feature that surfaces `username` elsewhere (per the still-unscoped `docs/tasks/planning/friends.md`/`leaderboard.md`) needs to account for username no longer being immutable once this ships.
