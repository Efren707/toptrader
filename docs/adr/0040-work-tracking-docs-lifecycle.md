# 0040 - Work-tracking docs organized by lifecycle stage under docs/tasks/

- Status: Accepted
- Date: 2026-08-07

## Context

`docs/planning-history.md` and `docs/pre-deployment-checklist.md` had grown organically: the former missed archiving Milestones #9-#12 (only one-liners existed in `ROADMAP.md`), and the latter still carried entirely blocked-until-deploy content (#102, #105) inside what was meant to become a closed checklist. Fixing both surfaced the same underlying gap — there was no explicit convention for where a body of work's tracking doc lives as it moves from scoped, to actively worked, to done, or for when a new Milestone earns a dedicated multi-section doc (like `pre-deployment-checklist.md` or the new `aws-infrastructure-implementation.md`) versus just a write-up folded into the history archive.

## Options considered

### Folder layout

- **`docs/tasks/{planning,in-progress,completed}/`** - nests all three lifecycle stages under one parent, keeping `docs/` root uncluttered and making the relationship between the three folders explicit (they're stages of the same thing, not independent categories like `docs/adr/` or `docs/architecture/`).
- **Three top-level folders (`docs/planning/`, `docs/in-progress/`, `docs/completed/`)** - flatter, one less path segment, but reads as three unrelated categories at the same level as `docs/adr/`/`docs/architecture/`/`docs/guides/`/`docs/requirements/`, which aren't lifecycle stages of each other.

### When a Milestone gets its own tracking doc

- **Only multi-session Milestones with enough sub-items to need a checklist** - matches how `pre-deployment-checklist.md` (Milestone #13) and `aws-infrastructure-implementation.md` (Milestone #14) actually came about organically; smaller Milestones (MVP build-order #8-#12) never needed one and went straight into `planning-history.md` as a short write-up once done.
- **Every Milestone gets a dedicated doc** - more mechanical/consistent, but overkill for a 1-2 issue Milestone; would produce doc files nobody needs to reference mid-flight.

## Decision

- **Folder layout**: `docs/tasks/planning/`, `docs/tasks/in-progress/`, `docs/tasks/completed/`.
- **File lifecycle**: a doc is created in `docs/tasks/planning/` when a Milestone is scoped, moved (`git mv`) to `docs/tasks/in-progress/` the moment real execution starts, and moved to `docs/tasks/completed/` once every item in it is done — the file itself is the unit that migrates, not its content getting copy-pasted between docs.
- **Doc creation threshold**: a Milestone gets its own dedicated tracking doc only when it's expected to span multiple sessions and has enough sub-items to warrant a checklist broken into sections. Smaller Milestones skip the dedicated doc entirely and get a short write-up appended directly to `docs/tasks/completed/planning-history.md` once done, as the MVP build-order Milestones (#8-#12) always did.
- `docs/tasks/completed/planning-history.md` keeps its existing role as the single running index/archive of everything closed out — small Milestones get their full detail written there directly; large Milestones with their own doc get a one-liner + "Full detail: [that doc]" pointer instead, once that doc itself lands in `docs/tasks/completed/`.
- **Closing out a doc** is four things done together at end-of-session, not just the file move:
  1. Move the file: `git mv docs/tasks/in-progress/<doc>.md docs/tasks/completed/` and fix its relative links for the new depth.
  2. Update the doc's own status indicator (an intro blockquote note or a `## Status` section) to say it's done, with a date — never leave it reading "Not started" / "In progress" once frozen.
  3. Close the corresponding GitHub Milestone once all its issues are closed: `gh api repos/Efren707/toptrader/milestones/<n> -X PATCH -f state=closed`.
  4. Add the `planning-history.md` pointer entry (previous bullet) and update `ROADMAP.md`'s "Current focus" line to whatever's next.

## Consequences

- Judging "is this Milestone big enough to need its own doc" is a per-Milestone call, not a mechanical rule — consistent with this project's "always check in before deciding" working agreement rather than something to automate away.
- Every file move changes every relative link inside the moved file (one more or fewer `../` depending on direction) — a real but mechanical cost each time a doc's lifecycle stage changes, worth double-checking rather than skipping.
- `docs/tasks/in-progress/` will sit empty between Milestones — that's expected, not a sign something's missing.
- Nothing enforces the four-step close-out checklist automatically — a GitHub Milestone can sit closeable-but-open, or a doc's status line can go stale, if the person finishing the last issue doesn't also do the doc/Milestone bookkeeping in the same session. Treat it as one atomic step, not three optional ones.
