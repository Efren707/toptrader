# Leaderboards (Global & Friends)

> Status: **In progress**. Originally a high-level backlog stub; scoped into the decisions and sections below on 2026-08-22, once the Friends milestone (which the friends-scoped variant depends on) shipped; moved to `docs/tasks/in-progress/` (per [ADR 0040](../../adr/0040-work-tracking-docs-lifecycle.md)) when work on Section 1 began.

Working agreement applies as usual: one section at a time, check in before deciding anything not already settled below.

## Envisioned scope

- Two leaderboard widgets on the Dashboard: **global** (all eligible users) and **friends** (the viewer + their accepted friends only).
- Each shows the **top 5** users ranked by profit, cut short if fewer than 5 eligible candidates exist — e.g. a user with only 1 friend sees 2 rows (themselves + that friend), a fresh global leaderboard with only 3 eligible users shows 3 rows.
- Ranking metric: **total portfolio value change** (cash + holdings market value) over a rolling window, either the **last 1 day** or the **last 7 days**, switched by a single **Daily/Weekly toggle** that drives both widgets at once.
- Values update **once daily** (a scheduled batch job, not live/continuous) — see [ADR 0050](../../adr/0050-leaderboard-daily-snapshot-ranking.md) for why and how.
- The shared demo account is **excluded** from both leaderboards entirely.
- The **top 3** entries get a medal icon with the position number inside — gold (1st), silver (2nd), bronze (3rd); ranks 4-5 show a plain number.
- The **global** leaderboard also shows the viewer's own rank as a pinned row if they're outside the top 5. The **friends** leaderboard doesn't need this — it already includes the viewer by construction.

## Decided now

### Ranking metric and window: total portfolio value change, rolling 1/7 days
"Profit" means total portfolio value change (`cash_balance + Σ(holding quantity × current price)`), not just realized gains from completed trades — this way a user holding a stock that jumped in value counts, even if they haven't sold. The window is rolling (last 24h / last 7 days from whenever the value is computed), not a fixed calendar boundary (e.g. "yesterday" meaning the previous calendar day) — simpler, no timezone-boundary edge cases, and matches how the values are actually computed (see next decision).

### Values come from a daily batch snapshot, not live computation
Both the "current" value and the historical baseline value are read from a new `portfolio_snapshots` table, populated once daily by a new scheduled job — not computed live from Finnhub quotes at leaderboard-view time. This is the single biggest architectural decision in this feature (first scheduled job, first cross-user backend computation, first persisted valuation in the codebase) and gets its own ADR: see [ADR 0050](../../adr/0050-leaderboard-daily-snapshot-ranking.md) for the full reasoning, including why the job runs every calendar day (not just trading days) and why ranking is computed in plain Java over flat queries rather than a more clever SQL query.

### Missing-history users are excluded, not shown with a partial number
A user without a snapshot old enough for a given timeframe (e.g. joined yesterday — no 7-day-old snapshot yet) doesn't appear in that timeframe's ranking at all, rather than showing a number computed against a shorter, misleading window. Direct consequence: **every real user's weekly leaderboard will be empty for the first 6 days after this feature ships**, since nobody has 7 days of snapshot history immediately. This needs a deliberate empty state ("check back once you've been trading a week"), not silence that could look like a bug — the demo showcase account is exempted from this gap by Section 7's backdated seed data.

### Demo account: excluded from both leaderboard scopes, unconditionally
The shared demo account never appears on the global leaderboard, and never appears on anyone's friends leaderboard either — even though it has three real `ACCEPTED` friendships from `V9__seed_demo_friends.sql`. Enforced at the query/filter level in both scopes (belt-and-suspenders, mirroring `GET /users/search`'s `AND u.isDemo = false` precedent), not just the global one, since the friends scope would otherwise legitimately include it. One accepted quirk: if the demo account itself views its own friends leaderboard, it sees its 3 friends ranked against each other with no "self" row — consistent, not a bug.

### Viewer-rank pin: global only
If the viewer isn't in the global leaderboard's top 5, an extra pinned row shows their own rank ("you: #N"). The friends leaderboard needs no equivalent — it's built from {viewer} ∪ {accepted friends} in the first place, so the viewer is always present in it (or the leaderboard is empty if they have zero friends). A viewer who isn't ranked yet for the selected timeframe (no baseline snapshot) or who is the demo account gets no pinned row.

### One shared Daily/Weekly toggle, two separate endpoints
A single toggle above/between the two widgets controls the timeframe for both at once, rather than each widget having its own independent toggle — one mental model of "what timeframe am I looking at," matching how this was actually asked for. The backend still exposes `GET /leaderboard/global` and `GET /leaderboard/friends` as two separate endpoints (each taking a `timeframe` param) rather than one combined payload — consistent with ADR 0028's "no combined portfolio endpoint" precedent and how the Friends feature shipped multiple focused list endpoints rather than one aggregate response.

### Rate limiting
Extends the existing mechanism from [ADR 0034](../../adr/0034-api-rate-limiting.md) — no new ADR. One new `LEADERBOARD` group covering both `GET /leaderboard/global` and `GET /leaderboard/friends`, user-keyed, 20/minute (matches `QUOTE`/`SEARCH` — same shape of risk, a frequent scriptable read).

### No demo-account 403 guard, no dedicated IDOR test
Both leaderboard endpoints are read-only and scope entirely from the authenticated principal (no path/body parameter names another user's resource to spoof) — consistent with how the Friends feature's four read-only list endpoints needed neither a demo guard nor a dedicated IDOR test.

### No new formal User Story
Following the Friends/`user-profile-management.md` precedent: this stays a Milestone + Issues + ADR, with no new entry added to `docs/requirements/user-stories.md`/`acceptance-criteria.md`. (`vision.md` already names "a leaderboard ranking simulated portfolio performance across users" as a Full Vision item.)

## Sections

### 1. Backend — snapshot schema & entity

- [ ] `V10__create_portfolio_snapshots_table.sql` — `portfolio_snapshots` table: `user_id` (FK → `users`, `ON DELETE CASCADE`), `snapshot_date` (`DATE`), `total_value`, `created_at`; unique constraint on `(user_id, snapshot_date)`; separate index on `snapshot_date` alone (every ranking query's leading predicate is "all users on date X")
- [ ] `PortfolioSnapshot` entity (`@ManyToOne` lazy `user`, plain fields, no Lombok per ADR 0023)
- [ ] `PortfolioSnapshotRepository` — `findByUserAndSnapshotDate`, `findBySnapshotDate(date)` only; no ranking queries yet (section 3)
- [ ] `docs/architecture/data-model.md` updated with the new table
- [ ] Backend tests: entity mapping, unique-constraint violation on a duplicate `(user_id, snapshot_date)` insert

Depends on nothing new (uses existing `users`). GitHub Issue: [#188](https://github.com/Efren707/toptrader/issues/188)

### 2. Backend — scheduled snapshot job

- [ ] `@EnableScheduling` added to `BackendApplication`
- [ ] `UserRepository.findByIsDemoFalse()`, `HoldingRepository.findByUserIsDemoFalse()` (derived queries, keeping demo-exclusion in the query)
- [ ] `PortfolioSnapshotJob` (new `leaderboard` package) — `@Scheduled(cron = "0 30 16 * * *", zone = "America/New_York")` thin wrapper calling a testable `snapshotAll(LocalDate today)`; fetches one `FinnhubClient.fetchQuote` per distinct ticker held platform-wide (not per user, not via `QuoteService`); upserts one row per non-demo user per day; a user holding a ticker whose quote fetch failed that day is skipped entirely for that day (not recorded with a wrong value), not the whole job
- [ ] Backend tests: `snapshotAll` given a fixed date with mocked `FinnhubClient` — correct total value, idempotent re-run (upsert not duplicate), demo user excluded, a failed-ticker fetch excludes only its holders

Depends on section 1. GitHub Issue: [#189](https://github.com/Efren707/toptrader/issues/189)

### 3. Backend — ranking logic & friends-scoping

- [ ] `FriendshipRepository.findFriendUserIds(User user)` — new projection query returning just the accepted-friend user ids for a given user (nothing existing returns bare ids today)
- [ ] `LeaderboardService` — timeframe (`DAILY`/`WEEKLY`) → baseline-date mapping; fetches `findBySnapshotDate(today)` and `findBySnapshotDate(baselineDate)` as two flat lists and does the diff/sort/top-5-slice/viewer-rank lookup in Java (see ADR 0050 for why this is deliberately simple rather than a SQL-side ranking query); friends scope filters the candidate list to `{self} ∪ friendUserIds` before diffing; `isDemo = false` filter applied in both scopes unconditionally
- [ ] Percent-change computed as `(current − baseline) / baseline * 100`, matching `Quote.percentChange()`'s existing backend convention
- [ ] Backend tests: top-5 cutoff, fewer-than-5-eligible truncation (both scopes), exclusion of a user missing a baseline snapshot, friends scope correctly limited to self + accepted friends, demo excluded from both scopes even though it has accepted friends, viewer-pin present when outside top 5 / absent when inside it or not eligible / absent for the demo account

Depends on section 1 (test data can be inserted directly; doesn't strictly need section 2's job to exist). GitHub Issue: [#190](https://github.com/Efren707/toptrader/issues/190)

### 4. Backend — endpoints

- [ ] `LeaderboardTimeframe` enum (`DAILY`/`WEEKLY`), `LeaderboardEntry` record (`rank, userId, username, avatarKey, valueChange, percentChange`), `GlobalLeaderboardResponse` record (`entries, viewerEntry` — `viewerEntry` is `null` when the viewer is already in `entries`, not eligible for the timeframe, or is the demo account; the frontend can tell these apart by checking whether its own id is already in `entries`)
- [ ] `GET /leaderboard/global?timeframe=` → `GlobalLeaderboardResponse`; `GET /leaderboard/friends?timeframe=` → `List<LeaderboardEntry>` (no pin wrapper needed — see "Decided now")
- [ ] New `LEADERBOARD` entry in `RateLimitGroup` (per ADR 0034) — both paths, user-keyed, 20/minute; update `security-architecture.md`'s rate-limiting table to match
- [ ] `docs/architecture/api-contract.md` and `openapi.yaml` updated
- [ ] Backend tests: 400 on an invalid/missing `timeframe`, correct payload shape for both endpoints, rate-limit-exceeded (429) case added to `RateLimitFilterTest`

Depends on section 3. GitHub Issue: [#191](https://github.com/Efren707/toptrader/issues/191)

### 5. Frontend — LeaderboardService, Dashboard layout, shared toggle, Global widget

- [ ] `LeaderboardService` (`core/services/leaderboard.service.ts`, modeled on `friend.service.ts`): `getGlobalLeaderboard(timeframe)`, `getFriendsLeaderboard(timeframe)`, exported `LeaderboardEntry`/`GlobalLeaderboardResponse`/`LeaderboardTimeframe` types
- [ ] Dashboard restructured to a two-column grid for the two widgets (it currently has no grid/widget layout at all — just `.account-summary` then `.holdings-section` stacked in flow), adapting the Friends page's `.friends-layout` two-column grid pattern (collapsing to one column under a narrow viewport); first use of `app-card` on the Dashboard
- [ ] Shared Daily/Weekly toggle (one signal driving both widgets' `timeframe` param)
- [ ] Global leaderboard widget — rows follow the Performance page's `.stat-row` pattern (label-left/value-right, bottom border) plus the Friends page's avatar/username identity block (`avatarSrcFor`); new inline medal SVGs (gold/silver/bronze, matching the app's existing icon style) for ranks 1-3, plain rank number for 4-5; gain/loss `$`/`%` colored via the existing app-wide `positive`/`negative` convention; "you: #N" pinned row rendered when `viewerEntry` is present; empty state for a viewer with no snapshot history yet for the selected timeframe
- [ ] Frontend tests (`dashboard.spec.ts` extended, `HttpTestingController`): toggle switches both requests' `timeframe` param, medal shown only for ranks 1-3, viewer-pin row shown/hidden correctly, empty/loading/error states

Depends on section 4. GitHub Issue: [#192](https://github.com/Efren707/toptrader/issues/192)

### 6. Frontend — Friends leaderboard widget

- [ ] Friends leaderboard widget, reusing the row/medal styling built in section 5, no viewer pin
- [ ] Empty state for a viewer with too few friends or too little history to show anything meaningful yet
- [ ] Frontend tests + manual smoke test in a browser

Depends on section 5. GitHub Issue: [#193](https://github.com/Efren707/toptrader/issues/193)

### 7. Backend — seed historical snapshots for demo showcase

So a recruiter logging into the read-only demo account sees populated Daily *and* Weekly leaderboards on day one, not an empty Weekly view for the feature's first 6 days (see "Missing-history users" above) — same motivation as `V6__seed_demo_account.sql`/`V9__seed_demo_friends.sql`'s existing showcase seeding.

- [ ] New seed migration inserting ~8 days of backdated `portfolio_snapshots` rows for the demo account and its 3 seeded friends (`JP_Sullivan`/`Mike_Wazowski`/`Randall_Boggs` from `V9`) only — not for other real users, whose history can't be fabricated
- [ ] No new tests — seed data, verified via the manual smoke test below (consistent with `V6`/`V9`'s precedent)
- [ ] Manual smoke test: log into the demo account, confirm both Daily and Weekly leaderboards (global and friends) show populated, sensible rankings

Depends on sections 1 (table must exist) and `V9` (seeded friends must exist); sequenced last as a finishing touch, not core functionality. GitHub Issue: [#194](https://github.com/Efren707/toptrader/issues/194)

Each section also updates `docs/architecture/api-contract.md` and `docs/architecture/openapi.yaml` as part of its own PR where it adds/changes an endpoint, matching how other endpoint work has documented itself.
