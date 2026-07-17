# 0004 - Authentication strategy: server-side sessions, Argon2id, DB-tracked lockout

- Status: Accepted
- Date: 2026-07-16

## Context

TopTrader is a Spring Boot REST API with a separate Angular SPA frontend, deployed on AWS (hosting shape not yet decided). Requirements already locked in `docs/requirements/`: plain email/password only (no social login), 8-char minimum password length with no forced complexity, passwords never stored/logged in plaintext, basic brute-force protection on login is in MVP scope, authenticated state must persist across a page refresh, and a user must only ever access their own data (server-side authorization on every request). This ADR covers three sub-decisions: API auth mechanism (sessions vs. JWT), password hashing algorithm, and brute-force protection approach.

## Options considered

### API auth mechanism

- **Server-side sessions (Spring Session, cookie-based)** - session state lives server-side; browser holds only a session-id cookie. Gives free CSRF mitigation via `SameSite`, trivial server-side revocation (delete session = instant logout everywhere), and no client-side token storage/XSS exposure.
- **JWT (stateless bearer tokens)** - no server-side session state. Sold on statelessness for multi-service/mobile/third-party-client scenarios, none of which apply here. Introduces a genuine storage dilemma (localStorage = XSS-readable; httpOnly cookie = same cross-origin cookie complexity as sessions, without gaining real revocation, since a leaked JWT stays valid until expiry). Requires an access+refresh token pattern (rotation, refresh endpoint, 401-retry interceptor) — real engineering overhead not justified for a solo demo app.

### Password hashing

- **Argon2id** - first-class Spring Security support (`Argon2PasswordEncoder`, one-line via `DelegatingPasswordEncoder`). Current OWASP Password Storage Cheat Sheet primary recommendation.
- **BCrypt** - still cryptographically sound if tuned (cost factor 12-13), but OWASP now positions it as "legacy systems only" guidance. No reason to pick it for a new 2026 project over Argon2id.

### Brute-force protection

- **DB-tracked failed-attempt counter** - `failedAttempts` + `lockedUntil` columns on the user table, populated via Spring Security's built-in `AuthenticationFailureBadCredentialsEvent`/`AuthenticationSuccessEvent` listeners. Fixed lockout window (e.g. 5 attempts → 15-minute lockout). No extra library.
- **Dedicated rate-limiting library / IP-based throttling** - more robust but adds complexity (and IP-based throttling is a poor fit behind shared/rotating IPs) not justified for this app's threat model.

## Decision

- **Server-side sessions** (Spring Session) for API authentication between the Angular SPA and Spring Boot backend.
- **Argon2id** for password hashing, via Spring Security's `DelegatingPasswordEncoder`.
- **DB-tracked failed-attempt counter** (columns + auth event listeners) for brute-force protection, fixed window, not permanent lockout.

## Consequences

- Session cookies need `SameSite=Lax; Secure` and a shared parent domain between frontend and backend (e.g. `app.toptrader.com` / `api.toptrader.com`) for clean, browser-fragility-free cookie sharing. **This means the upcoming AWS deployment-shape spike should plan for a custom domain with frontend/backend on subdomains of it** — using unrelated default AWS domains for each would force `SameSite=None`, which is workable but increasingly unreliable under Safari ITP / Firefox strict tracking protection. This is now a carried-forward input to that spike, not a re-litigated question.
- The default in-memory `HttpSession` store is fine as long as the AWS hosting shape stays single-instance (or uses sticky sessions on a load balancer). If that spike lands on multiple instances without sticky sessions, session storage will need to move to something shared (e.g. Spring Session backed by the RDS Postgres instance or Redis) — flagged for that spike, not solved here.
- CORS must be configured with `allowCredentials=true` and an explicit (non-wildcard) allowed origin; Angular `HttpClient` calls need `withCredentials: true`.
- Argon2id is memory-hard by design — hash timing should be sanity-checked once the AWS instance size is chosen, to avoid unexpectedly slow logins on an undersized instance.
- No token refresh/rotation logic needed (a session-based simplification vs. JWT), reducing MVP auth surface area.
