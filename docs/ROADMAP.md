# Planning Roadmap & Status

> Last updated: 2026-08-11
> This file tracks *where we are right now* — current focus only, nothing else. Session-by-session history lives in git/PR history and the Issue each PR closes, not here. **How work is tracked:** every unit of work (MVP user story, hardening item, or infra section) is a GitHub Issue on a [Milestone](https://github.com/Efren707/toptrader/milestones), closed by a PR; the PR holds full implementation detail, an ADR (`docs/adr/`) holds the "why" for notable decisions. Status/tracking docs live under `docs/tasks/`, grouped by lifecycle stage — see [ADR 0040](./adr/0040-work-tracking-docs-lifecycle.md) for the convention: `docs/tasks/completed/` (closed out, frozen), `docs/tasks/planning/` (scoped, not yet started), `docs/tasks/in-progress/` (actively being executed). Requirements/backlog detail lives in `docs/requirements/`; guides live in `docs/guides/`.

## Current focus

MVP (US-1–US-9), the UI/UX polish pass, and pre-deployment hardening are all done. TopTrader is now live at `app.toptrader.dev`. The **[AWS Deployment Infrastructure milestone](https://github.com/Efren707/toptrader/milestone/14)**'s sections 1-8 are all done (cutover complete) — see [docs/tasks/in-progress/aws-infrastructure-implementation.md](./tasks/in-progress/aws-infrastructure-implementation.md) for detail. The milestone itself stays open until its remaining hardening issues (#102, #105) close. Demo/showcase-readiness work (screenshots, seed migration) is split out into its own issue, [#134](https://github.com/Efren707/toptrader/issues/134), tracked against [docs/tasks/planning/demo-account.md](./tasks/planning/demo-account.md).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
