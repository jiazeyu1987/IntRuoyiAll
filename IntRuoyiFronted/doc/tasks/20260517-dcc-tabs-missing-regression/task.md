# Task: DCC Tabs Missing Regression

## Goal

Restore the visible `DCC下发` and `DCC培训` entry points so they are available
from the DCC governance area again, without changing the existing business
contracts for distribution or training.

## Scope

- Reproduce the missing-tab symptom in the local frontend.
- Identify whether the break is caused by routing, menu metadata, or UI
  rendering conditions.
- Add a regression test or Playwright check that proves the tabs are visible
  again.
- Keep unrelated dirty files and unrelated DCC behavior out of scope.
- Do not add fallback behavior.

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260516-dcc-training-closed-loop/task.md`
- Status before this task: completed and committed.

## Milestones

- [x] M1: Reproduce the missing-tab symptom and record the failing path.
- [x] M2: Add a regression test that fails before the fix.
- [x] M3: Implement the smallest frontend fix.
- [x] M4: Verify the tabs are visible again with real UI evidence.
- [x] M5: Commit only task-scoped files after verification passes.

## Expected Verification

- Playwright regression for the DCC governance sidebar entries
- Runtime menu repair evidence for `system_menu` and role-menu assignment

## Cleanup Keep

- doc/tasks/20260517-dcc-tabs-missing-regression/bug-regression-evidence.md
- doc/tasks/20260517-dcc-tabs-missing-regression/scripts/inspect-dcc-menu.mjs
- doc/tasks/20260517-dcc-tabs-missing-regression/scripts/repair-dcc-governance-menus.mjs

## Current Status

Completed in the local runtime. The DCC governance sidebar now shows
`DCC下发`, `DCC培训`, and `DCC我的培训` again after repairing the missing and
corrupted `system_menu` rows through a UTF-8 repair script and reassigning the
admin role menu set.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-tabs-missing-regression run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-tabs-missing-regression\scripts\inspect-dcc-menu.mjs`
  -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-tabs-missing-regression\scripts\repair-dcc-governance-menus.mjs`
  -> PASS
- Real runtime result:
  - `controlled-file/distribution` menu restored as `DCC下发`
  - `controlled-file/training` menu restored as `DCC培训`
  - `controlled-file/training-mine` label repaired to `DCC我的培训`
