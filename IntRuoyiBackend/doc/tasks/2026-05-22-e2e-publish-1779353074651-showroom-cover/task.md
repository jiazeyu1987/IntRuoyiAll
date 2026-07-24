# Task: E2E-PUBLISH-1779353074651 Showroom Cover Image

## Goal

Generate one square premium medical-device showroom cover image for product `E2E-PUBLISH-1779353074651` using only the provided product facts.

## Milestones

1. Create task records.
2. Generate one square PNG with native image generation.
3. Copy the final PNG into the workspace and record completion status.

## Expected Verification

- Exactly one native image-generation call is used.
- Output is a local PNG file in the workspace.
- Prompt uses only provided product facts and avoids prohibited text, branding, and fabricated device claims.

## Current Status

- Completed: Milestones 1-3
- Pending: None
- Blockers: None

## Verification Evidence

- Task directory created before generation.
- Native image generation used exactly once.
- Output PNG: `C:\Users\BJB110\.codex\generated_images\019e4dbb-99c6-78e2-8ccc-b70dd52e4920\ig_00ea9929af081177016a0fce11a0b48191b960d32d082a08a3.png`
- Verified file format `PNG` and square dimensions `1254x1254`.
