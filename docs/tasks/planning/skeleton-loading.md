# Skeleton Loading States

> Status: Not yet scoped. High-level backlog entry captured ahead of time — to be broken into GitHub issues, user stories, and acceptance criteria when picked up for implementation.

## Envisioned scope

- A general loading-state UX pattern: replace spinners with **skeleton placeholders** that mimic the shape of the content being loaded (e.g. a card-shaped gray block where a stock card will render).
- Cross-cutting rather than a single dedicated feature — intended to be applied broadly across pages/components, likely as they're touched rather than in one dedicated pass.
- Open questions for scoping time: which components get skeletons first, whether to build a shared reusable Angular skeleton component/directive versus one-off per component.
