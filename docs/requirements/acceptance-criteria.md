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

## US-10 — Navigate between auth pages, and log out

- Login page shows a "Not registered? Create an account" link that navigates to `/register`.
- Register page shows an "Already have an account? Log in" link that navigates to `/login`.
- The TopTrader wordmark in the navbar is clickable and navigates to `/dashboard` from any page it appears on.
- Navbar account menu gains a "Log out" action.
- `AuthService` gains a `logout()` method that calls the backend, clears `currentUser` client-side, and redirects to `/login`.
- Backend: `POST /auth/logout` is explicitly configured (custom `logoutUrl`, session/cookie cleared, plain success response — no redirect) so it matches the documented contract and behaves correctly for a SPA caller.
- After logout, hitting a protected route (dashboard, transactions, performance) redirects to login, same as an expired/missing session today.

## US-11 — Consistent, evenly-spaced navbar

- Navbar appears on all authenticated pages, including stock details (currently missing there).
- Navbar does not appear on login or register.
- Navbar content spans the full width of the page container, with the account menu pinned to the right edge instead of leaving a dead gap after the search field.
- Existing responsive behavior at the `40rem` breakpoint is unaffected.

## US-12 — Correct search result row layout and click behavior

- Search result row shows ticker on the left and company name on the right, evenly spaced (price removed from the row).
- Clicking a search result navigates to the stock's details page without any error message flashing.
- The result button has an explicit `type="button"` so it can no longer trigger an accidental native form submission.
- The row's flex/spacing layout is applied to the actual content container (the button), not the unused `<li>` wrapper.

## US-13 — Dashboard holdings as a list with day change, linking to stock details

- Backend: Finnhub's `dp` (percent change) field is captured and threaded through `Quote` → `QuoteService` (`/quotes/{ticker}`) and `HoldingResponse` (`/trades/holdings`), which currently discard it.
- Frontend `Quote` and `Holding` models include the new day-change-percent field.
- Dashboard holdings render as a list, not a table: each row shows ticker + share quantity on the left, current price + daily % change (visually distinguished positive vs. negative) on the right.
- Clicking a holding row navigates to that stock's details page (`/stocks/:ticker`).
- Existing loading/error/empty states for holdings are preserved.

## US-14 — Redesign the stock details page (layout, position stats, trade form)

- Page uses a 2-column layout: ~65% left, ~35% right (collapsing to a single column on narrow viewports, consistent with the existing responsive breakpoint pattern).
- Left column: company name, current price, price/% change for the day; if the stock is held, also show equity, today's return ($ and %), total return ($ and %), average cost basis, and shares owned. Company news is explicitly out of scope (already-deferred backlog item).
- Right column: a single `TradeForm` with a Buy/Sell toggle at the top that switches the form's side — replacing today's two separately-rendered Buy and Sell forms.
- Trade form fields: quantity input plus a live-calculated cost (quantity × price).
- Submitting the form shows a "Review Order" step: an order summary (e.g. "You're placing a market order to buy $X.XX of TICKER. Your order will be routed to market makers. The final execution price may vary due to market volatility. Once executed, the transaction may not be undone."), a Submit [Buy/Sell] button, and a Back button to return to editing.
- Cash balance is shown near the submit action (e.g. "$X.XX available"), sourced from `AuthService.currentUser()`.
- Existing trade success/error states (order filled, server error) are preserved.

## US-15 — Edit profile

- A logged-in user can view and edit their username, email, password, and avatar from a dedicated profile page.
- Username/email changes are rejected with a clear "already in use" error if the new value collides with another account (same check as registration).
- Changing the email updates it immediately (login switches to the new address right away) and resets verification: `email_verified_at` is cleared and a new verification email is sent, reusing the existing verify/resend flow (ADR 0037).
- Changing the password or email invalidates the user's other active sessions (same mechanism as password reset), so a stolen session elsewhere is forced to re-authenticate.
- No current password re-entry is required to make any of these changes — session auth alone gates the endpoint, consistent with the rest of the app.
- The avatar picker offers a fixed set of preset icons; the selection is saved as `avatar_key` and reflected wherever the account menu/avatar is shown (navbar).
- The demo account cannot access this page/endpoint — attempting to do so is rejected (403) and the UI does not offer the option.
- Field-level validation errors (invalid email format, too-short password, blank username) are shown before hitting the server, same pattern as registration.

## US-16 — Delete account

- A logged-in user can permanently delete their account from the profile page, behind an explicit danger-zone confirmation step (no single-click deletion).
- If the user currently has any holdings or any transaction history, deletion is rejected (409) with a clear message directing them to liquidate their positions first — no partial deletion occurs.
- A brand-new account with no trades (still at the $500 starting balance) can delete freely.
- On successful deletion, the user's session is ended and they're returned to the login/register flow; their account, and their own password-reset/email-verification token rows, are removed.
- The demo account cannot access this action — attempting to do so is rejected (403) and the UI does not offer the option.
