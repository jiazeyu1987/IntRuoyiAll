# Task: Material edit dialog ERP BOM sync button

## Goal

Add a product-level ERP BOM replacement endpoint so users editing one MES material / product can sync the latest approved ERP BOM for that item and replace the current local BOM rows.

## Scope

- Block the previous same-repository backend task before starting this work.
- Record BDD scenarios and strict TDD evidence for product-level ERP BOM replacement.
- Add the dedicated backend endpoint under `mes/md/product-bom`.
- Reuse `ErpKingdeeBomClient` and fail fast on missing item code, missing ERP BOM, multiple approved versions, missing local child items, or recursive child BOMs.
- Replace only the current product's `mes_md_product_bom` rows.

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-workorder-row-freeze-toggle-action/task.md`
- Status before this task: blocked by user priority switch.
- Impact: the paused work-order row-freeze backend task remained isolated and did not block this ERP BOM sync slice.

## Milestones

- [x] M1: Block the previous same-repository backend task and create this task package first.
- [x] M2: Record BDD scenarios and RED verification for product-level ERP BOM replacement.
- [x] M3: Implement the minimal backend sync endpoint, service, result VO, and error mapping.
- [x] M4: Run targeted backend verification and update evidence.
- [x] M5: Preview closeout artifacts and prepare a task-scoped backend commit.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesKingdeeProductBomSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260518-md-item-erp-bom-sync-button/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260518-md-item-erp-bom-sync-button --mode preview`

## Current Status

Completed. Backend implementation, targeted verification, evidence validation, and closeout preview are complete.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesKingdeeProductBomSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260518-md-item-erp-bom-sync-button/backend-api-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260518-md-item-erp-bom-sync-button --mode preview` -> PASS

## Blocker And Impact

- None currently.
