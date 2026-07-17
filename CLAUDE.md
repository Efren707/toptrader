# TopTrader — Working Agreement

Stock trading simulator (virtual cash, buy/sell at real/delayed prices). Stack: Spring Boot + PostgreSQL + Angular, deployed on AWS. This is a software engineering student's resume/learning project — **the planning and documentation process is as much the deliverable as the working app.**

## Status

Current phase, completed work, and next steps live in **[docs/ROADMAP.md](./docs/ROADMAP.md)** — read that first in any new session to pick up where things left off. Don't rely on conversation memory alone; the roadmap file is the source of truth.

## How to work on this project

- **One step at a time.** Do not build ahead or auto-complete multiple features/phases in one go.
- **Always check in before deciding.** Never assume scope, technical approach, or requirements — ask. This applies to architecture choices, library/tool picks, and scope changes alike, not just big decisions.
- **Explain, then act.** State what was just done and what's next after each step.
- Every notable technical/process decision gets an **ADR** in `docs/adr/` (see `docs/adr/0000-use-adrs.md`).
- Docs live as Markdown **in-repo** under `docs/` (`requirements/`, `architecture/`, `guides/`, `adr/`) — no external doc tools.
- Update `docs/ROADMAP.md` whenever a task/phase completes or the plan changes, so the next session doesn't need to be re-briefed.

## Repo conventions

- Public GitHub repo (`Efren707/toptrader`), MIT licensed.
- Trunk-based development: `main` is always deployable; work happens on short-lived `feature/*` or `fix/*` branches merged via PR (see ADR 0002).
- Task tracking via GitHub Issues + Projects.

## Key docs

- Vision/scope: `docs/requirements/vision.md`, `user-stories.md`, `nfr.md`, `acceptance-criteria.md`
- Decisions: `docs/adr/`
- Roadmap/status: `docs/ROADMAP.md`
