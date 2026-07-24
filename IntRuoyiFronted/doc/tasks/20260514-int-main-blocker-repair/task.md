# Task: Repair int_main frontend blocker

## Goal

Repair the current `int_main` frontend blocker that prevents the active MES
page work on this branch from completing static or runtime verification.

## Scope

- Frontend repository only.
- Reproduce the current `int_main` frontend build, lint, or runtime failure.
- Isolate the minimal frontend code issue that causes the failure.
- Implement the smallest fix without fallback behavior.
- Re-run focused frontend verification for the affected MES page slice.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260514-electronic-batch-record-doc-report-implementation/task.md`
- Status before this task: completed for the frontend slice.
- Impact: the electronic batch-record frontend is available in source, but any
  remaining `int_main` frontend blocker must be resolved before the branch can
  be treated as verified.

## Milestones

- [x] F1: Check the latest frontend task status before starting new frontend work.
- [x] F2: Create this frontend task document before production code changes.
- [x] F3: Record BDD scenarios and RED evidence for the current frontend blocker.
- [x] F4: Investigate the reproduced frontend blocker and confirm whether an owned-file fix is required.
- [x] F5: Run focused frontend verification and update evidence.

## Expected Verification

- `pnpm exec eslint src/views/mes/pro/batchrecordtemplate/index.vue src/views/mes/pro/batchrecordtemplate/DesignerWrapper.vue src/api/mes/pro/batchrecordreport/index.ts`
- `node scripts/electronic-batch-record-report-page.test.mjs`

## Current Status

Completed for the scoped batch-record-report frontend verification path. The
owned `batchrecordreport` page files pass focused lint and regression checks;
the remaining `vue-tsc` failures are repository-wide pre-existing TypeScript
issues in unrelated BPM, mall, pay, and system modules.
