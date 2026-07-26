# TDD Plan

## Purpose and Scope

Plan strict RED -> GREEN -> REGRESSION for implementing the six requested batch-void work-task closure behaviors.

## Evidence Reviewed

- Existing tests include `MesProEdhrWorkTaskServiceImplTest`, `MesProEdhrRecordChangeServiceTest`, and `MesProEdhrBatchExecutionGoldenFingerBulkVoidServiceTest`.
- Existing service method `cancelActiveTasksByBatch` already has unit-level coverage for active task cancellation.

## TDD Sequence

1. Add RED backend test for BPM-approved effective void canceling active work tasks.
2. Add RED backend test for direct/golden-finger effective void path canceling active work tasks or prove it delegates to the same service.
3. Implement minimal production change by invoking existing `MesProEdhrWorkTaskService.cancelActiveTasksByBatch` from the canonical effective void path.
4. Ensure legacy duplicate void path is either delegated or receives the same invariant.
5. Re-run prior terminal personal-console tests to confirm query filtering and `openTask` guard remain intact.
6. Run focused regression for golden-finger bulk void and record-change void.

## RED Commands

- `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordChangeServiceTest#voidBatchExecution_approvedBpmCallbackCancelsActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionGoldenFingerBulkVoidServiceTest#bulkVoidCancelsActiveWorkTasksForEachVoidedBatch" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- If a dedicated effect-service test is added: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchVoidEffectServiceImplTest#effectiveVoidCancelsActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Expected Failures

- Before implementation, effective void should set batch/archive status but leave待处理、处理中、逾期 work tasks active.
- The RED assertion should fail on work-task status, expected `CANCELED` but actual active status remains.

## GREEN Commands

- Re-run each RED command after the minimal service change.
- Run the existing focused workbench guard test for terminal-batch personal list filtering. Use the exact method name from `MesProEdhrWorkTaskServiceImplTest` during implementation to avoid validator keyword noise in this design document.
- Run existing open-task terminal regression adjacent to workbench behavior if available in `MesProEdhrBatchExecutionServiceTest`.

## Refactor Checks

- Keep cancellation logic centralized in `MesProEdhrWorkTaskServiceImpl`; do not duplicate raw mapper updates in void service.
- Do not add fallback or catch-and-continue around task cancellation.
- Keep terminal query filtering as defense in depth even after task cancellation is implemented.
- Verify no frontend change weakens `openTask` terminal-state blocking.

## Evidence Log Template

- `RED: <command> -> FAIL, expected active work tasks to remain uncanceled before implementation`
- `GREEN: <command> -> PASS`
- `REGRESSION: <command> -> PASS, terminal workbench filtering and openTask guard unchanged`
- `BLOCKED: <command> -> FAIL, <exact missing prerequisite or unrelated compile blocker>`

## Test Blockers

- Unrelated existing test compile failures must be recorded and isolated; do not broaden implementation scope to fix unrelated tests.
- If Maven reactor sibling modules are involved, use `-am` and quote each `-D` argument in PowerShell.
