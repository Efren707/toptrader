# Security Architecture

> Consolidates security decisions already made across ADR 0004 (auth), ADR 0006 (CI/CD secrets), ADR 0007 (OWASP baseline), ADR 0009 (local dev secrets), ADR 0010 (data model), and ADR 0012 (API contract) into one place, organized by concern. Fills a few remaining implementation-level gaps (transport security headers, input validation approach, XSS posture) that hadn't been made explicit anywhere yet — flagged below rather than buried in prose.

## Authentication & session management

- Server-side sessions (Spring Session), `SESSION` cookie: `HttpOnly; Secure; SameSite=Lax` (ADR 0004, ADR 0012).
- Passwords hashed with Argon2id via Spring Security's `DelegatingPasswordEncoder` (ADR 0004); never logged or stored in plaintext.
- Brute-force protection: DB-tracked `failed_attempts`/`locked_until` on `users` (ADR 0004, ADR 0010), fixed lockout window, populated via Spring Security's auth event listeners.
- Session fixation protection stays on Spring Security's default (`sessionFixation().migrateSession()`) — confirmed, not disabled (ADR 0007, OWASP A07).
- Logout uses Spring Security's default `/logout` endpoint, which invalidates the `HttpSession` server-side — not a client-side-only cookie clear (ADR 0007).
- Login errors are a single generic "invalid email or password" message regardless of which was wrong — no user enumeration (per `acceptance-criteria.md`).

## Access control (OWASP A01)

- Every controller method that touches a user-owned resource re-derives ownership server-side (`WHERE user_id = :sessionUserId` in the query) — never "load by ID, then check." (ADR 0007)
- One IDOR integration test per resource type (portfolio, transactions) asserting user A gets 403/404 on user B's data — part of the core test suite expectations from `nfr.md`.
- `GET /portfolio` and `GET /transactions` never take a user ID from the client at all — the session is the only source of "whose data" (consistent with `openapi.yaml`).

## Transport & network

- HTTPS everywhere in the deployed environment — no plaintext HTTP endpoints (`nfr.md`), TLS terminated at CloudFront on both the `app.` and `api.` subdomains (ADR 0005).
- CORS: explicit allowed origin (`https://app.toptrader.com` prod / `localhost:4200` dev — never a wildcard), `allowCredentials(true)`, explicit allowed methods/headers including `X-XSRF-TOKEN` (ADR 0007).
- **Security headers (new in this doc)** — applied via a CloudFront Response Headers Policy (AWS-managed, free, covers both the S3 frontend and the CloudFront-fronted backend) rather than hand-rolled in app code:
  - `Strict-Transport-Security: max-age=63072000; includeSubDomains`
  - `X-Content-Type-Options: nosniff`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Content-Security-Policy` — a `default-src 'self'` baseline restricted to the app's own origins; exact directives (in particular whether Angular's production build needs any style/script exceptions) need verifying against the actual build output once the frontend exists, so this is flagged as an implementation-time task, not locked in with false precision here.

## CSRF

- Kept enabled (not disabled, despite that being a common SPA-tutorial mistake) — cookie + header pattern: `CookieCsrfTokenRepository.withHttpOnlyFalse()` server-side, Angular's `HttpClientXsrfModule` client-side (ADR 0007). This is the correct posture specifically *because* auth uses session cookies, not JWT bearer tokens.

## XSS (new in this doc)

- No dedicated ADR exists for this since Angular's default template binding (`{{ }}`, property binding) auto-escapes/sanitizes by default — the main discipline required is simply *not* reaching for `[innerHTML]` or `bypassSecurityTrust*` APIs without a specific, reviewed reason. Noted here as a standing convention for Phase 5+ frontend implementation rather than a decision with real alternatives to weigh.
- The CSP above is the defense-in-depth backstop for this same risk class.

## Input validation & injection (OWASP A03)

- JPA/Hibernate parameter binding is the default for all queries; no string-concatenated SQL (`nfr.md`, ADR 0007).
- Any dynamic query building (search/sort/filter) validates `sort`/`direction` params against an allowlist rather than passing raw values into `ORDER BY` (ADR 0007).
- **Request validation (new in this doc)**: DTOs for all write endpoints (`RegisterRequest`, `LoginRequest`, `TradeRequest` in `openapi.yaml`) use Jakarta Bean Validation annotations (`@NotBlank`, `@Email`, `@Size(min=8)`, `@Min(1)`) enforced server-side regardless of client-side validation — matching `nfr.md`'s "all user input validated/sanitized server-side" requirement and producing the `400` `ProblemDetail` responses already specified in the API contract (ADR 0012).

## Security misconfiguration (OWASP A05)

- `server.error.include-stacktrace=never`, `spring.jpa.show-sql=false` in prod (ADR 0007).
- Actuator: only `/actuator/health` exposed, `show-details=never`, everything else excluded (ADR 0007).
- All errors return RFC 7807 `application/problem+json` via Spring's `ProblemDetail` — no raw stack traces or framework default error pages ever reach the client (ADR 0012).

## Secrets management

End-to-end flow, consolidating ADR 0006 and ADR 0009:

- **CI-time secrets** (SSH private key, OIDC role ARN, S3/CloudFront IDs): GitHub encrypted secrets.
- **App-runtime secrets** (DB password, Finnhub API key, session-signing secret): SSM Parameter Store, standard tier, `SecureString` via the AWS-managed `aws/ssm` KMS key (no customer-managed-key cost). Pulled onto EC2 at deploy/boot time — never baked into the jar or committed.
- **Local dev secrets**: gitignored `application-local.yml` + a committed `.example` template (ADR 0009) — Docker Compose runs Postgres only.
- No secret is ever logged; `spring.jpa.show-sql=false` and stack traces off in prod (above) double as guardrails against accidental leakage into logs.

## Dependency management (OWASP A06)

- GitHub Dependabot, weekly schedule, `maven` + `npm` ecosystems, free on the public repo (ADR 0007). Requires a lightweight solo-dev triage habit to stay useful.

## Financial data integrity

- All monetary/quantity fields use exact decimal types (`BigDecimal`/Postgres `numeric`) — never floating-point (`nfr.md`).
- Every trade is an immutable `transactions` record; `cash_balance`/`holdings` are updated transactionally alongside it, with row-level locking to prevent lost updates under concurrent requests (ADR 0010).
- Trade execution price is always fetched server-side at confirm time — never client-supplied — closing off price manipulation via the request body (ADR 0012).

## OWASP Top 10 mapping (summary)

| Category | Mitigation | Source |
|---|---|---|
| A01 Broken Access Control | Server-derived ownership on every query, IDOR tests | ADR 0007 |
| A02 Cryptographic Failures | Argon2id hashing, HTTPS everywhere, TLS at CloudFront | ADR 0004, ADR 0005 |
| A03 Injection | JPA parameter binding, allowlisted sort/filter params, Bean Validation | ADR 0007, this doc |
| A04 Insecure Design | Explicit confirm step + server-fetched pricing on trades | ADR 0012 |
| A05 Security Misconfiguration | Actuator locked down, stack traces off, RFC 7807 errors | ADR 0007, ADR 0012 |
| A06 Vulnerable Components | Dependabot, weekly | ADR 0007 |
| A07 Auth Failures | Session fixation protection, DB-tracked lockout, generic login errors | ADR 0004, ADR 0007 |
| A08 Software/Data Integrity | Immutable transaction log, transactional balance updates | ADR 0010 |
| A09 Logging/Monitoring Failures | CloudWatch + SNS alerting (see `docs/adr/0008-observability-basics.md`) | ADR 0008 |
| A10 SSRF | N/A — no user-supplied URLs are fetched server-side (Finnhub calls use a fixed base URL) | — |

## Explicitly out of scope for MVP

- General-purpose API rate-limiting beyond login lockout (`user-stories.md`).
- Password reset / email verification flows (`user-stories.md`).
- Automated penetration testing / SAST tooling beyond Dependabot (not raised as a requirement yet — revisit if this ever needs to look more "production-grade" for resume purposes).
