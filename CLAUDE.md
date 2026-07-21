# TopTrader — Working Agreement

Stock trading simulator (virtual cash, buy/sell at real/delayed prices). Stack: Spring Boot + PostgreSQL + Angular, deployed on AWS. This is a software engineering student's resume/learning project — **the planning and documentation process is as much the deliverable as the working app.**

## Status

Current phase, completed work, and next steps live in **[docs/ROADMAP.md](./docs/ROADMAP.md)** — read that first in any new session to pick up where things left off. Don't rely on conversation memory alone; the roadmap file is the source of truth.

## Session workflow

- **Start of session:** read `CLAUDE.md` (this file) and `docs/ROADMAP.md`'s "Current focus" line before doing anything else.
- **End of session:** update `docs/ROADMAP.md` (checkboxes + "Current focus") to reflect what actually happened, and commit/push it — even if mid-task. The next session should never need to be re-briefed verbally.

## How to work on this project

- **One step at a time.** Do not build ahead or auto-complete multiple features/phases in one go.
- **Always check in before deciding.** Never assume scope, technical approach, or requirements — ask. This applies to architecture choices, library/tool picks, and scope changes alike, not just big decisions.
- **Explain, then act.** State what was just done and what's next after each step.
- Every notable technical/process decision gets an **ADR** in `docs/adr/` (see `docs/adr/0000-use-adrs.md`).
- Docs live as Markdown **in-repo** under `docs/` (`requirements/`, `architecture/`, `guides/`, `adr/`) — no external doc tools.
- Update `docs/ROADMAP.md` whenever a task/phase completes or the plan changes, so the next session doesn't need to be re-briefed.

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
- Task tracking via GitHub Issues + Projects, with Milestones mapped to roadmap phases (and later, MVP build-order features).
- Commit messages use Conventional Commit prefixes: `feat:`, `fix:`, `docs:`, `chore:`, `research:` (for spike-related commits), `refactor:`, `test:`. Adopted going forward from 2026-07-16 — earlier commits predate this and were left as-is.

## Key docs

- Vision/scope: `docs/requirements/vision.md`, `user-stories.md`, `nfr.md`, `acceptance-criteria.md`
- Decisions: `docs/adr/`
- Roadmap/status: `docs/ROADMAP.md`
