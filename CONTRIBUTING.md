# Contributing to TopTrader

This is a solo, resume/learning project, but it's built with the habits of a real engineering workflow — that discipline is part of the point. This doc is the day-to-day reference for how work moves through the repo: planning, branching, committing, testing, and opening a PR.

For the bigger picture (what the app is, current status, architecture decisions), see [CLAUDE.md](./CLAUDE.md), [docs/ROADMAP.md](./docs/ROADMAP.md), and [docs/adr/](./docs/adr/). For getting a local dev environment running, see [docs/guides/developer-setup-guide-outline.md](./docs/guides/developer-setup-guide-outline.md).

## 1. Plan before implementing

Before writing code for a non-trivial feature or fix, work out the approach and open questions first — which layer it touches, which existing pattern to follow, any scope questions. Don't start typing implementation code and figure out the design as you go. This mirrors the "always check in before deciding" rule in `CLAUDE.md` and keeps changes small and reviewable, one step at a time rather than several features built ahead of plan.

## 2. Branching strategy

Trunk-based development (ADR [0002](./docs/adr/0002-branching-strategy.md)): `main` is always deployable. Create a short-lived branch for each unit of work:

- `feature/<short-description>` for new functionality
- `fix/<short-description>` for bug fixes

There's no `develop` branch and no staging environment — branch off `main`, merge back to `main` via PR, and delete the branch once it's merged.

**Merge strategy: merge commit** (GitHub's "Create a merge commit," not squash or rebase). This keeps every step-by-step commit from the branch visible on `main`'s history, alongside one merge commit tying them together — consistent with committing after each step (below) and with this project's history being part of the resume story, not just the end state.

## 3. Commit as you go

Commit **after each completed step**, not in one large commit at the end of a session. Small, frequent commits make the history readable and make it easy to see how a feature actually came together — useful for a project whose commit history is itself part of the deliverable.

Commit messages use Conventional Commit prefixes: `feat:`, `fix:`, `docs:`, `chore:`, `research:` (spike-related commits), `refactor:`, `test:`. (Adopted 2026-07-16 — earlier commits predate this and were left as-is.)

## 4. Testing — before you call a step "done"

A step or feature isn't done just because it compiles. Before marking it complete, it needs passing tests for whatever it touches:

- **Backend** (JUnit 5 + Spring Boot Test, `backend/src/test/java/...`):
  ```
  ./mvnw -B spotless:check test
  ```
- **Frontend** (Angular's test runner, `frontend/src/.../*.spec.ts`):
  ```
  npm run lint
  npx ng test --watch=false
  ```

These are the exact commands CI runs (ADR [0016](./docs/adr/0016-pipeline-stages.md)) — running them locally first means the PR isn't the first time you find out something's broken.

**Frontend/UI changes also need a manual smoke test.** Passing `.spec.ts` tests doesn't confirm something actually looks or behaves right — start the dev server and click through the change (the golden path and the obvious edge cases) in a browser before calling it done. Automated tests catch regressions; they don't tell you the UI reads well or the flow makes sense.

## 5. Before opening a PR

Run the full local suites again (both backend and frontend, whichever the branch touches) and confirm they're green — don't rely on CI to catch what you could have caught locally. Then open a PR against `main` using the [PR template](./.github/pull_request_template.md), which expects a summary, a list of changes, how it was tested, and a checklist.

There's no required reviewer (solo project, ADR [0017](./docs/adr/0017-merge-deploy-gates.md)) — the merge gate is CI status checks (lint, test, build) passing, not an approval. Treat the PR checklist as your own self-review, not a formality. Merge with **"Create a merge commit"** (see branching strategy above) — not squash, not rebase.

## 6. Definition of done

Pulling the above together, a feature/fix is done when:

- [ ] The code is implemented, following existing patterns/conventions.
- [ ] Tests are written and passing for whatever it touches (backend and/or frontend).
- [ ] For frontend/UI changes: manually smoke-tested in a browser, not just unit-tested.
- [ ] Lint is clean (Spotless / ESLint+Prettier).
- [ ] Any notable decision made along the way has an ADR (see below).
- [ ] `docs/ROADMAP.md` reflects the change.
- [ ] A PR is open from the template, and CI is green.

## 7. Issue and project tracking

Work is tracked via GitHub Issues + a Projects board, with Milestones mapped to roadmap phases (and later, MVP build-order features and pre-deployment hardening work). Every non-trivial unit of work — a user story, a bug, or a pre-deployment/security/ops item — gets an Issue on the relevant Milestone before it's built, closed by the PR that implements it (`Closes #NN`). This applies to hardening/checklist work too, not just user-facing features: file an Issue instead of adding a paragraph straight to `docs/pre-deployment-checklist.md`. Use the issue templates under `.github/ISSUE_TEMPLATE/`:

- **Bug report** — something broken
- **Feature request** — new, user-facing functionality
- **Hardening task** — pre-deployment security/reliability/ops work that isn't a user-facing feature
- **Research spike** — an open question that needs investigation before it can be planned

## 8. Writing an ADR

Any notable technical or process decision gets a short ADR in `docs/adr/` (see ADR [0000](./docs/adr/0000-use-adrs.md)) — not just big architecture calls, but things like this workflow itself. Number sequentially, and use the existing format:

```
# NNNN - Title

- Status: Accepted
- Date: YYYY-MM-DD

## Context
## Options considered   (optional — include when there was a real choice to make)
## Decision
## Consequences
```

## 9. Keeping the roadmap current

Update `docs/ROADMAP.md` (checkboxes + "Current focus") whenever a task or phase completes or the plan changes — including mid-task at the end of a session. The goal is that the next session (or the next person) never needs to be re-briefed verbally; the roadmap file is the source of truth.
