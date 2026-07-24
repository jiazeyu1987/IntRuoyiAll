# Task: DCC Category Directory Binding Duplicate Fix

## Goal

Fix the DCC category-directory binding save path so rebinding an existing category to the same directory no longer crashes with `Duplicate entry ... uk_dcc_category_directory_binding`.

## Scope

- Check the previous task state in this repository before changing code.
- Create this task package before production code changes.
- Record BDD scenarios and strict TDD evidence for the duplicate-binding failure.
- Change only the DCC category-directory binding backend path and focused regression tests needed for this bug.
- Do not add fallback branches, silent duplicate swallowing, or unrelated schema changes in this task.

## Previous Task Check

- Previous repository task reviewed: `doc/tasks/20260516-electronic-batch-record-image-routef-calibration-fix/task.md`
- Repository-wide blocked task noted: `doc/tasks/20260516-electronic-batch-record-image-performance-optimization/task.md`
- Status before this task:
  - the Route F calibrator task is completed;
  - the performance-optimization task remains explicitly blocked by unrelated MES compile issues.
- Impact:
  - those blocked MES issues are outside the DCC category binding slice, so this task stays scoped to `yudao-module-dcc`.

## BDD Scenarios

BDD: Rebinding the same category to the same directory stays idempotent -> Given a category is already bound to a directory, When the admin saves the same directory binding again, Then the backend updates the binding state without throwing a duplicate-key database error.

BDD: Rebinding after prior logical deletions can restore the binding -> Given the category binding table already contains a historical row for the same category-directory pair, When the admin binds that category back to the same directory, Then the backend finishes successfully and leaves one active binding for that pair.

## Milestones

- [x] M1: Review previous task state and create this task package.
- [x] M2: Add a RED regression test that reproduces the duplicate binding failure.
- [x] M3: Implement the minimal backend fix for category-directory rebinding.
- [x] M4: Run focused verification and update task evidence.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -am -Dtest=DccFileCategoryAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -am -Dtest=DccFileCategoryAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
