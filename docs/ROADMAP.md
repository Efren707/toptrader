# Planning Roadmap & Status

> Last updated: 2026-08-07
> This file tracks *where we are right now* — current focus only, nothing else. Session-by-session history lives in git/PR history and the Issue each PR closes, not here. **How work is tracked:** every unit of work (MVP user story, hardening item, or infra section) is a GitHub Issue on a [Milestone](https://github.com/Efren707/toptrader/milestones), closed by a PR; the PR holds full implementation detail, an ADR (`docs/adr/`) holds the "why" for notable decisions. Status/tracking docs live under `docs/tasks/`, grouped by lifecycle stage — see [ADR 0040](./adr/0040-work-tracking-docs-lifecycle.md) for the convention: `docs/tasks/completed/` (closed out, frozen), `docs/tasks/planning/` (scoped, not yet started), `docs/tasks/in-progress/` (actively being executed). Requirements/backlog detail lives in `docs/requirements/`; guides live in `docs/guides/`.

## Current focus

MVP (US-1–US-9), the UI/UX polish pass, and pre-deployment hardening are all done. Work now proceeds through the **[AWS Deployment Infrastructure milestone](https://github.com/Efren707/toptrader/milestone/14)**, one section at a time — see [docs/tasks/in-progress/aws-infrastructure-implementation.md](./tasks/in-progress/aws-infrastructure-implementation.md) for the section-by-section breakdown, status, and everything blocked on it (including the demo/showcase-readiness work in its cutover section).

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.
