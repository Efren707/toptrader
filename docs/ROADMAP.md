# Planning Roadmap & Status

> Last updated: 2026-07-16
> This file tracks *where we are*. For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each phase below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**Phase 3 complete.** All architecture docs done: system architecture, data model/ERD (ADR 0010), schema migration tooling (Flyway, ADR 0011), API contract (ADR 0012), security architecture, frontend architecture (ADR 0013), and deployment/infra architecture (ADR 0014 — default VPC, CloudFront-only EC2 exposure, HTTP origin protocol, $15/$25 budget thresholds). Next step: kick off **Phase 4 — CI/CD & Environment Strategy**, starting with defining local/prod environments.

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

- [x] Market data API research (real-time vs. delayed, rate limits, ToS, and market-hours/stale-price behavior) — **Finnhub selected, see ADR 0003.** Finnhub has no market-status field, so market-open/closed still needs to be computed from a trading calendar — carried forward as an open item.
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

## Phase 4 — CI/CD & Environment Strategy — ⏳ Not started

- [ ] Environments defined (local/prod)
- [ ] Pipeline stages (lint → test → build → deploy)
- [ ] Merge/deploy test gates
- [ ] Secrets/config management per environment
- [ ] Post-MVP feature release strategy

## Phase 5 — User-Facing Documentation Planning — ⏳ Not started

- [ ] End-user guide outline
- [ ] Developer setup guide outline
- [ ] Contribution/workflow guide outline
- [ ] README structure finalized
- [ ] Demo/showcase readiness (seeded demo account so reviewers see a populated portfolio, README screenshots/GIF, live demo link callout)

## Phase 6 — MVP Scope Freeze & Execution Handoff — ⏳ Not started

- [ ] Consolidate into GitHub Issues backlog
- [ ] Confirm feature build order
- [ ] Final go/no-go before writing application code
