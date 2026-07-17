# 0005 - AWS deployment shape: EC2 + CloudFront, RDS, S3+CloudFront, Route 53

- Status: Accepted
- Date: 2026-07-16

## Context

TopTrader needs an AWS hosting shape for the Spring Boot backend, PostgreSQL database, and Angular SPA frontend, deployed via CI/CD from `main`. Constraints carried in from prior decisions: session-based auth (ADR 0004) requires a single backend instance (or a shared session store/sticky sessions, neither wanted here), and a custom domain with frontend/backend on subdomains of the same parent domain for clean `SameSite=Lax` session cookies. This is a self-funded student project — cost and the ability to set spend alerts matter — while also being a resume artifact where hands-on AWS experience has some value, but not enough to justify materially higher guaranteed monthly cost for a near-zero-traffic demo.

## Options considered

### Backend compute

- **EC2 (t4g.micro)** - plain VM. ~$6/mo on-demand after free tier (free tier terms now split: legacy accounts get 750 hrs/mo for 12 months, accounts created after July 15, 2025 get 6 months instead, funded by general signup credit rather than dedicated free hours). Always-on, single instance by nature — trivially satisfies the session-store constraint. ACM certs can't attach directly to a bare EC2 instance, so TLS/custom domain requires CloudFront (or a self-managed cert) in front.
- **ECS on Fargate** - containerized. No free tier for Fargate compute; a small always-on task is ~$9-10/mo, but a stable HTTPS endpoint needs an ALB, which is a ~$16+/mo *fixed* cost regardless of traffic. Total ~$25-26/mo for compute+LB alone. More cloud-native/resume-relevant pattern (containers, ECS), but the ALB cost isn't offset by any functional need at this traffic level (only one target, no real requirement for load balancing or sticky sessions).
- **Elastic Beanstalk** - PaaS wrapper over EC2; same underlying cost as plain EC2 plus platform overhead, no meaningful advantage over EC2 directly for this use case.
- **AWS App Runner** - **ruled out**: AWS stopped accepting new App Runner customers in April 2026 (maintenance mode), steering new workloads to a newer "ECS Express Mode" offering instead. Not viable to build on today.

### Database

- **RDS PostgreSQL (db.t4g.micro)** - free tier (legacy accounts: 750 hrs/mo, 20GB storage, 12 months; newer accounts: general credit pool only). After free tier, ~$12-13/mo Single-AZ, reasonable to leave running; can be stopped for up to 7 days at a stretch (auto-restarts) if cost-cutting during idle stretches is ever needed.

### Frontend hosting

- **S3 + CloudFront** - no fixed cost, pay-per-GB/request (negligible at this traffic). Deploys cleanly from GitHub Actions via `aws s3 sync` + CloudFront invalidation, matching the backend's CI/CD pattern.
- **AWS Amplify Hosting** - comparable cost after its own free tier, trades some control for simpler git-push deploys — not chosen since GitHub Actions is already the standard pipeline and S3+CloudFront keeps both frontend and backend deploys consistent in approach.

### Domain / TLS / budget

- **Route 53** for domain registration (~$12-14/yr) and hosted zone (~$0.50/mo), giving the `app.` / `api.` subdomain split needed for session cookies.
- **ACM** (free) certs attach cleanly to CloudFront (covers both the S3 frontend and, via a CloudFront distribution in front of EC2, the backend's TLS termination too).
- **AWS Budgets** alerts are free to configure at any threshold, independent of compute choice.

## Decision

- **Backend**: EC2 t4g.micro, single instance, with CloudFront in front for TLS termination and the custom `api.` subdomain.
- **Database**: RDS PostgreSQL db.t4g.micro, Single-AZ.
- **Frontend**: S3 + CloudFront for the Angular SPA, on the `app.` subdomain.
- **Domain**: Route 53, one custom domain with `app.` and `api.` subdomains sharing the parent domain (satisfies ADR 0004's session-cookie requirement).
- **Budget guardrails**: AWS Budgets alerts configured at defined thresholds (specific dollar amounts to be set at implementation time, not part of this ADR).

Estimated realistic cost after free tier: **~$18-20/mo** (EC2 ~$6 + RDS ~$13 + Route 53 ~$1 + negligible S3/CloudFront).

## Consequences

- Deployment from GitHub Actions is a straightforward SSH/SCP-and-restart (or `docker pull`+restart if containerized later) for the backend, and `s3 sync`+invalidation for the frontend — both simple, well-documented CI/CD patterns, feeding directly into the upcoming Phase 2 CI/CD pipeline design spike.
- Chosen over ECS Fargate primarily on cost (~$18-20/mo vs. ~$37-40/mo with a mandatory ALB) — the resume-value trade-off of containers/ECS was explicitly considered and set aside in favor of lower guaranteed spend for a near-zero-traffic demo. This can be revisited post-MVP if traffic or learning goals change; it is not a one-way door (moving from EC2 to a containerized service later is a re-platforming exercise, not a rewrite).
- Because it's a single EC2 instance with no auto-scaling, a crash/reboot means brief downtime until it's manually or automatically restarted — acceptable per the project's "no formal uptime SLA" NFR, but worth a basic health-check/restart policy (e.g. via a process supervisor or a simple CloudWatch alarm) when Phase 4 (CI/CD & environment strategy) is designed.
- RDS free-tier terms depend on whether the AWS account predates July 15, 2025 — needs to be checked against the actual account used at implementation time, since the two tiers have materially different free allowances.
