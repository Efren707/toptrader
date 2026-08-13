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

## Decision

Preset avatar picker. New nullable `avatar_key VARCHAR` column on `users` (null = fall back to a default icon in the UI). The valid set of keys is a small fixed list shipped with the frontend as static assets (e.g. `frontend/src/assets/avatars/`); the backend only stores and returns the key string, it does not validate against an enum of assets server-side beyond basic length/format checks — the frontend owns the actual icon set.

## Consequences

- Cheap to build and test: a finite, enumerable set of values, no file storage, no upload edge cases (wrong file type, oversized image, malicious payload).
- Less personalization than a real uploaded photo — accepted trade-off given the effort/infra cost gap.
- If real image upload is wanted later, it can be layered on without breaking this: `avatar_key` could stay as the "preset" path while a separate nullable `avatar_url` column (or a reinterpretation of the same column) is added for uploaded images — that would be its own ADR when/if it's actually needed.
