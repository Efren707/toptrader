# 0016 - Pipeline stages: Spotless + ESLint/Prettier lint, dorny/paths-filter, integration tests in CI

- Status: Accepted
- Date: 2026-07-17

## Context

ADR 0006 decided the pipeline shape (lint → test → build, required on PRs; deploy on merge to `main`) but left the concrete stage contents undecided: which lint tool per stack, how the monorepo avoids running backend jobs on frontend-only PRs (and vice versa — explicitly flagged as unresolved), and how much of the backend test suite the required "test" stage actually runs. This is the second Phase 4 checklist item, following environment definition (ADR 0015, `docs/architecture/environments.md`).

## Options considered

### Backend lint/format
- **Spotless** (`google-java-format` via the `com.diffplug.spotless` Maven plugin) - CI runs `spotless:check`; developers run `spotless:apply` locally to auto-fix. No ruleset to author or tune.
- **Checkstyle** - rule-based static analysis via a `checkstyle.xml` ruleset and `maven-checkstyle-plugin`. More configurable (can catch complexity/structure issues beyond formatting) but requires authoring and maintaining a ruleset, and doesn't auto-fix violations.

### Frontend lint/format
- **ESLint + Prettier** - `ng lint` (Angular ESLint schematic, the CLI's current default since TSLint's removal) for code-quality rules, Prettier for formatting, `eslint-config-prettier` to prevent rule conflicts between the two.
- **ESLint only** - skips a dedicated formatter, relying on ESLint's own style rules.

### Monorepo path filtering
- **`dorny/paths-filter`, single workflow** - one workflow file triggers on every PR unconditionally; an early job uses `dorny/paths-filter` to detect which of `backend/**` / `frontend/**` changed, and downstream jobs run conditionally (`if:`) on that output. The workflow (and its required job) always reports a status, regardless of which paths changed.
- **Native `paths:` on separate per-stack workflow triggers** - two workflow files, each gated by a trigger-level `paths:` filter. Simpler YAML, but GitHub's required-status-check branch protection can leave a PR blocked indefinitely if the required workflow's trigger-level path filter means it never runs at all for that PR (no run means no status is ever reported) — a known GitHub Actions gotcha with trigger-level path filtering plus required checks.

### Test stage scope
- **Unit + integration tests, including the IDOR/access-control tests ADR 0007 requires**, run against a Postgres service container (GitHub Actions' built-in free `services:` container, same major version as ADR 0009's local Docker Postgres and RDS). No numeric coverage threshold gate — pass/fail only.
- **Unit tests only** (mocked dependencies, no DB) - faster CI, no service container needed, but leaves the IDOR integration tests ADR 0007 calls "part of the core test suite expectations" outside the automated required gate.

## Decision

- **Backend lint**: Spotless (`google-java-format`), enforced via `spotless:check` in CI, `spotless:apply` for local auto-fix.
- **Frontend lint**: ESLint (Angular ESLint schematic) + Prettier, with `eslint-config-prettier` to avoid rule conflicts.
- **Monorepo path filtering**: single GitHub Actions workflow, always triggered; `dorny/paths-filter` job gates which of the backend/frontend lint+test+build jobs run, keeping branch protection's required check reliably reporting on every PR.
- **Test stage**: full backend suite (unit + integration, including IDOR tests) against a Postgres `services:` container matching the local/RDS major version. No coverage threshold enforced.
- **Stage order stays as ADR 0006 set it**: lint → test → build, required via branch protection on PRs; deploy only on merge to `main`.

## Consequences

- `pom.xml` gains the Spotless plugin; `package.json` gains ESLint (already scaffolded by `ng lint`) and Prettier plus `eslint-config-prettier`. A one-time `spotless:apply` / `prettier --write` pass will be needed once real backend/frontend code exists, to avoid the first CI run failing on pre-existing formatting.
- The `dorny/paths-filter` job adds one third-party action dependency (well-established, widely used) to an otherwise all-first-party-actions pipeline (ADR 0006's SSH/SCP and OIDC actions).
- Running Postgres as a CI service container duplicates (in a lighter-weight form) the Docker Compose Postgres from ADR 0009 — acceptable since it's GitHub Actions' native mechanism for a job-scoped ephemeral database, not a new tool to maintain.
- No coverage percentage is tracked or gated; if that becomes wanted later (e.g. a JaCoCo report), it would be an additive CI step, not a rework of this decision.
- This does not yet specify the deploy stage's own steps in pipeline-YAML detail (SCP/SSH commands, OIDC role assumption syntax) — that remains implementation work when workflows are actually written, not a design gap.
