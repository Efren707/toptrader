# 0038 - Backend logging framework: structured JSON, externalized prod log path, email logged on failed login

- Status: Accepted
- Date: 2026-08-06

## Context

Issue [#97](https://github.com/Efren707/toptrader/issues/97) is implementing the backend logging framework ADR 0033 anticipated: no `Logger`/Slf4j usage exists anywhere in the codebase yet. ADR 0033 set the content policy (identifiers/event names, not full bodies; masked-secret DTOs) but left two things open that need deciding before writing the code: the wire format for the file ADR 0008's CloudWatch agent will later tail, and how to handle the one case where "identifier" and "PII" are the same field — a failed login attempt, where the only identifier available is the submitted email.

## Options considered

### Log format
- **Structured JSON** (chosen) — via the `logstash-logback-encoder` dependency, encoding each log line as JSON with key-value fields (event name, userId, ticker, tradeId, etc.) instead of a string-interpolated message. Makes fields queryable in CloudWatch Insights later without re-parsing. Costs one new dependency and a bit more setup than the default pattern layout.
- **Plain text pattern** — Logback's default, zero new dependencies, greppable via SSH. Rejected: loses queryable structure in CloudWatch Insights, which is a one-time setup cost now versus a recurring parsing cost later if the logs ever need real querying.

### Failed-login identifier
- **Log the submitted email** (chosen) — `LoginService.recordFailedAttempt(email)` already keys its brute-force lockout tracking (ADR 0004 amendment) off the submitted email; a failed-auth event has no `userId` yet, so email is the only identifier available. Since the email is already the security-relevant key this code path correlates on internally, logging it doesn't newly expose anything beyond what the lockout mechanism already does in the database.
- **No identifier, event only** — safer read of ADR 0033's "identifiers, not object graphs," but makes a logged failed-login event useless for spotting a targeted attack against one account, and ADR 0034's general rate limiting already covers blunt brute-force volume, not per-account correlation.

## Decision

- Add `logstash-logback-encoder` to `backend/pom.xml`.
- `logback-spring.xml` with profile-scoped appenders:
  - **default (local)**: console, human-readable pattern (current Spring Boot default behavior, unchanged).
  - **`prod` profile**: file appender using `LogstashEncoder` (JSON), path externalized as a property (`logging.file.name` in `application-prod.properties`, not hardcoded in the XML) so the actual filesystem path is a deploy-time config concern, not a code concern. Local rolling policy (size+time based) to bound disk usage on the EC2 instance independently of CloudWatch's own retention setting (ADR 0008) — the two are separate knobs, one local disk, one AWS-side.
- Structured fields are logged as key-value arguments (e.g. `StructuredArguments.kv("userId", id)`), not string-concatenated into the message, so they land as queryable JSON fields.
- Content policy (applying ADR 0033 per-event):
  - Login success: INFO, `userId`.
  - Login failure: WARN, submitted `email` (see decision above) + reason.
  - Account lockout triggered: WARN, `userId`/`email`.
  - Registration success: INFO, `userId`.
  - Password reset / email verification requested and completed: INFO, `userId` when known.
  - Trade executed (buy/sell): INFO, `userId`, `ticker`, `tradeId`.
  - Unhandled exception (new catch-all in `GlobalExceptionHandler`): ERROR, full exception logged server-side; the client response stays a generic, stack-trace-free `ProblemDetail` (consistent with `server.error.include-stacktrace=never`).
  - Exact per-line judgment calls beyond this (e.g. whether a specific low-traffic event is worth an INFO line at all) are implementation detail, not re-litigated here.

## Consequences

- New runtime dependency (`logstash-logback-encoder`); worth a quick check that it doesn't drag in anything unexpected via `mvn dependency:tree`.
- The local rolling policy's retention should be spot-checked against real disk usage once the app has actual prod traffic, same caveat ADR 0008 already notes for CloudWatch-side retention.
- `logging.file.name` needs a real value in `application-prod.properties` (or an env override) before this is meaningful in prod — ties into EC2 bootstrap/ADR 0014, tracked under #102, not this issue.
- Deliberately logging the submitted email on failed login is a narrower, justified exception to ADR 0033's general rule, not a reopening of it — future PII-adjacent logging decisions should still default to identifiers-only unless a similarly concrete justification applies.
