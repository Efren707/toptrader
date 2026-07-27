# API Contract

> Overview of the REST API design. Full formal spec is [openapi.yaml](./openapi.yaml) (OpenAPI 3.0.3) — paste it into [editor.swagger.io](https://editor.swagger.io) or any OpenAPI viewer for the interactive version. Companion to [system-architecture.md](./system-architecture.md) and [data-model.md](./data-model.md).

## Conventions

- **Base path**: `https://api.<domain>` (see ADR 0005 for the `api.` subdomain), unversioned `/api/...` — no `/v1` prefix. A versioning scheme can be introduced later if/when a breaking change is actually needed; not designed for upfront on a single-client MVP.
- **Auth**: server-side session cookie (ADR 0004), `SESSION`, `HttpOnly; Secure; SameSite=Lax`. Set on both register and login (registration auto-authenticates — no separate login step required right after signup, per US-3's "begin trading immediately"). Every endpoint except `/auth/register` and `/auth/login` requires an active session.
- **Errors**: RFC 7807 `application/problem+json` (Spring Boot's built-in `ProblemDetail`) for every error response — validation failures, 401s, 404s, and uncaught exceptions alike, so the frontend has exactly one error shape to handle.
- **Trade pricing**: `POST /trades/buy` and `/trades/sell` take only `{ticker, quantity}` — never a client-supplied price. The server fetches the current Finnhub quote at the moment the (already-confirmed) request is processed and executes at that price. This is what satisfies the acceptance criteria that the execution price must be "the quote price shown at the confirmation step, not silently re-fetched at a different price after the user confirms": there is exactly one price fetch, tied to the confirm action itself.
- **Pagination**: none for MVP (`/transactions` returns the full list). Expected trade volume on a demo account is small enough that this isn't a real constraint yet; add `limit`/`offset` later if it becomes one.

## Endpoints

| Endpoint | Story | Auth required |
|---|---|---|
| `POST /auth/register` | US-1 | No |
| `POST /auth/login` | US-2 | No |
| `POST /auth/logout` | US-2 | Yes |
| `GET /auth/session` | US-2 | Yes |
| `GET /quotes/{ticker}` | US-4 | Yes |
| `POST /trades/buy` | US-5 | Yes |
| `POST /trades/sell` | US-6 | Yes |
| `GET /trades/holdings/{ticker}` | US-6 | Yes |
| `GET /trades/holdings` | US-7 | Yes |
| `GET /transactions` | US-8 | Yes |

US-7 ended up not needing the unified `/portfolio` endpoint originally planned here. Holdings (with per-position market value/unrealized P&L already computed server-side) come from `GET /trades/holdings`; cash balance is already available from the existing `GET /auth/session` response (`UserSummary.cashBalance`) — the frontend combines the two client-side on the dashboard rather than round-tripping to a combined endpoint. `GET /trades/holdings/{ticker}` (added for US-6) covers the single-ticker lookup used to conditionally show the Sell form. US-9 (overall profit/loss) hasn't been built yet — revisit then whether it needs its own endpoint or can extend one of these.

## Not covered here

- Request/response examples beyond what's in `openapi.yaml`'s schemas.
- Rate limiting (post-MVP, per `user-stories.md`).
- Any endpoint versioning strategy (not needed until a breaking change is actually on the table).
