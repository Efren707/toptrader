# AWS Deployment Infrastructure — Implementation Plan

> Tracks provisioning the AWS infrastructure itself — the last major body of work before TopTrader is actually live. The *shape* of this infra is already fully decided, not being redesigned here: [ADR 0005](../../adr/0005-aws-deployment-shape.md) (EC2+CloudFront/RDS/S3+CloudFront/Route 53), [ADR 0006](../../adr/0006-cicd-pipeline-design.md) (CI/CD), [ADR 0014](../../adr/0014-deployment-infrastructure.md) (network/IAM/budget detail), [ADR 0016](../../adr/0016-pipeline-stages.md)/[ADR 0017](../../adr/0017-merge-deploy-gates.md) (pipeline stages/gates), and consolidated in [docs/architecture/deployment-architecture.md](../../architecture/deployment-architecture.md). This doc breaks that decided shape into workable sections, sequenced by dependency, so it can be picked up one section at a time across multiple sessions — mirroring how [docs/tasks/completed/pre-deployment-checklist.md](../completed/pre-deployment-checklist.md) tracked hardening work. Each section is its own GitHub Issue under the [AWS Deployment Infrastructure milestone](https://github.com/Efren707/toptrader/milestone/14).

Two items already existed as GitHub Issues under the old Pre-Deployment Hardening milestone before this doc existed, and were reassigned onto this milestone since they're blocked on this same infra: **[#102](https://github.com/Efren707/toptrader/issues/102)** (CloudWatch log shipping + `StatusCheckFailed` alarm) picks up once section 4 (EC2) exists; **[#105](https://github.com/Efren707/toptrader/issues/105)** (automated rollback — last-known-good jar swap, [ADR 0039](../../adr/0039-rollback-strategy.md)) picks up once section 7 (CI/CD deploy wiring) exists. They're referenced inline below, not duplicated as new issues.

Working agreement applies as usual: one section at a time, check in before deciding anything not already settled by an ADR.

## Status

In progress — sections 1-2 done.

## Sections

### 1. Domain & AWS account foundation

- [x] Register the real domain (Route 53) — `toptrader.com`/`.app`/`.io`/`.net` were all taken; registered **`toptrader.dev`** instead. Resolved the placeholder prod `apiUrl` in `frontend/src/environments/environment.ts` and every other `toptrader.com`/`.example` doc reference to match.
- [x] Create the Route 53 hosted zone (auto-created by Route 53 on domain registration)
- [x] Non-root IAM user/role for manual provisioning (not using the AWS root account day-to-day) — `toptrader-admin` IAM user, console access, `AdministratorAccess` policy
- [x] AWS Budgets: $15 warning / $25 critical thresholds, both emailing (ADR 0014) — single $25 monthly cost budget with alerts at 60%/100%

GitHub Issue: [#107](https://github.com/Efren707/toptrader/issues/107)

### 2. Network & security groups

- [x] Confirm default VPC is in use (no custom subnets/route tables/NAT — ADR 0014) — region **us-east-2**, default VPC `vpc-05baaee7f9cc06301`
- [x] EC2 security group: app port inbound restricted to CloudFront's managed origin-facing prefix list; SSH on a non-default port, open to `0.0.0.0/0` with fail2ban (GitHub-hosted runners have no static IP to allowlist) — `toptrader-ec2-sg` (`sg-0e1cba638b3f1b191`): 8080/TCP from prefix list `com.amazonaws.global.cloudfront.origin-facing`, SSH on port **3333** from `0.0.0.0/0`, default (all traffic) outbound
- [x] RDS security group: inbound 5432 only from the EC2 security group (SG-to-SG reference, not a CIDR range), `publicly accessible = No` — `toptrader-rds-sg` (`sg-00b6c9363c90dbec6`): 5432/TCP from `toptrader-ec2-sg`, default (all traffic) outbound; `publicly accessible = No` to be set at RDS provisioning (section 3)

GitHub Issue: [#108](https://github.com/Efren707/toptrader/issues/108)

### 3. Database — RDS

- [ ] Provision RDS PostgreSQL, db.t4g.micro, Single-AZ
- [ ] Confirm free-tier terms for the actual AWS account used (legacy vs. post-July-2025 account — ADR 0005 note)
- [ ] Verify Flyway migrations (ADR 0011) run automatically against this instance at Spring Boot startup

GitHub Issue: [#109](https://github.com/Efren707/toptrader/issues/109)

### 4. Backend compute — EC2

- [ ] Provision EC2 t4g.micro, default public subnet
- [ ] Spring Boot jar running under `systemd`, restart-on-failure
- [ ] systemd health-check timer polling `/actuator/health` with auto-restart (ADR 0008)
- [ ] IAM instance role: `ssm:GetParameters`/`GetParametersByPath` + `kms:Decrypt` (on `aws/ssm`), scoped to `/toptrader/prod/*`

Follow-on once this section is done: **#102** (CloudWatch agent + `StatusCheckFailed` → SNS alarm) becomes unblocked.

GitHub Issue: [#110](https://github.com/Efren707/toptrader/issues/110)

### 5. CDN & TLS

- [ ] ACM certs for `app.` and `api.` subdomains
- [ ] CloudFront distribution #1: fronts S3 (frontend, `app.` subdomain)
- [ ] CloudFront distribution #2: fronts EC2 (backend, `api.` subdomain), origin protocol HTTP-only (stays inside AWS's network — ADR 0014, no cert to manage on EC2)

GitHub Issue: [#111](https://github.com/Efren707/toptrader/issues/111)

### 6. Frontend hosting — S3

- [ ] S3 bucket for the Angular production build
- [ ] Wire the bucket to its CloudFront distribution (section 5)

GitHub Issue: [#112](https://github.com/Efren707/toptrader/issues/112)

### 7. CI/CD deploy wiring

- [ ] Frontend: OIDC-federated IAM role (repo/branch-scoped trust policy), `aws s3 sync` + CloudFront invalidation via GitHub Actions (ADR 0006)
- [ ] Backend: SSH-key GitHub secret, SCP the built jar + `systemctl restart` (ADR 0006)
- [ ] SSM Parameter Store secrets pulled at deploy time: DB password, Finnhub API key, session-signing secret (ADR 0006/0015/0018)
- [ ] Wire the deploy stage into the existing lint→test→build pipeline, gated on merge to `main` only (ADR 0016/0017)

Follow-on once this section is done: **#105** (automated last-known-good jar swap on failed post-deploy smoke test, ADR 0039) becomes unblocked.

GitHub Issue: [#113](https://github.com/Efren707/toptrader/issues/113)

### 8. Cutover & smoke test

- [ ] Route 53 DNS cutover to the live CloudFront distributions
- [ ] End-to-end smoke test against the real domain (register, login, quote lookup, buy/sell, portfolio/transactions/performance views)
- [ ] Build a real SES-backed `EmailSender` — password reset / email verification are implemented but not end-to-end usable without it (ADR 0036/0037)
- [ ] Check the frontend's blank-page-on-network-error behavior in `checkSession()`'s app initializer against the real API origin (network error / wrong or unreachable API origin currently renders a blank page instead of falling back to a logged-out view)
- [ ] Resolve GitHub's outstanding Dependabot alert on `main` (https://github.com/Efren707/toptrader/security/dependabot/17), put off since 2026-08-05 — revisit before going live
- [ ] Pick back up [demo-account.md](./demo-account.md)'s blocked items now that there's a live URL: README screenshots/GIF, live demo link callout, write the actual seed migration
- [ ] Update `docs/guides/readme-structure-outline.md`'s status line / README itself with the live link

GitHub Issue: [#114](https://github.com/Efren707/toptrader/issues/114)

## Cost summary

No change from ADR 0005/0014's estimate: **~$18-20/mo** after free tier (EC2 ~$6 + RDS ~$13 + Route 53 ~$1 + negligible S3/CloudFront/Budgets).
