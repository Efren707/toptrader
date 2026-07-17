# Environments: local vs. prod

> Phase 4 deliverable. Consolidates the environment-specific pieces already decided across ADR 0005 (AWS shape), ADR 0006 (CI/CD), ADR 0007 (security baseline), ADR 0009 (local dev), ADR 0015 (runtime secrets delivery), and ADR 0018 (secrets/config management) into one side-by-side reference. No staging environment exists — the project runs two: **local** (solo developer's machine) and **prod** (the one deployed AWS environment). See [deployment-architecture.md](./deployment-architecture.md) for the prod infra detail this doc's "prod" column summarizes.

## Environment matrix

| Aspect | Local | Prod |
|---|---|---|
| Spring profile | `local` | `prod` (default, no profile flag needed, or explicit `prod`) |
| Backend runs as | `mvnw spring-boot:run` / IDE run, plain process | Jar under `systemd`, `Restart=on-failure` |
| Database | Dockerized Postgres (`docker compose up -d`), image version matching RDS major version | RDS PostgreSQL, db.t4g.micro, Single-AZ, `publicly accessible = No` |
| Schema migrations | Flyway, runs at app startup against local Postgres | Flyway, runs at app startup against RDS |
| Config/secrets source | Gitignored `application-local.yml` (from committed `.example` template), each developer supplies their own Finnhub key + session-signing value | SSM Parameter Store, unified `/toptrader/prod/*` path for every prod config key (`SecureString` for secrets, `String` for non-sensitive values, ADR 0018), fetched by the EC2 instance role during deploy (ADR 0015) |
| Frontend origin (CORS) | `http://localhost:4200` | `https://app.toptrader.com` |
| Backend URL | `http://localhost:8080` | `https://api.toptrader.com` (CloudFront → EC2, HTTP inside AWS's network) |
| TLS | None (plain HTTP, loopback only) | ACM cert terminated at CloudFront edge |
| Logging | Console / local file, dev-friendly format | Local logback file, unified CloudWatch agent ships it to CloudWatch Logs, 14-30 day retention |
| Error responses | Stack traces may be visible (dev convenience) | `server.error.include-stacktrace=never`, `spring.jpa.show-sql=false` |
| Actuator | Fully open locally for debugging | Only `/actuator/health` exposed, `show-details=never` |
| Health/restart | None — developer restarts manually | systemd timer polling `/actuator/health`, auto-restart on repeated failure |
| Process supervision | None | systemd, non-root service user |

## Secrets & config inventory

Every environment-specific value the app or pipeline needs, and where it lives. "Sensitive" drives `SecureString` vs. `String` in SSM (ADR 0018); CI-only rows never touch the app's own config at all.

| Key | Sensitive? | Local (ADR 0009) | Prod (ADR 0015/0018) |
|---|---|---|---|
| DB host | No | `localhost` (Docker Compose) | `/toptrader/prod/db-host` (`String`) |
| DB port | No | `5432` (Docker Compose default) | `/toptrader/prod/db-port` (`String`) |
| DB name | No | `toptrader` (Docker Compose) | `/toptrader/prod/db-name` (`String`) |
| DB username | No | throwaway local value, hardcoded in `docker-compose.yml` | `/toptrader/prod/db-username` (`String`) |
| DB password | **Yes** | throwaway local value, hardcoded in `docker-compose.yml` | `/toptrader/prod/db-password` (`SecureString`) |
| Finnhub API key | **Yes** | `application-local.yml` (gitignored) | `/toptrader/prod/finnhub-api-key` (`SecureString`) |
| Session-signing secret | **Yes** | `application-local.yml` (gitignored) | `/toptrader/prod/session-signing-secret` (`SecureString`) |
| App server port | No | `8080` (Spring default) | `/toptrader/prod/server-port` (`String`) |

CI-time-only values (never read by the app itself, GitHub encrypted secrets per ADR 0006 — SSH private key, EC2 host, EC2 SSH port, AWS OIDC role ARN, S3 bucket name, CloudFront distribution ID) aren't repeated here; see ADR 0006 for that list.

## Secret scanning

Per ADR 0018: GitHub native secret scanning + push protection (automatic on this public repo, blocks a push containing a recognized secret pattern) as the standing backstop, plus `gitleaks` as a CI job (part of the lint stage, ADR 0016) with a project-specific ruleset for this app's own secret value shapes that GitHub's provider-pattern list wouldn't otherwise catch.

## What's out of scope here

- **Dependency vulnerability scanning** (Dependabot) is environment-agnostic — runs in CI regardless of target environment, already fully decided in ADR 0007. No entry needed in the matrix.
- A **staging environment** was considered out of scope for this pass — the project's solo-developer, low-traffic, cost-constrained profile doesn't currently justify a third environment. Revisit if that changes (e.g. a second contributor joins, or pre-prod verification becomes a real need).

## Promoting a config value from local to prod

Because local config keys (ADR 0009) and SSM parameter names (ADR 0015/0018) intentionally share naming, moving a new config value from "works on my laptop" to prod is a rename, not a format translation: add the key to `application-local.yml.example` for local, and as an SSM `SecureString` (or plain `String` if non-sensitive) under `/toptrader/prod/<key>` for prod — see the inventory table above for existing examples.
