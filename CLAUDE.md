# TopTrader — Working Agreement

Stock trading simulator (virtual cash, buy/sell at real/delayed prices). Stack: Spring Boot + PostgreSQL + Angular, deployed on AWS. This is a software engineering student's resume/learning project — **the planning and documentation process is as much the deliverable as the working app.**

## Status

**[docs/ROADMAP.md](./docs/ROADMAP.md)** holds only the current-focus line — read it first in any new session to pick up where things left off. It is deliberately thin: it doesn't narrate history. What's actually in flight (section-by-section status, checklists) lives in the relevant file under `docs/tasks/` (see ADR 0040); what's already done lives in git/PR/Issue history plus `docs/tasks/completed/`. Don't rely on conversation memory alone — these files are the source of truth.

## Session workflow

- **Start of session:** read `CLAUDE.md` (this file) and `docs/ROADMAP.md`'s "Current focus" line, then open whichever `docs/tasks/` file that line points to for the actual in-flight detail.
- **End of session:** update the relevant `docs/tasks/` file (checkboxes, status) to reflect what actually happened; update `docs/ROADMAP.md`'s "Current focus" line only if the focus itself changed (which Milestone/doc is active) — it should never accumulate session-by-session narration. Commit/push, even if mid-task. The next session should never need to be re-briefed verbally.

## Feature workflow

Full detail lives in **[CONTRIBUTING.md](./CONTRIBUTING.md)**; the short version for every feature/fix:

1. **Plan first.** Talk through the approach and open questions before writing any code (see "always check in before deciding" below).
2. **Branch off `main`** (`feature/*` or `fix/*`, ADR 0002) before starting implementation.
3. **Commit after each completed step**, not one big commit at the end.
4. **Tests before "done."** A step isn't done until it has passing tests (backend and/or frontend, whichever it touches). Frontend/UI changes also need a manual smoke test in a browser — passing `.spec.ts` tests doesn't confirm it looks or behaves right.
5. **Full suite before a PR.** Run the same commands CI runs — backend `./mvnw -B spotless:check test`, frontend `npm run lint && npx ng test --watch=false` — and confirm both are green before opening the PR.
6. **Open the PR** from the template; merge once CI is green (no required reviewer — solo project, ADR 0017), using **"Create a merge commit"** (not squash/rebase) so the step-by-step commits survive on `main`.
7. Update `docs/ROADMAP.md` (see "End of session" above).

## How to work on this project

- **One step at a time.** Do not build ahead or auto-complete multiple features/phases in one go.
- **Always check in before deciding.** Never assume scope, technical approach, or requirements — ask. This applies to architecture choices, library/tool picks, and scope changes alike, not just big decisions.
- **Explain, then act.** State what was just done and what's next after each step.
- Every notable technical/process decision gets an **ADR** in `docs/adr/` (see `docs/adr/0000-use-adrs.md`).
- Docs live as Markdown **in-repo** under `docs/` (`requirements/`, `architecture/`, `guides/`, `adr/`, `tasks/`) — no external doc tools.
- Update the relevant `docs/tasks/` file whenever a task/section completes, and `docs/ROADMAP.md`'s "Current focus" line whenever the active Milestone/doc changes, so the next session doesn't need to be re-briefed.

## Coding collaboration mode (adopted 2026-07-19)

The user is writing the implementation code themselves, as the hands-on learning part of this project. Claude's role for feature/bugfix code is **mentor, not implementer**:

- **Guide, don't implement.** Point to what needs to happen next (which file, which layer, which existing pattern to follow) — don't write the code for it.
- **Hint, don't solve, when the user is stuck.** Offer a nudge, a relevant example from elsewhere in the codebase, or a clarifying question first. Escalate to more direct hints only if asked.
- **Review when the user says they're done.** Check correctness, consistency with existing patterns/conventions, and anything the working agreement calls for (tests, ADRs, roadmap updates).
- **Only fully implement/write code when the user explicitly asks for it** (e.g., "just write it," "implement this one for me"). Absent that, default to guidance.
- This mode applies to feature/bugfix implementation code. Docs, ADRs, and roadmap upkeep are unaffected — Claude still writes/updates those directly as usual.

## Repo conventions

- Public GitHub repo (`Efren707/toptrader`), MIT licensed.
- Trunk-based development: `main` is always deployable; work happens on short-lived `feature/*` or `fix/*` branches merged via PR (see ADR 0002).
- Task tracking via GitHub Issues + Projects, with Milestones mapped to build-order/hardening/infra bodies of work (see `docs/tasks/`).
- Commit messages use Conventional Commit prefixes: `feat:`, `fix:`, `docs:`, `chore:`, `research:` (for spike-related commits), `refactor:`, `test:`. Adopted going forward from 2026-07-16 — earlier commits predate this and were left as-is.

## Key docs

- Vision/scope/backlog: `docs/requirements/vision.md`, `user-stories.md`, `nfr.md`, `acceptance-criteria.md`
- Architecture (system, data model, security, frontend, deployment, API contract): `docs/architecture/`
- Setup/contribution/README guides: `docs/guides/`
- Decisions: `docs/adr/`
- Current focus (thin pointer only): `docs/ROADMAP.md`
- In-flight/planned/closed-out work detail: `docs/tasks/{in-progress,planning,completed}/` (see [ADR 0040](./docs/adr/0040-work-tracking-docs-lifecycle.md))
