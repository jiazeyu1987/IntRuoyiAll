# Task: MES batch record execution phase 2 backend

## Goal

Implement the backend Phase 2 slice after template import: create a single-template execution instance from a saved batch-record template, bind it to a work order and batch code, save draft cell values, reopen the draft, and submit the execution.

## Scope

- Continue on `feature/mes-paperless-batch-plan`
- Reuse the existing Phase 1 template records as the source
- Deliver a single-template execution model, not the full multi-template batch bundle yet
- Keep signoff, review, deviation, and export out of scope for this slice

## Milestones

- [x] M1: Confirm the previous Phase 1 backend task is completed before starting Phase 2.
- [x] M2: Create this Phase 2 task document, execution log, and API contract before production code changes.
- [x] M3: Record BDD scenarios and RED evidence for execution create/save/submit behavior.
- [x] M4: Implement execution persistence, APIs, and focused tests.
- [x] M5: Verify focused backend tests and runtime behavior against the clean backend.
- [x] M6: Update final status and commit only current task changes.

## Expected Verification

- A saved template can create one execution instance from `templateId + workOrderId + batchCode`.
- The execution detail returns the template snapshot and editable cell values.
- Draft values can be saved and reopened.
- Submitted executions change status and reject further draft updates.
- Focused controller/service tests cover create, page, get, save-draft, and submit without adding signoff/review/export.

## Current Status

Completed. Controller and service tests pass for the Phase 2 execution contract, the clean runtime schema/permission baseline is aligned, and the backend supported the frontend real-user execution smoke on port `48083`.

## Verification Evidence

- `mvn -f yudao-module-mes/pom.xml "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- Direct HTTP verification of `/page`, `/create`, `/get`, `/save-draft`, and `/submit` on `http://127.0.0.1:48083/admin-api` -> PASS
- Frontend Playwright smoke against the clean backend -> PASS
