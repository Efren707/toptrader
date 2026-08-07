# 0039 - Rollback strategy: automated last-known-good jar swap, schema stays fix-forward

- Status: Accepted
- Date: 2026-08-06

## Context

ADR 0005/0006/0011/0017/0019 collectively accepted a "fix-forward" posture: single EC2 instance, `systemctl restart` deploys, Flyway forward-only migrations, no blue/green. ADR 0017's post-deploy smoke test (`/actuator/health` with retry/backoff) fails the CI job if a bad deploy doesn't come up healthy, but explicitly does not touch the instance — a failed deploy leaves EC2 running whatever the restart left it in until the developer notices and ships a fix. [#98](https://github.com/Efren707/toptrader/issues/98) asks for an actual rollback plan instead of relying on that gap.

Two things were evaluated before deciding on a mechanism: whether rollback should also cover the database schema, and what the app-level mechanism itself should be. A non-AWS provider switch (Render, Railway, Fly.io, DigitalOcean App Platform, Heroku, GCP Cloud Run — several of which have built-in rollback as a platform feature) was also discussed and explicitly declined: AWS is a committed goal in `docs/requirements/vision.md`, and ADR 0005's cost/resume-value trade-off for EC2 was reaffirmed rather than reopened.

This ADR covers recovery from a **bad deploy** specifically (new jar broken on arrival, or fails its own smoke test). Recovery from an unrelated runtime crash on an app that was already healthy is ADR 0008's systemd health-check timer + auto-restart + CloudWatch/SNS alarm — a different problem, not addressed here.

## Options considered

### Scope: does rollback cover the database schema too?

- **App-only** - rollback means reverting the jar. Schema stays forward-only, relying on ADR 0019's expand/contract discipline (additive migration → backfill → app cutover → drop old shape in a later migration) to guarantee the previous app version keeps working against the current schema.
- **App + schema** - also build a mechanism to undo a bad migration. Rejected: Flyway Community has no real automated rollback (undo migrations are a paid Teams/Enterprise feature); the realistic alternative is an RDS point-in-time restore, which creates a **new** instance (10-20+ min, not in-place), loses every write made after the restore point (real data loss, unlike a stateless jar swap), and forces app rollback to happen in lockstep since a reverted schema may not match the currently-deployed jar. It also mostly duplicates protection ADR 0019's expand/contract already provides, for materially higher complexity and risk on a solo project.

### App rollback mechanism

- **Automatic: keep last-known-good jar on EC2** - the deploy step keeps the previous jar as a `.bak` before overwriting. If the post-deploy smoke test (ADR 0017) fails, the deploy job swaps back to the `.bak`, restarts, and re-checks health. Self-heals within the same CI run for the common case (a broken deploy failing its own smoke test) with no human intervention. No new AWS resource — negligible extra EBS storage for one extra jar copy (tens of MB against a volume with 30GB free-tier headroom), no added EC2 compute cost (billed per instance-hour regardless of restart count), no added GitHub Actions cost (already $0 on this public repo per ADR 0006).
- **Manual: re-deploy a previous git tag** - rollback triggered by hand, re-running the deploy workflow against the last-good semver tag (ADR 0019 already tags releases). Simpler deploy script, but slower (a full CI run) and doesn't self-heal — requires the developer to notice and act before service is restored.
- **Blue/green via a second EC2 instance** - real zero-downtime rollback, but reopens ADR 0005's cost-first EC2 decision (roughly doubles compute cost, likely needs an ALB or a DNS-switch mechanism). Rejected on the same cost grounds ADR 0005 already used to choose EC2 over ECS Fargate.

## Decision

- **Scope**: application (jar) rollback only. Database schema remains fix-forward, per ADR 0011/0019 — no new schema-undo mechanism.
- **Mechanism**: the deploy job keeps the previous jar as a `.bak` on the EC2 instance before deploying a new one. If the post-deploy smoke test (ADR 0017) fails, the deploy job automatically restores the `.bak`, restarts, and re-checks health, in addition to still failing the CI run so the developer is notified.

Added cost: **$0** — no new AWS resources; negligible EBS storage for one extra jar copy.

## Consequences

- Only covers the case where a bad deploy fails its own post-deploy smoke test. A latent bug that passes the smoke test but crashes later is not addressed by this mechanism — that scenario would need ADR 0008's health-check timer to also know to swap jars (not just restart the same, still-broken one), which is a related but separate change to that ADR, not decided here.
- The swap-and-restart still incurs the same few-seconds `systemctl restart` blip as a normal deploy — this is recovery-time improvement (minutes instead of however long a fix-forward cycle takes), not zero-downtime; the project's "no formal uptime SLA" NFR still applies.
- App rollback implicitly depends on schema changes always being backward-compatible with the previous app version, which is exactly what ADR 0019's expand/contract pattern is supposed to guarantee — that discipline needs to actually be followed at migration-writing time, not just documented, for this mechanism to be safe.
- The deploy script needs to handle the first-ever-deploy case (no `.bak` exists yet) and a failure of the swap itself — implementation detail for when the deploy workflow is actually written (still blocked on EC2 provisioning, same as [#102](https://github.com/Efren707/toptrader/issues/102)).
- Non-AWS providers with built-in rollback (Heroku's one-command rollback, GCP Cloud Run's revision-based rollback) were considered and declined — see Context. Not expected to be revisited unless the project's cost or resume-value priorities change.
