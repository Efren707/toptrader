# 0050 - Leaderboard ranking: daily batch snapshots, computed server-side

- Status: Accepted
- Date: 2026-08-22

## Context

The Leaderboard feature (`docs/tasks/in-progress/leaderboard.md`) ranks users by portfolio profit over a rolling 1-day or 7-day window, in two scopes (global, friends). Nothing in the codebase precedes this in two ways at once:

1. **It's the first feature needing *other users'* portfolio data.** ADR 0030 deliberately kept US-9 (profit/loss) entirely client-side — "no new backend surface, compute from data the frontend already has" — because the frontend already had everything it needed for the *authenticated user's own* P&L. A leaderboard ranking many users has no such option: no client can legitimately fetch another user's cash balance or holdings. ADR 0028's "no combined portfolio endpoint" precedent is also only a partial fit here — that decision was about avoiding a combined endpoint for *one* user's own data; this feature's underlying problem (aggregating *many* users' valuations) is a different shape of problem neither ADR anticipated.
2. **There's no notion of a portfolio value at a point in time anywhere in the codebase.** `TradeService.toHoldingResponse` computes unrealized gain/loss per-holding, per-request, from a live Finnhub quote — nothing is ever persisted. To know "profit over the last day/week" requires diffing against a value from 1 or 7 days ago, which doesn't exist unless something starts recording it.

## Options considered

### How to obtain a historical portfolio value to diff against

- **Daily batch snapshot (chosen)** — a new scheduled job (first `@Scheduled` job in this codebase) computes and stores every non-demo user's total portfolio value (`cash_balance + Σ(holding qty × price)`) once a day. Ranking becomes pure arithmetic: `today's snapshot − snapshot from N days ago`. No Finnhub calls at leaderboard-view time; one Finnhub call per *distinct ticker held platform-wide*, once a day, regardless of how many users hold it.
- **Live historical price lookup** — rejected. Computing "value N days ago" live would mean replaying each user's holdings as of that date and calling Finnhub's historical/candle endpoint per distinct ticker *per ranked user* on every leaderboard view — an unbounded, repeated cost for data that doesn't change once the day has passed. The batch approach computes it once and reuses it forever.

### Job cadence: every calendar day vs. only trading days

- **Every calendar day (chosen)**, including weekends — a weekend snapshot just re-records the prior close's unchanged value. Necessary because ranking is exact-date-offset arithmetic (`today − 1`, `today − 7`); if the job only ran on trading days, "1 day ago" would have no row every Monday (Sunday has none), silently breaking the daily leaderboard on a predictable, recurring schedule.
- **Trading-days-only, matching ADR 0021's market-calendar/hours logic** — rejected for this feature specifically, despite being the more "correct" mirror of real market activity, because it reintroduces exactly the gap above. ADR 0021's calendar logic stays exactly as-is for its own purpose (gating live trading); this job's date arithmetic is a different concern and doesn't reuse it.

### Ranking computation: SQL-side ranking vs. plain Java

- **Fetch flat snapshot rows, rank in the service layer (chosen)** — `PortfolioSnapshotRepository` exposes only `findByUserAndSnapshotDate` and `findBySnapshotDate(date)`; `LeaderboardService` fetches the current and baseline date's rows as two lists, builds a lookup map, and does the diff/sort/top-5-slice/viewer-rank-lookup in Java. Consistent with how the rest of the codebase already puts business logic in the service layer over simple queries (e.g. `FriendshipService.toFriendResponse` picks the "other" side of a friendship in Java, not SQL) rather than in increasingly clever queries. At this app's scale (no pagination anywhere yet, per `api-contract.md`), loading a day's full snapshot set into memory to rank it is negligible.
- **A ranking query using an ad-hoc entity-to-entity JPQL join plus a `COUNT`-based rank lookup for the viewer pin** — considered and rejected. Every existing `@Query` in this codebase joins through a mapped `@ManyToOne` association; an ad-hoc `JOIN Entity ON` between two otherwise-unrelated entities, plus a separate ranking-by-count query, would both be firsts with no precedent to lean on, for a performance benefit this app doesn't need yet. Simpler wins.

### Demo-account exclusion

- Excluded from ranking in both scopes (global and friends) unconditionally, at the query/filter level — mirroring `UserRepository.searchByUsername`'s `AND u.isDemo = false` precedent (bake exclusion into the data path, don't filter after the fact). This matters concretely here because the demo account has three real `ACCEPTED` friendships (`V9__seed_demo_friends.sql`) — without an explicit guard in the *friends* scope too, the demo account would otherwise legitimately qualify as a "candidate" for its own friends leaderboard.

### Missing-history eligibility

- **Exclude a user from a timeframe if they lack a snapshot old enough (chosen)** — e.g. a user who joined yesterday has no 7-day-old snapshot and simply doesn't appear in the weekly ranking, rather than showing a number computed against a shorter, misleading window.
- Consequence accepted knowingly: **every real user's weekly leaderboard will be empty for the first 6 days after this feature ships**, since nobody has 7 days of snapshot history yet. This needs a deliberate empty state ("check back once you've been trading a week"), not silence that could look broken. The demo showcase account is exempted from this gap by seeding backdated snapshot history directly (Section 7 of the planning doc), the same way `V6`/`V9` backdate the demo account's other showcase data.

## Decision

- New `portfolio_snapshots` table: `id`, `user_id` (FK → `users`, `ON DELETE CASCADE`, per ADR 0048's precedent), `snapshot_date` (`DATE`, not a timestamp — ranking is exact-date arithmetic), `total_value`, `created_at`. Unique constraint on `(user_id, snapshot_date)`, making the nightly job idempotent (safe to re-run same-day). A separate index on `snapshot_date` alone serves the "all users on date X" access pattern every ranking query needs.
- A new daily `@Scheduled` job (`PortfolioSnapshotJob`, new `leaderboard` package) computes every non-demo user's total portfolio value once a day (4:30pm ET, after NYSE close) and upserts one row per user per day. It fetches one Finnhub quote per distinct ticker held platform-wide (not per user, not via `QuoteService`, which does unrelated company-profile lookups and throws HTTP-shaped exceptions with no meaning in a background job) — a ticker whose quote fetch fails that day causes only its holders to be skipped, not the whole job; they simply reappear once a fresh consecutive snapshot pair accumulates. Runs every calendar day, not just trading days.
- Ranking is computed in `LeaderboardService` from two flat `findBySnapshotDate` fetches (current date, baseline date = `today − 1` or `today − 7`), diffed and sorted in Java — no ranking SQL, no window functions.
- Both leaderboard scopes (global, friends) exclude the demo account unconditionally at the filter level, even in the friends scope where the demo account has real accepted friends.
- A user without a snapshot at the required baseline date is excluded from that timeframe's ranking entirely.

## Consequences

- This is the first server-side computation of *other users'* financial data in the codebase, and the first scheduled/batch job — a real, deliberate architectural departure from ADR 0030's "no new backend surface" precedent for P&L-shaped features. ADR 0030 itself is unaffected: `/performance` stays exactly as it is, client-side, for the authenticated user's own P&L.
- The leaderboard is only ever as fresh as the last successful daily run — acceptable per the original planning stub's own framing ("no need for continuous/real-time updates"), but means a missed run creates a temporary, self-healing gap (affected users just don't appear until a fresh consecutive snapshot pair exists), not a visible error. Worth remembering before chasing a "why did I vanish from the leaderboard" report as a bug.
- If a future feature needs live/real-time ranking (e.g. an intraday leaderboard), this batch model doesn't support it — that would need a genuinely different design (live valuation, likely with caching), not an extension of this one.
- One Finnhub call per distinct platform-wide ticker, once daily, is a sequential loop of blocking HTTP calls in the job — fine at this app's expected scale (ADR 0005's single-EC2, low-traffic deployment) but worth a glance if the distinct-ticker set ever grows very large.
- Section 1 of the Leaderboard milestone (`docs/tasks/in-progress/leaderboard.md`) implements this migration/entity; `docs/architecture/data-model.md` gets the new table documented as part of that section's PR, not this ADR.
