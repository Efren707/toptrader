# 0009 - Local dev environment: Docker Compose for Postgres only, Spring profiles for secrets

- Status: Accepted
- Date: 2026-07-16

## Context

This is the last Phase 2 research spike: local dev environment tooling. Prod hosting (ADR 0005) is a plain Spring Boot jar on EC2 via systemd (not containerized, chosen to avoid ECS/Fargate/ALB cost) backed by RDS PostgreSQL. Prod secrets are pulled from SSM Parameter Store at deploy time (ADR 0006). This spike decides how a solo developer runs Postgres and the Spring Boot app locally, and how local secrets/config are supplied, without committing anything sensitive.

## Options considered

### Local PostgreSQL
- **Docker Compose, single `postgres` container**, image pinned to the same major version as the RDS instance - avoids the dev/prod dialect drift that an embedded substitute (H2) introduces (H2's SQL dialect, sequence/identity handling, and JSON/date functions diverge from Postgres in ways that pass locally and fail on RDS). No host package management, trivial reset via `docker compose down -v`.
- **Native PostgreSQL install** - works, but adds host package management and version-drift risk across dev machines/OS updates; more effort than Docker for no benefit here.
- **H2 (embedded/in-memory)** - rejected; the dialect-divergence risk directly undermines the point of matching prod's database.

### Local Spring Boot app
- **Run directly** (`mvn spring-boot:run` / `./gradlew bootRun` / IDE run), pointed at the Dockerized Postgres on `localhost` - no Dockerfile needed for the app itself.
- **Containerize the app locally too (full `docker-compose.yml` with app + Postgres)** - rejected. Prod runs a plain jar, not a container, so a local-only Dockerfile buys no dev/prod parity (there is no prod Dockerfile to match) — it would be a second build definition to keep in sync with the Maven/Gradle build, maintenance overhead with no offsetting benefit for a solo developer. It also slows iteration: running the jar directly supports Spring Boot DevTools hot reload and direct debugger attach, both slower or more awkward through a rebuilt Docker image. This would be worth revisiting only if a second developer joins and host-environment drift (JDK version, OS quirks) starts causing real "works on my machine" bugs — not a risk that exists for a solo project.

### Local env vars / secrets
- **Spring profiles**: gitignored `application-local.yml` (or `.properties`), activated via `spring.profiles.active=local`, with a committed `application-local.yml.example` template - Spring Boot's externalized config is native, so this needs no extra library. Profile-scoped keys map directly onto the same names SSM will hold in prod (ADR 0006), so promoting a config key from local to prod later is a rename, not a format translation.
- **`.env` file** (with `spring-dotenv` or similar to parse it) - rejected; reaches for an extra dependency to replicate config-loading Spring Boot already provides natively. `.env` is a common pattern in other stacks (Node, etc.) precisely because those stacks lack Spring's built-in profile/externalized-config system — that reason doesn't apply here.

## Decision

- **Local Postgres**: Docker Compose, Postgres-only service, image version matching RDS's major version.
- **Local app**: run directly (no Dockerfile), against the Dockerized Postgres.
- **Local secrets/config**: gitignored `application-local.yml` + committed `application-local.yml.example` template, activated via the `local` Spring profile. The Postgres container's own local credentials can be hardcoded in `docker-compose.yml` (throwaway local-only values, not a real secret). Each developer supplies their own free-tier Finnhub key and a local session-signing value in their gitignored file.

Net new tooling: `docker-compose.yml` (Postgres only), `application-local.yml.example` (committed), `application-local.yml` (gitignored, added to `.gitignore`). No Dockerfile for the app.

## Consequences

- Using Docker for local Postgres while prod runs an uncontainerized jar is not an inconsistency with ADR 0005 — that ADR ruled out containers for *production compute* on cost grounds; Docker here is a local convenience tool for one stateless dependency, and running the app doesn't require any Docker knowledge.
- This closes out **Phase 2 — Research Spikes** in full. Phase 3 (Technical & Architecture Documentation) is next.
- The clone-to-running onboarding sequence (`docker compose up -d` → copy the `.example` file and fill in local values → `mvnw spring-boot:run` → `npm start`) becomes the basis for Phase 5's developer setup guide — no further tooling decision needed there, just documenting this sequence.
- `.gitignore` needs an entry for `application-local.yml` (or the local profile file's actual name/extension) before any secrets are filled in, to prevent an accidental commit.
