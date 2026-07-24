# Task: Commit DCC controlled-file frontend surface

## Goal

Review, verify, and commit the discovered DCC controlled-file frontend source surface so it stops lingering as an untracked residual in the frontend repository.

## Scope

- `src/api/dcc/controlledFile/**`
- `src/views/dcc/controlled-file/**`
- `src/router/modules/remaining.ts`
- `scripts/check-dcc-controlled-file-entrypoints.mjs`
- Task evidence under `doc/tasks/20260514-dcc-controlled-file-frontend-surface/`

## Milestones

- [x] M1: Previous frontend cleanup task is explicitly blocked before this feature task starts.
- [x] M2: Create this frontend feature task document before staging DCC source files.
- [x] M3: Record BDD and RED evidence for the discovered DCC source surface.
- [x] M4: Verify the DCC controlled-file source surface with targeted frontend checks.
- [x] M5: Update final status and prepare the scoped frontend commit if verification passes.

## Expected Verification

- `node scripts/check-dcc-controlled-file-entrypoints.mjs` passes.
- Targeted frontend lint passes for the DCC controlled-file source surface.
- `remaining.ts` contains the hidden controlled-file detail route entrypoint.

## Current Status

Completed. The discovered DCC controlled-file frontend source surface is verified and ready for a scoped frontend commit.

## Blocker And Impact

- Blocker: none currently discovered.
- Impact: until this task is committed, the frontend worktree remains dirty with a real DCC feature surface.

## Final Verification Result

- `node scripts/check-dcc-controlled-file-entrypoints.mjs`
- `pnpm exec eslint "src/router/modules/remaining.ts" "src/api/dcc/controlledFile/**/*.ts" "src/views/dcc/controlled-file/**/*.ts" "src/views/dcc/controlled-file/**/*.vue" "scripts/check-dcc-controlled-file-entrypoints.mjs"`
- `git diff --check`
