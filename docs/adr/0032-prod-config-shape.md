# 0032 - Prod config shape: committed application-prod.properties + env-var secrets

- Status: Accepted
- Date: 2026-07-30

## Context

A pre-production security review found that `backend/src/main/resources/application.properties` (the base file) is missing several prod-specific settings, and there's no `prod` Spring profile file at all. Concretely: `toptrader.frontend-origin` (`SecurityConfig.java:33`) falls back to `http://localhost:4200` if unset — safe-ish (fails closed on CORS rather than open) but silent — and `spring.datasource.*` (DB host/port/name/user/password) has no source for prod whatsoever. ADR 0015 anticipated this gap but explicitly deferred it: the deploy-time SSM-fetch script would "write the app's config file (e.g. `application-prod.yml` or an env file)," calling the exact shape "an implementation detail for whenever EC2 is actually provisioned." This ADR resolves that detail now, ahead of deployment execution.

## Options considered

- **Committed `application-prod.properties`** - a real file in the repo, activated via `spring.profiles.active=prod`, holding only non-secret, environment-agnostic-to-write policy settings (explicit CORS origin via `${TOPTRADER_FRONTEND_ORIGIN}`, actuator/error-handling made explicit rather than relying on Spring Boot's defaults). Secrets and per-deploy values (DB connection, Finnhub key) are never written into it — they arrive as environment variables that Spring Boot's `Environment` abstraction auto-binds (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `TOPTRADER_FINNHUB_API_KEY`), set by the not-yet-written SSM-fetch script (ADR 0015) as systemd `Environment=` directives (or an `EnvironmentFile`). Gives a single, diffable, reviewable file describing prod's explicit policy, consistent with how `application-local.yml` already documents local's policy.
- **No committed prod file, env vars only** - every prod-specific setting (including the explicit policy ones, not just secrets) delivered purely via env vars the deploy script exports from SSM. Matches `environments.md`'s literal "prod (default, no profile flag needed)" framing, but means there is no single file in the repo describing prod's policy — it would only exist as whatever the deploy script happens to export, invisible to anyone reading the codebase.

## Decision

**Committed `application-prod.properties`**, activated by an explicit `prod` Spring profile (not the implicit "no profile" default `environments.md` originally sketched — making prod explicit-by-flag matches how `local` already works, and removes the asymmetry where accidentally launching with no profile flag at all silently becomes "prod"). It holds only settings that are safe to commit and don't vary per deploy:

- `toptrader.frontend-origin=${TOPTRADER_FRONTEND_ORIGIN}` - no unsafe localhost fallback in prod; a missing env var now fails startup instead of silently misconfiguring CORS.
- `server.error.include-stacktrace=never`, `spring.jpa.show-sql=false` - explicit instead of relying on Spring Boot defaults matching by coincidence.
- `management.endpoints.web.exposure.include=health`, `management.endpoint.health.show-details=never` - explicit actuator lockdown, same reasoning.

Everything else prod needs (`spring.datasource.url`/`username`/`password`, `toptrader.finnhub.api-key`) comes from environment variables Spring Boot auto-binds, populated by the ADR 0015 SSM-fetch script at deploy time — never written to any file in the repo or on disk in plaintext outside that script's own runtime handoff to systemd.

## Consequences

- The EC2 systemd unit (not yet written, part of upcoming deployment execution work) must launch the jar with `-Dspring.profiles.active=prod` explicitly — there's no "it just works by default" fallback anymore, which is the intended fail-closed behavior.
- The ADR 0015 SSM-fetch script's job is now narrower and clearer: fetch `/toptrader/prod/*` parameters and export exactly `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `TOPTRADER_FINNHUB_API_KEY` as environment variables for the systemd service — it does not need to template or write any `.properties`/`.yml` content itself.
- `session-signing-secret` (present in `environments.md`'s config inventory and `application-local.yml.example`, but not referenced anywhere in the actual Java code) is flagged as dead config by this review — not resolved here, left as a follow-up decision (remove from docs/template, or wire it up if a future feature needs it).
- If a `staging` environment is ever added, it follows the same pattern: its own `application-<env>.properties` plus env-var secrets under `/toptrader/staging/*` (ADR 0015's own consequence already anticipated this).
