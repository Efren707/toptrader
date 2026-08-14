# User Profile Management

> Status: In progress. Tracked under the [User Profile Management milestone](https://github.com/Efren707/toptrader/milestone/16). Originally a high-level backlog stub (`docs/tasks/planning/user-profile-management.md`), scoped out into the decisions and sections below.

Working agreement applies as usual: one section at a time, check in before deciding anything not already settled below.

## Status

All 5 sections complete: 1 (backend profile data model & edit-profile endpoint), 2 (backend delete-account endpoint, cascade per ADR 0048), 3 (frontend edit-profile page incl. avatar picker), 4 (frontend delete-account flow), 5 (navbar avatar/username display). PR open, closing issues #151-#155.

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

### 3. Frontend — Edit Profile page (incl. avatar picker) — complete

- [x] `/profile` route, inside the existing `authGuard`-protected `Layout` children group
- [x] Frontend `UserSummary` interface gains `avatarKey`
- [x] New `features/profile/` component: reactive form (username, email, password) following `register.ts`'s pattern (`FormBuilder.nonNullable.group`, `submitting`/`formError` signals, `ApiError.fieldErrors` mapped onto controls). Unlike `register.ts`, every field is independently optional (partial update): blank or unchanged-from-current is omitted from the submit payload per-field (password included only when non-blank; username/email included only when non-blank *and* different from the prefilled value); if the built payload ends up empty, `submit()` skips the HTTP call entirely.
- [x] Generate the preset avatar set: `@dicebear/core` + `@dicebear/styles` (**Critters** style — CC0-licensed, first-party DiceBear style) as dev-only dependencies; `frontend/scripts/generate-avatars.mjs` (`npm run generate:avatars`) exports 16 fixed-seed SVGs into `frontend/public/avatars/` — **not** `frontend/src/assets/avatars/` as originally planned above: this Angular project's actual static-assets root is `public/` (see `angular.json`'s `assets` config and the existing `/fonts/...` references in `styles.css`), a detail this milestone's earlier planning got wrong. No runtime DiceBear dependency in the shipped app.
- [x] Preset avatar picker UI: clicking the avatar opens a modal overlay (backdrop + centered panel) showing all 16 presets in a grid; Cancel discards the pick, Confirm stages it into the form's `avatarKey` control only (no separate API call — it rides along with the next "Save changes" submit, same as any other field). Backdrop click (target-checked, not `stopPropagation`) and Escape both dismiss, for accessibility-lint compliance and keyboard support.
- [x] `AuthService.updateProfile(...)` calling `PATCH /auth/me`, updates `currentUser` signal on success (`tap()` pattern)
- [x] Success/error feedback via `NotificationService` / `ApiError.detail`, plus server field-errors (409 collisions) mapped onto the matching control
- [x] `profile.spec.ts` covers prefill, avatar fallback/selection, empty/partial payload construction, password inclusion, client-side validation, in-flight button state, success handling, field-level and generic error handling, and all four avatar-picker interactions (17 tests)
- [x] Manual smoke test in a browser

Two related fixes surfaced while building this section, outside the original scope list above:
- `SecurityConfig.java`'s CORS `allowedMethods` was missing `PATCH` (only had `GET/POST/PUT/DELETE/OPTIONS`), so every `PATCH /auth/me` call failed the browser's CORS preflight. Added `PATCH` to the list.
- `shared/ui/input/input.ts`'s CVA `value`/`disabled` fields were plain (non-signal) class properties. They didn't reliably re-render after being set programmatically post-initial-render (e.g. via `form.patchValue()` clearing the password field after a successful save) — Angular wasn't consistently re-checking that nested child view for plain-field mutations, unlike signal-driven bindings (`Button`'s `[disabled]="submitting()"` worked fine by contrast). Converted both to `signal()`. This is a shared component used by every form in the app, not just this page.

Depends on section 1. GitHub Issue: [#153](https://github.com/Efren707/toptrader/issues/153)

### 4. Frontend — Delete Account flow — complete

- [x] "Delete account" trigger on the `/profile` page, below "Save changes" in the edit-profile form
- [x] Two-step confirm flow, but as a modal (backdrop + centered panel, `confirming` signal) rather than the originally-planned inline danger-zone card — matches the avatar picker's existing modal pattern on the same page (backdrop click / Escape both dismiss) instead of introducing a second, inline-panel interaction style; `submittingDeleteAccount` (separate from the edit-form's `submitting`, since the two actions are independent) disables both Cancel/Confirm during the request
- [x] `AuthService.deleteAccount()` calling `DELETE /auth/me`, `tap()`-clears `currentUser` on success (mirrors `logout()`)
- [x] On success: redirect to `/login` (matches existing `logout()` flow); on failure (e.g. demo-account 403): error shown in the modal, panel stays open for retry
- [x] Added a `danger` variant to the shared `Button` component (outlined red, fills on hover) for the delete trigger and the modal's confirm button — first use of a third button variant, styled via the component itself rather than a CSS override, since Angular's emulated view encapsulation blocks parent stylesheets from reaching into a child component's own template
- [x] `profile.spec.ts` covers opening the modal, cancel (no API call), confirm success (redirect + notification), and confirm failure (error shown, buttons re-enabled)
- [x] Manual smoke test in a browser

Depends on section 2 and section 3 (shares the `/profile` route/component). GitHub Issue: [#154](https://github.com/Efren707/toptrader/issues/154)

### 5. Frontend — Navbar avatar/username display — complete

- [x] Account-menu trigger shows the user's avatar (falls back to the `nova` preset when `avatarKey` is null, matching `profile.ts`'s own fallback) + username, replacing the previous icon-only trigger
- [x] Fixed the non-reactive username read (was `navbar.ts:21`'s one-time signal snapshot) — now holds a direct reference to `authService.currentUser` itself so the template re-reads it on every change-detection pass, reflecting an in-session profile edit immediately
- [x] "Profile" entry in the account-menu dropdown, navigating to `/profile` (this already existed in the markup from earlier section work; this section added its test coverage)
- [x] `navbar.spec.ts` covers avatar src (incl. `nova` fallback), username display, the reactivity fix (user changes without recreating the component), and the Profile link
- [x] Manual smoke test in a browser

Also removed the account-button's border and added a rotating chevron icon (points down/up with dropdown state) alongside the avatar/username, as a small polish pass on top of the section's original scope.

Depends on section 3 (so `/profile` exists to link to) and section 1 (`avatarKey` on `UserSummary`). GitHub Issue: [#155](https://github.com/Efren707/toptrader/issues/155)
