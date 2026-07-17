# 0019 - Post-MVP feature release strategy: no feature flags, semver git tags, expand/contract migrations

- Status: Accepted
- Date: 2026-07-17

## Context

The post-MVP backlog (`docs/requirements/user-stories.md`) already lists deposit cash, fractional shares, and avatar selection as tracked-but-not-built stories. Fractional shares in particular changes an existing column's type (whole-share `int` → decimal) on a table that will hold real (demo) data by the time it's built. This is the fifth and final Phase 4 checklist item: how features get released after MVP ships, on top of trunk-based development (ADR 0002) and continuous deployment on merge to `main` (ADR 0017), both already decided.

## Options considered

### Feature flagging
- **No flags — short-lived branches only** - a post-MVP feature stays on its `feature/*` branch (ADR 0002) until actually complete, then merges and ships as a whole in one deploy (ADR 0017). No partial/hidden functionality ever lands in `main`.
- **Config-based flag mechanism** - an SSM parameter or config toggle lets an incomplete feature merge to `main` behind a flag, enabled later without a new deploy. More resume-relevant as a demonstrated pattern, but this project has no real user base to stage a gradual rollout against — the actual problem flags solve doesn't exist here.

### Versioning
- **Git tags / GitHub Releases per milestone** - tag `v1.0.0` at MVP completion (Phase 6 go/no-go), then `v1.1.0`, `v1.2.0`, etc. as each post-MVP feature ships, each with a short GitHub Release note. Trivial to add (a tag + a few lines), gives a reviewer skimming the repo a visible shipped-when timeline that raw commit history doesn't surface as clearly.
- **No formal versioning** - rely on commit/PR history alone; `main` is always deployable so there's no "release" distinct from "the current state of main." Simpler, but loses the at-a-glance milestone markers.

### Schema-change safety for live data
- **Document an expand/contract pattern now** - a short guideline: additive migration first (new nullable column/table alongside the old), backfill existing rows, cut the application over to the new column, then a later migration drops the old one. Avoids a single breaking migration landing at the same moment as a continuous-deployment restart (ADR 0006/0017 already accept a brief interruption on deploy, but not the same as a destructive migration with no easy way back). Cheap to write down now, before any real schema exists to retrofit against.
- **Handle it case-by-case later** - defer until fractional shares is actually being implemented, with full context on the real schema at that time. Less upfront documentation, but risks re-deriving the same reasoning under time pressure when the feature is actually being built.

## Decision

- **Feature flags**: none. Post-MVP features are built on short-lived branches per ADR 0002 and merged only when complete; merge-to-`main` continues to mean "ships now" per ADR 0017.
- **Versioning**: git tags following semver (`vMAJOR.MINOR.PATCH`), each paired with a short GitHub Release note. `v1.0.0` at MVP completion (Phase 6); each post-MVP feature bumps the minor version.
- **Schema-change safety**: expand/contract for any migration that changes or removes an existing column/table with live data — add the new shape, backfill, cut over the application, then drop the old shape in a subsequent migration (never combine "add new" and "remove old" in the same Flyway migration when existing data is at stake).

## Consequences

- Tagging is a manual step at release time (no automation added) — low effort, but relies on remembering to do it; worth a line item in whatever release checklist Phase 5's documentation work produces.
- The expand/contract rule specifically applies to fractional shares' `int` → decimal change: the actual migration plan (new nullable decimal column, backfill from the int column, app cutover, drop the int column in a later migration) will be designed in full when that story is actually built — this ADR only commits to the *pattern*, not the specific migration SQL.
- No feature-flag infrastructure means a post-MVP feature branch that takes a long time to finish just stays unmerged longer — acceptable given ADR 0002's "short-lived" branch expectation and the fact that a solo developer isn't blocked by anyone else's work landing in the meantime.
- This closes out **Phase 4 — CI/CD & Environment Strategy** in full (environments, pipeline stages, merge/deploy gates, secrets/config management, post-MVP release strategy all decided: ADR 0015-0019). Phase 5 (User-Facing Documentation Planning) is next.
