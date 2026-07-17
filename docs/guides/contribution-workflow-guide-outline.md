# Contribution / Workflow Guide — Outline

> Status: Draft outline only — no prose yet. Scope: how work actually moves through this repo day-to-day — branching, commits, PRs, issues, and the ADR process. Assumes local setup is already done (see [developer-setup-guide-outline.md](./developer-setup-guide-outline.md)).

## 1. Branching strategy
Trunk-based development: `main` is always deployable, work happens on short-lived `feature/*` or `fix/*` branches merged via PR (ADR 0002). Notes there's no `develop` branch and no staging environment.

## 2. Commit message conventions
Conventional Commit prefixes (`feat:`, `fix:`, `docs:`, `chore:`, `research:`, `refactor:`, `test:`), and that this convention applies going forward from 2026-07-16 — earlier commits predate it.

## 3. Opening a pull request
How to open a PR against `main`, what the PR template expects (summary, changes, testing, checklist), and that there's no required reviewer (solo project) but CI must pass before merge (ADR 0017).

## 4. Issue and project tracking
How work is tracked: GitHub Issues + Projects board, Milestones mapped to roadmap phases (and later, MVP build-order features). Points to the three issue templates (bug report, feature request, research spike) and when to use each.

## 5. Writing an ADR
When a decision needs an ADR (any notable technical/process decision, per `docs/adr/0000-use-adrs.md`), the numbering convention, and the lightweight template (Context / Options considered / Decision / Consequences).

## 6. CI/CD pipeline expectations
What runs automatically on a PR/merge (lint → test → build → deploy per ADR 0016), and what a contributor should expect to see pass before merging (Spotless, ESLint+Prettier, integration/IDOR tests in the required gate).

## 7. Keeping the roadmap current
The end-of-session habit from `CLAUDE.md`: update `docs/ROADMAP.md` (checkboxes + "Current focus") whenever a task/phase completes or the plan changes, so no session needs to be re-briefed verbally.
