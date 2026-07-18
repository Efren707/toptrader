# 0023 - Skip Lombok

- Status: Accepted
- Date: 2026-07-17

## Context

`start.spring.io` doesn't include Lombok by default, and it wasn't selected when the backend was scaffolded — an omission, not a decision. It came up explicitly while implementing US-1: `User.java` (the first JPA entity) has a full set of hand-written getters/setters/constructor, and more entities (`Holding`, `Transaction`) and DTOs are coming, which is exactly the boilerplate Lombok (`@Getter`/`@Setter`, `@NoArgsConstructor`, etc.) trims.

## Options considered

- **Add Lombok** - less boilerplate per class going forward, at the cost of a build-time annotation-processor dependency and an IDE plugin requirement for every environment (including for anyone else who ever clones this resume-project repo) for its generated code to be recognized.
- **Skip it, keep plain Java** - no extra dependency or annotation-processor config, no IDE plugin requirement, nothing "magic" happening to class bytecode. The entity/DTO count on this project's scope (3 tables, a handful of DTOs) is small enough that the boilerplate stays mechanical and low-risk rather than becoming a real maintenance burden.

## Decision

**Skip Lombok.** Keep hand-written getters/setters/constructors in plain Java.

## Consequences

- Every future entity/DTO carries its own boilerplate; accepted given this project's small, fixed entity count (`users`, `holdings`, `transactions` per `data-model.md`).
- No IDE plugin requirement for anyone building this project, and no annotation-processing step in the Maven build.
- Revisit only if entity/DTO count grows meaningfully beyond what's already scoped in the MVP data model — not expected.
