# 0022 - CSRF bootstrap gap: exempt /auth/register and /auth/login

- Status: Accepted
- Date: 2026-07-17

## Context

ADR 0007 decided to keep Spring Security's default CSRF protection enabled (cookie + header pattern), and this was implemented while building US-1's security config. Testing that implementation surfaced a gap ADR 0007 didn't address: Spring Security's default `CsrfTokenRequestHandler` defers reading the CSRF token (a BREACH-attack mitigation added in Spring Security 5.8+), which means the `XSRF-TOKEN` cookie is never actually written until something server-side reads the token — it is not set automatically on a plain `GET`. A brand-new browser session therefore has no way to obtain a CSRF token before its first request, permanently blocking unauthenticated `POST /auth/register` and `POST /auth/login`, since there is no prior authenticated page load that would trigger a token read.

## Options considered

- **Exempt `/auth/register` and `/auth/login` from CSRF protection** - both are unauthenticated endpoints; there is no existing session for a forged cross-site request to ride on. The classic CSRF threat model (attacker leverages a victim's *existing authenticated session*) doesn't apply. The residual risk is "login CSRF" (tricking a victim into unknowingly submitting the attacker's credentials), a lower-severity, largely cosmetic issue for a solo demo trading-sim app with no sensitive victim-side state to manipulate pre-auth.
- **Add a dedicated CSRF bootstrap endpoint** (e.g. `GET /auth/csrf`) that forces the token to be read/cookie set, called once by the frontend before showing the register/login form - keeps CSRF protection universal, but adds a new endpoint and a required frontend bootstrap step for every fresh session, for protection against a threat (pre-auth CSRF) that barely applies here.
- **Switch to eager CSRF cookie issuance on every response** - removes the bootstrap problem globally with no extra endpoint, but gives up Spring Security's BREACH-attack mitigation *everywhere*, not just on the two exempted endpoints. Trades away a real protection to fix a gap that only actually matters for two pre-auth endpoints.

## Decision

**Exempt `/auth/register` and `/auth/login` from CSRF protection**, via `csrf.ignoringRequestMatchers("/auth/register", "/auth/login")`. Every other state-changing endpoint (trades, logout, etc.) remains CSRF-protected under ADR 0007's original cookie + header pattern, which continues to apply once a session exists.

## Consequences

- No new endpoint or required frontend bootstrap step; the Angular app's first POST (register or login) can succeed without a prior GET.
- The CSRF cookie/token only becomes relevant *after* authentication, which is exactly when Angular's `HttpClientXsrfModule` needs it for subsequent state-changing calls (trades, logout) — those still get the full protection ADR 0007 specified.
- Residual login-CSRF exposure on `/auth/login` is accepted as out of scope for this project's threat model (no sensitive pre-auth state to manipulate, no ADR-worthy mitigation identified beyond what's already here).
- This refines, not reverses, ADR 0007's CSRF decision — CSRF stays enabled by default; this carves out only the two endpoints that cannot have a valid token to present.
