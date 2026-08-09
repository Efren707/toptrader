# 0043 - Allocate an Elastic IP for the EC2 instance

- Status: Accepted
- Date: 2026-08-08

## Context

Neither ADR 0005 nor ADR 0014 addressed EC2's public IP stability. Without an Elastic IP, an EC2 instance's default public IPv4 address is dynamic - it's released and reassigned on stop/start (not on reboot). Setting up CloudFront distribution #2 (section 5, backend/`api.` subdomain) requires pointing a custom origin at the EC2 instance's public DNS name, which is a fixed value configured once - if the underlying IP ever changed, the origin would silently break until someone noticed and manually updated it.

## Options considered

- **Allocate an Elastic IP, associate with the instance** - static public IP that survives a stop/start, not just a reboot. Free while attached to a running instance (AWS only charges for an *unattached* Elastic IP, or for excessive remapping). No cost change to the ~$6/mo EC2 estimate.
- **Use the default dynamic public IP/DNS** - one less resource to track, but any incidental stop/start (AWS maintenance, manual intervention) silently breaks the CloudFront origin until manually fixed - a debugging trap disproportionate to the near-zero cost of avoiding it.

## Decision

**Allocate an Elastic IP** and associate it with the EC2 instance; use its stable address (or the Elastic IP's associated DNS name) as CloudFront distribution #2's custom origin domain.

## Consequences

- No cost impact as long as the Elastic IP stays associated with a running instance - if the instance is ever terminated and not promptly replaced, the now-unattached Elastic IP would start accruing a small hourly charge until released.
- The instance's public IP is now permanent from CloudFront's perspective - a stop/start no longer risks breaking the `api.` origin.
- If the EC2 instance is ever replaced (new instance ID, e.g. disaster recovery or a resize), the Elastic IP can be re-associated to the new instance without needing to update CloudFront's origin domain at all - a minor added resilience benefit beyond the immediate problem.
