# Deployment & Infrastructure Architecture

> Infra-level detail behind the topology shown in [system-architecture.md](./system-architecture.md). Consolidates ADR 0005 (AWS shape), ADR 0006 (CI/CD), ADR 0008 (observability), and ADR 0009 (local dev), and fills the remaining infra-design gaps (network layout, security groups, IAM roles, budget thresholds) in ADR 0014. Environment-specific config strategy (local vs. prod profiles, per-environment secrets wiring, ADR 0015) is consolidated in [environments.md](./environments.md), not this doc.

## Network layout

- **Default VPC**, no custom subnets/route tables/NAT gateway — isolation comes from security groups, not subnet topology, which is sufficient for one EC2 instance and one RDS instance.
- **EC2**: in the default public subnet (needs a public IP so CloudFront can reach it as a custom origin).
- **RDS**: `publicly accessible = No`; security group allows inbound on 5432 **only** from the EC2 security group (SG-to-SG reference, not a CIDR range).

## Compute — EC2

- t4g.micro, single instance, running the Spring Boot jar under `systemd` (restart-on-failure).
- Security group:
  - App port (e.g. 8080): inbound only from AWS's managed **CloudFront origin-facing prefix list** — prevents bypassing CloudFront (and its TLS termination/security headers from `security-architecture.md`) by hitting the EC2 public IP directly.
  - SSH: non-default port, open to `0.0.0.0/0` (GitHub-hosted runners have no static IPs to allowlist) with fail2ban, per ADR 0006.
- IAM instance role: `ssm:GetParameters`/`GetParametersByPath` + `kms:Decrypt` (on `aws/ssm`), scoped to the `/toptrader/prod/*` parameter path prefix — pulls DB password, Finnhub key, session-signing secret during the SSH deploy step (ADR 0006, ADR 0015), plus the CloudWatch agent's standard `cloudwatch:PutMetricData` / `logs:*` permissions for ADR 0008.

## Database — RDS

- PostgreSQL, db.t4g.micro, Single-AZ (ADR 0005).
- Flyway migrations (ADR 0011) run automatically against this instance at Spring Boot startup.

## CDN / TLS

- Two CloudFront distributions: one fronting S3 (frontend, `app.` subdomain), one fronting EC2 (backend, `api.` subdomain) — both terminate TLS with ACM certs (ADR 0005).
- **CloudFront → EC2 origin protocol: HTTP**, staying inside AWS's network backbone rather than the public internet — client-facing TLS is still fully terminated at the CloudFront edge either way. Avoids managing/rotating a cert on the EC2 instance for a hop that never touches the public internet.

## IAM roles (CI/CD)

- **GitHub Actions OIDC role** (frontend deploy): trust policy scoped to this specific repo/branch; permissions limited to `s3:PutObject`/`s3:DeleteObject` on the frontend bucket and `cloudfront:CreateInvalidation` on that one distribution (ADR 0006).
- **Backend deploy**: SSH key GitHub secret, not an IAM role — SCP + `systemctl restart` (ADR 0006).

## Observability

Per ADR 0008: local logs + CloudWatch agent, systemd health-check timer polling `/actuator/health` with auto-restart on failure, default free EC2 metrics, a CloudWatch Alarm wired to an SNS topic that emails on failure.

## Budget guardrails

AWS Budgets, monthly cost budget, two thresholds (against the ADR 0005 realistic estimate of ~$18-20/mo):
- **$15** — warning, fires before steady-state cost is even reached (catches a runaway/misconfigured resource early).
- **$25** — critical, meaningfully above the estimate, leaving headroom for normal variation without false alarms.

Both notify via email (SNS or AWS Budgets' native email action — implementation detail, not a design fork).

## Cost summary

No change to ADR 0005's estimate: **~$18-20/mo** after free tier (EC2 ~$6 + RDS ~$13 + Route 53 ~$1 + negligible S3/CloudFront/Budgets, all $0-cost decisions in this doc).
