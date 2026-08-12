# Planning History Archive

> Full narrative detail for planning phases and completed build-order milestones — kept here so [ROADMAP.md](../../ROADMAP.md) stays a lean, current-state tracker. This file is pure history: once a phase/milestone is closed and archived here, it doesn't change. For *why* decisions were made, see `docs/adr/`; for requirements detail, see `docs/requirements/`.

## Milestone #8 — Auth & Account Foundation — ✅ Done

Groundwork completed ahead of US-1, per ADR 0020's build order:

- [x] Backend skeleton — Spring Boot 4.1.0, Java 21, Maven, `com.toptrader.backend` (Spring Boot 3.x is no longer offered by start.spring.io; 4.1.0 was the current stable GA at implementation time)
- [x] Frontend skeleton — Angular CLI 22, standalone components, routing
- [x] Backend tooling — Spotless (`google-java-format`) per ADR 0016
- [x] Frontend tooling — ESLint (`@angular-eslint/schematics`) + Prettier + `eslint-config-prettier`, Tailwind CSS v4
- [x] Frontend test runner — **Vitest**, not Jest as `frontend-architecture.md` originally specified; Angular CLI 22 now ships Vitest as its own built-in default (doc updated to match)
- [x] Local Postgres — Docker Compose, Postgres 17 (also the version future RDS will match), `application-local.yml.example` template per ADR 0009
- [x] CI — GitHub Actions PR quality gate (`dorny/paths-filter` → lint/test/build per stack), per ADR 0016; deploy-to-AWS stage intentionally deferred until EC2/RDS/S3/CloudFront/OIDC infra exists (ADR 0006)
- [x] Branch protection on `main` — `backend-ci`/`frontend-ci` required status checks, strict, admins included (matches ADR 0002's PR-based trunk workflow — direct pushes to `main` are no longer possible, including for this assistant)
- [x] US-1 (Register) backend — `POST /auth/register` per `openapi.yaml`: Flyway migration for `users`, `User`/`UserRepository`, `SecurityConfig` (Argon2id, session cookie, CORS, CSRF), `UserDetailsServiceImpl`/`UserPrincipal` (shared with future US-2 login), `RegistrationService` + `AuthController`, global RFC 7807 field-level validation errors, integration tests. Package-by-feature convention adopted (`user`, `auth`, `config`, `web`) — no Lombok (**ADR 0023**). **ADR 0022** written mid-implementation for a CSRF-bootstrap gap discovered by testing. Merged via PR #11.
- [x] Frontend visual design system — dark-only monochrome UI, IBM Plex Sans/Mono (self-hosted), shared `Button`/`Input`/`Card` components in `shared/ui/`, Tailwind v4 `@theme` tokens in `styles.css` — **ADR 0024**.
- [x] US-1 (Register) frontend — `Register` component (`features/auth/register/`) using Reactive Forms + the shared UI components; `AuthService`, `credentialsInterceptor`/`errorInterceptor`, environment-based `apiUrl`. Verified end-to-end in-browser against the real backend: successful registration + auto-login, duplicate-email 409 banner, and client + server field-level validation errors all render correctly. Merged via PR #12 (2026-07-18).
- [x] US-2 (Log in) — `POST /auth/login`/`GET /auth/session` per `openapi.yaml`: `LoginRequest`/`LoginService` (authenticates via `AuthenticationManager`, single generic "Invalid email or password" for both bad credentials and unknown emails — no user enumeration), inline brute-force lockout (5 failed attempts → 15-minute lock, reset on success) — **ADR 0025** refines ADR 0004's original auth-event-listener approach to keep it in one testable service method. `SecurityConfig` gained an explicit `HttpStatusEntryPoint(UNAUTHORIZED)`, fixing a gap where unauthenticated requests fell back to Spring Security's default 403 instead of the documented 401 (was silently breaking the frontend's session check on every page load). `AuthControllerLoginTest` covers valid login, wrong password, unknown email, lockout, and reset-on-success. Frontend: `Login` component (mirrors `Register`), `AuthService.login()`/`checkSession()`, `checkSession()` wired into a `provideAppInitializer` so a valid session survives a refresh, a minimal `Dashboard` placeholder route, and `authGuard`/`guestGuard` route guards (login/register → dashboard on success or if already authenticated; dashboard → login if not) — `Register` also updated to redirect to `/dashboard` instead of an inline welcome state, for consistency. Merged via PR #14.
- [x] US-3 (Receive starting virtual cash) — the $500 grant itself was already correct from US-1's `RegistrationService` (exactly once, at registration, never re-applied on login); the gap closed here was visibility. Added `cashBalance` to the `UserSummary` contract (`openapi.yaml`, backend record, frontend interface) and rendered it on `Dashboard`; added `jsonPath("$.cashBalance")` assertions to `AuthControllerRegisterTest`/`AuthControllerLoginTest`. Merged via PR #16.

## Milestone #9 — Market Data Integration — ✅ Done

- [x] US-4 (Look up a stock quote) backend — Finnhub client wiring (`MarketDataConfig`, `FinnhubTokenInterceptor`, `FinnhubClient`) per ADR 0003; `Quote` DTO, `QuoteService` (unknown-ticker detection + provider-failure mapping), `QuoteController` exposing `GET /quotes/{ticker}` per `openapi.yaml`; `QuoteControllerTest` covering the happy path, both not-found branches, both 502 branches, and the 401-unauthenticated case. Along the way, fixed a pre-existing break where `toptrader.finnhub.api-key` had no test value, causing every `@SpringBootTest` to fail to start its context. Merged via PR #18.
- [x] US-4 frontend — quote lookup UI added directly on the dashboard (no standalone route — nothing else in the MVP needed it elsewhere): a navbar search box (search icon left, clear icon right, Enter-to-submit only) with a results dropdown showing the matched ticker/company/price or a "No stocks found" state, dismissing on outside click. Fixed `QuoteService.getQuote()` normalizing tickers to lowercase instead of uppercase, which broke every real lookup against Finnhub. Merged via PR #19 (2026-07-23), closes #4.

## Milestone #10 — Trading Core — ✅ Done

- [x] US-5 (Buy shares) — backend trade execution (`TradeService.buyStock`, `POST /trades/buy`) plus a frontend `TradeForm` (quantity entry, explicit "Buy N TICKER @ $price — Confirm?" step per the acceptance criteria, success/cancel/error handling), reachable via a new `/stocks/:ticker` route from the dashboard's quote search. First non-stub `.spec.ts` in the repo (`trade-form.spec.ts`, 9 cases). Along the way, found and fixed a pre-existing gap where CSRF protection was silently non-functional for every authenticated mutating endpoint, not just trades — see [ADR 0026](../../adr/0026-csrf-spa-token-handshake.md) for the root-cause writeup. Merged via PR #21.
- [x] US-6 (Sell shares) — `TradeService.sellStock`/`POST /trades/sell`, same locked-row (`findByIdForUpdate`), live-quote, all-or-nothing `@Transactional` shape as `buyStock`: rejects a sell exceeding the held quantity, decreases (or removes, if fully liquidated) the `Holding`, credits cash. New `TradeService.getHolding`/`GET /trades/holdings/{ticker}` read endpoint establishes an `Optional`-return convention for query-style `TradeService` methods (commands like `buyStock`/`sellStock` keep throwing on every failure) — see [ADR 0027](../../adr/0027-holdings-lookup-endpoint.md). Frontend reused `TradeForm`/`TradeService` (already `side`-aware); `StockDetails` fetches the holding alongside the quote and only renders the Sell form when one exists. Along the way, caught and reverted a find-replace slip that had swapped `buyStock`/`sellStock`'s row-locking `findByIdForUpdate` for a plain `findById` — the 25-case test suite across `buyStock`/`sellStock`/`getHolding` surfaced it immediately. Merged via PR #23, closes #6.

## Milestone #11 — Portfolio & Reporting — ✅ Done

- [x] US-7 (View portfolio) — `TradeService.getHoldings`/`GET /trades/holdings` lists all of a user's current holdings, reusing a shared `toHoldingResponse` helper (also used by `buyStock`/`sellStock`/`getHolding`) for the market-value/unrealized-P&L calc. Frontend displays holdings on the **dashboard** rather than a separate portfolio page — a deviation from the original plan in ADR 0012/0013, recorded in [ADR 0028](../../adr/0028-portfolio-view-no-combined-endpoint.md). Dashboard gained a `computed()` total portfolio value (cash + holdings' market value) above the holdings table. Merged via PR #25.
- [x] US-8 (View transaction history) — `TradeService.getTransactions`/`GET /trades/transactions` (most recent first), reusing existing `Transaction`/`TransactionResponse` plumbing via a shared `toTransactionResponse` helper. Frontend: dedicated `/transactions` page plus a shared `Navbar` component (extracted from the dashboard) with an account-menu dropdown as the nav path to it — see [ADR 0029](../../adr/0029-transaction-history-page-and-shared-navbar.md) for why this landed as its own page instead of the dashboard's previously-planned reserved column, and why Logout was left out of the menu for now. Merged via PR #27.
- [x] US-9 (View profit/loss) — `Performance` page (`/performance`) showing overall P&L (current portfolio value minus the $500 starting balance) as both a dollar amount and a percentage, computed entirely client-side, no backend changes — see [ADR 0030](../../adr/0030-profit-loss-client-side-performance-page.md). Reachable via a new "Performance" item in the `Navbar` account menu, alongside "Transaction history". Merged via PR #29, closes #9.

## Milestone #12 — UI/UX Polish Pass — ✅ Done

- [x] US-10 (Auth nav/logout) — login/register cross-links instead of dead-end auth pages; shared `Navbar` wordmark links to `/dashboard`; backend `SecurityConfig.logout(...)` replaces Spring Security's default (redirecting `/logout`) with an explicit `logoutUrl("/auth/logout")`, `deleteCookies("SESSION")`, and a `logoutSuccessHandler` returning a bare `204`, matching the documented `openapi.yaml` contract (stays CSRF-protected per ADR 0026, since it's only called while authenticated); frontend `AuthService.logout()`/`Navbar.onLogoutClick()` clears state and redirects to `/login`, with protected routes redirecting to `/login` afterward via the existing `authGuard`. Merged via PR #41, closes #31.
- [x] US-11 (Consistent, evenly-spaced navbar) — shared `Layout` shell route (`shared/layout/`) so every authenticated page (`dashboard`, `stocks/:ticker`, `transactions`, `performance`) gets `<app-navbar/>` structurally instead of each page remembering to render it (fixed stock details, which had no navbar at all); fixed the navbar's own uneven spacing (`justify-content: space-between`); added real test coverage for `layout.spec.ts` and `navbar.spec.ts` (previously nonexistent/a placeholder). See [ADR 0031](../../adr/0031-authenticated-layout-shell-route.md) (revises ADR 0029). Merged via PR #43, closes #34.
- [x] US-12 (Search result row layout and click behavior) — dropped price from the navbar search result row (ticker left, company name right only); fixed the result button defaulting to `type="submit"` and re-triggering the search form's own submit handler (was flashing a spurious "Required" error on click); fixed the result dropdown staying open after a result was clicked; fixed the stock details page not refreshing when searching a new ticker while already on a `/stocks/:ticker` page (component instances are reused across same-route-template navigations, so `ngOnInit` now subscribes to `route.paramMap` instead of reading a one-time snapshot). Merged via PR #45, closes #35.
- [x] US-13 (Dashboard holdings as a list with day change) — threaded Finnhub's `dp` (percent change) through `FinnhubQuoteResponse` → `Quote` → `QuoteService` → `HoldingResponse` (previously silently discarded); dashboard holdings now render as a list (ticker + quantity left, current price + colored daily % change right) instead of a table, each row navigating to `/stocks/:ticker` on click. Merged via PR #47, closes #37.
- [x] US-14 (Stock details redesign) — restructured into a 2-column layout (~65/35, collapsing to one column under 60rem) with a position-stats block (equity, today's/total return $ and %, average cost basis, shares owned, portfolio diversity) shown when the user holds the stock; collapsed `TradeForm` from two side-by-side Buy/Sell instances into one with `side` as an internal signal switched by a text-tab toggle; digit-filtered quantity field with live-calculated cost; submitting expands into a full Review Order step (order-summary sentence using the actual order total, cash balance shown near the actions, Cancel/Confirm split 50/50). Merged via PR #49, closes #39.

## Milestone #13 — Pre-Deployment Hardening — ✅ Done

13 hardening items closed: CI secret scanning & dependency hygiene, prod config lockdown, session timeout/fixation, frontend 401/403 handling, logging/PII policy, CSP directives, general API rate limiting, ticker input validation, frontend error handling gaps, authorization guard, password-reset flow, email verification at signup, backend logging framework, and rollback strategy decided. Full detail: [pre-deployment-checklist.md](./pre-deployment-checklist.md).

## Milestone #14 — AWS Deployment Infrastructure — ✅ Done

10 sections provisioning the real AWS stack (ADR 0005/0006/0014/0016/0017): domain/account foundation, network/security groups, RDS, EC2, CloudFront/TLS, S3 frontend hosting, CI/CD deploy wiring, cutover/smoke test, CloudWatch log shipping + `StatusCheckFailed` alarm (#102), and automated last-known-good jar rollback (#105, ADR 0039). TopTrader has been live at `app.toptrader.dev` since section 8's cutover. Full detail: [aws-infrastructure-implementation.md](./aws-infrastructure-implementation.md).

## Phase 0 — Repo & Working Agreement Setup — ✅ Done

- [x] GitHub repo (public, `Efren707/toptrader`), README, `.gitignore`, MIT LICENSE
- [x] `/docs` folder structure
- [x] GitHub Projects board ("TopTrader Roadmap") + issue templates + PR template
- [x] Branching strategy: trunk-based (ADR 0002)
- [x] ADR process adopted (ADR 0000)

## Phase 1 — Requirements & Vision Documentation — ✅ Done

- [x] `docs/requirements/vision.md` — problem statement, target user, MVP vs. full-vision definition of done
- [x] `docs/requirements/user-stories.md` — 9 MVP stories (US-1..US-9), out-of-scope list, post-MVP backlog ($500 starting cash, whole shares only for MVP; deposit cash + fractional shares tracked post-MVP)
- [x] `docs/requirements/nfr.md` — security, financial data integrity, performance, availability, accessibility, browser support, maintainability
- [x] `docs/requirements/acceptance-criteria.md` — testable criteria per story (8-char min password, explicit trade confirmation step)

## Phase 2 — Research Spikes — ✅ Done

Each spike produces a recommendation + trade-offs for review, then an ADR.

- [x] Market data API research (real-time vs. delayed, rate limits, ToS, and market-hours/stale-price behavior) — **Finnhub selected, see ADR 0003.** Finnhub has no market-status field; market-open/closed is computed from a hardcoded NYSE-hours + static holiday list — see ADR 0021 (resolved during the Phase 6 go/no-go review).
- [x] Auth strategy (session vs. JWT vs. OAuth2, password hashing) — **server-side sessions + Argon2id + DB-tracked lockout, see ADR 0004.** Carries forward a requirement into the AWS spike: plan for a custom domain (frontend/backend on subdomains of it) for clean cross-origin session cookies.
- [x] AWS deployment shape (EC2 vs. ECS/Fargate vs. Beanstalk vs. App Runner; RDS; frontend hosting; budget alerts / free-tier guardrails; custom domain for session cookie sharing) — **EC2 t4g.micro + CloudFront (backend), RDS db.t4g.micro, S3+CloudFront (frontend), Route 53 domain, see ADR 0005.** App Runner ruled out (deprecated April 2026). Chosen over ECS Fargate to avoid its mandatory ~$16+/mo ALB cost.
- [x] CI/CD pipeline design (GitHub Actions stages, deploy triggers to the EC2/S3 targets from ADR 0005) — **monorepo, lint→test→build→deploy, SSH/SCP to EC2, OIDC to S3/CloudFront, GitHub secrets + SSM Parameter Store, see ADR 0006.** $0 added AWS cost.
- [x] Security baseline (OWASP Top 10 applied, secrets management, CORS) — **access control pattern + IDOR tests, CORS/CSRF config, Dependabot, Actuator locked to /health only, see ADR 0007.**
- [x] Observability basics (logging, CloudWatch, health checks) — **local logs + CloudWatch agent, systemd health-check timer + auto-restart, default free EC2 metrics, CloudWatch Alarm + SNS email, see ADR 0008.** $0 added AWS cost.
- [x] Local dev environment tooling (Docker Compose for Postgres, local env var/secrets setup) — **Docker Compose for Postgres only (no app Dockerfile), gitignored application-local.yml + committed .example template, see ADR 0009.**

## Phase 3 — Technical & Architecture Documentation — ✅ Done

- [x] System architecture diagram — `docs/architecture/system-architecture.md`
- [x] Data model / ERD — `docs/architecture/data-model.md`
- [x] Schema migration tooling — **Flyway**, see ADR 0011
- [x] API design/contract (OpenAPI) — `docs/architecture/openapi.yaml`, `api-contract.md`, ADR 0012
- [x] Security architecture doc — `docs/architecture/security-architecture.md`
- [x] Frontend architecture (Angular structure, state management) — `docs/architecture/frontend-architecture.md`, ADR 0013
- [x] Deployment/infra architecture doc — `docs/architecture/deployment-architecture.md`, ADR 0014

## Phase 4 — CI/CD & Environment Strategy — ✅ Done

- [x] Environments defined (local/prod) — `docs/architecture/environments.md`, ADR 0015
- [x] Pipeline stages (lint → test → build → deploy) — ADR 0016
- [x] Merge/deploy test gates — ADR 0017
- [x] Secrets/config management per environment — ADR 0018, `docs/architecture/environments.md`
- [x] Post-MVP feature release strategy — ADR 0019

## Phase 5 — User-Facing Documentation Planning — ✅ Done

- [x] End-user guide outline — `docs/guides/end-user-guide-outline.md`
- [x] Developer setup guide outline — `docs/guides/developer-setup-guide-outline.md`
- [x] Contribution/workflow guide outline — `docs/guides/contribution-workflow-guide-outline.md`
- [x] README structure finalized (as outline; `README.md` itself unchanged until deploy) — `docs/guides/readme-structure-outline.md`
- [x] Demo account & showcase readiness outline — mechanism and content decided ahead of time, [docs/tasks/planning/demo-account.md](../planning/demo-account.md) (moved out of `docs/guides/` once the remaining work became execution-blocked-on-deploy rather than a pure reference outline). Everything plannable ahead of deploy is done; the remaining execution (screenshots, live demo link, the actual seed migration) is blocked until deploy and tracked in [docs/tasks/in-progress/aws-infrastructure-implementation.md](../in-progress/aws-infrastructure-implementation.md)'s cutover section, not here.

## Phase 6 — MVP Scope Freeze & Execution Handoff — ✅ Done

- [x] Confirm feature build order — ADR 0020 (4 groups: Auth & Account Foundation → Market Data Integration → Trading Core → Portfolio & Reporting)
- [x] Consolidate into GitHub Issues backlog — 9 issues (US-1..US-9), milestones [#8](https://github.com/Efren707/toptrader/milestone/8)-[#11](https://github.com/Efren707/toptrader/milestone/11)
- [x] Final go/no-go before writing application code — **GO** (2026-07-17). Closed carried-forward open items (ADR 0021 market hours, `side` column type) and a full NFR audit (2 doc gaps closed: browser/responsive support, backend test framework). Phase 0-6 GitHub milestones closed; build-order milestones (#8-11) are the active backlog going forward.
