# MVP User Stories

> Status: Accepted — 2026-07-16
> Scope: derived from the MVP Definition of Done in [vision.md](./vision.md)

## Stories

**US-1 — Register**
As a new user, I want to create an account, so that I have a persistent identity to hold virtual cash and trades under.

**US-2 — Log in**
As a returning user, I want to log in securely, so that I can access my portfolio.

**US-3 — Receive starting virtual cash**
As a newly registered user, I want to be given a starting virtual cash balance of **$500** automatically, so that I can begin trading immediately without setup steps. $500 is intentionally modest (vs. the common $100k demo-account convention) to keep position sizing and trade-off decisions realistic.

**US-4 — Look up a stock quote**
As a logged-in user, I want to search for a stock by ticker and see its current (or delayed, per Phase 2 research) price, so that I can decide whether to trade it.

**US-5 — Buy shares**
As a logged-in user, I want to buy a whole-share quantity of a stock at its current price, so that I can build a simulated position — provided I have sufficient virtual cash. MVP supports whole shares only (see Out of Scope); fractional shares are a tracked post-MVP story given the $500 starting balance.

**US-6 — Sell shares**
As a logged-in user, I want to sell whole shares I currently hold, so that I can realize simulated gains/losses and free up cash — limited to shares I actually own.

**US-7 — View portfolio**
As a logged-in user, I want to see my current holdings (ticker, quantity, average cost, current value) and remaining cash balance, so that I understand my overall position.

**US-8 — View transaction history**
As a logged-in user, I want to see a chronological list of my past buy/sell transactions, so that I can review my trading activity.

**US-9 — View profit/loss**
As a logged-in user, I want to see my overall profit or loss relative to my starting cash, so that I can gauge how I'm doing.

## Explicitly Out of Scope for MVP

- Leaderboard / comparing performance across users
- Price caching layer (calling the market data API directly is acceptable for MVP)
- Rate-limiting / abuse protection beyond basic auth
- Password reset / email verification flows (may be needed before "real" public use, but not required to prove the core trading loop)
- Social login (Google/GitHub OAuth) — plain email/password only for MVP, pending the auth research spike
- Options, crypto, forex, or any non-equity instrument
- Order types beyond simple market buy/sell (no limit orders, stop-loss, etc.)
- Mobile-native app (responsive web only)
- Real-time push updates (polling/refresh is acceptable)
- Fractional share trading (whole shares only for MVP — see Post-MVP Backlog)
- Depositing additional virtual cash beyond the initial $500 (see Post-MVP Backlog)

## Post-MVP Backlog (tracked, not built yet)

In addition to leaderboard / price caching / rate-limiting from [vision.md](./vision.md):

- **Deposit additional virtual cash** — let a user add more virtual cash to their account after the initial $500, so the sim isn't a one-shot bankroll.
- **Fractional share support** — allow buying/selling partial shares, so the $500 starting balance remains meaningfully usable across higher-priced stocks.
- **Avatar selection** — let a user pick a profile avatar from a set of preset characters, so accounts feel more personalized (surfaced during Phase 3 data-model design; see `docs/architecture/data-model.md`).
