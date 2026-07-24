# Task: Material ERP BOM garbled error message fix

## Goal

Fix the newly added product-level ERP BOM sync error messages so clicking `从ERP同步` returns readable Chinese failure text instead of mojibake when local child-item mappings are missing.

## Scope

- Confirm the latest same-repository backend task is explicitly completed or blocked before starting this follow-up.
- Create this backend task package before production code changes.
- Record BDD scenarios and RED verification for readable ERP BOM error text.
- Fix only the newly introduced product-level ERP BOM sync message literals and directly related operator-facing text.
- Do not change the ERP BOM sync control flow, fallback behavior, or unrelated MES error messages.

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-md-item-erp-bom-sync-button/task.md`
- Status before this task: completed.
- Impact: the previous product-level ERP BOM feature delivery is closed, so this targeted message-fix follow-up can proceed independently.

## Milestones

- [x] M1: Create this backend follow-up task package after confirming the previous task is closed.
- [x] M2: Record BDD scenarios and add RED verification for readable Chinese error messages.
- [x] M3: Implement the minimal message-literal fix.
- [x] M4: Run targeted verification and update evidence.
- [x] M5: Preview closeout artifacts and prepare a task-scoped backend commit.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesKingdeeProductBomSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260518-md-item-erp-bom-garbled-error-message-fix/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260518-md-item-erp-bom-garbled-error-message-fix --mode preview`

## Current Status

Completed for code delivery and verification. The follow-up literal fix, targeted test pass, evidence validation, and closeout preview are complete.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesKingdeeProductBomSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS

## Blocker And Impact

- None currently.
