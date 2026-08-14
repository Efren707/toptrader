# User Profile Management

> Status: In progress. Tracked under the [User Profile Management milestone](https://github.com/Efren707/toptrader/milestone/16). Originally a high-level backlog stub (`docs/tasks/planning/user-profile-management.md`), scoped out into the decisions and sections below.

Working agreement applies as usual: one section at a time, check in before deciding anything not already settled below.

## Status

In progress — sections 1 (backend profile data model & edit-profile endpoint) and 2 (backend delete-account endpoint, cascade per ADR 0048) complete. Sections 3-5 (frontend) not started. No PR opened yet for either backend section — holding off until all sections are done, per issue-per-section but PR-per-milestone-ready-to-ship on this one.

## Decided now

### Avatar: preset picker, not upload — DiceBear-generated SVGs
A fixed set of preset icon assets shipped with the frontend; the user picks one, stored as `users.avatar_key`. No new AWS infra, no upload UI. Avatar is folded into the edit-profile work (data model + endpoint + page) from the start rather than bolted on afterward, to avoid reworking the endpoint/form twice. Asset source: **DiceBear** (open-source, MIT/CC0-licensed styles), pre-generated once via a local script into static SVGs — no runtime dependency on DiceBear in the shipped app. A specific Figma community icon pack was considered and rejected for now since its export/redistribution license couldn't be verified. See [ADR 0046](../../adr/0046-profile-avatar-preset-picker.md).

### Editable fields & email re-verification
Username, email, password, and avatar are all editable from one profile page/endpoint. Username/email uniqueness re-checked the same way as registration. Email change updates immediately (login switches right away, since login is by email) and resets `email_verified_at` to `null`, triggering a new verification email via the existing ADR 0037 flow. See [ADR 0047](../../adr/0047-profile-editing-and-account-deletion.md).

### Session invalidation
Password or email change invalidates the user's other active sessions, reusing `PasswordResetService`'s existing `SessionRegistry` pattern unchanged.

### No re-authentication required
No current-password re-entry for email/password change or account deletion — session auth alone gates these endpoints, consistent with the rest of the app (e.g. trade execution).

### Demo account exclusion
The shared demo account (`isDemo=true`) is rejected with 403 from both edit-profile and delete-account endpoints, mirroring the existing read-only-trading guard from ADR 0045. Without this, any visitor could rename or delete the one shared demo account.

### Delete-account cascade: cascade, not block
`DELETE /auth/me` deletes the user's holdings, transactions, and token rows (password-reset/email-verification) along with the user row itself, unconditionally (subject only to the demo-account guard) — no 409, no liquidate-first requirement. Originally scoped as block-not-cascade; reversed because blocking turned out to have no real exit for any user who had ever traded (selling holdings doesn't clear `transactions`, so the 409 never cleared). See [ADR 0048](../../adr/0048-account-deletion-cascade.md), which supersedes the relevant part of [ADR 0047](../../adr/0047-profile-editing-and-account-deletion.md).

## Sections

### 1. Backend — profile data model & edit-profile endpoint

- [x] `V7__add_avatar_key_to_users.sql` — nullable `avatar_key VARCHAR` column on `users`
- [x] `User.java` — `avatarKey` field + getter/setter; add missing setters for `username`/`email`
- [x] `UserSummary` (backend record) gains `avatarKey`, mapped in `from(User)`
- [x] New `UpdateProfileRequest` record (username, email, password, avatarKey — partial update)
- [x] `PATCH /auth/me` on `AuthController`, `@AuthenticationPrincipal UserPrincipal`, session-authenticated (not CSRF-exempt)
- [x] Username/email uniqueness checks (409 on collision), same as `RegistrationService`
- [x] Email change: update immediately, reset `emailVerifiedAt` to `null`, send new verification email (reuse ADR 0037 verify/resend flow)
- [x] Password or email change invalidates the user's other active sessions (reuse `PasswordResetService`'s `SessionRegistry` pattern)
- [x] `isDemo=true` users rejected with 403
- [x] Backend tests: successful update (incl. avatar), each uniqueness collision, email-change re-verification + session invalidation, password-change session invalidation, demo-account rejection

GitHub Issue: [#151](https://github.com/Efren707/toptrader/issues/151)

### 2. Backend — delete-account endpoint

- [x] `DELETE /auth/me` on `AuthController`, session-authenticated
- [x] `HoldingRepository.deleteByUser(User user)` and `TransactionRepository.deleteByUser(User user)` added (alongside the token repositories', per ADR 0047)
- [x] Deletion cascades unconditionally: holdings, transactions, `password_reset_tokens`, `email_verification_tokens`, then the user row, all within one `@Transactional` service method, then ends the session (per [ADR 0048](../../adr/0048-account-deletion-cascade.md))
- [x] `isDemo=true` users rejected with 403
- [x] Backend tests: cascade delete (holdings/transactions/tokens/user, in order), session termination (all sessions incl. current expired, `SecurityContext` cleared, session cookie cleared), demo-account rejection

Soft dependency on section 1 (shares the demo-account guard pattern; not a hard blocker). GitHub Issue: [#152](https://github.com/Efren707/toptrader/issues/152)

### 3. Frontend — Edit Profile page (incl. avatar picker)

- [ ] `/profile` route, inside the existing `authGuard`-protected `Layout` children group
- [ ] Frontend `UserSummary` interface gains `avatarKey`
- [ ] New `features/profile/` component: reactive form (username, email, password) following `register.ts`'s pattern (`FormBuilder.nonNullable.group`, `submitting`/`formError` signals, `ApiError.fieldErrors` mapped onto controls)
- [ ] Generate the preset avatar set: `@dicebear/core` + a chosen collection (e.g. `adventurer`) as a dev-only dependency, a short local script exports ~12-16 seeds as static SVGs into `frontend/src/assets/avatars/`, committed to the repo (no runtime DiceBear dependency)
- [ ] Preset avatar picker UI rendering the generated icon set, served via `<img src>`/static asset reference (not inline raw SVG)
- [ ] `AuthService.updateProfile(...)` calling `PATCH /auth/me`, updates `currentUser` signal on success (`tap()` pattern)
- [ ] Success/error feedback via `NotificationService` / `ApiError.detail`
- [ ] `profile.spec.ts` covers success/validation/error paths
- [ ] Manual smoke test in a browser

Depends on section 1. GitHub Issue: [#153](https://github.com/Efren707/toptrader/issues/153)

### 4. Frontend — Delete Account flow

- [ ] Danger-zone section on the `/profile` page, styled with `--color-danger`/`--color-danger-soft` tokens
- [ ] Two-step confirm flow following `trade-form.ts`'s pattern (`confirming` signal, inline Cancel/Confirm panel, `submitting` disables both buttons)
- [ ] `AuthService.deleteAccount()` calling `DELETE /auth/me`
- [ ] On success: clear `currentUser`, redirect to `/login` (matches existing `logout()` flow)
- [ ] Spec covers confirm/cancel/success paths
- [ ] Manual smoke test in a browser

Depends on section 2 and section 3 (shares the `/profile` route/component). GitHub Issue: [#154](https://github.com/Efren707/toptrader/issues/154)

### 5. Frontend — Navbar avatar/username display

- [ ] Account-menu trigger shows the user's avatar (fallback default icon when `avatarKey` is null) + username, replacing the current icon-only trigger
- [ ] Fix the non-reactive username read (`navbar.ts:20`, currently a one-time signal snapshot) so an in-session profile edit reflects immediately
- [ ] Add a "Profile" entry to the account-menu dropdown, navigating to `/profile`
- [ ] `navbar.spec.ts` covers the new trigger content and the Profile link
- [ ] Manual smoke test in a browser

Depends on section 3 (so `/profile` exists to link to) and section 1 (`avatarKey` on `UserSummary`). GitHub Issue: [#155](https://github.com/Efren707/toptrader/issues/155)
