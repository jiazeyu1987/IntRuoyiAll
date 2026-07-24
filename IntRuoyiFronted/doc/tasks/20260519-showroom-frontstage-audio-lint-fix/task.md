# Task: Fix Showroom Frontstage Audio Lint Error

## Goal

Fix the Vite ESLint blocker in `src/views/showroom-frontstage/index.vue` where the native `<audio>` element is self-closing.

## Scope

- Replace the native self-closing `<audio />` element with an explicit closing tag.
- Run focused syntax/lint contract verification for the Showroom frontstage slice.
- Preserve unrelated local modifications already present in the frontend workspace.

## Non-Scope

- Do not modify Showroom business behavior.
- Do not touch unrelated AI, MES, DCC, or task artifact files.

## Milestones

- [x] M1: Record the reported ESLint error.
- [x] M2: Apply the minimal Vue template fix.
- [x] M3: Run focused verification.
- [x] M4: Commit only the task-scoped fix and task record.

## Expected Verification

- `pnpm exec eslint src/views/showroom-frontstage/index.vue`
- `node --test scripts/showroom-frontstage.test.mjs`

## Current Status

Completed.

## Final Verification Result

- PASS: `pnpm exec eslint src/views/showroom-frontstage/index.vue`
- PASS: `node --test scripts/showroom-frontstage.test.mjs`

## Cleanup Keep

- `doc/tasks/20260519-showroom-frontstage-audio-lint-fix/task.md`
- `doc/tasks/20260519-showroom-frontstage-audio-lint-fix/execution-log.md`
- `src/views/showroom-frontstage/index.vue`
