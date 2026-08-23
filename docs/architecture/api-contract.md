# API Contract

> Overview of the REST API design. Full formal spec is [openapi.yaml](./openapi.yaml) (OpenAPI 3.0.3) — paste it into [editor.swagger.io](https://editor.swagger.io) or any OpenAPI viewer for the interactive version. Companion to [system-architecture.md](./system-architecture.md) and [data-model.md](./data-model.md).

## Conventions

- **Base path**: `https://api.<domain>` (see ADR 0005 for the `api.` subdomain), unversioned `/api/...` — no `/v1` prefix. A versioning scheme can be introduced later if/when a breaking change is actually needed; not designed for upfront on a single-client MVP.
- **Auth**: server-side session cookie (ADR 0004), `SESSION`, `HttpOnly; Secure; SameSite=Lax`. Set on both register and login (registration auto-authenticates — no separate login step required right after signup, per US-3's "begin trading immediately"). Every endpoint except `/auth/register` and `/auth/login` requires an active session.
- **Errors**: RFC 7807 `application/problem+json` (Spring Boot's built-in `ProblemDetail`) for every error response — validation failures, 401s, 404s, and uncaught exceptions alike, so the frontend has exactly one error shape to handle.
- **Trade pricing**: `POST /trades/buy` and `/trades/sell` take only `{ticker, quantity}` — never a client-supplied price. The server fetches the current Finnhub quote at the moment the (already-confirmed) request is processed and executes at that price. This is what satisfies the acceptance criteria that the execution price must be "the quote price shown at the confirmation step, not silently re-fetched at a different price after the user confirms": there is exactly one price fetch, tied to the confirm action itself.
- **Pagination**: none for MVP (`/trades/transactions` returns the full list). Expected trade volume on a demo account is small enough that this isn't a real constraint yet; add `limit`/`offset` later if it becomes one.

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
| `GET /trades/transactions` | US-8 | Yes |
| `GET /users/search` | Friends (ADR 0049) | Yes |
| `POST /friends/requests` | Friends (ADR 0049) | Yes |
| `DELETE /friends/requests/{friendshipId}` | Friends (ADR 0049) | Yes |
| `POST /friends/requests/{friendshipId}/accept` | Friends (ADR 0049) | Yes |
| `POST /friends/requests/{friendshipId}/decline` | Friends (ADR 0049) | Yes |
| `DELETE /friends/{userId}` | Friends (ADR 0049) | Yes |
| `GET /friends/requests/incoming` | Friends (ADR 0049) | Yes |
| `GET /friends/requests/outgoing` | Friends (ADR 0049) | Yes |
| `GET /friends` | Friends (ADR 0049) | Yes |

Friends predates any formal user story (see `friends.md`'s "No new formal User Story" decision), so it's referenced by ADR instead of a US-code. `POST /friends/requests` returns `201` for a newly-created `PENDING` row, or `200` when it instead resolves a crossed request straight to `ACCEPTED` (ADR 0049) — no new row is created in that case. `DELETE /friends/requests/{friendshipId}` cancels the caller's own pending outgoing request; `DELETE /friends/{userId}` removes an existing friendship, addressed by the other user's id rather than the friendship row's id (either party may call it). All friend-mutating endpoints (send/cancel/accept/decline/remove) reject the shared demo account with `403`, mirroring ADR 0045/0047's read-only demo guard; `GET /users/search` additionally excludes the demo account from its results entirely, since a request sent to it could never be resolved. `POST /friends/requests` and `GET /users/search` are also rate-limited (20/hour and 20/minute respectively, per ADR 0034) — see `security-architecture.md`'s rate-limiting table.

US-7 ended up not needing the unified `/portfolio` endpoint originally planned here. Holdings (with per-position market value/unrealized P&L already computed server-side) come from `GET /trades/holdings`; cash balance is already available from the existing `GET /auth/session` response (`UserSummary.cashBalance`) — the frontend combines the two client-side on the dashboard rather than round-tripping to a combined endpoint. `GET /trades/holdings/{ticker}` (added for US-6) covers the single-ticker lookup used to conditionally show the Sell form. US-9 (overall profit/loss) hasn't been built yet — revisit then whether it needs its own endpoint or can extend one of these.

US-8's endpoint ended up nested as `GET /trades/transactions` rather than the standalone `/transactions` originally sketched here, to stay consistent with `TradeController` owning everything trade-related (buy, sell, holdings). It lives on a dedicated `/transactions` frontend route rather than the dashboard, unlike holdings.

## Not covered here

- Request/response examples beyond what's in `openapi.yaml`'s schemas.
- Rate limiting (post-MVP, per `user-stories.md`).
- Any endpoint versioning strategy (not needed until a breaking change is actually on the table).
