# 0048 - Account deletion: cascade holdings/transactions instead of blocking

- Status: Accepted
- Date: 2026-08-13
- Supersedes: ADR 0047's "Delete-account cascade" decision

## Context

ADR 0047 decided `DELETE /auth/me` would return `409 Conflict` if the user had any `holdings` or `transactions` rows, directing them to "liquidate first" — reasoning that cascade-deleting `transactions` conflicted with `data-model.md`'s documented intent for `transactions` as an immutable audit log (itself grounded in the NFR at `docs/requirements/nfr.md:20`: "every buy/sell is recorded as an immutable transaction record").

Walking through the actual user flow under that decision exposed a dead end: a user can sell every holding down to zero, but `transactions` never empties — every historical buy/sell stays recorded forever. So the 409 never clears once a user has made a single trade, regardless of what they do afterward. "Liquidate first" implies deletion becomes possible after selling; it doesn't. In practice, only an account that has never traded can ever self-delete.

Re-reading the NFR that motivated the block: it requires transaction records not be *edited* after the fact, so balances stay derivable and trustworthy while the account is active. It says nothing about records surviving the account's own deletion. ADR 0047 read "immutable" as "must outlive the account," which is a stronger property than the NFR actually asks for.

TopTrader is a paper-trading simulator with no real money and no regulatory or compliance retention requirement — there's no admin panel or audit consumer that would ever read a deleted user's trade history. Preserving that history at the cost of making account deletion unusable for any active user is a bad trade for this app.

## Options considered

### Keep blocking (ADR 0047's decision)
Rejected — as shown above, this isn't a real self-service path for any user who has ever traded, which is the app's core use case. The 409 message promises a way out ("liquidate first") that doesn't exist.

### Soft-delete / anonymize
Mark the row deleted, scrub PII, keep historical rows. Rejected again for the same reason ADR 0047 rejected it: widens every existing read path (every query touching `users` would need to account for deleted-but-present rows) for a need — historical audit access after deletion — that doesn't exist in this app.

### Cascade delete (chosen)
`DELETE /auth/me` deletes the user's `holdings`, `transactions`, and token rows, then the user row itself, unconditionally (subject only to the existing demo-account guard). No holdings/transactions check, no 409 for this reason.

### Cascade mechanism: DB-level `ON DELETE CASCADE` vs. application-level deletes
- **DB-level cascade** - alter the `holdings.user_id`/`transactions.user_id` FK constraints via a new Flyway migration so deleting the user row cascades automatically. Rejected: this app has no existing precedent for FK-level cascade, and it would make deletion order invisible in application code.
- **Application-level deletes** (chosen) - `DeleteAccountService` explicitly calls `holdingRepository.deleteByUser(user)` and `transactionRepository.deleteByUser(user)` inside the same `@Transactional` method that already deletes the token rows and the user row. Consistent with the token-cleanup approach ADR 0047 already established (explicit `deleteByUser` calls, not a DB-level mechanism), and keeps the full deletion sequence readable in one place.

## Decision

- `DELETE /auth/me` no longer checks for existing `holdings` or `transactions`. It unconditionally deletes (in order, within one `@Transactional` service method): the user's `holdings`, `transactions`, `password_reset_tokens`, `email_verification_tokens`, then the `users` row itself, then ends the session.
- The `isDemo=true` guard (403) is unaffected and still applies.
- No re-authentication requirement change — still session-auth-only, per ADR 0047.
- `HoldingRepository` and `TransactionRepository` each gain a `deleteByUser(User user)` method (alongside the token repositories', which ADR 0047 already required).
- `docs/architecture/data-model.md:68` and the `user-profile-management.md` task file's "Decided now" section are updated to match; ADR 0047 keeps its original text with a pointer to this ADR at the superseded line, per this project's ADR-revision convention (see ADR 0028).

## Consequences

- Account deletion is now a genuine self-service action for every non-demo user, including active traders — no dead end.
- A deleted user's trading history is gone permanently along with the account; there is no recovery, admin view, or export path. Acceptable given this app has neither real money nor any consumer of historical data after account deletion.
- If a future feature needs post-deletion audit retention (e.g., an admin dashboard, abuse investigation, or a move to handling real money), this decision would need revisiting — likely via soft-delete/anonymize at that point, once there's an actual consumer to justify the read-path cost ADR 0047 and this ADR both declined to pay today.
- Section 2 of the User Profile Management milestone (`DELETE /auth/me`, issue #152) drops its block-on-holdings/block-on-transactions tests and gains cascade-delete tests instead (holdings deleted, transactions deleted, token rows deleted, user row gone, session ended).
