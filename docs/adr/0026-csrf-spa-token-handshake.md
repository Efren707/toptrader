# 0026 - CSRF/SPA token handshake: cookie bootstrap, cross-origin header, plain token comparison

- Status: Accepted
- Date: 2026-07-24

## Context

ADR 0007 kept Spring Security's default CSRF protection enabled (cookie + header pattern). ADR 0022 later found that `CookieCsrfTokenRepository`'s deferred token resolution (a BREACH-attack mitigation) means the `XSRF-TOKEN` cookie is never actually written until something server-side reads it, and fixed the resulting bootstrap gap for the two unauthenticated endpoints (`/auth/register`, `/auth/login`) by exempting them from CSRF entirely.

Implementing US-5's buy flow exercised the first *authenticated* mutating endpoint (`POST /trades/buy`) and surfaced that CSRF was still completely broken for every such endpoint — every attempt failed with a 403, cookie and header both present and byte-identical. This turned out to be three separate, stacked gaps, not one bug:

1. **The deferred-token gap from ADR 0022 was never actually closed for the rest of the app** - it was worked around for register/login by exempting them, but nothing ever forced the token to resolve (and the cookie to be written) for any other route, so the `XSRF-TOKEN` cookie genuinely never existed post-login.
2. **Angular's built-in XSRF interceptor never fires for this app.** It hardcodes a skip for any request whose URL starts with `http://`/`https://`, on the assumption that an absolute URL means a possibly-untrusted different origin. `environment.apiUrl` (`http://localhost:8080` in dev; a separate subdomain in prod per ADR 0004) makes every API call in this app absolute, so the interceptor was a permanent no-op — it never attached `X-XSRF-TOKEN` to anything.
3. **Even with a correct, present header, validation still failed.** `SecurityConfig` never called `.spa()` or set an explicit `CsrfTokenRequestHandler`, so Spring Security fell back to its own default, `XorCsrfTokenRequestAttributeHandler` — built for BREACH protection on server-rendered forms, it expects the submitted token to be a random-pad-XORed, Base64url-encoded value. Angular (and any standard double-submit-cookie client) sends the *plain* raw cookie value. Decoding a plain value as if it were masked fails or produces garbage, so the comparison always failed, regardless of whether cookie and header actually matched. Confirmed via raw `curl`/`fetch` calls with byte-identical cookie/header values, and via an unrelated endpoint (`POST /logout`) failing the same way — ruling out anything trade-specific.

## Options considered

**Gap 1 (cookie never written):**
- **A filter that forces the deferred token to resolve on every request** (chosen) - `CsrfCookieFilter`, added after `BasicAuthenticationFilter` in the chain, calls `csrfToken.getToken()` purely for the side effect of making `CookieCsrfTokenRepository` write the cookie. Generalizes ADR 0022's fix from two exempted endpoints to every request.
- **Switch to eager, non-deferred cookie issuance app-wide** - considered and rejected by ADR 0022 already for the same reason: gives up BREACH mitigation everywhere to fix a gap that only needs a targeted fix.

**Gap 2 (header never attached, cross-origin):**
- **A custom interceptor that reads the cookie via `HttpXsrfTokenExtractor` and attaches `X-XSRF-TOKEN` regardless of URL shape** (chosen) - `xsrf.interceptor.ts`, registered alongside `credentialsInterceptor`.
- **Make frontend API calls relative, via a dev proxy** - would only fix local dev; ADR 0004 already commits to separate frontend/backend subdomains in production, which is still cross-origin from Angular's perspective, so this wouldn't generalize and would leave prod broken by the same mechanism.

**Gap 3 (XOR handler mismatch):**
- **`csrf.spa()`** (chosen) - Spring Security's built-in preset for exactly this scenario: sets both `CookieCsrfTokenRepository.withHttpOnlyFalse()` (already what we wanted) and `SpaCsrfTokenRequestHandler`, which compares the plain value when a header is present and only falls back to XOR-decoding for parameter-based (non-header) submissions. Replaces the manual `csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())` call with no loss of existing behavior.
- **Manually set `CsrfTokenRequestAttributeHandler` (plain, non-XOR, unconditionally)** - would also fix the mismatch, but drops BREACH masking for all submission paths, not just the header path `SpaCsrfTokenRequestHandler` optimizes for, and is less self-documenting than `.spa()` about *why* this app needs it.

## Decision

All three fixes were applied together, since each is necessary but none is sufficient on its own:

1. `CsrfCookieFilter` (new, `backend/.../config/`) forces deferred CSRF token resolution on every request.
2. `xsrfInterceptor` (new, `frontend/.../core/interceptors/`) manually attaches `X-XSRF-TOKEN` from the cookie, registered in `app.config.ts`'s interceptor chain.
3. `SecurityConfig`'s CSRF configuration switched from manually setting `csrfTokenRepository(...)` to `csrf.spa()`.

This refines, not reverses, ADR 0007/0022 - CSRF stays enabled by default for all authenticated mutating endpoints; this closes the gap in how the token actually gets issued, transmitted, and compared for a cross-origin Angular SPA.

## Consequences

- CSRF protection now functions end-to-end for every authenticated mutating endpoint (trades, logout, and any future one), not just the two exempted pre-auth endpoints. No per-endpoint wiring needed going forward - all three fixes are global (filter/interceptor/handler-level).
- Loses BREACH masking on the header submission path specifically (the path this SPA always uses). Accepted for the same reason ADR 0022 accepted its own residual risk: this is a pure JSON REST API with no server-rendered view ever reflecting the token into HTML, so the BREACH scenario (secret token compressed alongside attacker-influenced reflected content) doesn't apply here.
- The debugging path took three iterations to fully resolve because each fix, once applied, surfaced the *next* gap as a new-looking failure (same 403, different cause) rather than a variation of the same symptom - worth remembering if CSRF-adjacent issues resurface, so the whole chain gets checked rather than assuming a single point fix.
