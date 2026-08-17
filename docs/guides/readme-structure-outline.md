# README Structure — Outline

> Status: **Done** as of 2026-08-16 — `README.md` now covers every section below, plus three not originally scoped here: a Problem statement (expanded from `vision.md`), an Architecture section (Mermaid diagram + component notes), and a Tech decisions & trade-offs table (pulled from the relevant ADRs). Kept as a historical outline; `README.md` itself is the source of truth for actual content going forward.

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
