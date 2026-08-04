# Bug Regression Evidence

## Bug Summary

自动重排当前遇到任一阻断 issue 时会中止整批应用，导致没有问题的工单也无法重排；用户期望只有存在阻断的工单标红并显示原因，其它工单继续重排。

## Expected Behavior

- 可归因到单个工单的 BLOCKING issue 只阻断该工单。
- 没有阻断的工单继续按自动重排结果应用。
- 阻断工单写入并展示阻断原因。
- 全局或无法归因阻断仍 fail fast。

## Reproduction

- RED command: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProAutoScheduleAlgorithmContractTest#apply_shouldPersistBlockedIssueAndContinueSchedulableWorkOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test`.
- RED result: FAIL, expected reason: existing apply path threw `PRO_AUTO_SCHEDULE_PREFLIGHT_BLOCKED` and aborted the entire apply when selected work order `501` lacked a route, even though another selected work order was schedulable.

## Root Cause

- Apply preflight and schedule computation treated any BLOCKING issue as a whole-action blocker. The apply path also deleted/synced by the full requested work-order scope, so it lacked a formal healthy-vs-blocked work-order boundary for persistence and downstream updates.

## Regression Test

- Added `MesProAutoScheduleAlgorithmContractTest#apply_shouldPersistBlockedIssueAndContinueSchedulableWorkOrders`.
- Added `tests/e2e/mes-pro-schedule-order-partial-replan-blockers-static.spec.js` for frontend row red state, visible reason, and partial-apply gating.

## RED

- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProAutoScheduleAlgorithmContractTest#apply_shouldPersistBlockedIssueAndContinueSchedulableWorkOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, whole-apply abort on attributable work-order blocker.
- RED: `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-static.spec.js` -> FAIL, schedule order row type lacked `blockingIssueCount?: number`.

## GREEN

- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProAutoScheduleAlgorithmContractTest#apply_shouldPersistBlockedIssueAndContinueSchedulableWorkOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS before final downstream-filter patch, `BUILD SUCCESS`, 1 test, 0 failures.
- GREEN: `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-static.spec.js` -> PASS.
- Frontend adjacent and type checks passed as recorded in `verification-report.md`.

## Verification

- Verification: frontend focused static, adjacent replan static contracts, and `pnpm.cmd ts:check` passed.
- Verification: final backend JUnit re-run remains blocked by concurrent Maven/Windows javac class-writing hang; see blockers.

## Risk And Regression Scope

- 排产应用删除/重建任务范围必须收敛到可应用工单，避免删除阻断工单已有任务。
- issue 持久化必须刷新本次涉及工单的阻断原因，避免旧问题误导 UI。
- 日历 token、全局 preflight 和无法归因阻断仍必须保持 fail-fast。

## Blockers And Follow-Up

- Final backend JUnit re-run after the downstream filter patch is blocked by concurrent same-module Maven activity and a Windows javac `FileDescriptor.close0` / `ClassWriter.writeClass` hang with no Surefire output.
- Re-run the target backend test once the shared backend Maven activity is clear.
