# 0011 - Schema migration tooling: Flyway

- Status: Accepted
- Date: 2026-07-16

## Context

The data model (ADR 0010, `docs/architecture/data-model.md`) defines a 3-table Postgres schema (`users`, `holdings`, `transactions`). The backend needs a schema migration tool to version and apply that schema — and future changes to it — deployed alongside the Spring Boot jar (ADR 0005/0006). This is a single-Postgres-instance, solo-developer, trunk-based project; cost and simplicity are weighted heavily throughout (per the project's standing cost-first bias).

## Options considered

- **Flyway** - versioned migrations as plain SQL files (`V1__description.sql`), applied automatically on Spring Boot startup via built-in autoconfiguration (`flyway-core` on the classpath, no extra wiring). Community edition is free and sufficient here; forward-only (no automated rollback — that's a paid Teams feature). Simplest mental model: migrations are just SQL.
- **Liquibase** - changelogs in XML/YAML/JSON/SQL, with database-agnostic abstraction and auto-generated rollback support for many change types even in its open-source core. More powerful (diffing, preconditions, multi-DB targets) than this project needs, since it will run against Postgres exclusively for its lifetime, and more verbose for simple, mostly-linear schema changes.

## Decision

**Flyway**, using plain SQL migration files under the backend's `src/main/resources/db/migration/` (Flyway's default location), applied automatically at Spring Boot startup.

## Consequences

- Migrations are forward-only: a mistake is fixed by writing a new migration, not by rolling back an applied one — an accepted trade-off for a solo trunk-based project, consistent with ADR 0005/0006's "fix forward" deploy posture (no blue/green, no automated rollback tooling there either).
- Migrations run automatically when the jar starts (on EC2, via the `systemctl restart` deploy step from ADR 0006) — no separate CI migration step is required, though a `flywayValidate`/dry-run could be added to the CI build stage later as a safety check if desired (not decided here).
- If the project ever needed to support multiple database engines or wanted built-in auto-rollback, this would need revisiting — not expected to happen for this project's scope.
- Naming convention for migration files (`V<version>__<description>.sql`) and how versions map to feature branches/PRs is an implementation detail to settle when the first migration is written, not part of this ADR.
