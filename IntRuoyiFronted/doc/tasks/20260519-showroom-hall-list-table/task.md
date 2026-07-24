# Task: Showroom Hall List Table

## Goal

Implement an isolated `HallListTable` component for `/showroom/hall` so the hall management page can render real hall rows with manual numeric ordering and a hall-product mapping launcher.

## Scope

- Add `src/views/showroom-admin/components/HallListTable.vue`.
- Add task-local test coverage in `scripts/showroom-admin-hall-list.test.mjs`.
- Record BDD and strict TDD evidence in this task directory.
- Keep integration into `src/views/showroom-admin/index.vue` out of scope for the main agent.

## Non-Scope

- Do not modify `src/views/showroom-admin/index.vue`.
- Do not modify showroom frontstage files.
- Do not implement drag sorting, cover image management, recommendation slots, or online/offline actions.
- Do not create mock hall data inside the component.

## Previous Task Check

- Previous task checked: `doc/tasks/20260519-showroom-frontstage-shared-foundation/task.md`.
- Status before this task: completed.
- Impact: no previous-task blocker for this isolated worktree task.

## Milestones

- [x] M1: Create isolated worktree and confirm previous task status.
- [x] M2: Create task record and BDD scenario.
- [x] M3: Add RED hall-list test proving the component is missing.
- [x] M4: Implement `HallListTable` with real input rows, explicit normalization, required columns, and no forbidden controls.
- [x] M5: Run GREEN node test and ESLint from the main workspace dependencies.
- [x] M6: Run closeout cleanup preview and commit only task-owned files.

## Expected Verification

- `D:\Programs\node.exe --test scripts/showroom-admin-hall-list.test.mjs`
- `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/components/HallListTable.vue scripts/showroom-admin-hall-list.test.mjs`

## Current Status

Completed.

## Verification Result

- RED: `D:\Programs\node.exe --test scripts/showroom-admin-hall-list.test.mjs` -> FAIL, `HallListTable.vue` was missing.
- GREEN: `D:\Programs\node.exe --test scripts/showroom-admin-hall-list.test.mjs` -> PASS.
- GREEN: `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/components/HallListTable.vue scripts/showroom-admin-hall-list.test.mjs` -> PASS.
- CLEANUP PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-showroom-hall-list-table --mode preview --worktree-closeout off` -> READY, delete none.
- REVIEW ROUND 1 RED: `D:\Programs\node.exe --test scripts/showroom-admin-hall-list.test.mjs` -> FAIL, updated test required real `ShowroomHall` contract fields and rejected unavailable raw `status/updateTime/productCount/displayOrder` hall fields.
- REVIEW ROUND 1 GREEN: `D:\Programs\node.exe --test scripts/showroom-admin-hall-list.test.mjs` -> PASS.
- REVIEW ROUND 1 GREEN: `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/components/HallListTable.vue scripts/showroom-admin-hall-list.test.mjs` -> PASS.

## Remaining Blockers

- Main agent must integrate `HallListTable` into `src/views/showroom-admin/index.vue` later because this worker intentionally does not touch that file.
- Full worktree closeout apply was not run because this worker must preserve the requested worktree path and the shared main workspace has unrelated dirty changes.
- Backend API gap: current public controller `ShowroomHall` does not expose hall-level `displayOrder`, `status`, or `updateTime`; the component now renders only truthful current-contract columns and derives product count plus mapping order details from `productMappings`.

## Cleanup Keep

- `doc/tasks/20260519-showroom-hall-list-table/task.md`
- `doc/tasks/20260519-showroom-hall-list-table/execution-log.md`
