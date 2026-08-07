# Pre-Deployment Security Checklist

> The full pre-production security checklist — kept here so [ROADMAP.md](../../ROADMAP.md) stays a lean, current-state tracker. Each item is a pointer, not a re-narration — full implementation detail lives in its PR, "why" lives in its ADR. **Closed out as of 2026-08-07**: every item below is done. The two items that were still open (#102 CloudWatch/alarms, #105 automated rollback) turned out to be entirely blocked-until-deploy work, not actively workable ahead of AWS infra existing — they've moved to [docs/tasks/planning/aws-infrastructure-implementation.md](../planning/aws-infrastructure-implementation.md) and the "AWS Deployment Infrastructure" milestone, sequenced alongside the infra work that unblocks them.

Compiled from a full audit (docs/ADRs vs. actual backend/frontend/CI code) on 2026-07-29; re-audited 2026-07-31 against 7 named items (authorization, input validation/sanitization, CORS, password reset link expiration, frontend error handling, logging/alerts in prod, rollback strategy). Several items previously implied as covered turned out to be narrower than they looked, so the still-open list grew from 1 to 6 items before eventually closing out entirely.

## Done

- [x] CI secret scanning & dependency hygiene — `gitleaks` + Dependabot, PR [#53](https://github.com/Efren707/toptrader/pull/53) (amends ADR 0007); dependency fixes PR [#51](https://github.com/Efren707/toptrader/pull/51), [#52](https://github.com/Efren707/toptrader/pull/52), [#54](https://github.com/Efren707/toptrader/pull/54) — frontend at 0 known vulnerabilities
- [x] Prod config lockdown — explicit `application-prod.properties` + actuator lockdown, [ADR 0032](../../adr/0032-prod-config-shape.md), PR [#68](https://github.com/Efren707/toptrader/pull/68)
- [x] Session timeout/fixation — explicit migration + 30m timeout, PR [#69](https://github.com/Efren707/toptrader/pull/69)
- [x] Frontend 401/403 handling — mid-session 401s now redirect to `/login` via `session-expired.interceptor.ts`, PR [#70](https://github.com/Efren707/toptrader/pull/70)
- [x] Logging/PII policy — binding policy recorded for whenever logging is added (see the logging framework item below), [ADR 0033](../../adr/0033-logging-pii-policy.md)
- [x] CSP directives — finalized and verified against the real prod build output, `docs/architecture/security-architecture.md`
- [x] General API rate limiting — [ADR 0034](../../adr/0034-api-rate-limiting.md), PR [#82](https://github.com/Efren707/toptrader/pull/82)
- [x] Ticker input validation — `@Pattern`-constrained, 400 + RFC 7807 on violation, PR [#84](https://github.com/Efren707/toptrader/pull/84)
- [x] Frontend error handling gaps — `stock-details` per-panel error signals instead of silent swallow, PR [#85](https://github.com/Efren707/toptrader/pull/85)
- [x] Authorization guard — `@EnableMethodSecurity` + `@PreAuthorize` ownership checks on all `TradeService` methods, [ADR 0035](../../adr/0035-authorization-guard.md), PR [#86](https://github.com/Efren707/toptrader/pull/86)
- [x] Password-reset flow — [ADR 0036](../../adr/0036-password-reset-flow.md), PR [#89](https://github.com/Efren707/toptrader/pull/89). **Known gap, tracked not silent:** prod isn't end-to-end usable until a real SES-backed `EmailSender` is built as part of AWS deployment sequencing.
- [x] Email verification at signup — split out from password reset per ADR 0036's scope note, [ADR 0037](../../adr/0037-email-verification-at-signup.md), PR [#95](https://github.com/Efren707/toptrader/pull/95). **Known gap:** same SES-sender dependency as password reset above.
- [x] Logging framework in backend — narrowed from its original scope (CloudWatch/alarm split to #102, now under the AWS Deployment Infrastructure milestone — see the note above), closes #97, [ADR 0038](../../adr/0038-backend-logging-framework.md), PR [#104](https://github.com/Efren707/toptrader/pull/104) merged.
- [x] Rollback strategy — application-only scope, automated last-known-good jar swap on failed post-deploy smoke test; database schema stays fix-forward per ADR 0011/0019's expand/contract discipline. Non-AWS providers with built-in rollback (Heroku, GCP Cloud Run, etc.) considered and declined — AWS reaffirmed per `vision.md`. Closes #98, [ADR 0039](../../adr/0039-rollback-strategy.md). Implementation continues as #105, now under the AWS Deployment Infrastructure milestone (see the note above).

Full detail/evidence as of the 2026-07-31 re-audit: see the security review plan at the time it was written (`.claude/plans/before-moving-on-to-inherited-haven.md`, local to this machine, not repo-tracked).
