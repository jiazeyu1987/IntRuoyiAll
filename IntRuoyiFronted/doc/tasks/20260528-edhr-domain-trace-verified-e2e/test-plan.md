# Test Plan

## Task-Level Validation

test_case_id: TC-01
mapped_task_ids: T1
mapped_acceptance_ids: AC-01, AC-03
environment or setup: frontend worktree
steps: Run `node --test scripts/edhr-domain-trace-e2e-contract.test.mjs`
expected_result: RED before implementation, PASS after implementation.
evidence: command output and execution-log markers.

test_case_id: TC-02
mapped_task_ids: T2
mapped_acceptance_ids: AC-02
environment or setup: frontend worktree
steps: Run `pnpm e2e:edhr:domain-trace:check`
expected_result: PASS.
evidence: command output.

test_case_id: TC-03
mapped_task_ids: T2
mapped_acceptance_ids: AC-04
environment or setup: frontend 8081, backend current worktree, test tenant 122
steps: Run `pnpm e2e:edhr:domain-trace` with expected status `VERIFIED` and expected blocker count `0`.
expected_result: PASS with final status `VERIFIED`, final blocker count `0`, non-empty items, screenshots, trace, and JSON evidence.
evidence: `test-results/edhr-domain-trace/result.json` and evidence markdown.

test_case_id: TC-04
mapped_task_ids: T2
mapped_acceptance_ids: AC-05
environment or setup: frontend worktree
steps: Inspect contract test coverage for invalid expectations and fail-closed behavior.
expected_result: Contract test proves mismatch assertions exist.
evidence: contract test PASS.

## System-Level Validation

- The real E2E must use `http://localhost:8081` and test tenant credentials.
- The final report must not claim global eDHR production readiness.
