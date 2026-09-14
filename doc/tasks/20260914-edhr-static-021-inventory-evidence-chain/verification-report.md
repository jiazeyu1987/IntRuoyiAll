# EDHR-STATIC-021 Verification Report

## Summary

- Result: PASS. Implementation verified; integration, remote push, and task worktree removal completed.
- Scope: EDHR-STATIC-021 only. No E2E, DB write, service restart, or remote operation was performed.
- Implementation: active-order completion now records formal product-issue inventory trace rows before marking the order completed.
- Integration status: target implementation and static contract are already present on latest `int_main` HEAD `90adf7d6e623a80ec1eae4cc5ebf8bf1fc01abfc`; this closeout branch records the task evidence and cleanup.

## Files Changed For This Task

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderTransferTraceService.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderTransferTraceServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderTransferTraceServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesEdhrStatic021InventoryEvidenceChainContractTest.java`

## RED

- Command: `git show HEAD:IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceImpl.java` static contract check.
- Result: FAIL.
- Expected reason: HEAD did not contain `recordProductIssueInventoryTracesForActiveOrder`, so normal active-order completion could finish formal backfill without producing inventory trace evidence consumed by final release readiness.

## GREEN

- Command: `mvn -pl yudao-module-mes -am '-Dtest=MesActiveOrderTransferTraceServiceTest,MesTeamLeaderActiveOrderCompletionServiceTest,MesEdhrStatic021InventoryEvidenceChainContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- Result: PASS.
- Evidence: 19 tests run, 0 failures, 0 errors, 0 skipped.

## Static Contract

- `MesTeamLeaderActiveOrderCompletionServiceImpl` depends on `MesActiveOrderTransferTraceService`.
- Successful active-order completion writes formal backfill records, validates materialized ids, records inventory traces, and only then marks the active order completed.
- Trace generation reads finished `MesWmProductIssueDO` headers and locked `MesWmProductIssueDetailDO` rows for the active order work order.
- Each formal product-issue detail generates `TRANSFER`, `SHIPMENT`, and `BATCH_TRACE` evidence rows with stable active-order scoped idempotency keys.
- Every generated row preserves active order, work order, route, route version, material stock, item, batch, source object type/id/code/status, occurred time, and source snapshot JSON.
- Missing finished issue, missing source code, missing detail, missing stock/material/batch/batch code, or non-positive quantity fails fast before trace insert.

## Boundaries

- EDHR-STATIC-022 remains out of scope. This task does not change duplicate detection for multiple legal trace rows of the same source type.
- Existing unrelated dirty worktree changes were not reverted, staged, committed, or pushed.
- E2E was not executed because the task explicitly forbids Playwright/E2E.

## Closeout Status

- Current task status: `completed`.
- User closeout authorization: `提交并融合进int_main`.
- Worktree migration: C drive detached/mixed dirty worktree was not committed; a D drive task integration worktree was created on branch `codex/20260914-edhr-static-021-inventory-evidence-chain-closeout`.
- Port guard: PASS, `int_main` profile slot 30, frontend 8164, backend 48164.
- Cleanup preview: PASS. It kept `task.md`, `execution-log.md`, and `verification-report.md`; deleted `bug-regression-evidence.md`; blockers none.
- Cleanup apply: PASS with `--worktree-closeout off`, deleting only `bug-regression-evidence.md`. Because local `.git/info/exclude` ignores `doc/tasks/*`, task records will be force-added explicitly rather than relying on automatic closeout staging.
- Integration: commit `7393f67315e1ae5adf53fe9c4239da732d0e0efe` fast-forward merged into `int_main`; remote task branch and `origin/int_main` verified at the same commit using `git ls-remote`.
- Worktree removal: PASS; task-owned D drive directory removed after clean status and ancestor checks; physical path no longer exists.
- Post-merge runtime port guard: PASS, int_main/int_main 8081/48081.
- Runtime reservation: task-owned slot 30 released under the shared registry mutex after worktree removal and completion; active=false verified.

## Evidence Validators

- Bug regression evidence validator: PASS.
- Final diff check for task-owned paths: PASS, exit 0, no whitespace errors.
- Experience consolidation: PASS, merged into existing PowerShell/worktree memory docs and the experience index.
- Latest executed regression: the GREEN command above ran on `aad670dca`, with Surefire report timestamp 2026-09-14 13:27:58: 19 tests, 0 failures, 0 errors, 0 skipped. The earlier attribution to `90adf7d6e` was incorrect. `git diff aad670dca 7393f6731 -- IntRuoyiBackend/yudao-module-mes` is empty; later documentation commits did not change the verified MES code.
