# End-User Guide — Outline

> Status: Draft outline only — no prose yet. Scope: MVP features (US-1..US-9 in [user-stories.md](../requirements/user-stories.md)). This is the structure the real guide will be written into once the app is live.

## 1. What is TopTrader?
A one-paragraph pitch: a stock trading simulator where you trade real stocks with virtual cash, no real money at risk. Sets expectations that prices are real/delayed market data (per ADR 0003) but trades are simulated.

## 2. Creating an account (US-1)
How to register with email/password, what's required (8-char min password per acceptance criteria), and what happens immediately after signup.

## 3. Logging in (US-2)
How to log in, and a note on session-based auth (e.g. what "staying logged in" means, when you'll be asked to log in again).

## 4. Your starting cash (US-3)
Explains the automatic $500 starting virtual cash balance, and why it's modest rather than a large demo balance (to make position sizing feel real).

## 5. Looking up a stock (US-4)
How to search by ticker, what the quote screen shows (price, and whether it's real-time or delayed), and what to do if a ticker isn't found.

## 6. Buying shares (US-5)
Step-by-step: search → enter whole-share quantity → confirm trade → see updated cash/holdings. Notes the whole-shares-only limit and the explicit confirmation step (per acceptance criteria).

## 7. Selling shares (US-6)
Step-by-step: from portfolio or search → choose quantity to sell (up to what you own) → confirm → see updated cash/holdings.

## 8. Viewing your portfolio (US-7)
What the portfolio page shows: ticker, quantity, average cost, current value, and remaining cash — and how current value relates to live/delayed prices.

## 9. Viewing transaction history (US-8)
Where to find your chronological buy/sell log and what each entry shows.

## 10. Tracking profit/loss (US-9)
How overall P/L is calculated relative to the $500 starting cash, and where it's displayed.

## 11. FAQ / troubleshooting
Placeholder for common questions that will emerge during testing (e.g. "why didn't my price update," "why can't I buy X shares") — to be filled in once real usage surfaces them.
