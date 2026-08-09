# AWS Deployment Infrastructure — Implementation Plan

> Tracks provisioning the AWS infrastructure itself — the last major body of work before TopTrader is actually live. The *shape* of this infra is already fully decided, not being redesigned here: [ADR 0005](../../adr/0005-aws-deployment-shape.md) (EC2+CloudFront/RDS/S3+CloudFront/Route 53), [ADR 0006](../../adr/0006-cicd-pipeline-design.md) (CI/CD), [ADR 0014](../../adr/0014-deployment-infrastructure.md) (network/IAM/budget detail), [ADR 0016](../../adr/0016-pipeline-stages.md)/[ADR 0017](../../adr/0017-merge-deploy-gates.md) (pipeline stages/gates), and consolidated in [docs/architecture/deployment-architecture.md](../../architecture/deployment-architecture.md). This doc breaks that decided shape into workable sections, sequenced by dependency, so it can be picked up one section at a time across multiple sessions — mirroring how [docs/tasks/completed/pre-deployment-checklist.md](../completed/pre-deployment-checklist.md) tracked hardening work. Each section is its own GitHub Issue under the [AWS Deployment Infrastructure milestone](https://github.com/Efren707/toptrader/milestone/14).

Two items already existed as GitHub Issues under the old Pre-Deployment Hardening milestone before this doc existed, and were reassigned onto this milestone since they're blocked on this same infra: **[#102](https://github.com/Efren707/toptrader/issues/102)** (CloudWatch log shipping + `StatusCheckFailed` alarm) picks up once section 4 (EC2) exists; **[#105](https://github.com/Efren707/toptrader/issues/105)** (automated rollback — last-known-good jar swap, [ADR 0039](../../adr/0039-rollback-strategy.md)) picks up once section 7 (CI/CD deploy wiring) exists. They're referenced inline below, not duplicated as new issues.

Working agreement applies as usual: one section at a time, check in before deciding anything not already settled by an ADR.

## Status

In progress — sections 1-6 done, section 7 next.

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

- [x] Provision RDS PostgreSQL, db.t4g.micro, Single-AZ — instance identifier `toptrader`, region **us-east-2c**, Postgres 17 (matches local dev's `docker-compose.yml`), gp3 20 GiB, encrypted (default `aws/rds` KMS key), initial DB name `toptrader`, `toptrader-rds-sg` attached, public access No, master credentials self-managed (not AWS Secrets Manager — ADR 0006 already rejected Secrets Manager's per-secret fee), deletion protection off for now (revisit at section 8 cutover)
- [x] Confirm free-tier terms for the actual AWS account used (legacy vs. post-July-2025 account — ADR 0005 note) — account predates July 2025, but its 12-month free tier window has already elapsed, so RDS costs apply immediately (matches ADR 0005's ~$18-20/mo estimate, which was already post-free-tier)
- [x] Verify Flyway migrations (ADR 0011) run automatically against this instance at Spring Boot startup — deferred to section 4: `toptrader-rds-sg` only trusts inbound 5432 from `toptrader-ec2-sg`, so this can't be checked from outside AWS until EC2 exists; the jar's first startup there (systemd) is the verification, not a separate step

GitHub Issue: [#109](https://github.com/Efren707/toptrader/issues/109)

### 4. Backend compute — EC2

- [x] Provision EC2 t4g.micro, default public subnet — instance ID `i-0918607b17b50cbe8`, region **us-east-2**, AZ **us-east-2b**, Amazon Linux 2023 (arm64, kernel-6.1), `toptrader-ec2-sg` attached, public IP assigned; `sshd` reconfigured to listen on port 3333 (matching the security group) after a temporary port-22-from-my-IP bootstrap rule, which was removed once confirmed
- [x] Confirm Flyway migrations (V1–V4) apply cleanly on first Spring Boot startup against RDS (carried over from section 3) — verified via a one-off manual `java -jar` run with the `prod` profile and RDS connection env vars; all 4 migrations applied and the app started cleanly
- [x] Spring Boot jar running under `systemd`, restart-on-failure — `toptrader.service` unit, runs as the non-root `toptrader` system user (`/opt/toptrader`); `ExecStartPre` fetches all 5 `/toptrader/prod/*` SSM parameters into `/opt/toptrader/app.env` on every start (ADR 0041, refines ADR 0015's deploy-time-only fetch), `Restart=on-failure`/`RestartSec=5` (ADR 0008); verified `/actuator/health` returns `UP` and a `kill -9` on the process is auto-restarted with a fresh PID
- [x] systemd health-check timer polling `/actuator/health` with auto-restart (ADR 0008) — `toptrader-healthcheck.timer` fires every 1 min (2 min initial delay), running `health-check.sh` (3 retries, 5s apart, then `systemctl restart toptrader.service` on repeated failure); verified two successful timer firings with no restart triggered while healthy
- [x] IAM instance role: `ssm:GetParameters`/`GetParametersByPath` + `kms:Decrypt` (on `aws/ssm`), scoped to `/toptrader/prod/*` — customer-managed policy `toptrader-ssm-read-policy` + role `toptrader-ec2-role`, attached to the instance; verified with `aws sts get-caller-identity` (resolves to the assumed role) and both `get-parameter --with-decryption` and `get-parameters-by-path` against the 5 parameters created under `/toptrader/prod/*` (`spring-datasource-url`/`-username`/`-password`, `finnhub-api-key`, `frontend-origin`) — `session-signing-secret` intentionally skipped per ADR 0032 (flagged as dead config) — follow-up to actually resolve that flag moved to section 8

Follow-on once this section is done: **#102** (CloudWatch agent + `StatusCheckFailed` → SNS alarm) becomes unblocked.

GitHub Issue: [#110](https://github.com/Efren707/toptrader/issues/110)

### 5. CDN & TLS

- [x] ACM certs for `app.` and `api.` subdomains — single cert, `us-east-1` (CloudFront's hard region requirement, not where the rest of the infra lives), two SANs (`app.toptrader.dev`, `api.toptrader.dev`), DNS validation via Route 53, "allow export" left disabled — certificate ID `da270cdb-bdd9-4f79-850e-1ea2dad4d236`
- [x] CloudFront distribution #1: fronts S3 (frontend, `app.` subdomain) — `toptrader-frontend`, distribution ID `EBJQ07VSB22PM`; S3 origin `toptrader-frontend` bucket (created empty ahead of section 6 so OAC could reference a real origin) via Origin Access Control, bucket policy scoped to this exact distribution's ARN (`StringEquals` on `AWS:SourceArn`, not the broader `ArnLike` wildcard the console initially proposed); viewer protocol redirect-to-HTTPS, default root object `index.html`, custom error responses 403→200 and 404→200 both mapped to `/index.html` for Angular client-side routing; CloudFront **Free flat-rate plan** (ADR 0042) with WAF enabled in block mode (not monitor-only) and the `toptrader.dev` Route 53 hosted zone attached to this distribution's plan for its cost-bundling benefit
- [x] CloudFront distribution #2: fronts EC2 (backend, `api.` subdomain), origin protocol HTTP-only (stays inside AWS's network — ADR 0014, no cert to manage on EC2) — `toptrader-backend`, distribution ID `E2QV9MPC8DTN65`; custom ("Other") origin at the EC2 instance's Elastic IP DNS name (`ec2-18-226-207-9.us-east-2.compute.amazonaws.com` — ADR 0043 records why an Elastic IP was allocated first), port 8080, HTTP only; cache behavior manually configured (not "use recommended settings," which defaults toward static-asset assumptions that don't fit a session-based API) — all HTTP methods allowed, `CachingDisabled` managed policy, `AllViewerExceptHostHeader` managed origin request policy so cookies/headers/query strings reach the backend unmodified; same Free flat-rate plan + WAF block-mode choice as distribution #1

GitHub Issue: [#111](https://github.com/Efren707/toptrader/issues/111)

### 6. Frontend hosting — S3

- [x] S3 bucket for the Angular production build — `toptrader-frontend` (already created empty in section 5 so CloudFront's OAC had a real origin to reference); finished configuring it here: versioning **enabled** (gives the frontend a manual rollback path — restore a prior object version — mirroring ADR 0039's backend last-known-good-jar rollback, which never covered the frontend; no lifecycle rule to expire old versions set up yet), default encryption **SSE-S3** (AWS-managed keys, free — same cost-minimizing lean as RDS's self-managed master password in section 3), Block Public Access **all four settings on** (bucket is OAC-only, never public)
- [x] Wire the bucket to its CloudFront distribution (section 5) — the OAC/bucket-policy wiring itself was already done in section 5; verified here by building the Angular production bundle (`npm run build` → `dist/frontend/browser/`) and uploading it via the S3 console, then confirming CloudFront (`toptrader-frontend`, distribution `EBJQ07VSB22PM`, domain `dbunvnda6gcb6.cloudfront.net`) serves it correctly — including the 403/404 → `/index.html` SPA routing rewrite from section 5 on a direct (non-client-side) navigation to an in-app route. Hit one upload gotcha worth remembering: dragging the `browser` folder itself into the S3 console nests everything under a `browser/` prefix instead of the bucket root, which the OAC/default-root-object setup requires at the root — S3 also masks the resulting missing-object error as `403 AccessDenied` rather than `404 NoSuchKey` (no `ListBucket` in the OAC policy), which looks like a permissions problem but isn't one. Fix is uploading the folder's *contents*, not the folder. A blank-but-styled page on direct navigation to `/login` is expected pre-cutover (`checkSession()`'s `APP_INITIALIZER`, `app.config.ts:31-34`, fails ungracefully on the currently-unreachable `api.toptrader.dev` per `auth.service.ts:69-74`) — already tracked as a to-do in section 8, not a section 6 issue.

GitHub Issue: [#112](https://github.com/Efren707/toptrader/issues/112)

### 7. CI/CD deploy wiring

- [x] Frontend: OIDC-federated IAM role (repo/branch-scoped trust policy), `aws s3 sync` + CloudFront invalidation via GitHub Actions (ADR 0006) — GitHub OIDC identity provider added once at the account level (`token.actions.githubusercontent.com`, audience `sts.amazonaws.com`); IAM role `toptrader-frontend-deploy-role` with a web-identity trust policy scoped to `repo:Efren707/toptrader:ref:refs/heads/main` (console's GitHub org/repo/branch fields generated this, no hand-edited JSON), inline policy `toptrader-frontend-deploy-policy` scoped to `s3:ListBucket` on the `toptrader-frontend` bucket, `s3:PutObject`/`GetObject`/`DeleteObject` on its objects, and `cloudfront:CreateInvalidation` on distribution `EBJQ07VSB22PM` only; role ARN, bucket name, and distribution ID stored as GitHub repo secrets (`AWS_FRONTEND_DEPLOY_ROLE_ARN`, `FRONTEND_S3_BUCKET`, `FRONTEND_CLOUDFRONT_DISTRIBUTION_ID`) per ADR 0006. New `deploy-frontend` job in `.github/workflows/ci.yml`, gated on push-to-`main` and `frontend-ci` having actually succeeded (not skipped); downloads the `frontend-dist` artifact `frontend-ci` uploads rather than rebuilding, so what's deployed is exactly what passed lint/test/build.
- [ ] Backend: SSH-key GitHub secret, SCP the built jar + `systemctl restart` (ADR 0006)
- [x] ~~SSM Parameter Store secrets pulled at deploy time~~ — superseded by ADR 0041 (section 4): the `toptrader.service` unit's `ExecStartPre` fetches `/toptrader/prod/*` on every start, so the deploy script needs no SSM logic at all
- [ ] Post-deploy smoke test: curl `/actuator/health` with retry/backoff after restart, fail the deploy job if unhealthy (ADR 0017) — scoped into this section rather than deferred to #105, since #105's rollback needs this signal to trigger off
- [ ] Wire the deploy stage into the existing lint→test→build pipeline, gated on merge to `main` only (ADR 0016/0017)

Follow-on once this section is done: **#105** (automated last-known-good jar swap on failed post-deploy smoke test, ADR 0039) becomes unblocked.

GitHub Issue: [#113](https://github.com/Efren707/toptrader/issues/113)

### 8. Cutover & smoke test

- [ ] Route 53 DNS cutover to the live CloudFront distributions
- [ ] End-to-end smoke test against the real domain (register, login, quote lookup, buy/sell, portfolio/transactions/performance views)
- [ ] Build a real SES-backed `EmailSender` — password reset / email verification are implemented but not end-to-end usable without it (ADR 0036/0037)
- [ ] Check the frontend's blank-page-on-network-error behavior in `checkSession()`'s app initializer against the real API origin (network error / wrong or unreachable API origin currently renders a blank page instead of falling back to a logged-out view)
- [ ] Resolve GitHub's outstanding Dependabot alert on `main` (https://github.com/Efren707/toptrader/security/dependabot/17), put off since 2026-08-05 — revisit before going live
- [ ] Resolve the `session-signing-secret` dead-config flag from [ADR 0032](../../adr/0032-prod-config-shape.md) — present in `application-local.yml.example` but never referenced in code; no `/toptrader/prod/*` SSM parameter was provisioned for it in section 4 for the same reason. Either remove it from the docs/template, or wire it up if a real use case has emerged by cutover time
- [ ] Pick back up [demo-account.md](./demo-account.md)'s blocked items now that there's a live URL: README screenshots/GIF, live demo link callout, write the actual seed migration
- [ ] Update `docs/guides/readme-structure-outline.md`'s status line / README itself with the live link

GitHub Issue: [#114](https://github.com/Efren707/toptrader/issues/114)

## Cost summary

No change from ADR 0005/0014's estimate: **~$18-20/mo** after free tier (EC2 ~$6 + RDS ~$13 + Route 53 ~$1 + negligible S3/CloudFront/Budgets).
