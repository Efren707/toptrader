# MVP Acceptance Criteria

> Status: Accepted — 2026-07-16
> Maps 1:1 to the stories in [user-stories.md](./user-stories.md). Written to be concrete enough to drive test cases later (Phase 3/4), without prescribing implementation.

## US-1 — Register

- Given a valid, not-already-registered email and a password of at least 8 characters (length-only policy, no forced complexity rules — aligned with modern NIST guidance; real protection comes from hashing + brute-force throttling), submitting registration creates an account and the user ends up authenticated (or is sent to log in — exact flow decided in Phase 3).
- Given an email that's already registered, registration is rejected with a clear "already in use" error.
- Given an invalid email format or a password under 8 characters, registration is rejected with a field-level error before hitting the database.
- The password is never stored or logged in plaintext (per [nfr.md](./nfr.md)).

## US-2 — Log in

- Given correct credentials, the user is authenticated and can access their portfolio.
- Given incorrect credentials (wrong email or wrong password), a single generic "invalid email or password" error is shown — the system never reveals whether the email exists (avoids user enumeration).
- After repeated failed attempts against the same account, further attempts are temporarily throttled/locked (per [nfr.md](./nfr.md) brute-force protection).
- Authenticated state persists across a page refresh until logout or session/token expiry.

## US-3 — Receive starting virtual cash

- Immediately upon successful registration, the account is created with exactly **$500.00** virtual cash and this is visible on first login.
- The $500 starting grant happens exactly once per account — it is never re-applied on subsequent logins.

## US-4 — Look up a stock quote

- Given a valid, known ticker symbol, the user sees the company name, current (or delayed, per the pending data-source decision) price, and the as-of timestamp for that price.
- Given an unknown/invalid ticker, the user sees a clear "not found" message — no raw error or crash.
- If the market data provider is unreachable/erroring, the user sees a graceful error state, not a raw exception or blank page.

## US-5 — Buy shares

- Given a valid ticker and a whole-number quantity ≥ 1, the user is shown an explicit confirmation step (e.g. "Buy 3 AAPL @ $210.50 — Confirm?") before the trade executes — no single-click/immediate execution.
- Upon confirmation, given sufficient cash to cover `price × quantity`, the buy succeeds: cash balance decreases by the exact cost, the holding is created or increased, and a transaction record is created.
- Given insufficient cash, the buy is rejected with a clear error and no partial state change occurs (checked both before showing the confirm step and again at confirmation, in case the balance changed in between).
- Given a zero, negative, or fractional quantity, the buy is rejected client- and server-side (fractional shares are post-MVP, per user-stories.md).
- The price used for execution is the quote price shown at the confirmation step (not silently re-fetched at a different price after the user confirms).

## US-6 — Sell shares

- Given a quantity ≤ the shares currently held for that ticker, the user is shown an explicit confirmation step before the trade executes.
- Upon confirmation, the sell succeeds: cash balance increases by the exact proceeds, the holding decreases (or is removed if fully liquidated), and a transaction record is created.
- Given a quantity greater than what's held, the sell is rejected with a clear error and no partial state change occurs.
- Given a zero, negative, or fractional quantity, the sell is rejected.

## US-7 — View portfolio

- Displays current cash balance, and for each holding: ticker, quantity, average cost basis, current price, current market value, and unrealized gain/loss.
- Displays total portfolio value (cash + sum of holding values).
- A brand-new user with no trades yet sees a clean "just your $500 cash, no holdings" state — not an error or broken layout.
- Values reflect current prices as of the most recent quote fetch (per [nfr.md](./nfr.md), live-push is not required — refresh-driven is acceptable).

## US-8 — View transaction history

- Displays all of the logged-in user's past buy/sell transactions (ticker, side, quantity, price, total, timestamp), most recent first.
- A user only ever sees their own transactions, never another user's (server-side authorization enforced, not just hidden in the UI).
- A brand-new user with no trades sees a clean empty state.

## US-9 — View profit/loss

- Displays overall P&L as `current total portfolio value − $500 starting balance`, shown in both dollar amount and percentage.
- P&L reflects current holdings' market value, consistent with what's shown on the portfolio view (US-7).
- Negative P&L is displayed clearly as a loss (not just an unsigned/ambiguous number).
