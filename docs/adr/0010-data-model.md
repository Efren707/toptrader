# 0010 - Data model: stored running balances, bigint PKs, display-only username

- Status: Accepted
- Date: 2026-07-16

## Context

Phase 3 needed a concrete data model covering all nine MVP user stories (US-1..US-9): `users`, `holdings`, `transactions`, mapped out in `docs/architecture/data-model.md`. Several sub-decisions in that design go beyond just laying out tables/columns and warrant recording as their own ADR: how cash/holdings balances are maintained relative to the immutable transaction log (NFR: "portfolio/cash balances are derived from (or kept consistent with) that transaction history, not just a mutable running total"), primary key type, whether the newly-added `username` field participates in login, and how the $500 MVP starting cash is represented.

## Options considered

### Balance maintenance (`users.cash_balance`, `holdings.quantity`/`average_cost_basis`)

- **Stored, updated transactionally** - running totals updated in the same DB transaction as each `transactions` insert, with row-level locking to prevent lost updates under concurrent buy/sell requests. Cheap reads for the portfolio view (US-7); `transactions` remains the immutable audit log.
- **Derived from transaction history** - no stored running totals; cash/holdings recalculated by replaying/summing all transactions on every read. More strictly auditable by construction, but more DB work per portfolio view, and doesn't outright satisfy anything the stored approach doesn't already satisfy per the NFR's "derived from *or* kept consistent with" wording.

### Primary key type

- **Bigint auto-increment** - simple, small, fast joins/indexes.
- **UUID** - non-sequential, non-guessable IDs; more common in public-facing APIs or multi-database merge scenarios. Not a clear need here since authorization is already enforced server-side on every request (NFR) — no reliance on ID obscurity.

### Starting cash representation

- **App-level constant** - $500 lives in config/code, used both to set `cash_balance` at registration and as the P&L baseline (US-9). Simplest for MVP since every user's starting cash is identical (no deposit feature yet).
- **Stored per-user column** (`starting_cash`) - more future-proof for the post-MVP "deposit additional cash" story, at the cost of a column that's constant for every MVP user today.

### `username` field

- **Display-only handle** - unique, required at registration, shown in the UI, but login stays email+password only — no change to ADR 0004's auth flow or the US-2 acceptance criteria.
- **Also usable for login** - login accepts either email or username. More flexible, but expands ADR 0004's auth flow and the US-2 acceptance criteria for no requirement that asked for it.

## Decision

- **Balances are stored and updated transactionally**, not derived on read. `cash_balance` and `holdings` rows update in the same DB transaction as the corresponding `transactions` insert, using row-level locking to prevent race conditions.
- **Bigint auto-increment primary keys** for `users`, `holdings`, and `transactions`.
- **$500 starting cash is an app-level constant**, not a stored column.
- **`username` is a display-only handle** — unique, required at registration, not usable for login.

Full table/column detail lives in `docs/architecture/data-model.md`, not duplicated here.

## Consequences

- Buy/sell endpoints must wrap the `transactions` insert and the `users`/`holdings` balance updates in a single DB transaction with row-level locking (e.g. `SELECT ... FOR UPDATE` on the user row) to satisfy the NFR's "no lost updates or overspending from race conditions" requirement — a concrete implementation detail to carry into the eventual buy/sell service design.
- `transactions` remains the source of truth; if a stored balance and the transaction log ever disagree (bug, manual DB edit), the log is authoritative for any reconciliation/audit work.
- Revisit the app-level $500 constant once the post-MVP "deposit additional virtual cash" story is built — P&L (US-9) will then need to track net deposits rather than a fixed baseline, likely requiring a stored per-user starting/basis value at that point.
- Because `username` doesn't participate in auth, no rework is needed to ADR 0004's session/login flow or the US-2 acceptance criteria when it's added to the registration form.
- `avatar` selection (profile picture from a preset set) was raised during this design pass and deliberately deferred — tracked in `docs/requirements/user-stories.md`'s Post-MVP Backlog, not part of this schema.
