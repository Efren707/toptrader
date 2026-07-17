# 0007 - Security baseline: OWASP Top 10 mitigations, CORS/CSRF, dependency scanning

- Status: Accepted
- Date: 2026-07-16

## Context

TopTrader needs a concrete security baseline beyond what's already locked in: server-side sessions with Argon2id and DB-tracked lockout (ADR 0004), and the CI/CD secrets split between GitHub encrypted secrets and SSM Parameter Store (ADR 0006). This spike fills the remaining gaps from `docs/requirements/nfr.md`'s Security section: OWASP Top 10 applied concretely to this Spring Boot + Angular stack, CORS configuration, CSRF handling for the session-cookie setup, and dependency vulnerability scanning. Unlike prior spikes, this one has few real cost forks — nearly every mitigation here is a configuration or code-pattern choice, not infrastructure spend, so cost minimization mostly ruled nothing in or out here.

## Options considered

### Dependency vulnerability scanning
- **GitHub Dependabot** - free on all public repos, needs only a `.github/dependabot.yml` for the `maven` and `npm` ecosystems.
- **OWASP Dependency-Check** - free but self-hosted in CI, requires NVD feed sync and more ongoing maintenance.
- **Snyk (free tier)** - requires account/org setup and has lower free-tier scan limits.

Dependabot wins on effort-to-value for a solo project; the others were rejected as more setup/maintenance for no real gain at this scale.

### CSRF protection approach
- **Keep Spring Security's default CSRF protection enabled**, using the cookie + header pattern (`CookieCsrfTokenRepository.withHttpOnlyFalse()` server-side, Angular's built-in `HttpClientXsrfModule` client-side) - zero extra libraries, and correctly targets the actual risk: session cookies (not JWT bearer tokens) are exactly what CSRF exploits.
- **Disable CSRF protection** (a common but incorrect pattern seen in some SPA tutorials, usually copied from JWT-bearer-token setups where it's actually safe to disable) - rejected; disabling it here would reopen a real vulnerability given the session-cookie choice in ADR 0004.

### Actuator exposure
- **Enable only `/actuator/health`, exclude everything else** - a basic uptime/health check with no information disclosure.
- **Default/broad Actuator exposure** - rejected; endpoints like `/actuator/env`, `/actuator/beans`, and `/actuator/heapdump` can leak configuration and secrets, a classic Security Misconfiguration (OWASP A05) mistake.

## Decision

- **Broken Access Control (A01)**: every controller method taking a resource ID re-derives ownership server-side (`WHERE user_id = :sessionUserId` in the query), not "load by ID then check." One IDOR integration test per resource type (portfolio, trade/transaction) asserting user A gets 403/404 on user B's data.
- **Injection (A03)**: JPA/Hibernate parameter binding is the default; any dynamic query building (search/sort/filter features) must stay parameterized, with `sort`/`direction` params validated against an allowlist rather than passed raw into `ORDER BY`.
- **Security Misconfiguration (A05)**: `server.error.include-stacktrace=never` and `spring.jpa.show-sql=false` in prod; Actuator locked down as above.
- **Vulnerable/Outdated Components (A06)**: GitHub Dependabot alerts + version-update PRs, weekly schedule, `maven` + `npm` ecosystems.
- **Identification/Authentication Failures (A07)**: confirm Spring Security's default session-fixation protection (`sessionFixation().migrateSession()`) stays enabled; use Spring Security's default `/logout` endpoint (invalidates the `HttpSession` server-side, not just a client-side cookie clear) rather than a hand-rolled logout.
- **CORS**: explicit allowed origin (`https://app.toptrader.com` in prod, `localhost:4200` in a dev profile — never a wildcard, which Spring rejects anyway when `allowCredentials(true)` is set), `allowCredentials(true)` for the session cookie, explicit allowed methods/headers including `X-XSRF-TOKEN`.
- **CSRF**: kept enabled, cookie + header pattern as described above.
- **Dependency scanning**: Dependabot, as decided above.
- **Actuator**: only `/actuator/health` exposed publicly, `show-details=never`, all other endpoints excluded.
- **SSM secrets encryption**: confirmed no change needed to ADR 0006 — standard-tier SecureString parameters use the AWS-managed `aws/ssm` KMS key at no cost; only customer-managed KMS keys carry a fee, which this project doesn't need.

## Consequences

- The IDOR integration tests become part of the core test suite expectations referenced in `nfr.md`'s Maintainability section ("meaningful automated test coverage... for core trading logic") — this ADR extends that expectation to access-control tests specifically, not just trading math.
- CORS origins are environment-specific, so local dev (`localhost:4200`) and prod (`app.toptrader.com`, per ADR 0005's domain) need separate Spring profiles or config — a detail for Phase 3/4 implementation, not resolved here.
- Angular's `HttpClientXsrfModule` needs to be wired up in the frontend app config to complete the CSRF cookie+header pairing — noted for Phase 3 frontend architecture.
- Dependabot version-update PRs will need a lightweight solo-dev triage habit (review and merge or dismiss) to stay useful rather than becoming noise — a process note, not a blocker.
- This ADR does not cover rate-limiting/abuse protection beyond the login lockout already decided in ADR 0004 — general API rate-limiting remains explicitly out of MVP scope per `user-stories.md`.
