# 0018 - Secrets/config management: GitHub native scanning + gitleaks, unified SSM path for all prod config

- Status: Accepted
- Date: 2026-07-17

## Context

ADR 0015 decided *how* runtime secrets reach EC2 (instance IAM role pulling from SSM) but not which config values actually live in SSM versus a committed file, nor whether the public repo has any automated safety net against an accidentally committed secret beyond the gitignore discipline ADR 0009 already relies on. This is the fourth Phase 4 checklist item.

## Options considered

### Secret scanning (public repo)
- **GitHub native secret scanning + push protection** - automatically enabled on public repositories at no cost; scans for known provider secret patterns (AWS keys, API tokens, etc.) and, with push protection on, can block a push containing one before it ever lands in history. No config file to maintain.
- **gitleaks as an explicit CI step** - regex/entropy-based scanning of the PR diff, catching secrets that don't match a known provider pattern (e.g. the project's own session-signing secret or a raw DB password, which GitHub's provider-pattern list wouldn't recognize as a "known" secret type). Requires a `.gitleaks.toml` ruleset and a CI job, but is free and low-maintenance for a solo repo.
- **Both** - native scanning as the always-on push-time backstop (catches known-provider leaks immediately, even outside CI), gitleaks in CI as a diff-scoped second layer for this project's own custom secret values that GitHub's pattern list can't recognize.

### Prod config storage
- **Unified SSM path** (`/toptrader/prod/*`), every prod-specific config value stored there regardless of sensitivity — `SecureString` for genuine secrets (DB password, Finnhub key, session-signing secret), plain `String` for non-sensitive values (DB host, port, name, username, app server port). One fetch step (the deploy-time script from ADR 0015) pulls the entire prod config set in one pass.
- **Split storage**: only true secrets in SSM, non-sensitive values in a committed `application-prod.yml`. Keeps a clean secret/non-secret separation, but means a developer has to know, per key, which of two places to look, and the "promotion is a rename" story from `environments.md` would only hold for secret keys, not config keys generally.

## Decision

- **Secret scanning**: both layers. GitHub push protection (repo setting, confirmed enabled — public repos get scanning by default, push protection is opt-in and should be explicitly turned on) as the standing backstop; `gitleaks` added as a CI job (part of the lint stage from ADR 0016) with a project-specific ruleset covering this app's own secret value shapes.
- **Prod config storage**: unified SSM path. Every prod config key — secret or not — lives under `/toptrader/prod/<key>`, typed `SecureString` or `String` per its actual sensitivity.

## Consequences

- Every prod config key (not just secrets) now needs a corresponding SSM parameter created at provisioning time — expands the one-time EC2/SSM setup checklist from ADR 0015's consequences.
- `gitleaks` needs a `.gitleaks.toml` ruleset authored once real secret-shaped values exist in the codebase (env var names, key formats) to reduce false positives/negatives — an implementation detail for whenever the CI workflow YAML is actually written (ADR 0016), not resolved here.
- Local dev config placement is unaffected — ADR 0009's gitignored `application-local.yml` stays exactly as decided; this ADR only concerns prod.
- GitHub push protection can occasionally block a legitimate push if it misfires on a non-secret string that matches a known pattern; the documented escape hatch is GitHub's own "allow this secret" override flow on the blocked push, not a reason to disable the protection.
