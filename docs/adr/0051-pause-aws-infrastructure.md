# 0051 - Pause AWS infrastructure by snapshotting and deleting compute/database

- Status: Accepted
- Date: 2026-10-07

## Context

TopTrader has been live at `app.toptrader.dev` since the AWS Deployment Infrastructure milestone closed (2026-08-11), at roughly $18-20/mo per ADR 0005/0014 - plus ~$3.60/mo for the Elastic IP (ADR 0043), which that ADR assumed was free while attached but which AWS has charged for since its February 2024 change to bill every public IPv4 address, attached or not. The app's purpose is resume/showcase, and it's getting no traffic while I'm not actively job searching. Feature work (Leaderboard) continues locally in the meantime.

The goal is to cut the monthly bill close to zero while keeping a cheap, low-risk path back to the exact same live setup when job searching resumes. The hard part to reproduce isn't the AWS resources themselves - it's the hand-configured state on the EC2 instance (systemd units, health-check timer, `deploy` user and sudoers rule, sshd on port 3333, fail2ban, CloudWatch agent config, `/var/log/toptrader` permissions), none of which is automated (see `docs/tasks/completed/aws-infrastructure-implementation.md` sections 4, 7, 9).

## Options considered

- **Stop EC2 and RDS** - simplest, fully reversible with one click. But AWS automatically restarts a stopped RDS instance after 7 days, so it would need re-stopping weekly (and charges resume silently when forgotten). A stopped EC2 still bills its EBS volume and the Elastic IP. Saves well under half the bill in practice.
- **Snapshot-and-delete EC2 and RDS, keep everything else** - create an AMI of the EC2 instance and a final RDS snapshot, then terminate/delete both and release the Elastic IP. Snapshot storage for this data volume is cents per month. S3, both CloudFront distributions (Free flat-rate plan, ADR 0042), Route 53, ACM, SSM parameters, IAM, SNS, and Budgets stay in place - they're free or near-free and are the more tedious parts to rebuild. Restore is roughly an hour following a runbook.
- **Tear down everything** - true $0 (besides the domain), but restoring means redoing most of the AWS Deployment Infrastructure milestone from scratch, for savings of about $1/mo over the previous option.
- **Move to a cheaper host (e.g., a free-tier PaaS)** - would change the deployment shape decided in ADR 0005 and lose the AWS deploy pipeline that's part of the showcase. Out of scope for a temporary pause.

## Decision

**Snapshot-and-delete EC2 and RDS; keep everything else; release the Elastic IP.** Deploys are gated behind a `DEPLOY_ENABLED` GitHub Actions repo variable (both `deploy-frontend` and `deploy-backend` require it to equal `'true'`), so merges to `main` - feature work and Dependabot PRs alike - keep passing CI without trying to deploy to an instance that no longer exists. Unset counts as disabled, so the safe state is the default.

Both deploy jobs are gated by one variable rather than only the backend: the frontend could still deploy to S3, but shipping new frontend features against an offline backend has no value, and one switch is simpler to reason about on restore.

The step-by-step pause and restore procedure lives in [docs/guides/aws-pause-restore-runbook.md](../guides/aws-pause-restore-runbook.md).

## Consequences

- Monthly cost drops from ~$22 to roughly $1 or less (EBS/RDS snapshot storage, Route 53 queries, S3), plus the yearly `toptrader.dev` renewal, which must stay on auto-renew to keep the domain.
- `app.toptrader.dev` keeps loading from S3/CloudFront, but every API call fails - including the "Try Demo" button. README's status line is updated to say the live deploy is paused, so a visitor isn't left guessing.
- Releasing the Elastic IP means a restore gets a new public IP/DNS name, which has to be updated in two places: the `toptrader-backend` CloudFront origin and the hardcoded `host:` in `ci.yml`'s `deploy-backend` steps. The CloudFront origin is the same field misconfigured during the original cutover (aws-infrastructure-implementation.md section 8), so the runbook calls it out explicitly. Keeping the Elastic IP (~$3.60/mo) would avoid this; rejected as the larger recurring cost of the two, given the cost-minimizing lean throughout this project.
- Restoring the RDS snapshot under the same `toptrader` identifier yields the same endpoint hostname, so the `/toptrader/prod/spring-datasource-url` SSM parameter shouldn't need to change.
- The `toptrader-ec2-status-check-failed` CloudWatch alarm is tied to the terminated instance ID; it sits in `INSUFFICIENT_DATA` while paused and must be re-pointed at the new instance ID on restore.
- Supersedes ADR 0043's "free while attached" cost note (see Context) - otherwise ADR 0043's reasoning for using an Elastic IP still applies on restore.
