# 0012 - API contract: server-fetched trade pricing, auto-login, combined portfolio endpoint, RFC 7807 errors

- Status: Accepted
- Date: 2026-07-16

## Context

Phase 3 needed a REST API contract covering all nine MVP user stories, built on the data model (ADR 0010) and session auth (ADR 0004). The full endpoint list and schemas are in `docs/architecture/api-contract.md` and `docs/architecture/openapi.yaml`. Four sub-decisions in that design go beyond just listing endpoints and warrant recording here: how trade execution pricing is authorized, whether registration auto-authenticates, whether portfolio and P&L are one endpoint or two, and the error response format.

## Options considered

### Trade execution pricing (`POST /trades/buy`, `/trades/sell`)

- **Server always fetches fresh at execution** - client sends only `{ticker, quantity}`; the server fetches the current Finnhub quote at the moment the confirmed request is processed and executes at that price. No client-supplied price to validate or trust.
- **Client sends the quoted price, server validates against tolerance** - client includes the price shown at confirm time; server re-fetches and checks it's within some tolerance before executing. Adds a tolerance policy to design and doesn't add real protection, since the server-fetched price is authoritative either way — the client-sent value would only ever be used as a check, never as the actual execution price.

### Registration → session

- **Auto-login on register** - `POST /auth/register` sets the session cookie immediately, matching US-3's "begin trading immediately" framing and removing a redundant login step right after signup.
- **Separate login required** - registration only creates the account; frontend redirects to a login form. More separation of concerns, more friction for no requirement that asked for it.

### Portfolio + P&L

- **Combined `GET /portfolio`** - cash, holdings, total value, and P&L in one response. They're computed from the same data server-side and displayed together in the UI (US-7, US-9).
- **Separate `/portfolio` and `/portfolio/pnl`** - cleaner single-responsibility endpoints, but two round-trips for data that's already computed together.

### Error format

- **RFC 7807 `application/problem+json`** - Spring Boot's built-in `ProblemDetail` type. Zero extra library, and normalizes framework-level errors (e.g. malformed JSON, 404 routing) into the same shape as application-level errors for free.
- **Custom `{error, message}` JSON shape** - marginally simpler to read, but requires a hand-written exception handler to force every error path (including framework-level ones) into the same shape.

## Decision

- **Trade pricing is always server-fetched** at the moment of the confirmed buy/sell request; the client never sends a price.
- **Registration auto-authenticates** — the session cookie is set on `POST /auth/register`, same as on login.
- **Portfolio and P&L are combined** into a single `GET /portfolio` response.
- **All errors use RFC 7807 `application/problem+json`** via Spring Boot's `ProblemDetail`.

Full endpoint list, request/response schemas, and conventions live in `docs/architecture/api-contract.md` and `docs/architecture/openapi.yaml`, not duplicated here.

## Consequences

- Because pricing is always server-fetched, the UI's "confirm" step is purely a client-side gate before calling `/trades/buy`/`sell` — there's no server-side "lock in this price" concept to build, keeping the trade endpoints simple (single request/response, no intermediate quote-lock resource).
- A future feature that needs a longer-lived price lock (e.g. limit orders) would require revisiting this design — explicitly out of MVP scope per `user-stories.md`, so not a concern now.
- Auto-login on register means the registration endpoint carries the same security considerations as login (session fixation, cookie flags) — no additional design needed since it reuses the same session-creation code path as `/auth/login`.
- A single `/portfolio` endpoint means any future consumer that only wants P&L (unlikely for MVP) still pays for the full holdings computation — an acceptable trade-off at this scale.
- Using Spring's built-in `ProblemDetail` means controller advice/exception handlers should map domain exceptions (insufficient cash, unknown ticker, etc.) to appropriate HTTP statuses with `ProblemDetail` bodies — a concrete implementation detail for whenever the backend's global exception handling is built.
