# Task: Merge Showroom E2E Frontend Worktree Into int_main

## Goal

Fast-forward merge the current Showroom Phase 1 E2E frontend worktree branch into the frontend `int_main` branch so local `http://localhost:8081` can serve the Showroom routes from the main frontend workspace.

## Scope

- Merge branch `codex/showroom-phase1-e2e-cases` into `int_main`.
- Preserve existing unrelated local modifications in the main frontend workspace.
- Verify the Showroom route files and E2E scripts are present after merge.
- Run focused frontend regression commands that do not require unavailable real E2E accounts.

## Non-Scope

- Do not modify or commit unrelated dirty files already present in `int_main`.
- Do not run real browser E2E with mock accounts or fake data.
- Do not delete the source worktree unless explicitly requested after merge verification.

## Milestones

- [x] M1: Inspect main frontend dirty state and source worktree commit.
- [x] M2: Confirm merge diff does not overlap existing modified files.
- [x] M3: Fast-forward merge `codex/showroom-phase1-e2e-cases` into `int_main`.
- [x] M4: Run focused post-merge verification.
- [x] M5: Commit this merge task record only if verification passes without touching unrelated files.

## Expected Verification

- `git merge --ff-only codex/showroom-phase1-e2e-cases`
- `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs`
- `node scripts/run-showroom-phase1-e2e.mjs --dry-run`
- `git status --short --branch` confirms only pre-existing unrelated dirty files plus this task record remain uncommitted.

## Current Status

Completed. Branch `codex/showroom-phase1-e2e-cases` was fast-forward merged into `int_main`, Showroom route contracts passed, and unrelated local modifications were preserved.

## Final Verification Result

- PASS: `git merge --ff-only codex/showroom-phase1-e2e-cases`.
- PASS: `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs` -> 12 passed.
- PASS: `node scripts/run-showroom-phase1-e2e.mjs --dry-run`.
- PASS: route grep confirms `showroomAdminRoutes` and `showroomFrontstageRoutes` are imported by `src/router/modules/remaining.ts`.

## Cleanup Keep

- `doc/tasks/20260519-merge-showroom-e2e-frontend-int-main/task.md`
- `doc/tasks/20260519-merge-showroom-e2e-frontend-int-main/execution-log.md`
