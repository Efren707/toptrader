# Planning Roadmap & Status

> Last updated: 2026-07-17
> This file tracks *where we are*. For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each phase below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**Implementation started — project scaffolding, tooling, local dev DB, and CI are in place.** Planning (Phases 0-6) closed out 2026-07-17 with a final go/no-go review; see the "Implementation kickoff" section below for what's been built since. **Next step:** start on [issue #1 (US-1: Register)](https://github.com/Efren707/toptrader/issues/1), the first story in the Auth & Account Foundation milestone. (Phase 5's demo/showcase readiness item is still open and intentionally blocked until deploy — see `docs/guides/demo-showcase-readiness-outline.md`.)

## Implementation kickoff — Auth & Account Foundation (Milestone #8, in progress)

Groundwork completed ahead of US-1, per ADR 0020's build order:

- [x] Backend skeleton — Spring Boot 4.1.0, Java 21, Maven, `com.toptrader.backend` (Spring Boot 3.x is no longer offered by start.spring.io; 4.1.0 was the current stable GA at implementation time)
- [x] Frontend skeleton — Angular CLI 22, standalone components, routing
- [x] Backend tooling — Spotless (`google-java-format`) per ADR 0016
- [x] Frontend tooling — ESLint (`@angular-eslint/schematics`) + Prettier + `eslint-config-prettier`, Tailwind CSS v4
- [x] Frontend test runner — **Vitest**, not Jest as `frontend-architecture.md` originally specified; Angular CLI 22 now ships Vitest as its own built-in default (doc updated to match)
- [x] Local Postgres — Docker Compose, Postgres 17 (also the version future RDS will match), `application-local.yml.example` template per ADR 0009
- [x] CI — GitHub Actions PR quality gate (`dorny/paths-filter` → lint/test/build per stack), per ADR 0016; deploy-to-AWS stage intentionally deferred until EC2/RDS/S3/CloudFront/OIDC infra exists (ADR 0006)
- [x] Branch protection on `main` — `backend-ci`/`frontend-ci` required status checks, strict, admins included (matches ADR 0002's PR-based trunk workflow — direct pushes to `main` are no longer possible, including for this assistant)
- [ ] US-1 (Register) — not started

## Working agreement

See [CLAUDE.md](../CLAUDE.md) at repo root: one step at a time, always check in before deciding, ADR every notable decision.

---

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

## Phase 5 — User-Facing Documentation Planning — 🔄 In progress

- [x] End-user guide outline — `docs/guides/end-user-guide-outline.md`
- [x] Developer setup guide outline — `docs/guides/developer-setup-guide-outline.md`
- [x] Contribution/workflow guide outline — `docs/guides/contribution-workflow-guide-outline.md`
- [x] README structure finalized (as outline; `README.md` itself unchanged until deploy) — `docs/guides/readme-structure-outline.md`
- [ ] Demo/showcase readiness — mechanism + content decided, **rest blocked until deployed**, see `docs/guides/demo-showcase-readiness-outline.md`

## Phase 6 — MVP Scope Freeze & Execution Handoff — ✅ Done

- [x] Confirm feature build order — ADR 0020 (4 groups: Auth & Account Foundation → Market Data Integration → Trading Core → Portfolio & Reporting)
- [x] Consolidate into GitHub Issues backlog — 9 issues (US-1..US-9), milestones [#8](https://github.com/Efren707/toptrader/milestone/8)-[#11](https://github.com/Efren707/toptrader/milestone/11)
- [x] Final go/no-go before writing application code — **GO** (2026-07-17). Closed carried-forward open items (ADR 0021 market hours, `side` column type) and a full NFR audit (2 doc gaps closed: browser/responsive support, backend test framework). Phase 0-6 GitHub milestones closed; build-order milestones (#8-11) are the active backlog going forward.
