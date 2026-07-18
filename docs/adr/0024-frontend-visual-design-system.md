# 0024 - Frontend visual design system: dark-only monochrome, IBM Plex, shared UI components

- Status: Accepted
- Date: 2026-07-17

## Context

`frontend-architecture.md` (ADR 0013) already locked in Tailwind CSS as the styling approach, but left the actual visual identity — color palette, typography, light/dark support, and how consistency gets enforced as more screens get built — undecided. This came up directly while starting US-1's register form, the first real UI screen. Working through it live (visual-direction mockups, then two rounds of revision) produced concrete decisions worth recording before more screens are built against them.

## Options considered

### Visual direction
- **Dense trading terminal** (chosen) - dark, information-dense, monospace for data — fits a trading simulator's subject matter directly, distinct from a generic SaaS dashboard look.
- Minimal fintech (Stripe/Mercury-like) and friendly/approachable dashboard were also mocked up as directions; trading-terminal was preferred.

### Color mode
- **Dark only** (chosen) - one mode to keep consistent across every future screen; no light-theme maintenance burden for a solo project. Committing to a single visual world is an accepted, deliberate trade-off (not planned to add a light mode later).
- Light-only and light+dark toggle were considered and rejected — toggle in particular adds real ongoing cost (every component and semantic color has to work in both modes).

### Accent color
- **No decorative accent — background/surface greys plus white text only** (chosen, revised from an initial amber accent). Semantic colors for gains/losses and error states (green/red) are kept, but explicitly scoped as functional data signal, not brand color — the two are documented as separate concerns in the token reference.

### Typography
- **IBM Plex Sans (UI text, headings) + IBM Plex Mono (numbers, tickers, labels)** (chosen) - same type family, designed to pair; Plex's engineering/instrument-panel heritage fits a trading tool. Self-hosted as static `.woff2` files under `frontend/public/fonts/` (with the IBM OFL license text alongside, per the license's redistribution requirement) rather than linked from a font CDN — no third-party request on every page load, no FOUC risk from an external host being slow/down.

### Component consistency strategy
- **Small shared UI component set, built up front** (chosen) - `Button`, `Input`, `Card` in `frontend/src/app/shared/ui/`, styled once against the design tokens. Every feature composes from these instead of repeating Tailwind utility strings per screen.
- Raw Tailwind utilities per component (extract shared pieces later only if duplication shows up) was considered and rejected — "clean and consistent" is much harder to guarantee retroactively once several screens already exist with copy-pasted utility strings.

## Decision

- Dark-only, monochrome UI: background/surface grays (`--color-bg`, `--color-surface`, `--color-surface-raised`) plus white/gray text tokens (`--color-text`, `--color-text-muted`, `--color-text-faint`). No decorative accent color.
- Semantic-only color: `--color-success` / `--color-danger` reserved for gains/losses and validation errors — never used decoratively.
- Design tokens live as Tailwind v4 `@theme` custom properties in `frontend/src/styles.css` (colors, `--font-sans`, `--font-mono`, `--radius-sm/md/lg`), so both Tailwind utility classes (`bg-surface`, `font-mono`, etc.) and plain `var(--color-*)` CSS in component stylesheets stay backed by the same source of truth.
- IBM Plex Sans + IBM Plex Mono, self-hosted under `public/fonts/`.
- Primary button styling is a bordered/outline treatment (transparent background, white border and text) that inverts to solid white-on-dark on hover — not a filled button — consistent with the "no accent, monochrome" decision; a dimmer bordered variant serves as the secondary/lower-emphasis style.
- Shared components (`Button`, `Input`, `Card`) live in `frontend/src/app/shared/ui/`, each generated via Angular CLI schematics to match the project's existing file-naming convention (no `.component` suffix). `Input` implements `ControlValueAccessor` so it drops directly into Angular Reactive Forms (`formControlName`) like a native input.

## Consequences

- Every future screen (login, portfolio, trade, transactions) should compose from the same `shared/ui` components and `--color-*`/`--font-*`/`--radius-*` tokens rather than introducing new one-off colors or hand-styled buttons/inputs — the register screen is the first consumer, not a one-off.
- Adding a light mode later would be a deliberate, larger revisit (new token set, contrast re-check across every component) — not a small toggle, given the dark-only commitment above.
- New semantic colors (e.g., a "warning" state) should follow the same success/danger pattern — functional, not decorative — if one is ever needed.
- Self-hosted fonts mean any weight/family change requires re-downloading and re-inlining `.woff2` files (see the register-form implementation session for the exact process: Google Fonts CSS2 API → extract the `latin`-subset `.woff2` URLs → download → place under `public/fonts/`), rather than editing a CDN `<link>`.
