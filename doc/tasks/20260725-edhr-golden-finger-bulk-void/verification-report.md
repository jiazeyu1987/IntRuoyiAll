# Verification Report - eDHR 金手指批量作废

## Summary

- Backend focused Maven verification passed for the new gold-finger bulk void contract and service logic.
- Frontend static contract and TypeScript checks passed for permission visibility, current-filter submission, approval-path isolation and single-row void regression.
- No global BPM/approval configuration was changed.

## Commands

- `node --check tests\e2e\edhr-batch-execution-golden-finger-bulk-void-static.spec.js` -> PASS
- `node tests\e2e\edhr-batch-execution-golden-finger-bulk-void-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS
- `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrBatchExecutionGoldenFingerBulkVoidContractTest,MesProEdhrBatchExecutionGoldenFingerBulkVoidServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, Tests run: 5, Failures: 0, Errors: 0, Skipped: 0

## Regression Coverage

- Bulk submit uses `goldenFingerBulkVoidEdhrBatchExecutions` and `buildGoldenFingerBulkVoidFilter()`.
- Bulk submit does not call `resolveVoidBatchExecutionApproval`.
- Single-row void still calls `requestVoidBatchExecution` and `/mes/pro/edhr-change/void-batch-execution/request`.
- Service prechecks candidates before direct void execution and rejects non-golden-finger users before selecting batches.

## Optional E2E

- Not executed. Required prerequisites were not confirmed in this turn: local frontend/backend runtime, gold-finger role-bound test account and traceable writable batch execution data.

## Closeout Status

- Implementation and required verification are complete.
- Cleanup/commit/push are not completed because the shared workspace currently contains unrelated concurrent dirty files and local commits ahead of `origin/int_main`; mixing them into this task commit would violate task ownership.
## Cleanup Evidence

- Cleanup preview: PASS, no delete/blocked/warnings.
- Cleanup apply: PASS, no deleted paths.
- Task remains `ready_for_closeout` because implementation/closeout commit and push are blocked by unrelated concurrent dirty files and local ahead commits in the shared workspace.