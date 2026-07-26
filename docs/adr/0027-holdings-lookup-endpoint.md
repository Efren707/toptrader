# 0027 - Holdings lookup endpoint and Optional-return convention for query methods

- Status: Accepted
- Date: 2026-07-25

## Context

US-6 (Sell shares) needed the stock details page to show a Sell form only when the logged-in user actually holds shares of that ticker. No existing endpoint could answer "does this user hold ticker X" - `HoldingRepository`/`HoldingResponse` existed only as internals of `TradeService.buyStock`/`sellStock`, and the only holding data ever reaching the client was embedded in a `TradeResult` returned by a completed trade.

## Options considered

- **`GET /trades/holdings/{ticker}`** (chosen) - a single-ticker lookup, scoped exactly to what the stock details page needs.
- **`GET /trades/holdings` (full list)** - would also serve Milestone #11's portfolio view (US-7), but that page doesn't exist yet and its shape isn't decided; building the list endpoint now would be speculative. Deferred until US-7 is actually being implemented.
- **Embed holding state in the quote response** - rejected: conflates market data (`QuoteService`, ticker-scoped, no auth) with per-user portfolio state, which would break the existing quote endpoint's caching/shape for every caller.

A second decision followed from the first: how should `TradeService` signal "no holding exists" back to the endpoint, given that's a normal outcome (most tickers a user looks up, they don't hold), not an error.

- **Return `null`** - matches how `buyStock`/`sellStock` already unwrap `HoldingRepository`'s `Optional<Holding>` internally via `.orElse(null)`, but a nullable return type doesn't self-document at the call site.
- **Return `Optional<HoldingResponse>`** (chosen) - makes "may be absent" explicit in the signature, appropriate for a pure query method. Scoped deliberately to query methods only: `buyStock`/`sellStock` were *not* changed to return `Optional`, since neither has a legitimate empty-but-not-an-error outcome - they always either return a populated `TradeResult` or throw `ResponseStatusException` (bad quantity, insufficient cash, no such holding to sell, etc.). Wrapping an always-present result in `Optional` would just be ceremony.

## Decision

Added `TradeService.getHolding(long userId, String ticker) -> Optional<HoldingResponse>` and `GET /trades/holdings/{ticker}` in `TradeController`, mapping `Optional` present/empty to HTTP 200/404 via `.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build())`.

`getHolding` looks up the user via a plain `findById`, not `findByIdForUpdate` - unlike `buyStock`/`sellStock`, it never mutates cash or holdings, so it doesn't need the pessimistic row lock those methods take to serialize concurrent trades against the same user.

The frontend (`TradeService.getHoldings`, `StockDetails.fetchHoldingData`) treats a 404 from this endpoint as "no holding, don't show the sell form" - not an application error, and distinct from the `notFound` flag used for "ticker doesn't exist" (driven solely by the quote fetch).

Going forward, this `Optional`-return convention applies to *query*-style `TradeService` methods (this one, and future reads like a portfolio list for US-7) where absence is a normal result. Command-style methods (`buyStock`, `sellStock`, and future ones like them) keep throwing on failure rather than returning `Optional`.

## Consequences

- Establishes a clear split in `TradeService`: queries return `Optional<T>` and never throw for "not found"; commands return a concrete result type and throw `ResponseStatusException` for every failure mode, including "not found." Future methods should follow whichever shape matches their own nature rather than copying whichever sibling was written most recently.
- The single-ticker endpoint is intentionally not the final shape for portfolio data - US-7 will likely need a list endpoint, at which point this one may end up superseded or kept alongside it for this narrower use case.
