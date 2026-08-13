# 0046 - Profile avatar: preset picker over image upload

- Status: Accepted
- Date: 2026-08-12

## Context

`docs/tasks/planning/user-profile-management.md` left the avatar's image source undecided: uploaded image, generated initials, or a fixed default. `docs/architecture/data-model.md` had already flagged this ahead of time in its "carried forward / not yet built" section, anticipating an `avatar_key` column (or reference table) rather than committing to a mechanism.

Research into the current codebase found zero file-upload infrastructure anywhere: no `MultipartFile`/multipart handling on the backend, no file-input/image-picker component on the frontend. S3 is already provisioned (`docs/architecture/deployment-architecture.md`), but only as the static-website host for the built Angular bundle — the CI IAM role is scoped to `s3:PutObject`/`s3:DeleteObject` on that one frontend bucket only, not general user-content storage.

## Options considered

- **Uploaded image (S3-backed)** - rejected. Requires a new S3 bucket, a new IAM policy, a presigned-URL or backend-proxied upload flow, image validation/size limits, and a file-upload UI built from scratch (nothing to reuse on either side). Real new infrastructure and complexity for a personalization feature, disproportionate to where this project is right now.
- **Generated identicon/initials avatar** (e.g. a deterministic color + initials derived from the username, computed client-side) - considered as a zero-storage alternative. Not chosen: it removes user choice entirely, and the docs had already anticipated letting the user pick something.
- **Preset avatar picker** (chosen) - a small, fixed set of built-in icon assets shipped as static frontend assets. The user picks one; the choice is stored as a short string key on `users.avatar_key`. No new AWS infrastructure, no upload/validation/virus-scanning surface, no new IAM policy — matches the `avatar_key` column `data-model.md` already anticipated.

### Where the preset icon assets come from

Compared three sources for the actual artwork, weighing implementation effort, security/privacy surface, and licensing risk (this is a public, MIT-licensed repo):

- **A specific Figma Community file** ("3D Game Icons Pack") the user found - rejected for this pass. Figma Community files each carry publisher-set license terms that don't automatically grant export-and-redistribute rights outside Figma; that file's specific terms could not be verified (Figma blocked an automated fetch of the page), and committing SVGs of unverified license into a public repo is a real legal-exposure risk not worth taking when zero-risk alternatives exist. Could be revisited if the license is manually confirmed to permit it.
- **Hand-curated open illustrated-people set** (e.g. Open Peeps) - viable and low-risk (stated "free for personal and commercial use"), but more manual labor (browsing, hand-selecting, recoloring, exporting/optimizing each SVG individually) than a scripted alternative, for no clear benefit over DiceBear.
- **Emoji-based avatars** (`avatar_key` stores an emoji, rendered via CSS, no asset files at all) - the lowest-effort, zero-licensing-risk option, considered as a fallback. Not chosen for the primary path since it reads as a reaction icon rather than a personified avatar, and DiceBear delivers the "preset character" feel at comparable effort/risk.
- **DiceBear** (chosen) - open-source, purpose-built for exactly this "pick a preset avatar" flow. Core library is MIT; the styles under consideration (e.g. `avataaars`, `adventurer`, `bottts`) are MIT/CC0. A small fixed set (~12-16) is pre-generated once via the `@dicebear/core` + a collection package (dev-only dependency, a short local script), and the resulting static SVGs are committed to `frontend/src/assets/avatars/` — no runtime dependency on DiceBear, no network call in the shipped app.

## Decision

Preset avatar picker using **DiceBear**-generated SVGs. New nullable `avatar_key VARCHAR` column on `users` (null = fall back to a default icon in the UI). The valid set of keys is a small fixed list of pre-generated DiceBear SVGs shipped with the frontend as static assets (`frontend/src/assets/avatars/`); the backend only stores and returns the key string, it does not validate against an enum of assets server-side beyond basic length/format checks — the frontend owns the actual icon set. Assets are served via `<img src>`/static asset reference, never injected as raw inline HTML, so there's no SVG-as-XSS surface even though SVG can technically embed script.

## Consequences

- Cheap to build and test: a finite, enumerable set of values, no file storage, no upload edge cases (wrong file type, oversized image, malicious payload).
- Less personalization than a real uploaded photo — accepted trade-off given the effort/infra cost gap.
- No ongoing dependency: DiceBear is only used once, offline, to generate the static SVG files; the shipped app has no runtime dependency on it and makes no calls to any DiceBear service.
- If real image upload is wanted later, it can be layered on without breaking this: `avatar_key` could stay as the "preset" path while a separate nullable `avatar_url` column (or a reinterpretation of the same column) is added for uploaded images — that would be its own ADR when/if it's actually needed.
