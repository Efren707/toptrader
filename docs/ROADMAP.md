# Planning Roadmap & Status

> Last updated: 2026-07-16
> This file tracks *where we are*. For *why* decisions were made, see `docs/adr/`. For requirements detail, see `docs/requirements/`. Each phase below also has a matching [GitHub Milestone](https://github.com/Efren707/toptrader/milestones) for visual progress tracking.

## Current focus

**Phase 2, not yet started.** Next step: kick off the first research spike — **market data API research** (compare Alpha Vantage, Finnhub, Twelve Data, IEX Cloud, Polygon.io) — see Phase 2 below.

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

## Phase 2 — Research Spikes — ⏳ Not started

Each spike produces a recommendation + trade-offs for review, then an ADR.

- [ ] Market data API research (real-time vs. delayed, rate limits, ToS, and market-hours/stale-price behavior — what quote/buy/sell should do when the market is closed) — **up next**
- [ ] Auth strategy (session vs. JWT vs. OAuth2, password hashing)
- [ ] AWS deployment shape (EC2 vs. ECS/Fargate vs. Beanstalk vs. App Runner; RDS; frontend hosting; budget alerts / free-tier guardrails)
- [ ] CI/CD pipeline design (GitHub Actions stages, containerization, deploy triggers)
- [ ] Security baseline (OWASP Top 10 applied, secrets management, CORS)
- [ ] Observability basics (logging, CloudWatch, health checks)
- [ ] Local dev environment tooling (Docker Compose for Postgres, local env var/secrets setup)

## Phase 3 — Technical & Architecture Documentation — ⏳ Not started

- [ ] System architecture diagram
- [ ] Data model / ERD (including schema migration tooling — Flyway vs. Liquibase)
- [ ] API design/contract (OpenAPI)
- [ ] Security architecture doc
- [ ] Frontend architecture (Angular structure, state management)
- [ ] Deployment/infra architecture doc

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
