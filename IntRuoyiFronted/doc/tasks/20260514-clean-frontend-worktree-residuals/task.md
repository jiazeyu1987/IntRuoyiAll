# Task: Clean frontend worktree residuals

## Goal

Clean the remaining frontend worktree residuals by ignoring generated `output/` artifacts, clearing the false-positive `MobileForm.vue` status noise, and deciding the correct repository fate of the untracked DCC entrypoint checker script.

## Scope

- Inspect the remaining frontend worktree residuals after the previous scoped commits.
- Ignore generated frontend `output/` artifacts in the frontend repository.
- Preserve the DCC controlled-file entrypoint checker as a tracked utility if it passes verification.
- Resolve the false-positive `src/views/Login/components/MobileForm.vue` modified state without changing behavior.

## Milestones

- [x] M1: Review the latest frontend task state before starting this cleanup task.
- [x] M2: Create this cleanup task document before editing repository files.
- [x] M3: Record BDD and RED evidence for the current frontend residual state.
- [x] M4: Update frontend ignore rules for generated `output/` artifacts.
- [x] M5: Verify and record the DCC controlled-file entrypoint checker script.
- [ ] M6: Resolve the residual `MobileForm.vue` status noise and verify the frontend worktree state.

## Expected Verification

- `git status --short` in `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` no longer reports generated `output/`.
- `node scripts/check-dcc-controlled-file-entrypoints.mjs` passes.
- `src/views/Login/components/MobileForm.vue` no longer appears as a false-positive modified file.

## Current Status

Blocked. Generated `output/` artifacts are now ignored, `MobileForm.vue` status noise is cleared, and the DCC entrypoint checker passes, but the frontend worktree still contains a discovered untracked DCC controlled-file source surface that must be handled as its own feature task.

## Blocker And Impact

- Blocker: the frontend repository contains a real untracked DCC controlled-file feature surface under `src/api/dcc/`, `src/views/dcc/`, `src/router/modules/remaining.ts`, and `scripts/`, which is beyond this cleanup task's hygiene-only scope.
- Impact: the frontend worktree cannot be fully clean until that DCC feature surface is reviewed and committed or explicitly blocked under a dedicated task.

## Partial Verification Result

- `git status --short output` no longer reports generated frontend `output/`.
- `node scripts/check-dcc-controlled-file-entrypoints.mjs` passes.
- `src/views/Login/components/MobileForm.vue` no longer appears in `git status --short`.
