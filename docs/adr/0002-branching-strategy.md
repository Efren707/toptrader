# 0002 - Trunk-based development with short-lived feature branches

- Status: Accepted
- Date: 2026-07-16

## Context

This is a solo project, but the branching workflow is itself a demonstrated practice for anyone reviewing the repo/commit history, and it needs to support CI/CD (MVP ships, then features land incrementally).

## Options considered

- **Trunk-based, short-lived feature branches** - `main` is always deployable; each feature/fix is a short branch merged back via PR. Matches how most modern teams and CI/CD pipelines actually operate.
- **GitFlow-lite (`main` + `develop`)** - adds a `develop` integration branch between feature work and production. More ceremony/branch management overhead; more relevant when coordinating scheduled releases across a team.

## Decision

Use trunk-based development: `main` is always deployable, work happens on short-lived `feature/*` (or `fix/*`) branches merged back via pull request.

## Consequences

- Keeps the CI/CD pipeline simple: every merge to `main` is a release candidate.
- Requires discipline to keep feature branches small and short-lived, and to gate merges on CI passing (build/test) even as a solo developer.
- No separate `develop` branch to keep in sync.
