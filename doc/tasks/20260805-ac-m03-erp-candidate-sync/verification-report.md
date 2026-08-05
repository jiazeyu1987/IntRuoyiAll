# Verification Report

## Scope

- AC-M03 backend/source proof for ERP candidate synchronization idempotency.
- Production order sync formal source key behavior.
- Existing active-order transfer and batch trace idempotency behavior.

## Results

- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesKingdeeProductionOrderSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS detail: 20 tests run, 0 failures, 0 errors.
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceServiceTest,MesActiveOrderTransferTraceSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS detail: 5 tests run, 0 failures, 0 errors.
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260805-ac-m03-erp-candidate-sync/backend-api-evidence.md`
- PASS: `git diff --check -- <AC-M03 touched files>`
- PASS: UTF-8 readback for all task Markdown files.

## RED Evidence

- Before implementation, `syncWorkOrders_usesSourceRecordWorkOrderWhenBillNoChanges` failed because `createdCount` was `1` instead of `0`.
- Before implementation, full `MesKingdeeProductionOrderSyncServiceImplTest` failed with 2 AC-M03 failures: duplicate order fact risk and missing conflict fail-fast behavior.

## GREEN Evidence

- ERP order sync now reuses source-linked work order when `billNo` changes.
- ERP order sync now fails fast when a source-linked record conflicts with another work order already owning the incoming `billNo`.
- Transfer/batch trace tests still prove existing idempotency key behavior and schema binding.

## Blockers

- AC-M03 is not fully accepted yet. M6 real E2E/result ledger coverage remains open for the complete matrix acceptance gate.
- No commit/push closeout performed in this run because the workspace already had many unrelated dirty changes and branch `int_main` was ahead before this task began.
