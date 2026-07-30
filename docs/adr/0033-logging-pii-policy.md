# 0033 - Logging/PII policy: no full bodies, masked-secret DTOs, identifiers not object graphs

- Status: Accepted
- Date: 2026-07-30

## Context

A pre-production security review found no logging framework wired up anywhere in the backend (`src/main/java` has zero `Logger`/`Slf4j` usage). Nothing is at risk of leaking today — `RegisterRequest`/`LoginRequest`, the only two DTOs carrying a credential, already override `toString()` to mask the password — but there's also no *enforced* guard against a future accidental leak once real request logging gets added (e.g. as part of ADR 0008's CloudWatch pipeline, which is deployment-execution work not yet started). This ADR records the policy now, ahead of that work, rather than leaving it as an implicit assumption.

## Options considered

- **Write the policy now, implement later** - decide and record the rule while it's front of mind from this review, so whoever (likely the same author, later) wires up ADR 0008's logging has a binding reference instead of re-deriving judgment calls from scratch mid-implementation.
- **Defer entirely to ADR 0008's implementation** - fold the decision into that future work instead of writing it separately now. Rejected: ADR 0008 is about log *infrastructure* (CloudWatch shipping, retention), not log *content* policy — conflating the two would bury a security decision inside an infra ticket.

## Decision

- **Never log full request or response bodies.** Log specific fields/identifiers (user id, ticker, trade id) instead of an entire object graph.
- **Any DTO carrying a credential or secret must override `toString()` to mask it** before it can ever be logged incidentally (already true for `LoginRequest`/`RegisterRequest`; applies to any future DTO carrying a password, token, or similar).
- **Structured logging, whenever it's added** (ADR 0008's CloudWatch pipeline), logs identifiers and event names, not serialized entities — consistent with the two rules above.

## Consequences

- No code changes today - there is nothing to log yet, so nothing to fix. This ADR is a binding constraint on the *next* time logging is added to the codebase, not a retroactive audit finding.
- When ADR 0008's CloudWatch pipeline is implemented, that work should cite this ADR rather than re-deciding log-content policy from scratch.
- If a future DTO is added that carries a credential/secret and doesn't override `toString()`, that's a violation of this ADR, not just a style nit — worth catching in review.
