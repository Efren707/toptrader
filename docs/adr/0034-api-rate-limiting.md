# 0034 - General API rate limiting: bucket4j in-memory, mixed IP/user keying, tiered thresholds

- Status: Accepted
- Date: 2026-07-30

## Context

ADR 0004/0025 cover brute-force protection on login specifically (DB-tracked lockout, inline in `LoginService`), but ADR 0007 and `docs/requirements/user-stories.md` explicitly left general API rate-limiting out of MVP scope. The pre-production security review (see `docs/ROADMAP.md`) requires it before going live: `POST /auth/register`, `GET /quotes/{ticker}`, `POST /trades/buy`, and `POST /trades/sell` currently have no throttle at all, unlike login.

The deployment shape matters here: ADR 0005/0014 fix this at a **single EC2 instance**, no auto-scaling, no Redis/ElastiCache anywhere in the architecture. That rules distributed rate-limiting stores in or out before cost even enters the discussion — an in-process limiter is not a compromise here, it's the correct fit for the actual deployment.

## Options considered

### Library / mechanism

- **Hand-rolled counters** (`ConcurrentHashMap<String, ...>` keyed by client, fixed or sliding window logic written by hand) - zero new dependencies, maximum learning value, but the implementer owns thread-safety, window-reset correctness, and — critically — eviction of stale entries. A raw map that only ever grows as new IPs/users show up is a slow memory leak.
- **bucket4j (local/in-memory mode)** - a well-tested token-bucket implementation; no Redis or distributed backend required, it runs entirely in-process. Paired with its Caffeine-backed local proxy manager (`bucket4j-caffeine`), buckets live in a bounded, TTL-expiring cache instead of an unbounded map, solving the eviction problem for free.
- **AWS WAF rate-based rule** (CloudFront-level) - offloads enforcement to infrastructure, protects EC2 from ever seeing the excess traffic. Rejected: recurring monthly cost (WAF Web ACL + per-rule + per-request charges) not accounted for in ADR 0014's $15/$25 budget thresholds, not testable locally, and not needed given the app-level option is free and sufficient at this traffic scale.

**Decision: bucket4j-core + bucket4j-caffeine.** Free, in-process (matches the single-instance deployment), and avoids the memory-leak footgun of a hand-rolled map without pulling in a distributed store the architecture doesn't otherwise need.

### Key extraction (what identifies "a client")

- **Per-IP for everything** - simplest, one code path, works pre-authentication. But a single IP (shared office/school network, or CloudFront obscuring the real client without correct `X-Forwarded-For` handling) can over-throttle multiple legitimate users, and a single user hopping IPs evades it entirely.
- **Per-authenticated-user for everything** - more correct for endpoints that already require a session, but `POST /auth/register` has no user yet - there's nothing to key on before the account exists.
- **Mixed: per-IP where there's no session yet, per-user where there is** - matches what's actually available at each endpoint. Rejected complexity concern (two key-extraction code paths instead of one) is minor relative to the correctness gain.

**Decision: mixed.** `/auth/register` keys on client IP (read from `X-Forwarded-For`'s first hop, since CloudFront fronts EC2 per ADR 0005/0014 - keying on CloudFront's own edge IP would make the limit meaningless). `/quotes/{ticker}`, `/trades/buy`, `/trades/sell` key on the authenticated user's ID via the same `UserPrincipal` already resolved by Spring Security for these endpoints.

### Thresholds

Considered a single uniform limit across all endpoints (simpler to reason about) versus tiering by endpoint risk/usage profile (register is a rare one-time action prone to bot signup loops; quote lookups are frequent, low-risk reads; trades are frequent but state-mutating and the highest-value target for scripted abuse). Tiering wins - a single number would either be too loose for register or too tight for legitimate quote-browsing.

**Decision:**

| Endpoint(s) | Key | Limit |
|---|---|---|
| `POST /auth/register` | Client IP | 5 requests / hour |
| `GET /quotes/{ticker}` | User ID | 20 requests / minute |
| `POST /trades/buy`, `POST /trades/sell` (shared bucket) | User ID | 10 requests / minute |

Buy and sell share one bucket (both are "mutating trade actions") rather than getting independent limits.

### Response shape

**Decision:** `429 Too Many Requests`, RFC 7807 `application/problem+json` body via Spring's `ProblemDetail` (consistent with every other error response per ADR 0012 - no bespoke rate-limit error format), plus a `Retry-After` header set to the number of seconds until the bucket next refills.

## Decision (summary)

- `bucket4j-core` + `bucket4j-caffeine` as new backend dependencies (both free, no Redis, no distributed store).
- One `RateLimitFilter extends OncePerRequestFilter`, wired into `SecurityConfig`'s filter chain the same way `CsrfCookieFilter` already is (`.addFilterBefore`/`.addFilterAfter` relative to a named Spring Security filter), with a small per-path configuration selecting the bucket group and key-extraction strategy per request.
- Key extraction: client IP (`X-Forwarded-For` first hop) for `/auth/register`; authenticated user ID for `/quotes/{ticker}`, `/trades/buy`, `/trades/sell`.
- Thresholds per the table above.
- On limit exceeded: `429` + RFC 7807 `ProblemDetail` + `Retry-After` header.

This closes the "general API rate limiting" gap ADR 0007 explicitly deferred, and removes the corresponding "out of scope for MVP" line from `docs/architecture/security-architecture.md`.

## Consequences

- Two new runtime dependencies (`bucket4j-core`, `bucket4j-caffeine`) - the first rate-limiting-specific libraries in the project; Dependabot (ADR 0007) will now also track these for updates.
- Because everything is in-process, this protection is per-instance. If the deployment shape ever moves off a single EC2 instance (ADR 0005 flags EC2→ECS/Fargate as a possible future non-one-way-door move), these buckets would need to move to a shared/distributed backend (e.g. Redis) to stay correct across instances - not a concern today, but a specific thing to revisit if that migration ever happens.
- `X-Forwarded-For` trust is now security-relevant for the register endpoint's limit: it must be read as the header CloudFront actually sets (first hop = real client), not blindly trusted from arbitrary callers if the app is ever reachable by a path that bypasses CloudFront. ADR 0014 already restricts EC2's app port to CloudFront's origin-facing prefix list, which is what makes trusting this header safe here.
- The per-user buckets for quotes/trades mean a user's own rapid-fire legitimate usage (e.g. quickly checking several tickers) is bounded by the same limit as abuse would be - thresholds were picked generously (20/min, 10/min) specifically to stay out of the way of real usage while still stopping scripted floods.
- Login retains its own separate, stricter, DB-persisted lockout (ADR 0004/0025) - this ADR does not change or replace that mechanism, it only adds coverage for the endpoints that had none.
