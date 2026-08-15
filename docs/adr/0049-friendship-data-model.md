# 0049 - Friendship data model: single table, symmetric, two-party authorization

- Status: Accepted
- Date: 2026-08-14

## Context

The Friends feature (`docs/tasks/planning/friends.md`) needs to model a request → accept/decline → friendship lifecycle between two users. Nothing in the codebase precedes this: every existing relation is a simple `@ManyToOne` FK from a single-owner entity to `User` (`Holding`, `Transaction`), and every existing authorized endpoint scopes to exactly one user — the authenticated principal themselves. ADR 0035 explicitly flagged this gap as out of scope when it was written: "does not, by itself, guard a future endpoint that fetches by some *other* resource ID... that shape needs a per-entity ownership check... written when such an endpoint is added." A friend request is exactly that shape — its owner is not one user but a pair (requester, addressee), and different actions on it are only valid for one particular side of that pair.

## Options considered

### Data model: one table vs. two

- **Single `friendships` table** (chosen) — one row per relationship, `requester_id`/`addressee_id` FKs to `users`, a `status` enum (`PENDING`/`ACCEPTED`). Sending a request inserts a `PENDING` row; accepting flips it to `ACCEPTED`; declining, canceling, or removing a friend all just delete the row. One table, one set of queries, no move/copy step between "request" and "friendship" state.
- **Separate `friend_requests` + `friendships` tables** — rejected. Accepting a request would need a transactional move (delete from one table, insert into the other) for no behavioral benefit at this app's scale; the "request" and "friendship" states are really the same relationship at different points in its lifecycle, not two different kinds of data worth separate retention/query patterns.

### Relationship symmetry

- **Symmetric friendship** (chosen) — once `ACCEPTED`, the relationship is mutual with no directionality; `requester_id`/`addressee_id` only matter while `PENDING` (to know who may accept/decline vs. who may cancel).
- **One-way follow model** — rejected as unnecessary complexity; nothing in the envisioned scope (`friends.md`) calls for asymmetric relationships, and the Leaderboard feature's friends variant (which depends on this shipping) also assumes mutual friendship.

### Crossed requests (A→B pending, then B→A arrives)

- **Auto-accept** (chosen) — if a request arrives from B to A while an A→B request is already `PENDING`, the existing row flips straight to `ACCEPTED` instead of a second `PENDING` row being created. Avoids two simultaneous pending rows for the same pair (which the unique constraint on the unordered pair would reject anyway) and matches user intent: both sides already wanted to be friends.
- **Reject the second request with an error** — rejected; would surface as a confusing dead end ("request already pending") to a user who was about to accept anyway.

### Uniqueness & self-request

- One unique constraint on the *unordered* pair of user IDs (not one on `(requester_id, addressee_id)` alone) — otherwise A→B and B→A could coexist as two distinct rows, defeating the crossed-request handling above. Enforced at the service layer (canonicalize the pair, e.g. always query/insert with the lower `id` first) plus a DB-level check, mirroring the belt-and-suspenders approach `RegistrationService`'s uniqueness checks already take alongside DB unique constraints.
- A DB `CHECK (requester_id <> addressee_id)` constraint plus a service-layer guard prevent a self-request, consistent with defense-in-depth elsewhere in the codebase (e.g. demo-account guards exist at the service layer even though the frontend also wouldn't offer the action).

### Authorization: extending ADR 0035 to a two-party resource

ADR 0035's `@PreAuthorize` pattern binds a `userId` *method parameter* to the authenticated principal — it assumes one owner. A `Friendship` row has two parties, and the valid actor differs by action:

- **Send** — either user, scoped by the endpoint acting as the authenticated principal (no target-side check needed; the "recipient" is just whoever the search picked).
- **Cancel** — only the `requester` of that specific `PENDING` row.
- **Accept / decline** — only the `addressee` of that specific `PENDING` row.
- **Remove** (delete an `ACCEPTED` row) — either party.

None of these fit "bind a `userId` parameter to the principal" directly, since the resource is looked up by its own ID (`friendshipId`) and the check depends on *which column* the principal matches, conditional on the action. Chosen approach: same mechanism as ADR 0035 (`@EnableMethodSecurity` + `@PreAuthorize` at the service layer, not the controller), but the SpEL expression loads the `Friendship` first and compares against `requester.id`/`addressee.id` per action — e.g. `@PreAuthorize("@friendshipAuthorization.isAddressee(#friendshipId, authentication.principal)")` for accept/decline, a similarly named check for cancel, and an "is either party" check for remove. A small `FriendshipAuthorization` bean holds these named checks (one bean, several methods) rather than one generic ownership check, since the four actions have four different validity rules, not one.

## Decision

- One `friendships` table: `id`, `requester_id` (FK → `users`, `ON DELETE CASCADE`), `addressee_id` (FK → `users`, `ON DELETE CASCADE`), `status` (`PENDING`/`ACCEPTED`), `created_at`, `responded_at` (nullable, set on accept). Unique constraint on the unordered `(requester_id, addressee_id)` pair; `CHECK (requester_id <> addressee_id)`.
- Symmetric friendship — no directionality once `ACCEPTED`.
- Crossed requests auto-accept the existing `PENDING` row rather than erroring or creating a second row.
- Authorization is service-layer `@PreAuthorize`, per-action, via a small `FriendshipAuthorization` bean with one named check per action (isRequester / isAddressee / isEitherParty), following ADR 0035's mechanism but extended to a resource looked up by its own ID rather than by a `userId` parameter.
- Deleting a user cascades (`ON DELETE CASCADE`) to delete all `friendships` rows involving them, consistent with ADR 0048's precedent for other user-owned data.
- The shared demo account (`isDemo=true`) is rejected with 403 from every friend-mutating endpoint, mirroring the existing guard from ADR 0045/0047.

## Consequences

- This is the first many-to-many/join-table entity and the first two-party-authorization resource in the codebase — `FriendshipAuthorization` is a new pattern, not a reuse of an existing one, and is worth a look during review since there's no prior art in this app to compare it against.
- The unique-pair constraint means lookups (e.g. "does a relationship already exist between A and B") must canonicalize the pair consistently everywhere a query is written, or use an `OR`-based lookup (`(requester=A AND addressee=B) OR (requester=B AND addressee=A)`) instead of relying on argument order — worth a shared repository method rather than repeating this at each call site.
- If a future feature needs one-way following (e.g. a public "watch this trader" feature distinct from mutual friendship), this model doesn't support it — it would need its own table rather than reusing `friendships`, since this ADR deliberately chose symmetry over generality.
- Section 1 of the Friends milestone (`docs/tasks/planning/friends.md`) implements this migration/entity; `docs/architecture/data-model.md` gets the new table documented as part of that section's PR, not this ADR.
