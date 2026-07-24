# Task: DCC Controlled File Unhandled API Error

## Goal

Fix the unhandled promise errors triggered by `getFileCategoryList()` and `getDirectoryTree()` in the DCC controlled-file frontend, and make the affected pages fail fast with explicit blocker information instead of throwing uncaught runtime errors.

## Milestones

- [x] M1: Check the latest frontend task in this repository and explicitly block it before switching scope.
- [x] M2: Create this bug task document before production code changes.
- [x] M3: Reproduce the live controlled-file API failures and identify whether the issue is missing backend routes, response-shape mismatch, or missing frontend error handling.
- [x] M4: Add a failing regression test for the unhandled error behavior.
- [x] M5: Implement the minimal frontend fix so the pages surface clear error state instead of uncaught promise crashes.
- [x] M6: Run targeted verification and record RED/GREEN evidence.

## Expected Verification

- The affected controlled-file pages no longer emit uncaught promise errors for these API failures.
- The UI shows a clear failure state describing the missing prerequisite or backend error.
- Regression coverage proves the failed behavior before the fix and the handled behavior after the fix.

## Current Status

Completed. The live backend endpoints currently return `code=0`, so the current reproducible root cause is not a response-shape mismatch in the active backend. The frontend defect was that the `approval-tasks` and `directories` pages allowed startup request failures to escape as uncaught promises instead of surfacing page-level blocker state.

## Final Verification Result

- `node --test scripts/dcc-controlled-file-load-error.test.mjs` -> PASS
- `pnpm exec eslint scripts/dcc-controlled-file-load-error.test.mjs src/views/dcc/controlled-file/shared/utils.ts src/views/dcc/controlled-file/approval-tasks/index.vue src/views/dcc/controlled-file/directories/index.vue` -> PASS
- `pnpm build:local` -> PASS

## Completion Status

Completed.
