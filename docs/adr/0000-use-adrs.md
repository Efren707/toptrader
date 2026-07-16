# 0000 - Record architecture decisions with ADRs

- Status: Accepted
- Date: 2026-07-16

## Context

This project is a learning-focused resume project. Beyond the working app, the decision-making process itself is a deliverable — it demonstrates engineering judgment to anyone reviewing the repo, and gives the author a record of *why* choices were made when revisiting the project later.

## Decision

We will record every notable technical or process decision (tech stack choices, hosting shape, auth strategy, branching strategy, etc.) as a short ADR in `docs/adr/`, numbered sequentially. Each ADR captures: context, the decision, and consequences/trade-offs — not just the choice itself.

## Consequences

- Every research spike in the planning roadmap produces an ADR, not just a Slack-style verbal decision.
- Future contributors (or future self) can see why an option was rejected, not just what was picked.
- Adds a small amount of overhead per decision; acceptable given the learning goals of this project.
