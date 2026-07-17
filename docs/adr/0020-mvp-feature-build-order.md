# 0020 - MVP feature build order

- Status: Accepted
- Date: 2026-07-17

## Context

Planning (Phases 0-5) is complete: requirements, architecture, CI/CD, and user-facing docs are all decided. Before writing application code (Phase 6), the 9 MVP user stories (`docs/requirements/user-stories.md`, US-1..US-9) need a build order — implementation should follow real dependencies between stories rather than the arbitrary order they were written in, and this order drives how the GitHub Issues backlog and milestones get structured next.

## Options considered

- **Dependency-ordered groups** - group stories by what unlocks what (auth before trading, trading before reporting), building roughly bottom-up through the stack.
- **Vertical slices per story** - build each of the 9 stories as an independent end-to-end slice in written order (US-1 through US-9), without explicit grouping.

## Decision

Build in 4 dependency-ordered groups:

1. **Auth & Account Foundation** - US-1 (Register), US-3 (starting virtual cash, granted at registration), US-2 (Log in)
2. **Market Data Integration** - US-4 (look up a stock quote)
3. **Trading Core** - US-5 (Buy shares), US-6 (Sell shares)
4. **Portfolio & Reporting** - US-7 (view portfolio), US-8 (view transaction history), US-9 (view profit/loss)

Each group unlocks the next: you can't trade without an account and a price, and there's nothing to report on until trades exist.

## Consequences

- GitHub Issues/Milestones for Phase 6 onward will be organized around these 4 groups rather than one issue per user story in isolation.
- Group 1 bundles US-1/US-2/US-3 together rather than as separate milestones, since starting cash is a side effect of registration, not a separable feature.
- If a group turns out to be too large once broken into issues, it can be split further without changing this ordering.
