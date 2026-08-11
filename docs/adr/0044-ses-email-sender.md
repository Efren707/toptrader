# 0044 - SES-backed EmailSender: domain identity, IAM scope, and failure handling

- Status: Accepted
- Date: 2026-08-10

## Context

ADR 0036 (password reset) and ADR 0037 (email verification) both built their full request/token/validate flow against an `EmailSender` interface, but deferred the actual delivery transport: only `LogEmailSender` (`@Profile("!prod")`) existed, and prod had no working sender — `/auth/forgot-password` and the verification-email send both returned `503`/failed silently in prod, tracked explicitly as a known gap in both ADRs, deferred until "AWS deployment sequencing" actually happened. That's now section 8 (cutover) of the AWS Deployment Infrastructure milestone, with a live domain and a running EC2 instance in place, so this ADR resolves the deferred piece: a real `SesEmailSender`.

## Options considered

**Sending identity verification.**
- *Single email address* - fastest to verify (click a confirmation link), no DNS changes, but locks sending to that one exact address and gets no DKIM signing, hurting deliverability.
- *Whole domain (`toptrader.dev`), Easy DKIM* (chosen) - a few DKIM CNAME records in Route 53 (auto-published by SES directly into the zone, since it's the same AWS account - no manual record copying needed), but lets the app send from any address at the domain and signs outgoing mail with DKIM, which is the standard posture for a real deployed sender. Custom MAIL FROM domain (which would add its own MX/SPF records for return-path alignment) was explicitly skipped - the default SES return-path domain is enough at this scale, and it's one less thing to configure/maintain.

**Sandbox vs. production access.** New SES accounts are sandboxed (verified recipients only, 200 emails/day). Requested production access immediately rather than deferring it, since a resume/portfolio project is meant to be tried by outside reviewers, not just pre-verified test addresses - staying sandboxed would mean the feature only works for the developer, not for anyone actually evaluating the app.

**IAM scope.** New customer-managed policy (`toptrader-ses-send-policy`, mirroring the existing `toptrader-ssm-read-policy` pattern from section 4) granting only `ses:SendEmail`/`ses:SendRawEmail`, scoped by resource ARN to the single `toptrader.dev` identity - not a wildcard SES resource. Attached to the existing `toptrader-ec2-role` rather than creating a second role, since it's the same instance doing the sending.

**Config delivery.** New `/toptrader/prod/mail-from-address` SSM parameter (e.g. `noreply@toptrader.dev`), same `SecureString` type as the other five `/toptrader/prod/*` parameters even though it isn't actually secret - keeps the fetch script and the instance role's existing `kms:Decrypt` scope uniform rather than special-casing one parameter's type. Exported by the on-instance `fetch-secrets.sh` as `TOPTRADER_MAIL_FROM_ADDRESS`, bound in `application-prod.properties` as `toptrader.mail-from-address=${TOPTRADER_MAIL_FROM_ADDRESS}` - same shape as `frontend-origin` and `csrf-cookie-domain`.

**SDK module.** AWS SDK v2's `ses` module (classic `SesClient`/`SendEmailRequest`), not `sesv2` - `sesv2` adds contact-list and templating surface area this app has no use for; `EmailSender.send(to, subject, body)` only ever needs a plain destination/subject/body send.

**Body content type.** SES `Body.text(...)`, not `.html(...)` - the actual strings passed by `PasswordResetService`/`EmailVerificationService` are plain text with a bare link, not markup, so HTML would misrepresent the content and require no code change on the calling side anyway.

**Send-failure handling.**
- *Let exceptions propagate* - simplest, but a transient SES failure (throttling, network blip) would surface as an unhandled exception -> 500, which behaves differently from the "email doesn't exist" path that `resetRequest()` already silently no-ops on for enumeration-safety (ADR 0025).
- *Catch and log, don't rethrow* (chosen) - `SesEmailSender.send()` catches `SdkException` (the SDK's common base, covering both service-level `SesException` and lower-level client/network failures) and logs at `WARN` with the recipient and cause attached, without rethrowing. Keeps the caller's response behavior consistent regardless of whether the send actually succeeded, matching the enumeration-safe posture the reset flow already has for the "user doesn't exist" case.

## Decision (summary)

- SES domain identity for `toptrader.dev`, Easy DKIM, records auto-published to the existing Route 53 hosted zone. No custom MAIL FROM domain.
- Production access requested (out of sandbox) so real users' emails are deliverable, not just pre-verified test addresses.
- New `toptrader-ses-send-policy` (customer-managed, `ses:SendEmail`/`SendRawEmail` scoped to the one identity ARN) attached to the existing `toptrader-ec2-role`.
- New `/toptrader/prod/mail-from-address` SSM parameter (`SecureString`, matching the existing five), exported as `TOPTRADER_MAIL_FROM_ADDRESS` by `fetch-secrets.sh`, bound as `toptrader.mail-from-address` in `application-prod.properties`.
- `SesEmailSender` (`@Profile("prod")`, `software.amazon.awssdk:ses`) implements `EmailSender` using plain-text SES `Body`. Send failures are caught (`SdkException`), logged at `WARN` with the recipient and cause, and not rethrown.

## Consequences

- Closes the prod gap ADR 0036/0037 both flagged and deferred: `Optional<EmailSender>` in `PasswordResetService`/`EmailVerificationService` now resolves to a working bean in prod, so `/auth/forgot-password` and the verification-email send are no longer `503`/silently non-functional in prod - no code change was needed in either service beyond this new bean existing.
- No change to the ~$18-20/mo cost estimate from ADR 0005/0014: SES gives 62,000 free outbound emails/month when sent from EC2 (a standing SES offer, distinct from - and not gated by - the account's already-lapsed 12-month AWS free tier), and this app's expected volume is a handful of emails a month at most.
- Skipping a custom MAIL FROM domain and an SES configuration set (bounce/complaint routing to SNS) keeps setup simpler now but means bounce/complaint handling is only what SES does by default - worth revisiting only if real delivery problems or bulk volume ever materialize, not needed at this project's scale.
- `SesEmailSender`'s swallow-and-log failure handling means a broken SES integration (e.g. a revoked IAM permission, or falling back into sandbox restrictions) would fail silently from the caller's perspective - only visible in application logs, not in any user-facing signal or alarm. Acceptable at this scale given `security-architecture.md`'s existing logging posture, but not wired to any alerting.
