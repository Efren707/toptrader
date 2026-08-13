# README Structure — Outline

> Status: Draft outline only. Target structure for the root `README.md` once the app exists (demo link, screenshots) — see [Demo Account & Showcase Readiness](../tasks/completed/demo-account.md) for what unlocked the sections marked "later." Sections 2 (status line) and 3 (screenshots) are done as of 2026-08-13; sections 4-9 (Features, Tech stack, Documentation, Local dev quickstart, Contributing, License) remain outline-only — `README.md`'s current structure doesn't yet match this outline beyond sections 1-3.

## 1. Title + one-line pitch
Project name and a single sentence: stock trading simulator, virtual cash, real/delayed market data — matches the current README opening.

## 2. Status line
Done — swapped to the live demo link (`app.toptrader.dev`) in `README.md`.

## 3. Screenshots / GIF
Placeholder section for now (nothing to show pre-build). Once there's a working app, a few screenshots or a short GIF of the core buy/sell flow.

## 4. Features
User-facing bullet list derived from the MVP user stories (register, starting virtual cash, look up quotes, buy/sell whole shares, portfolio view, transaction history, P/L) — written for a reviewer skimming, not as story IDs.

## 5. Tech stack
Backend/DB/frontend/deployment — matches the current README's Stack section.

## 6. Documentation
Links to `/docs` subfolders (requirements, architecture, adr, guides) — matches the current README, kept as-is.

## 7. Local dev quickstart
Short version of setup (clone, Docker Compose, config, run) with a link to the full [developer setup guide](./developer-setup-guide-outline.md) for details, rather than duplicating all steps inline.

## 8. Contributing
Brief note on trunk-based workflow + PR expectations, linking to the [contribution/workflow guide](./contribution-workflow-guide-outline.md) for the full process.

## 9. License
MIT — matches the current README, kept as-is.
