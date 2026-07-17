# Non-Functional Requirements

> Status: Accepted — 2026-07-16
> Scope: applies to MVP and grows as post-MVP features land. Some items (exact hosting shape, rate-limiting specifics) will be sharpened once the Phase 2 research spikes are decided — flagged below where that's the case.

## Security

- All traffic is served over HTTPS in the deployed environment — no plaintext HTTP endpoints.
- Passwords are never stored in plaintext; hashed with a modern algorithm (BCrypt or Argon2 — final choice is part of the Phase 2 auth research spike).
- No secrets (API keys, DB credentials, JWT signing keys) are committed to the repo — managed via environment variables locally and a secrets manager in AWS (mechanism decided in Phase 2).
- All user input is validated/sanitized server-side (Spring) regardless of client-side validation, to guard against injection and malformed data.
- Database access uses parameterized queries/ORM (Spring Data JPA) exclusively — no string-concatenated SQL.
- Basic brute-force protection on login (e.g. attempt throttling/lockout) is in MVP scope as a security baseline — distinct from the general-purpose API rate-limiting feature, which remains post-MVP.
- Dependencies are kept current and scanned for known vulnerabilities as part of CI (tooling decided alongside the Phase 4 CI/CD design).
- A user can only ever view/modify their own portfolio and transaction data (authorization enforced server-side on every request, not just hidden in the UI).

## Financial Data Integrity

- All monetary and share-quantity calculations use exact decimal types (e.g. Java `BigDecimal`, Postgres `numeric`) — floating-point types are not used for cash, prices, or holdings, to avoid rounding errors.
- Every buy/sell is recorded as an immutable transaction record; portfolio/cash balances are derived from (or kept consistent with) that transaction history, not just a mutable running total.

## Performance

- This is a low-traffic personal/portfolio project, not a high-throughput system — there is no target requests/sec or concurrent-user SLA.
- Typical user-facing actions (portfolio view, buy/sell submission, quote lookup) should feel responsive in normal use (informal target: well under a couple of seconds under normal load), acknowledging that external market-data API latency is outside our control.

## Availability

- No formal uptime SLA. Cold starts, brief downtime during deploys, or scale-to-zero behavior (if the chosen AWS hosting shape supports it, per the Phase 2 hosting spike) are acceptable trade-offs for cost control.
- Deploys should not require manual downtime coordination — the CI/CD pipeline (Phase 4) should support redeploying without data loss.

## Accessibility

- Aim for baseline accessibility on the Angular frontend: semantic HTML, keyboard-navigable forms/controls, sufficient color contrast, and alt text for meaningful images/icons.
- Full WCAG AA conformance is an aspirational target, not a gated requirement, for MVP.

## Browser/Device Support

- Modern evergreen browsers only (Chrome, Firefox, Edge, Safari — current and previous major version).
- Responsive layout that works on both desktop and mobile web viewports; no native mobile app (per Out of Scope in user-stories.md).

## Maintainability

- Code follows conventional Spring Boot layering (controller/service/repository) and Angular module/component conventions, documented in the architecture docs (Phase 3).
- Meaningful automated test coverage is expected for core trading logic (buy/sell, balance calculations) given this is a priority learning area — exact coverage targets/tooling to be defined in Phase 3/4.
