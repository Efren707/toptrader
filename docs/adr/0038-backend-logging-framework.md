# 0038 - Backend logging framework: structured JSON, externalized prod log path, email logged on failed login

- Status: Accepted
- Date: 2026-08-06

## Context

Issue [#97](https://github.com/Efren707/toptrader/issues/97) is implementing the backend logging framework ADR 0033 anticipated: no `Logger`/Slf4j usage exists anywhere in the codebase yet. ADR 0033 set the content policy (identifiers/event names, not full bodies; masked-secret DTOs) but left two things open that need deciding before writing the code: the wire format for the file ADR 0008's CloudWatch agent will later tail, and how to handle the one case where "identifier" and "PII" are the same field — a failed login attempt, where the only identifier available is the submitted email.

## Options considered

### Log format
- **Structured JSON via Spring Boot's native structured logging** (chosen) — Spring Boot 3.4+ (including the 4.1 line this project is on) supports `ecs`/`gelf`/`logstash` JSON formats out of the box, fully property-driven (`logging.structured.format.file=logstash`), with no additional dependency. Key-value fields come from SLF4J 2.x's own fluent API (`log.atInfo().addKeyValue(...)`), which Boot's structured formatters fold into the JSON output automatically. Originally scoped around the `logstash-logback-encoder` library before this native support was found — see below.
- **`logstash-logback-encoder` dependency** — the pre-3.4 way to get JSON output; rejected once Boot's native support was confirmed to cover the same `logstash` format with zero added dependencies and less custom XML.
- **Plain text pattern** — Logback's default, greppable via SSH. Rejected: loses queryable structure in CloudWatch Insights, which is a one-time setup cost now versus a recurring parsing cost later if the logs ever need real querying — and with native support, that one-time cost is now just a couple of properties, not a new dependency.

### Failed-login identifier
- **Log the submitted email** (chosen) — `LoginService.recordFailedAttempt(email)` already keys its brute-force lockout tracking (ADR 0004 amendment) off the submitted email; a failed-auth event has no `userId` yet, so email is the only identifier available. Since the email is already the security-relevant key this code path correlates on internally, logging it doesn't newly expose anything beyond what the lockout mechanism already does in the database.
- **No identifier, event only** — safer read of ADR 0033's "identifiers, not object graphs," but makes a logged failed-login event useless for spotting a targeted attack against one account, and ADR 0034's general rate limiting already covers blunt brute-force volume, not per-account correlation.

## Decision

- No new Maven dependency, no `logback-spring.xml`. Everything is properties-only, using Spring Boot's existing profile mechanism (`application-prod.properties`, already present in this project):
  - **default (local)**: unset — Boot's normal human-readable console output, unchanged.
  - **`prod` profile** (`application-prod.properties`): `logging.structured.format.file=logstash`, plus `logging.file.name` set to the (deploy-time-supplied) log file path — externalizing the actual filesystem path as a deploy-time config concern, not a code concern. Rotation via Boot's built-in `logging.logback.rollingpolicy.*` properties (`max-file-size`, `total-size-cap`, `max-history`) to bound disk usage on the EC2 instance independently of CloudWatch's own retention setting (ADR 0008) — the two are separate knobs, one local disk, one AWS-side.
- Structured fields are logged via SLF4J's fluent API (e.g. `log.atInfo().addKeyValue("userId", id).log("Login succeeded")`), not string-concatenated into the message, so they land as queryable JSON fields under Boot's structured formatter.
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

- No new dependency — zero added build/classpath surface, and one less thing to keep patched.
- The local rolling policy's retention should be spot-checked against real disk usage once the app has actual prod traffic, same caveat ADR 0008 already notes for CloudWatch-side retention.
- `logging.file.name` needs a real value in `application-prod.properties` (or an env override) before this is meaningful in prod — ties into EC2 bootstrap/ADR 0014, tracked under #102, not this issue.
- Deliberately logging the submitted email on failed login is a narrower, justified exception to ADR 0033's general rule, not a reopening of it — future PII-adjacent logging decisions should still default to identifiers-only unless a similarly concrete justification applies.
