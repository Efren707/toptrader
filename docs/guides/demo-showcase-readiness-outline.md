# Demo / Showcase Readiness — Outline

> Status: Partially decided. This item can only be finished once the app is built and deployed (see the "Blocked" section) — this doc captures what could be decided ahead of time so there's less to figure out later. **Revisit this doc once the app is deployed to prod** to finish the blocked items below.

## Decided now

### Demo account seeding mechanism
A Flyway seed migration (versioned, e.g. `V<n>__seed_demo_account.sql`), using an idempotent guard (`INSERT ... ON CONFLICT DO NOTHING`) keyed on a fixed demo email. Runs automatically at startup like every other migration (ADR 0011) — no new mechanism, no new deploy step. Chosen over a backend startup seeder (extra demo-only code path) or a manual script (not reproducible/version-controlled, out of step with the rest of this project's process).

### Demo account content
- Fixed login (e.g. `demo@toptrader.com`), credentials documented in the README so reviewers can log in without registering.
- 3 holdings with a mix of gain / loss / flat performance, so the portfolio view and overall P/L (US-7, US-9) look realistic rather than a flat, untouched $500.
- Matching buy (and at least one sell) transactions in the history (US-8) that produced those holdings — the seed data tells a small coherent story, not just raw rows.
- Remaining cash left below $500 (not the full starting balance), so it reads as an account that's actually been used.

## Blocked until the app is deployed

- **README screenshots/GIF** — needs a working UI to capture. Revisit the [README structure outline](./readme-structure-outline.md)'s screenshots section once there's something to screenshot.
- **Live demo link callout** — needs a real prod URL (per `deployment-architecture.md`) to link to. Revisit the README structure outline's status-line section at the same time.
- **Writing the actual seed migration** — the mechanism and content shape are decided above, but the SQL itself depends on the final `users`/`holdings`/`transactions` schema as implemented (not just as designed in `data-model.md`) — write it once the backend + schema exist.
