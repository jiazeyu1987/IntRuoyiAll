# Task: Showroom Frontstage Shared Foundation

## Goal

Implement `F1 Shared Foundation` for showroom frontstage by extracting reusable domain logic out of the monolithic frontstage page while keeping current behavior stable.

## Scope

- Work only in showroom frontstage shared-domain partitions.
- Extract shared display types, shared constants, payload normalization helpers, narration helpers, and route-resolution helpers.
- Keep contextual frontstage routes from exposing broken direct-entry menu items when required route context is missing.
- Update the current frontstage page to consume the new shared foundation.
- Add task-local tests for the shared foundation.

## Non-Scope

- Do not implement `screen/pad/mobile` visual shells yet.
- Do not redesign the current frontstage UI in this task.
- Do not change back-office showroom behavior.
- Do not change backend API contracts.

## Previous Task Check

- Previous related planning task: `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260519-showroom-frontstage-delivery-slices\task.md`
- Status before this task: completed.
- Impact: F1 ownership, dependency order, and write boundaries are already defined and approved for execution.

## Repository Status Check

- Branch: `int_main`
- Unrelated dirty files exist in MES paths and unrelated task docs.
- Impact: this task must avoid touching or staging unrelated files.

## Milestones

- [x] M1: Create task record, confirm scope, and inspect current frontstage implementation.
- [x] M2: Add RED shared-foundation tests.
- [x] M3: Extract shared frontstage foundation modules, keep contextual detail routes out of the sidebar, and update the page to use them.
- [x] M4: Run GREEN tests and record verification evidence.
- [x] M5: Commit only F1-related files.

## Expected Verification

- `node --test scripts/showroom-frontstage-shared.test.mjs scripts/showroom-frontstage.test.mjs`
- `pnpm exec eslint src/router/modules/showroom.ts scripts/showroom-frontstage.test.mjs scripts/showroom-frontstage-shared.test.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-frontstage-shared-foundation\bug-regression-evidence.md`

## Current Status

Completed. The current `int_main` branch already contains the expected F1 shared-foundation production code; this task verified that implementation and updated the task-scoped evidence.

## Verification Result

- PASS: `node --test scripts/showroom-frontstage-shared.test.mjs scripts/showroom-frontstage.test.mjs`
- PASS: `pnpm exec eslint src/router/modules/showroom.ts scripts/showroom-frontstage.test.mjs scripts/showroom-frontstage-shared.test.mjs`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-frontstage-shared-foundation\bug-regression-evidence.md`

## Implementation Note

- The showroom frontstage shared-foundation production files already matched the expected F1 code shape on the current branch.
- This task turn therefore focused on verification, regression evidence, and task closeout records rather than introducing a new production diff.

## Remaining Blockers

- None.

## Cleanup Keep

- `doc/tasks/20260519-showroom-frontstage-shared-foundation/task.md`
- `doc/tasks/20260519-showroom-frontstage-shared-foundation/execution-log.md`
- `doc/tasks/20260519-showroom-frontstage-shared-foundation/bug-regression-evidence.md`
