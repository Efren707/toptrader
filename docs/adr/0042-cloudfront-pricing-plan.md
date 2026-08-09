# 0042 - CloudFront flat-rate Free plan, not pay-as-you-go

- Status: Accepted
- Date: 2026-08-08

## Context

ADR 0005/0014 assumed CloudFront would be billed pay-as-you-go, with cost folded into the "negligible S3/CloudFront/Budgets" line of the ~$18-20/mo estimate. Provisioning the two distributions in section 5 of the AWS infrastructure implementation surfaced a console change since those ADRs were written: CloudFront now leads distribution creation with a choice between flat-rate pricing plans (Free/Pro/Business/Premium, bundling CloudFront + AWS WAF + the Route 53 hosted zone + CloudWatch Logs ingestion into one monthly price) and traditional pay-as-you-go. This wasn't anticipated by either ADR and changes whether WAF ends up enabled at all.

## Options considered

- **Free flat-rate plan, one subscription per distribution** - $0/month per distribution (account allows up to 3 Free plans; this project needs 2). Bundles in basic AWS WAF + always-on DDoS protection at no extra charge - something not otherwise planned given WAF's standalone pay-as-you-go cost. Usage allowance (1M requests / 100GB transfer / month, per distribution) is far beyond what a resume/demo project will see. Origin Access Control (our S3 access mechanism) and a free ACM-managed cert are both supported on this tier (we're using our own already-issued cert instead). The one binding consequence: a WAF Web ACL must stay attached to a distribution on a flat-rate plan - detaching WAF requires switching that distribution back to pay-as-you-go first.
- **Pay-as-you-go (original assumption)** - matches ADR 0005/0014 as written. Billed per request/GB, already estimated as negligible at this project's traffic level. No bundled WAF - would need to be added separately, at its own additional cost, if wanted later.
- Flat-rate plans are unavailable to accounts still inside the AWS Free Tier window - not a blocker here, since section 3 already confirmed this account's 12-month free-tier window has elapsed.

## Decision

**Free flat-rate plan**, one subscription per CloudFront distribution (two total: `app.` fronting S3, `api.` fronting EC2).

## Consequences

- CloudFront cost for both distributions becomes a hard $0/month up to the combined usage allowance, rather than "negligible-but-technically-metered" - strictly better against the project's cost-minimization goal, no change to the ~$18-20/mo total estimate either way.
- Basic AWS WAF (5 custom/managed rules) and always-on DDoS protection are now active on both distributions at no cost - previously out of scope for this project given WAF's own pay-as-you-go pricing.
- Each distribution's WAF Web ACL can't be removed without first moving that distribution off the Free plan - accepted, since the WAF association is a free upside here, not something we want to disable anyway.
- The `toptrader.dev` Route 53 hosted zone is attached to the `app.` distribution's Free plan (a zone can only attach to one plan at a time) - folds the zone's DNS query/hosted-zone costs into that plan's $0 flat price, zeroing out the ~$1/mo Route 53 line from the ADR 0005/0014 estimate. Well within the Free tier's 50-records-per-zone quota (zone holds ~6 records: NS/SOA + 2 ACM validation CNAMEs + 2 CloudFront alias records). Reversible via "Manage Plan" if the zone ever needed to detach (e.g. quota growth).
- If this project's traffic model ever changed materially (real users, not a demo), the Free plan's 1M request / 100GB allowance would be the first thing to revisit - soft allowance, not a hard cutoff (AWS accommodates overage before requiring an upgrade), so not an urgent risk.
