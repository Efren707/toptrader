# Friends

> Status: **In progress**. Tracked under the [Friends milestone](https://github.com/Efren707/toptrader/milestone/17) (6 issues, #173-#178). Originally a high-level backlog stub, scoped into the decisions and sections below on 2026-08-14; moved to `docs/tasks/in-progress/` (per [ADR 0040](../../adr/0040-work-tracking-docs-lifecycle.md)) when work on Section 1 began.
>
> **Now up: Section 4** ([#176](https://github.com/Efren707/toptrader/issues/176), below) — Navbar Friends dropdown. Nothing else is blocked on a decision; every section below is ready to implement as-is.

Working agreement applies as usual: one section at a time, check in before deciding anything not already settled below.

## Envisioned scope

- Users can send friend requests to other users.
- Navbar gets a **"Friends" section** next to the account menu, on the right.
- An **orange circle badge** on it shows the count of pending friend requests.
- Clicking it opens a dropdown containing:
  - A **search input** to look up other users by username.
  - A list of **incoming friend requests**, each showing the requester's username on the left and two action buttons on the right — a checkmark (accept) and an x (deny).

## Decided now

### Friendship model: symmetric, single table with status enum
One mutual relationship once accepted (not a follow/mutual-follow model). Stored as a single `friendships` table with `requester_id`/`addressee_id` FKs to `users` and a `status` enum (`PENDING`/`ACCEPTED`) — no separate `friend_requests` table. Accept flips status; decline/cancel/remove all just delete the row. This is the first many-to-many/join-table entity and the first two-party resource in the codebase (existing `@PreAuthorize` authorization, per ADR 0035, was written for single-owner resources), so both get their own decision record: see [ADR 0049](../../adr/0049-friendship-data-model.md).

### Request workflow: cancel allowed, crossed requests auto-accept
The sender can cancel their own pending outgoing request. If B sends a request to A while A→B is already pending, the two are friended immediately instead of creating a second pending row (see ADR 0049).

### Search: partial match, relationship-status-aware
Username search is case-insensitive partial/prefix match, capped at a small result count, and excludes the searcher's own account. Each result row reflects current relationship state — "Add" (no relationship), "Requested" (pending outgoing, clickable to cancel), or "Friends" — rather than a bare "Add" button that just errors on a duplicate.

### Where friends live: navbar dropdown vs. Dashboard
The navbar dropdown stays exactly as originally scoped above — search input + incoming-requests list with accept/deny, plus the pending-count badge (badge counts incoming pending requests only, no broader notification concept). The list of **accepted friends** lives in a new section on the **Dashboard** instead, alongside the user's outgoing pending requests (each cancelable from there).

### Remove friend requires confirmation
Removing an accepted friend goes through a confirmation step (reusing the profile page's existing modal-confirm pattern — backdrop + centered panel, Escape/backdrop-click dismiss) rather than an immediate one-click remove.

### Account deletion & demo account
Deleting an account cascades to delete all of that user's friendship/request rows (`ON DELETE CASCADE`), consistent with [ADR 0048](../../adr/0048-account-deletion-cascade.md)'s precedent for other user-owned data. The shared demo account (`isDemo=true`) is rejected with 403 from every friend-mutating endpoint (send/cancel/accept/decline/remove), mirroring the existing guard from ADR 0045/0047. The demo account is also **excluded from `GET /users/search` results entirely** — otherwise a request sent *to* demo would sit `PENDING` forever, since demo can never accept/decline it (blocked above), leaving the sender's UI stuck on "Requested" with no resolution. Excluding it from search closes this off at the source rather than needing a second guard direction on the send endpoint.

### No blocking/muting feature (accepted limitation)
There's no way to prevent a specific user from re-sending a request after being declined — declining just deletes the row, so the same sender could immediately request again, bounded only by the `POST /friends/requests` rate limit (20/hour). Consistent with the original envisioned scope, which never included a block feature; not adding one preemptively per the project's "don't design for hypothetical future requirements" convention. Worth revisiting only if it turns out to be a real problem in practice.

### Unordered-pair uniqueness: enforced at the DB level
A unique index on `(LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id))` prevents A→B and B→A coexisting as two rows, backing up the service-layer canonicalized lookup rather than relying on it alone — belt-and-suspenders, consistent with how `RegistrationService`'s uniqueness checks pair a service-layer check with a DB unique constraint. See [ADR 0049](../../adr/0049-friendship-data-model.md).

### Rate limiting & other security measures
No new ADR — this extends the existing mechanism from [ADR 0034](../../adr/0034-api-rate-limiting.md) (`RateLimitGroup` enum + `RateLimitFilter`), not a new one. Two new groups, both user-keyed (session already exists for every friends endpoint, unlike `/auth/register`):
- `GET /users/search` — 20/minute per user (matches `QUOTE`'s threshold/window; same shape of risk — a frequent, scriptable read endpoint that could otherwise be used to scrape all usernames).
- `POST /friends/requests` (send) — 20/hour per user (looser than `REGISTER`'s 5/hr since it's not account-creation abuse, far tighter than `TRADE`'s per-minute since sending requests is an occasional action, not a rapid workflow — caps mass-requesting/harassment).

Accept/decline/cancel/remove get no dedicated bucket — each is inherently bounded by state that already exists (you can only act on a request/friendship that exists, and duplicate-request guards already prevent re-triggering), matching how `PATCH`/`DELETE /auth/me` also have none today. Also applying, per existing project convention rather than new decisions: a friendship IDOR/authorization test (security-architecture.md's "one IDOR test per resource type" requirement), search-query max-length validation via Bean Validation, and escaping `%`/`_` before building the `LIKE` pattern so a search for a wildcard character doesn't match every user.

### No new formal User Story
Following the `user-profile-management.md` precedent: this stays a Milestone + Issues + ADR, with no new entry added to `docs/requirements/user-stories.md`/`acceptance-criteria.md`.

## Sections

### 1. Backend — data model & send/cancel request endpoints

- [x] `V8__create_friendships_table.sql` — `friendships` table: `requester_id`/`addressee_id` (FK → `users`, `ON DELETE CASCADE`), `status` enum (`PENDING`/`ACCEPTED`), timestamps, `CHECK (requester_id <> addressee_id)`, plus a unique index on `(LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id))` to enforce the unordered-pair uniqueness at the DB level
- [x] `Friendship` entity (`@ManyToOne` to `User` for both `requester`/`addressee`, `@Enumerated(EnumType.STRING)` status, plain record DTOs elsewhere per ADR 0023 — no Lombok)
- [x] `FriendshipRepository` — needs a pair-lookup method covering both orderings (`(requester=A AND addressee=B) OR (requester=B AND addressee=A)`), used by send (duplicate/crossed-request check) and by search (status annotation)
- [x] `POST /friends/requests` — body `{ addresseeId }`; requester = authenticated principal
  - 201 + created row on success
  - 400 if `addresseeId` equals the caller's own id (self-request)
  - 404 if `addresseeId` doesn't exist
  - 403 if caller is the demo account
  - If a `PENDING` row already exists in the *same* direction (caller already requested this user): 409, idempotent no-op
  - If a `PENDING` row already exists in the *opposite* direction (target already requested caller): flip that row to `ACCEPTED` instead of creating a new one (crossed-request auto-accept, ADR 0049) — 200, not 201, since no new row was created
  - If already `ACCEPTED`: 409
  - `FriendshipService.sendFriendRequest` implements all of the above; `FriendshipController` picks the 200-vs-201 status per the note in ADR 0049
- [x] `DELETE /friends/requests/{id}` — cancel own pending outgoing request; 204 on success, 404 if not found or not `PENDING`
- [x] New `FRIEND_REQUEST` entry in `RateLimitGroup` (per ADR 0034) — `POST /friends/requests`, user-keyed, 20/hour; update `security-architecture.md`'s rate-limiting table to match
- [x] Backend tests: send success, self-request (400), nonexistent target (404), demo-account guard (403), duplicate same-direction (409), crossed-request auto-accept (200 + status ACCEPTED), already-friends (409), cancel success (204), cancel someone else's request (403, via `FriendshipAuthorization.isRequester`), cancel non-pending/nonexistent (404), rate-limit exceeded (429)

GitHub Issue: [#173](https://github.com/Efren707/toptrader/issues/173)

### 2. Backend — respond-to-request & remove-friend endpoints

- [x] `POST /friends/requests/{id}/accept` — only the addressee; flips `status` to `ACCEPTED`, sets `responded_at`; 200 + updated row; 404 if not found/not `PENDING`; 403 if caller isn't the addressee or is the demo account
- [x] `POST /friends/requests/{id}/decline` — only the addressee; deletes the row; 204; same 404/403 cases as accept
- [x] `DELETE /friends/{userId}` — remove an accepted friend, addressed by the *other user's id* (not the friendship row id); either party may call it; looks up the `ACCEPTED` row for (caller, userId) via the pair-lookup method from section 1; 204; 404 if no `ACCEPTED` row exists between the two; 403 if caller is the demo account
- [x] `FriendshipAuthorization` bean (per ADR 0049) — `isAddressee(friendshipId, principal)`, `isRequester(friendshipId, principal)`, wired into `@PreAuthorize` on the service methods for accept/decline/cancel; the remove endpoint doesn't need a bean check since it's looked up directly by (caller, userId) rather than by friendship id, so "either party" falls out of the lookup itself
- [x] Friendship IDOR/authorization test — a dedicated `FriendshipServiceAuthorizationTest` (mirroring `TradeServiceAuthorizationTest`) asserting user A gets 403 trying to accept/decline/cancel a friendship they're not the addressee/requester of, per `security-architecture.md`'s "one IDOR test per resource type" requirement
- [x] Backend tests: accept success, accept by non-addressee (403, exercises `FriendshipAuthorization.isAddressee`), accept non-pending/nonexistent (404), decline success + same auth/404 cases, remove success, remove a non-friend (404), remove while still `PENDING` (404, not yet friends), demo-account guard on accept/decline/remove — mirrors `TradeServiceAuthorizationTest`'s pattern for the deny-path tests

Depends on section 1. GitHub Issue: [#174](https://github.com/Efren707/toptrader/issues/174)

### 3. Backend — search & list endpoints

- [x] `GET /users/search?q=` — case-insensitive partial/prefix match on `username`, excludes the caller's own id **and any `isDemo=true` user**, capped at a small result count (e.g. top 10); 400 if `q` is blank/missing or exceeds a max length (Bean Validation `@NotBlank @Size(max=...)`, avoiding both an all-users query and an oversized query string)
  - `%`/`_` in the raw query are escaped before being embedded in the `LIKE` pattern (with an explicit `ESCAPE` clause), so searching for a literal wildcard character doesn't match every user
  - Each result: `{ id, username, avatarKey, relationshipStatus }` where `relationshipStatus` is one of `NONE | OUTGOING_PENDING | INCOMING_PENDING | FRIENDS` (per the search-status decision above) — computed via the same pair-lookup method from section 1
- [x] `GET /friends/requests/incoming` — caller's `PENDING` rows where caller is `addressee`; `{ id, requester: {id, username, avatarKey}, createdAt }` per row; this list's length is what drives the navbar badge count
- [x] `GET /friends/requests/outgoing` — caller's `PENDING` rows where caller is `requester`; same shape, `addressee` instead of `requester`
- [x] `GET /friends` — caller's `ACCEPTED` rows, returned as `{ id, username, avatarKey, friendsSince }` (the *other* user in each row, not the raw friendship rows); `friendsSince` is `respondedAt` (when the request was accepted), not `createdAt` (when it was first sent)
- [x] These four are read-only — no demo-account guard needed (consistent with demo being read-only elsewhere rather than blocked from viewing)
- [x] New `SEARCH` entry in `RateLimitGroup` (per ADR 0034) — `GET /users/search`, user-keyed, 20/minute (matches `QUOTE`); update `security-architecture.md`'s rate-limiting table to match
- [x] Backend tests for `GET /users/search`: excludes self, excludes demo account, partial/case-insensitive match, wildcard-character query treated literally, each of the four `relationshipStatus` values, blank/oversized query (400), result cap, rate-limit exceeded (429) — `UserSearchServiceTest` (unit) + `UserSearchControllerTest` (full-stack against real Postgres) + a case added to `RateLimitFilterTest`
- [x] Backend tests for the three list endpoints: each scoped correctly to the caller and excludes rows that don't belong to them — `FriendshipServiceTest` (unit, mapping + query-scoping cases for `getIncomingFriendRequests`/`getOutgoingFriendRequests`/`getFriends`)

Depends on section 1. GitHub Issue: [#175](https://github.com/Efren707/toptrader/issues/175)

### 4. Frontend — Navbar Friends dropdown

- [ ] `FriendsService` (`core/services/friends.service.ts`, modeled on `auth.service.ts`): `search(q)`, `sendRequest(addresseeId)`, `cancelRequest(id)`, `acceptRequest(id)`, `declineRequest(id)`, `getIncoming()`; typed request/response interfaces exported alongside
- [ ] Dropdown UI reusing the account-menu pattern (`shared/navbar/navbar.ts`/`.html`: viewChild + signal + outside-click `@HostListener` + `@if`-gated panel): search input (debounced, e.g. 300ms) showing status-aware result rows (Add / Requested-cancelable / Friends / Accept+Decline-if-incoming-pending), plus the incoming-requests list with accept/decline buttons
  - The "Add" click handler distinguishes the two possible `POST /friends/requests` outcomes: `201` (now pending — row flips to "Requested") vs. `200` (crossed request auto-accepted — row flips straight to "Friends", per ADR 0049)
- [ ] Orange pending-count badge on the Friends nav item — fetched on navbar init and refetched after any accept/decline action; hidden entirely when the count is 0, shown only for count > 0; no polling/websocket for this pass (out of scope, noted as a possible future improvement)
- [ ] Frontend tests (`navbar.spec.ts` extended, `HttpTestingController` pattern) + manual smoke test in a browser

Depends on sections 1-3. GitHub Issue: [#176](https://github.com/Efren707/toptrader/issues/176)

### 5. Frontend — Dashboard Friends list section

- [ ] New section on the dashboard: accepted friends list (username + avatar) with a remove button that opens a confirmation modal (reuse the profile page's modal pattern: backdrop + centered panel, Escape/backdrop-click dismiss) before calling `DELETE /friends/{userId}`
- [ ] Outgoing pending requests shown in the same section (or a clearly separated sub-section), each with a cancel action calling `DELETE /friends/requests/{id}`
- [ ] Empty states for both (no friends yet / no pending outgoing requests)
- [ ] Frontend tests + manual smoke test in a browser

Depends on sections 1-3. GitHub Issue: [#177](https://github.com/Efren707/toptrader/issues/177)

### 6. Backend — seed demo account with friends (showcase)

So a recruiter logging into the read-only demo account sees a populated Dashboard friends section, not an empty one — same motivation as `V6__seed_demo_account.sql`'s existing holdings/transactions seed (`docs/tasks/completed/demo-account.md`).

- [ ] `V9__seed_demo_friends.sql` — idempotent (`ON CONFLICT DO NOTHING`, keyed on fixed seed emails, same pattern as `V6`): 3 plain (non-demo) seed user accounts with realistic usernames/avatars, each an `ACCEPTED` `friendships` row with the demo account, backdated `created_at`/`responded_at` so it reads as an established friendship rather than "created seconds ago"
- [ ] These 3 seed accounts are otherwise ordinary users — **not** `isDemo=true` — so they're unaffected by the demo account's search exclusion (they still show up normally in search for any real user), and they don't need seeded holdings/transactions of their own (out of scope — this seed is only for populating demo's friends list, not a second demo-style showcase account)
- [ ] No new tests — this is seed data, verified via the manual smoke test below rather than an automated test (consistent with `V6`'s seed migration, which also added no dedicated test)
- [ ] Manual smoke test: log into the demo account, confirm the Dashboard friends section shows the 3 seeded friends

Depends on sections 1, 2 (needs the `friendships` table and `ACCEPTED` status to exist), and 5 (Dashboard friends section, to actually verify the seed visually) — sequenced last since it's a finishing touch on the completed feature, not core functionality. GitHub Issue: [#178](https://github.com/Efren707/toptrader/issues/178)

Each section also updates `docs/architecture/api-contract.md` and `docs/architecture/openapi.yaml` as part of its own PR, matching how other endpoint work has documented itself.

## Related

The [Leaderboard](./leaderboard.md) feature's friends variant depends on this feature shipping first.
