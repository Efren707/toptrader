# 0017 - Merge/deploy gates: status checks only, continuous deployment, post-deploy smoke test

- Status: Accepted
- Date: 2026-07-17

## Context

ADR 0006 established that the lint+test job is a required branch protection status check and that deploy runs only on merge to `main`, and ADR 0016 filled in what lint/test actually run. This is the third Phase 4 checklist item: deciding what else gates a merge, whether deploy needs a manual trigger beyond the merge itself, and whether a deploy is verified as successful before being considered done.

## Options considered

### PR review requirement
- **Status checks only, no required approving review** - branch protection requires the lint+test status check to pass; no reviewer approval requirement. Matches reality for a solo developer: GitHub's "require pull request reviews" setting blocks the PR author from approving their own PR by default, so enabling it would either lock out solo merges entirely or require weakening the protection (self-review exceptions, bypass lists) to work around a rule that has no one else to satisfy it.
- **Require an approving review** - would need an explicit bypass/exception for the repo owner to remain mergeable solo, at which point the rule enforces nothing real — added GitHub configuration that performs "team process" without a team.

### Deploy trigger
- **Automatic on merge to `main`** (continuous deployment) - the deploy stage ADR 0006 already scoped to "runs only on merge to main" fires immediately once the merge lands and the required checks passed pre-merge. Consistent with trunk-based development (ADR 0002): `main` is always deployable, so deploying it on every merge is the point of that model, not an extra risk on top of it.
- **Manual approval gate** (GitHub Environments with required reviewers on a `production` environment) - merge succeeds, deploy waits for a manual click. Adds a deliberate pause before prod changes and a small amount of GitHub Environment setup, but for a solo developer the "approver" is the same person who just merged — the manual click adds a step without adding a second set of eyes.

### Post-deploy verification
- **Smoke test inside the deploy job** - after `systemctl restart`, the same SSH deploy session curls `localhost:8080/actuator/health` with a short retry/backoff loop; if it doesn't return healthy within that window, the deploy job itself fails (visible as a red CI run, not just a later CloudWatch alarm). Catches a broken deploy (bad config, crash on startup, failed migration) within the same pipeline run that caused it.
- **No explicit check — rely on ADR 0008's health-check timer** - the systemd timer (polling `/actuator/health` on its own schedule, restarting on repeated failure, backed by a CloudWatch Alarm → SNS email) would eventually detect a wedged app. Simpler deploy job, but a bad deploy is only discovered via a separate out-of-band alert path rather than as a direct CI failure attributable to the triggering merge.

## Decision

- **Merge gate**: required status check on the lint+test job only (per ADR 0006/0016); no required PR-approval rule.
- **Deploy trigger**: automatic on merge to `main` — no manual approval step, no GitHub Environments reviewer gate.
- **Post-deploy verification**: the deploy job curls `/actuator/health` after restart, with a short retry/backoff window, and fails the job if the new version doesn't come up healthy in that window.

## Consequences

- Because there's no required-review rule, code quality on `main` depends entirely on the automated lint+test gate and the solo developer's own judgment before merging — an accepted trade-off consistent with this being a solo project, not a gap to fix later unless a second contributor joins.
- A failed post-deploy smoke test leaves the EC2 instance running whatever the previous `systemctl restart` left it in (the new jar may be partially deployed) — this ADR does not add automatic rollback (matching ADR 0006's already-accepted "fix forward or manually roll back" trade-off); it only makes the failure visible immediately as a red CI run instead of waiting for the separate CloudWatch/SNS path.
- The smoke-test retry/backoff window (exact seconds/attempts) is an implementation detail for when the deploy workflow YAML is actually written, not decided here.
- If a second contributor ever joins, revisit the no-required-review decision — the reasoning here is specific to there being no one else to review against.
