# Task: Kingdee Material Sync Page Until Empty

## Goal

Fix the Kingdee `BD_MATERIAL` sync so it no longer stops after the first configured total cap and instead keeps paging until the API returns an empty page, allowing PTCA materials in later pages to reach local ERP and MES sync results.

## Scope

- Change only the Kingdee material query client in `yudao-module-erp` and its direct regression tests.
- Keep the current login flow, filter string, sort order, and field mapping unchanged.
- Reproduce the bug with a failing regression test before the production fix.
- Update the task log and bug regression evidence for this repair.
- Do not touch the unrelated in-progress DCC files in the current workspace.

## Previous Task Check

- Latest repo task found before this task: `doc/tasks/20260515-dcc-file-category-import-from-intauth/task.md`
- Status observed before this task: in progress, with dirty files limited to `yudao-module-dcc`.
- Handling for this task: this repair remains limited to `yudao-module-erp` plus this task directory, so it does not modify the DCC task files.

## Milestones

- [x] M1: Check the latest repo task state and confirm this task boundary.
- [x] M2: Create the task document and execution log before production code changes.
- [x] M3: Record the BDD scenario and add a RED regression test.
- [x] M4: Change Kingdee material sync to keep paging until an empty page.
- [x] M5: Run target tests, regression verification, and evidence updates.
- [x] M6: Update final task status and create the scoped commit.

## Expected Verification

- Regression tests prove the material client keeps requesting later pages until an empty page is returned.
- The target tests pass and the sync is no longer limited by the old fixed `5000` total cap behavior.
- The execution log contains BDD, RED, and GREEN evidence.

## Current Status

Completed. The material client now pages `BD_MATERIAL` until the API returns an empty page, the regression evidence was validated, and the task is ready for its scoped Git commit.

## Final Verification Result

- `mvn -pl yudao-module-erp -Dtest=ErpKingdeeMaterialClientImplTest test` -> PASS
- `mvn -pl yudao-module-erp "-Dtest=ErpKingdeeMaterialClientImplTest,ErpKingdeeProductSyncServiceImplTest" test` -> PASS
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260515-kingdee-material-sync-page-until-empty\bug-regression-evidence.md` -> PASS

## Blocker And Impact

- Blocker: unrelated DCC changes are already present in the same workspace.
- Impact: this task must stay confined to ERP files and its own task documents to avoid crossing into the active DCC work.
