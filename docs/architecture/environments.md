# Environments: local vs. prod

> Phase 4 deliverable. Consolidates the environment-specific pieces already decided across ADR 0005 (AWS shape), ADR 0006 (CI/CD), ADR 0007 (security baseline), ADR 0009 (local dev), and ADR 0015 (runtime secrets delivery) into one side-by-side reference. No staging environment exists — the project runs two: **local** (solo developer's machine) and **prod** (the one deployed AWS environment). See [deployment-architecture.md](./deployment-architecture.md) for the prod infra detail this doc's "prod" column summarizes.

## Environment matrix

| Aspect | Local | Prod |
|---|---|---|
| Spring profile | `local` | `prod` (default, no profile flag needed, or explicit `prod`) |
| Backend runs as | `mvnw spring-boot:run` / IDE run, plain process | Jar under `systemd`, `Restart=on-failure` |
| Database | Dockerized Postgres (`docker compose up -d`), image version matching RDS major version | RDS PostgreSQL, db.t4g.micro, Single-AZ, `publicly accessible = No` |
| Schema migrations | Flyway, runs at app startup against local Postgres | Flyway, runs at app startup against RDS |
| Config/secrets source | Gitignored `application-local.yml` (from committed `.example` template), each developer supplies their own Finnhub key + session-signing value | SSM Parameter Store, `/toptrader/prod/*` path, standard-tier SecureString, fetched by the EC2 instance role during deploy (ADR 0015) |
| Frontend origin (CORS) | `http://localhost:4200` | `https://app.toptrader.com` |
| Backend URL | `http://localhost:8080` | `https://api.toptrader.com` (CloudFront → EC2, HTTP inside AWS's network) |
| TLS | None (plain HTTP, loopback only) | ACM cert terminated at CloudFront edge |
| Logging | Console / local file, dev-friendly format | Local logback file, unified CloudWatch agent ships it to CloudWatch Logs, 14-30 day retention |
| Error responses | Stack traces may be visible (dev convenience) | `server.error.include-stacktrace=never`, `spring.jpa.show-sql=false` |
| Actuator | Fully open locally for debugging | Only `/actuator/health` exposed, `show-details=never` |
| Health/restart | None — developer restarts manually | systemd timer polling `/actuator/health`, auto-restart on repeated failure |
| Process supervision | None | systemd, non-root service user |

## What's out of scope here

- **Dependency vulnerability scanning** (Dependabot) is environment-agnostic — runs in CI regardless of target environment, already fully decided in ADR 0007. No entry needed in the matrix.
- **CI pipeline stage structure** (lint → test → build → deploy, `paths:` filters for the monorepo) is the next Phase 4 checklist item, not this one.
- A **staging environment** was considered out of scope for this pass — the project's solo-developer, low-traffic, cost-constrained profile doesn't currently justify a third environment. Revisit if that changes (e.g. a second contributor joins, or pre-prod verification becomes a real need).

## Promoting a config value from local to prod

Because local config keys (ADR 0009) and SSM parameter names (ADR 0015) intentionally share naming, moving a new config value from "works on my laptop" to prod is a rename, not a format translation: add the key to `application-local.yml.example` for local, and as an SSM `SecureString` (or plain `String` if non-sensitive) under `/toptrader/prod/<key>` for prod.
