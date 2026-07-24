# Test Plan

## Task-Level Validation

### test_case_id: TC-01

mapped_task_ids: T1
mapped_acceptance_ids: AC-01, AC-02, AC-03, AC-07
environment or setup: Frontend worktree with Node.
steps: Run `node --test scripts\edhr-permission-tenant-matrix-contract.test.mjs`.
expected_result: Contract verifies package scripts, E2E fail-fast guard, live tenant guard, write guard, and fixture script.
evidence: Command output and execution log.

### test_case_id: TC-02

mapped_task_ids: T2
mapped_acceptance_ids: AC-06
environment or setup: MySQL container `int-ruoyi-mysql` available.
steps: Run fixture script without `--apply`.
expected_result: Status is `DRY_RUN`; no writes occur.
evidence: JSON output.

### test_case_id: TC-03

mapped_task_ids: T2
mapped_acceptance_ids: AC-05, AC-06
environment or setup: MySQL test tenant `122`.
steps: Run fixture script with `--apply`.
expected_result: Executor, approver, archiver, readonly, and denied users/roles exist only in tenant `122`.
evidence: JSON output and readonly SQL verification.

### test_case_id: TC-04

mapped_task_ids: T3
mapped_acceptance_ids: AC-01, AC-02
environment or setup: Frontend worktree.
steps: Run `pnpm e2e:edhr:permission-matrix:check`.
expected_result: Node syntax check passes.
evidence: Command output.

## End-to-End Validation

### test_case_id: TC-05

mapped_task_ids: T3
mapped_acceptance_ids: AC-01, AC-02, AC-03, AC-04, AC-05
environment or setup: Local frontend/backend running, real test tenant accounts, approved/sealed eDHR execution IDs.
steps: Run `pnpm e2e:edhr:permission-matrix`.
expected_result: Role matrix passes; readonly/formal admin write guards catch zero writes; denied user has explicit unauthorized evidence.
evidence: `test-results/edhr-permission-tenant-matrix/result.json`, screenshots, and execution log.

## Regression Checks

- Existing full eDHR approval/archive E2E script remains unchanged and syntax-valid.
- Existing frontend contract tests for eDHR pages still pass when run by reviewer.
- No test result artifacts are committed.
