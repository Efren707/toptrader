# 0006 - CI/CD pipeline design: GitHub Actions, monorepo, SSH deploy, OIDC

- Status: Accepted
- Date: 2026-07-16

## Context

TopTrader needs a CI/CD pipeline deploying to the AWS shape decided in ADR 0005 (EC2 t4g.micro backend as a plain jar behind CloudFront, S3+CloudFront frontend, RDS Postgres), on top of trunk-based development (ADR 0002) with a public GitHub repo. Cost is weighted heavily throughout this project (self-funded student project) — options are evaluated on lowest reasonable cost first, with complexity/practice-value as the tiebreaker when cost is equal.

## Options considered

### Repo layout
- **Monorepo** (`backend/` + `frontend/`) - one PR can span both when a feature touches the stack; single place for issues/ADRs; `paths:`-filtered CI jobs keep pipelines fast without needing two-repo coordination.
- **Two repos** - cleaner separation but adds cross-repo coordination overhead not justified for a solo developer.

### EC2 deploy mechanism
- **SSH/SCP** (`appleboy/scp-action` + `appleboy/ssh-action`) - SCP the built jar, SSH in, `systemctl restart`. Simple, standard for single-instance low-traffic deploys.
- **AWS CodeDeploy** - confirmed free for EC2 deployments (no CodeDeploy-specific charge), so cost isn't the differentiator. Rejected on complexity: requires the CodeDeploy agent, an IAM instance role, and deployment groups/configs — overhead built for blue/green or fleet rollouts, neither of which applies to one instance.

### Frontend deploy mechanism
- **OIDC-federated IAM role** + `aws s3 sync` + CloudFront invalidation - current AWS/GitHub best practice; short-lived (≤1hr), repo/branch-scoped credentials, no long-lived secret to leak or rotate. ~15-20 minutes more one-time setup (IAM OIDC provider + trust policy) than access keys.
- **Long-lived IAM access keys as GitHub secrets** - simpler to wire up initially, but a leaked key has no expiry and needs manual rotation. Rejected given OIDC's cost is $0 either way and the setup delta is small.

### Quality gate
- **Required GitHub branch protection status check** on the lint+test job for `main` - free, built into GitHub, blocks merging on a failing pipeline.

### Secrets management
- **GitHub encrypted secrets** (free) for CI-time values: SSH private key, OIDC role ARN, S3 bucket/CloudFront distribution IDs.
- **SSM Parameter Store, standard tier** (free, SecureString via AWS-managed KMS key) for app-runtime config the EC2 instance pulls at deploy time: DB password, Finnhub API key, session-signing secret.
- **AWS Secrets Manager** ($0.40/secret/month + API calls) - rejected; automatic rotation isn't a real requirement at this scale, so the per-secret fee buys nothing here. Revisit only if rotation becomes a genuine need later.

## Decision

- **Repo layout**: monorepo, `backend/` and `frontend/` top-level directories, `paths:`-filtered GitHub Actions jobs.
- **Pipeline stages**: lint → test → build, required on every PR via branch protection; deploy stage runs only on merge to `main`.
- **Backend deploy**: SCP the built jar to EC2 and `systemctl restart` via SSH, using an SSH-key GitHub secret.
- **Frontend deploy**: OIDC-federated IAM role assumed via `aws-actions/configure-aws-credentials`, then `aws s3 sync` + CloudFront cache invalidation.
- **Secrets**: GitHub encrypted secrets for CI-time values; SSM Parameter Store (standard tier) for app-runtime config pulled onto EC2 at deploy time.

Total added AWS cost from CI/CD itself: **$0** (GitHub Actions is free on public repos; CodeDeploy would also have been free but was rejected on complexity, not cost).

## Consequences

- The EC2 security group needs SSH access reachable from GitHub-hosted runners, which don't have static IPs — mitigated with a non-default SSH port and/or fail2ban rather than IP allowlisting, adequate for a demo project's threat model. Revisit if this ever needs tightening.
- Since deploy is a simple restart with no blue/green or rollback tooling, a bad deploy causes a brief service interruption until fixed forward or manually rolled back — acceptable per the project's "no formal uptime SLA" NFR, and consistent with ADR 0005's acceptance of that trade-off for cost savings.
- The EC2 instance needs an IAM instance role (or the deploy step needs the OIDC role) with `ssm:GetParameters` permission to pull runtime secrets from SSM at deploy/boot time — a setup detail for the Phase 4 CI/CD & environment strategy work, not resolved here.
- Choosing a monorepo means CI job definitions need `paths:` filters (or `dorny/paths-filter`) to avoid running backend jobs on frontend-only changes and vice versa — a detail to implement correctly in Phase 4, not a design gap.
