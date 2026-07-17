# 0014 - Deployment infrastructure: default VPC, CloudFront-only EC2 exposure, HTTP origin, budget thresholds

- Status: Accepted
- Date: 2026-07-16

## Context

ADR 0005 decided the AWS deployment shape (EC2 + CloudFront, RDS, S3 + CloudFront, Route 53) but deferred several infra-level details: network layout, exactly how EC2 is exposed, the CloudFront-to-origin protocol, and the specific AWS Budgets dollar thresholds ("specific dollar amounts to be set at implementation time, not part of this ADR"). Full infra design is in `docs/architecture/deployment-architecture.md`; this ADR records the four sub-decisions with real alternatives.

## Options considered

### Network layout

- **Default VPC** - EC2 in the default public subnet, RDS with `publicly accessible = No` and a security group restricted to EC2's security group. Isolation via security groups, not subnet topology.
- **Custom VPC with public/private subnets** - RDS in a dedicated private subnet with no internet route at all. More textbook defense-in-depth and a more "enterprise" resume pattern, but no cost difference (no NAT gateway needed either way) and no material security gain over the security-group-only approach for a single-instance, single-database deployment.

### EC2 app-port exposure

- **Restricted to CloudFront's managed origin-facing prefix list** - only CloudFront can reach the app port; direct requests to the EC2 public IP are rejected. Preserves the point of putting CloudFront in front at all (TLS termination, security headers from `security-architecture.md`).
- **Open to `0.0.0.0/0`** - simpler security group rule, but lets anyone bypass CloudFront (and its TLS/headers) by hitting the EC2 public IP directly over plain HTTP.

### CloudFront → EC2 origin protocol

- **HTTP within AWS's network** - client-facing TLS still fully terminates at the CloudFront edge; the CloudFront-to-origin hop never leaves AWS's backbone. No certificate to provision/rotate on EC2.
- **HTTPS with a self-signed cert on EC2** - encrypts the origin hop too, but adds real ongoing complexity (cert generation/rotation on EC2, CloudFront configured to accept a self-signed cert) for a hop that's already inside AWS's network, not the public internet.

### Budget alert thresholds

- **$15 warning / $25 critical** - warning fires before the ADR 0005 steady-state estimate (~$18-20/mo) is even reached, catching a runaway/misconfigured resource early; critical sits meaningfully above the estimate.
- **$25 warning / $40 critical** - looser, fewer alert emails, but a runaway cost would run further before detection.

## Decision

- **Default VPC**, security-group-based isolation (no custom subnets).
- **EC2 app port restricted to CloudFront's origin-facing prefix list.**
- **CloudFront → EC2 origin protocol: HTTP** (inside AWS's network only).
- **AWS Budgets thresholds: $15 warning, $25 critical**, both notifying by email.

## Consequences

- Because EC2 only accepts app-port traffic from CloudFront, any future need to hit the backend directly (debugging, a health check from outside CloudFront) must go through CloudFront too — a minor constraint, not expected to matter at this scale.
- The default-VPC choice means RDS's isolation depends entirely on its security group and `publicly accessible = No` being configured correctly — worth double-checking explicitly at RDS provisioning time, since there's no subnet-level backstop if that flag were ever misconfigured.
- If this project's threat model or audience ever changed (e.g. handling real money/PII instead of a virtual-cash demo), the HTTP-origin and default-VPC choices would be the first two things to revisit.
- Budget threshold emails will need an actual verified SNS/Budgets email recipient configured at implementation time — not resolved here.
