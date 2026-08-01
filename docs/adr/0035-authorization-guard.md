# 0035 - Authorization guard: service-layer @PreAuthorize binding userId to principal

- Status: Accepted
- Date: 2026-07-31

## Context

The pre-production security review (see `docs/ROADMAP.md`) audited authorization across the app. Finding: every controller (`TradeController`, plus the equivalent pattern in `QuoteController`) already derives the resource owner from `@AuthenticationPrincipal` -> `principal.getUser().getId()`, never from a client-supplied ID. No IDOR path exists today. But this correctness holds only by convention - nothing in the framework would stop a future endpoint from taking a client-supplied `userId` (or forgetting the `@AuthenticationPrincipal` derivation entirely) and silently skipping the check. `SecurityConfig` enforces authentication (`anyRequest().authenticated()`) but has no method-level authorization (`@EnableMethodSecurity` is not present anywhere in the codebase).

This app has 3 controllers and no endpoint that accepts a resource ID other than the authenticated user's own ID (tickers are public data, not owned resources). That scope matters for picking a mechanism - see Options below.

## Options considered

- **Default-deny method security** - a custom `AuthorizationManager`/interceptor requiring every controller/service method to carry an explicit authorization annotation, failing closed if one is missing. Strongest guarantee (catches *any* future omission, for any endpoint shape), but is infrastructure sized for a multi-contributor codebase where someone else's PR might silently miss a check. Rejected as disproportionate to a single-developer, 3-controller app - and it still wouldn't solve per-entity ownership (see Consequences) without additional per-entity wiring anyway.
- **Service-layer `@PreAuthorize` binding userId to principal** - enable Spring's `@EnableMethodSecurity`, add `@PreAuthorize` to `TradeService`'s public methods asserting the passed `userId` equals the authenticated principal's own ID. Lightweight (one config flag, a handful of annotations, zero new dependencies since `spring-security-config` already ships with `spring-boot-starter-security`). Matches the actual current risk shape: every existing/likely-near-term endpoint takes `userId` as its scoping parameter, so binding that parameter to the principal directly closes the gap the audit found.

**Decision: service-layer `@PreAuthorize` binding.** Proportionate to the app's current size and endpoint shape, and turns "forgot to derive userId from the principal" from a silent pass-through into a 403 at the service boundary - the specific regression this checklist item exists to prevent.

## Decision (summary)

- `@EnableMethodSecurity` added to security config (no new Maven dependency).
- `TradeService`'s public methods (`buyStock`, `sellStock`, `getHolding`, `getHoldings`, `getTransactions`) get `@PreAuthorize` binding their `userId` parameter to `authentication.principal.user.id`.
- Applies the same way to any future service method that takes a `userId` scoping parameter.

## Consequences

- Does not, by itself, guard a future endpoint that fetches by some *other* resource ID (e.g. a hypothetical `GET /trades/{tradeId}`) without a `userId` parameter to bind - that shape needs a per-entity ownership check (e.g. `@PreAuthorize("@ownership.isTradeOwner(#tradeId, principal)")`) written when such an endpoint is added. Noted here so it isn't assumed "solved" by this ADR; revisit if/when an endpoint like that appears.
- `@PreAuthorize` failures throw `AccessDeniedException`; needs a check that `GlobalExceptionHandler` maps it to a sensible response (403 + RFC 7807 body, consistent with every other error response per ADR 0012) rather than leaking a 500.
- Because every current controller already derives `userId` from the principal, these checks are currently unreachable in the "deny" branch through normal use - they're a regression guard, not a fix for a live bug. Tests for the deny path have to call the service layer directly with a mismatched userId/mocked `SecurityContext`, since no controller path can trigger it.
