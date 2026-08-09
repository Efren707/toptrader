# Leaderboards (Global & Friends)

> Status: Not yet scoped. High-level backlog entry captured ahead of time — to be broken into GitHub issues, user stories, and acceptance criteria when picked up for implementation.

## Envisioned scope

- Two leaderboards: **global** and **friends**.
- Each lists the **top 5 users by profit** made by the end of trading hours.
- Updates **once daily**, when the typical trading market closes — no need for continuous/real-time updates.
- The **top 3** entries get a medal icon with the position number inside: **gold** (1st), **silver** (2nd), **bronze** (3rd).

## Dependency

The friends leaderboard variant depends on the [Friends](./friends.md) feature existing first — it needs a friend relationship to scope which users appear on it. Sequence accordingly when these are picked up for real implementation.
