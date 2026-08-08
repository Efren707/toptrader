# 0041 - Secrets fetch trigger: systemd ExecStartPre, not deploy-time only

- Status: Accepted
- Date: 2026-08-08

## Context

ADR 0015 decided the EC2 instance role pulls `/toptrader/prod/*` SSM parameters at "deploy time," describing a script run once over the SSH deploy session (section 7, not yet built) that materializes a local env file before `systemctl restart`. Writing the actual `systemd` unit now (AWS Deployment Infrastructure milestone, section 4) surfaces a question ADR 0015 didn't resolve: what happens on an *automatic* restart (`Restart=on-failure`, ADR 0008) that isn't triggered by a deploy — does it reuse whatever env file happened to be on disk, or fetch fresh?

## Options considered

- **Deploy-time only (ADR 0015 as literally written)** - the fetch script runs solely during the SSH deploy step; `systemd`'s unit just reads a static env file via `EnvironmentFile=`. Simple unit definition, but an automatic crash-restart between deploys reuses a possibly-stale file, and if that file were ever missing (e.g. cleaned up, or before the first deploy has run), the service can't self-heal without external intervention.
- **`ExecStartPre` fetch on every start** - the `systemd` unit itself calls the SSM-fetch script before every `ExecStart`, including automatic restarts. The service becomes self-sufficient: any restart re-fetches current parameter values, so secret rotation or a stale/missing env file are handled without needing the deploy pipeline to run. Section 7's future deploy script gets simpler too — it only needs to SCP the new jar and `systemctl restart`, since fetching is `systemd`'s job, not the deploy script's.

## Decision

**`ExecStartPre` fetch on every start.** The `toptrader.service` unit's `ExecStartPre` runs the SSM-fetch script (`ssm:GetParametersByPath` on `/toptrader/prod/*`, using the instance role from ADR 0015) to (re)materialize `/opt/toptrader/app.env` before every `ExecStart` of the jar — on a fresh boot, a manual restart, or an automatic `Restart=on-failure` restart alike.

## Consequences

- Section 7's deploy script no longer needs any SSM-specific logic — SCP the jar, `systemctl restart toptrader`, done. The fetch happens inside the service start itself.
- Every restart makes one `ssm:GetParametersByPath` call — negligible cost/latency at this scale (single instance, infrequent restarts), and keeps the instance role's existing scoped permissions (ADR 0015) as the only access path; no broader change to that policy.
- If SSM were ever transiently unreachable during an automatic restart, the start would fail and `systemd` would retry per `RestartSec` — an accepted trade-off given SSM's own availability characteristics, not expected to matter at this scale.
- The materialized `/opt/toptrader/app.env` still exists as a plaintext file on disk between starts (unavoidable without a from-scratch secrets-injection mechanism at this project's scale) — permissions restricted to the `toptrader` service user only, consistent with the existing "no secret in the repo or logs" posture (`security-architecture.md`).
