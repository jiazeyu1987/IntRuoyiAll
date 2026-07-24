# Task: MES 生产工单同步编码改用 ERP 单据编号

## Goal

Make MES production work orders created from Kingdee ERP synchronization use the ERP production order bill number as the persisted work-order code.

## Scope

- Check the latest backend task status before starting this work.
- Create the task document and execution log before editing production code.
- Add or adjust backend regression tests first so the expected code mapping fails before the implementation change.
- Apply the minimal backend change only in the Kingdee production work-order sync path.
- Do not change manual work-order creation rules or introduce fallback code paths.

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-mes-item-sync-artifact-cleanup/task.md`
- Status before this task: completed.
- Impact: no blocker from the previous backend task.

## Milestones

- [x] M1: Confirm the previous backend task is completed and create this task document.
- [x] M2: Record BDD and RED evidence for the sync-code mapping regression test.
- [x] M3: Implement the minimal backend sync change so work-order code uses ERP bill number.
- [x] M4: Complete GREEN backend verification.
- [x] M5: Update evidence and create a scoped backend commit.

## Expected Verification

- `mvn --% -pl yudao-module-mes -Dtest=MesKingdeeProductionOrderSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed. The Kingdee production work-order sync path now persists ERP `billNo` as the MES work-order code, and the regression test suite passes.

## Blocker And Impact

- Blocker: none.
- Impact: newly synced ERP production work orders now align their persisted MES `code` with the ERP production order bill number.

## Final Verification Result

- `mvn --% -pl yudao-module-mes -am -Dtest=MesKingdeeProductionOrderSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- Verified behavior:
  - RED captured the old generated value `KDMO-310119-1558165540`.
  - GREEN confirmed `MesKingdeeProductionOrderSyncServiceImplTest` passes with `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`.
