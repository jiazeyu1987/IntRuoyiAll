# Test Plan

## Task-Level Cases

### TC-A2-01 Writer orchestration RED/GREEN

- `test_case_id`: TC-A2-01
- `mapped_task_ids`: A2-RED, A2-INTEGRATE
- `mapped_acceptance_ids`: AC-05, AC-11, AC-13, AC-14, AC-15
- `environment or setup`: yudao-module-mes unit-test context with mocked formal readers/writers.
- `steps`: assert plan order, three writer invocation, completion/precheck/submit order, CONFIRMED-only PQC, rollback/blocker, request/business idempotency.
- `expected_result`: RED before implementation; GREEN with exact order and no partial effects.
- `evidence`: Maven command, test count and assertions in `test-report.md`.

### TC-A3-01 Batch writer

- `test_case_id`: TC-A3-01
- `mapped_task_ids`: A3
- `mapped_acceptance_ids`: AC-03, AC-07, AC-10, AC-13, AC-14, AC-15
- `environment or setup`: formal BATCH binding, production submit/confirm/signatures and current batch/task.
- `steps`: write, repeat, remove binding/parameter/signature, use wrong batch/task.
- `expected_result`: current batch/task execution and audits on success; deterministic blockers/no side effects on failure; repeat idempotent.
- `evidence`: focused JUnit output and object assertions.

### TC-A4-01 Inspection writer

- `test_case_id`: TC-A4-01
- `mapped_task_ids`: A4
- `mapped_acceptance_ids`: AC-02, AC-04, AC-08, AC-10, AC-13, AC-14, AC-15
- `environment or setup`: CONFIRMED PQC task, aggregate rows, APPROVED signed review, PUBLISHED QA, traditional PROCESS_INSPECTION report/mapping.
- `steps`: success; SUBMITTED-only; missing aggregate/QA item/equipment/mapping/signature; mismatch result.
- `expected_result`: exact execution/audit/signature on success; specific blocker and no write otherwise.
- `evidence`: focused JUnit output and field/source assertions.

### TC-A5-01 Loss writer/completeness

- `test_case_id`: TC-A5-01
- `mapped_task_ids`: A5
- `mapped_acceptance_ids`: AC-03, AC-09, AC-10, AC-11, AC-13, AC-14, AC-15
- `environment or setup`: positive formal loss feedback/event/details, signed submit/confirm, traditional LOSS_REPORT report/mapping.
- `steps`: success; total/detail mismatch; missing reason/mapping/signature; zero loss without formal confirm field; incomplete one of three documents.
- `expected_result`: exact execution/audits on success; specific blockers; completeness never creates work task.
- `evidence`: focused JUnit output.

### TC-A1-01 Frontend contract

- `test_case_id`: TC-A1-01
- `mapped_task_ids`: A1
- `mapped_acceptance_ids`: AC-05, AC-11, AC-13, AC-14
- `environment or setup`: frontend dependencies installed.
- `steps`: run static spec and `pnpm ts:check`; inspect request, blocker and refresh-failure branches.
- `expected_result`: request only M0 fields, optional locator fields typed/rendered, success remains success if refresh fails.
- `evidence`: Node/pnpm outputs.

### TC-A6-01 Real flow

- `test_case_id`: TC-A6-01
- `mapped_task_ids`: A6
- `mapped_acceptance_ids`: AC-01 至 AC-15
- `environment or setup`: confirmed local runtime, authorized test tenant, five role accounts/signatures, formal route/report/QA/mapping fixture.
- `steps`: production submit/review/history; PQC submit/review/history; natural double 100; production leader apply; document/audit/signature view; release owner approve or reject; final read-only checks.
- `expected_result`: complete manifest and all real paths pass without console/page/backend errors.
- `evidence`: Playwright output, manifest, IDs and read-only assertions.

## Integration Cases

### TC-INT-01 Backend focused regression

- `test_case_id`: TC-INT-01
- `mapped_task_ids`: A2-INTEGRATE, A3, A4, A5
- `mapped_acceptance_ids`: AC-02 至 AC-15
- `environment or setup`: Java 17/Maven, clean relevant target path excluding known corrupt historical target.
- `steps`: run all release dossier JUnit, backend static/schema specs and compile.
- `expected_result`: all pass; no fallback/no-op writer; no stale test.
- `evidence`: independent tester report.

### TC-INT-02 Frontend regression

- `test_case_id`: TC-INT-02
- `mapped_task_ids`: A1
- `mapped_acceptance_ids`: AC-05, AC-11, AC-13, AC-14
- `environment or setup`: pnpm workspace.
- `steps`: static spec, ts:check, adjacent team leader specs.
- `expected_result`: all pass.
- `evidence`: independent tester report.

### TC-INT-03 End-to-end and idempotency

- `test_case_id`: TC-INT-03
- `mapped_task_ids`: A1, A2-INTEGRATE, A3, A4, A5, A6
- `mapped_acceptance_ids`: AC-01 至 AC-15
- `environment or setup`: TC-A6-01 fixture.
- `steps`: successful real path; repeat same snapshot; one missing-source negative path; owner approve/reject branch as fixture permits.
- `expected_result`: success creates exactly one of each formal object/task; repeat reuses; negative creates none; final transaction/event correct.
- `evidence`: Playwright plus final read-only counts/IDs.

## Regression and Safety

- Existing active-order list/add, production/PQC submission review, batch backfill, eDHR precheck/release tests remain green.
- No test may assert dynamic formBindings as batch record source.
- No E2E may write production/shared tenant data or log credentials.
- A failed/missing environment is BLOCKED, not PASS and not replaced with mock/API-only.
