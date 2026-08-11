# Demo Account & Showcase Readiness

> Status: Partially decided, unblocked as of 2026-08-11 now that the app is live at `app.toptrader.dev`. Originally tracked from [docs/tasks/in-progress/aws-infrastructure-implementation.md](../in-progress/aws-infrastructure-implementation.md)'s cutover section (section 8); split out into its own issue since it's showcase/polish work, not cutover — tracked as [#134](https://github.com/Efren707/toptrader/issues/134). Not yet picked up.

## Decided now

### Demo account seeding mechanism
A Flyway seed migration (versioned, e.g. `V<n>__seed_demo_account.sql`), using an idempotent guard (`INSERT ... ON CONFLICT DO NOTHING`) keyed on a fixed demo email. Runs automatically at startup like every other migration (ADR 0011) — no new mechanism, no new deploy step. Chosen over a backend startup seeder (extra demo-only code path) or a manual script (not reproducible/version-controlled, out of step with the rest of this project's process).

### Demo account content
- Fixed login (e.g. `demo@toptrader.dev`), credentials documented in the README so reviewers can log in without registering.
- 3 holdings with a mix of gain / loss / flat performance, so the portfolio view and overall P/L (US-7, US-9) look realistic rather than a flat, untouched $500.
- Matching buy (and at least one sell) transactions in the history (US-8) that produced those holdings — the seed data tells a small coherent story, not just raw rows.
- Remaining cash left below $500 (not the full starting balance), so it reads as an account that's actually been used.

## Now unblocked (tracked as #134)

- **README screenshots/GIF** — needs a working UI to capture. Revisit the [README structure outline](../../guides/readme-structure-outline.md)'s screenshots section once there's something to screenshot.
- **Live demo link callout** — needs a real prod URL (per `deployment-architecture.md`) to link to. Revisit the README structure outline's status-line section at the same time.
- **Writing the actual seed migration** — the mechanism and content shape are decided above, but the SQL itself depends on the final `users`/`holdings`/`transactions` schema as implemented (not just as designed in `data-model.md`) — write it once the backend + schema exist.
