# 0008 - Observability basics: local logs + CloudWatch, systemd health checks, SNS alerting

- Status: Accepted
- Date: 2026-07-16

## Context

TopTrader's last Phase 2 research spike: baseline observability (logging, health checks, metrics, alerting, process supervision) for the single-instance EC2 hosting shape decided in ADR 0005 (no load balancer, no containers). Cost is weighted heavily throughout this project; this spike also resolves the follow-up flagged in ADR 0005 — "a crash/reboot means brief downtime until manually or automatically restarted... worth a basic health-check/restart policy."

## Options considered

### Application logging
- **Local logback file + unified CloudWatch agent** tailing the file to CloudWatch Logs - app keeps writing local files normally (crash-safe, greppable via SSH), agent ships them asynchronously. CloudWatch Logs has a standing (not 12-month) free tier of 5 GB/month combined ingestion+storage+queries; a low-traffic demo app stays well under that, and capping retention at 14-30 days (vs. the "never expire" default) avoids storage cost creep.
- **Direct-to-CloudWatch logback appender** - rejected; couples the app to AWS SDK logging plumbing and can buffer/lose lines on hard crashes, unlike a local file.

### Health checks
- **systemd timer/cron script on the EC2 instance itself**, curling `localhost:8080/actuator/health` and restarting the service on repeated failure - $0, no AWS service required, standard self-healing pattern for single-instance deployments.
- **Route 53 health check** (~$0.50/mo) or **CloudWatch Synthetics canary** (~$10/mo at a 5-minute interval) - both rejected as unnecessary paid layers for this project's scale; CloudWatch also has no native way to poll an arbitrary HTTP(S) endpoint without one of these paid services layered on top, since there's no ALB in this architecture (ADR 0005). A Route 53 health check remains a reasonable *optional future upgrade* (~$6/yr) if verifying the full external path (DNS/CloudFront/security groups) rather than just the local process ever becomes worth the cost.

### Metrics
- **Default free EC2 metrics** (CPU, network, disk, status checks, 5-minute granularity) - sufficient for a demo app, $0.
- **Micrometer/Actuator metrics wired to CloudWatch as custom metrics** - rejected; $0.30/metric/month adds real cost and complexity for JVM/HTTP metrics with no audience beyond the developer.

### Alerting
- **CloudWatch Alarm on the free `StatusCheckFailed` EC2 metric → SNS email** - first 10 alarms/month free, SNS email free for the first 1,000 notifications/month (permanent, not time-limited tier). $0 at this scale.

### Process supervision
- **systemd unit with `Restart=on-failure`, `RestartSec=5`, running as a non-root service user** - standard, free, zero-dependency approach for a plain EC2 jar deployment (no supervisord/pm2/Docker needed, consistent with ADR 0005's decision not to containerize).

## Decision

- **Logging**: local logback file, unified CloudWatch agent tailing it to CloudWatch Logs, retention capped at 14-30 days.
- **Health checks**: systemd timer script curling `localhost:8080/actuator/health` (the endpoint locked down in ADR 0007), triggering a service restart on repeated failure.
- **Metrics**: default free EC2 metrics only; no custom Micrometer/CloudWatch metrics.
- **Alerting**: CloudWatch Alarm on `StatusCheckFailed` → SNS email notification.
- **Process supervision**: systemd unit, `Restart=on-failure`, `RestartSec=5`, non-root service user.

Total added AWS cost: **$0/month**, staying within CloudWatch Logs' standing free tier and SNS's free email allotment.

## Consequences

- This closes out **Phase 2 — Research Spikes** in full (market data, auth, AWS shape, CI/CD, security baseline, observability all decided). Phase 3 (Technical & Architecture Documentation) is next.
- Resolves the ADR 0005 follow-up on crash/restart handling: systemd's `Restart=on-failure` handles process crashes, and the health-check timer catches the "JVM alive but app wedged" case that a process-level restart policy alone wouldn't.
- The unified CloudWatch agent needs to be installed and configured as part of the EC2 instance setup (a Phase 3/4 implementation detail, not resolved here) — likely via a bootstrap script or baked into an AMI if one gets introduced later.
- No external path monitoring exists yet (nothing detects a DNS, CloudFront, or security-group misconfiguration if the EC2 process itself is healthy but unreachable) — the deferred Route 53 health check is the documented upgrade path if that gap ever needs closing.
- Log retention and CloudWatch cost should be spot-checked after the app has real usage patterns, in case actual log volume differs meaningfully from the low-traffic assumption this decision was based on.
