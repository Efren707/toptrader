# 0015 - Runtime secrets delivery: EC2 instance IAM role pulls SSM parameters at deploy time

- Status: Accepted
- Date: 2026-07-17

## Context

ADR 0006 decided SSM Parameter Store (standard tier) holds app-runtime config (DB password, Finnhub API key, session-signing secret) and said this is "pulled at deploy time" onto the EC2 instance, but explicitly left unresolved *which AWS principal* does the pulling and what permissions that requires. This is the first decision under Phase 4 (CI/CD & Environment Strategy), needed to fully specify the local/prod environment split.

## Options considered

- **EC2 instance IAM role** - EC2 gets its own IAM instance profile, scoped to `ssm:GetParameters`/`GetParametersByPath` (+ `kms:Decrypt` on the AWS-managed `aws/ssm` key) restricted to a `/toptrader/prod/*` parameter path prefix. A script on the instance, run over the existing SSH deploy session (ADR 0006), calls the AWS CLI locally to fetch parameters and write the app's config file (e.g. `application-prod.yml` or an env file consumed by the systemd unit) before `systemctl restart`. Matches ADR 0006's literal wording that "the EC2 instance pulls" config, and keeps secrets off the GitHub Actions runner and out of the SCP transfer entirely — they're fetched locally, over AWS's internal network, using short-lived instance-role credentials.
- **GitHub Actions OIDC role fetches, then SCPs the file** - extend the OIDC role already used for the S3/CloudFront frontend deploy (ADR 0006) with `ssm:GetParameters`, fetch secrets on the ephemeral GitHub-hosted runner, generate the config file there, and SCP it to EC2 alongside the built jar. Avoids standing up a second IAM principal, but secrets now exist (briefly) on a third-party-hosted runner and transit SCP, widening the exposure window relative to the instance-role approach for no cost or setup savings that matters at this scale.

## Decision

**EC2 instance IAM role.** The EC2 instance gets its own IAM instance profile with `ssm:GetParameters` / `ssm:GetParametersByPath` and `kms:Decrypt` (on the default `aws/ssm` key), scoped via resource ARN to a `/toptrader/prod/*` parameter path prefix — no broader SSM or KMS access. The SSH deploy step (ADR 0006) runs a small script on the instance that calls the AWS CLI to fetch parameters and materialize the local config file consumed by the systemd unit, then restarts the service.

## Consequences

- A one-time setup step is added to EC2 provisioning: create the IAM role/instance profile, attach the scoped policy, and attach the profile to the instance. This is an implementation detail for whenever EC2 is actually provisioned, not further design work.
- Secrets never leave AWS's network or touch the GitHub-hosted runner — the instance role's temporary credentials are fetched from the EC2 metadata service locally.
- The parameter path prefix (`/toptrader/prod/*`) becomes the naming convention SSM parameters must follow; local dev config (ADR 0009's `application-local.yml`) is unaffected since it never touches SSM.
- If a `staging`-style environment is ever added, it would get its own path prefix (e.g. `/toptrader/staging/*`) and either a separate instance role or a broadened policy — not needed for the current local/prod split.
