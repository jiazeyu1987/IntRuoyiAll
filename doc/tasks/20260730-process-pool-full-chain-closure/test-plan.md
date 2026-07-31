# Test Plan

## Task-Level Validation

### TC-01
- test_case_id: TC-01
- mapped_task_ids: T1
- mapped_acceptance_ids: AC-01
- environment or setup: Backend target tests in `IntRuoyiBackend`.
- steps: Run focused JUnit/static contract for frontline binding source implementations.
- expected_result: Production Spring beans resolve real binding data and fail fast on missing bindings.
- evidence: Maven output and execution log.

### TC-02
- test_case_id: TC-02
- mapped_task_ids: T2
- mapped_acceptance_ids: AC-02, AC-03
- environment or setup: Frontend static contract and backend submit tests.
- steps: Run static contract proving UI calls `ProFeedbackApi.frontlineSubmit`; run backend submit/PQC tests.
- expected_result: Production/PQC pages do not show success from validate-only path; PQC maps to pool result.
- evidence: Node/Maven output.

### TC-03
- test_case_id: TC-03
- mapped_task_ids: T3
- mapped_acceptance_ids: AC-04
- environment or setup: Backend target tests.
- steps: Seed multiple work orders with planned start times and available fragments; run FIFO orchestration.
- expected_result: Earlier planned start time consumes first; missing/duplicate invalid planning data fails fast.
- evidence: Maven output.

### TC-04
- test_case_id: TC-04
- mapped_task_ids: T4
- mapped_acceptance_ids: AC-05
- environment or setup: Backend target tests and frontend static contract.
- steps: Submit values below/inside/above configured limit and generate review copy.
- expected_result: Below clamps to min, above clamps to max, original and adjusted values are both retained.
- evidence: Maven/Node output.

### TC-05
- test_case_id: TC-05
- mapped_task_ids: T5
- mapped_acceptance_ids: AC-06
- environment or setup: Frontend static contract and backend read-model test.
- steps: Load team-leader workbench source and API/read model tests.
- expected_result: Workbench uses formal process-pool API, route/permission exists, no mock/browser storage.
- evidence: Node/Maven output.

## System-Level Validation

### TC-06
- test_case_id: TC-06
- mapped_task_ids: T6
- mapped_acceptance_ids: AC-07
- environment or setup: Worktree runtime `8082/48082`, confirmed login/test data/signature.
- steps: Use Playwright through the real frontend to submit production and PQC records, allocate FIFO, generate review copy, and view team-leader workbench.
- expected_result: Full chain passes or records exact blocker.
- evidence: Playwright output, screenshots, JSON evidence.

## Regression Checks
- Branch runtime port guard.
- `git diff --check`.
- Target backend/frontend tests for each modified area.
