# Execution Log

## 2026-08-04

- User intent: 自动重排遇到局部阻断时不要阻断整批；可正常重排的工单继续应用；有阻断的工单标红并可查看原因。
- Skill gates loaded: `bug-regression-fix-loop`, `backend-api-delivery`, `frontend-feature-delivery`.
- Trigger docs loaded: `docs/backend-development.md`, `docs/frontend-development.md`, `docs/e2e-rules.md`, `docs/task-closeout-rules.md`, `docs/powershell-memory.md`, `docs/powershell-encoding.md`, `docs/experience-index.md`, `IntRuoyiBackend/docs/system/mes-scheduling-domain-contracts.md`.
- BDD: Mixed replan scope applies healthy orders -> Given one selected work order can be scheduled and one selected work order has an attributable BLOCKING issue, When auto replan apply is executed, Then the schedulable work order is deleted/recreated or preserved per algorithm and the blocked work order persists a BLOCKING issue without aborting the whole apply.
- BDD: All selected orders blocked -> Given all selected work orders have attributable BLOCKING issues, When auto replan apply is executed, Then no replaceable tasks are deleted, the response is not applied, and BLOCKING issues are returned and persisted.
- BDD: Blocked orders visible in list -> Given a replan persisted a BLOCKING issue for a work order, When the schedule order list is opened, Then the row is marked red and the latest blocking reason is visible to the user.

## Dirty Worktree Baseline

- Concurrent baseline note: commit `ae0cf0d96 chore: baseline concurrent residual before dcc approval detail fix` was created by another task while this task was starting and included this task's initial documentation files. It did not include this task implementation code.
- Baseline commit: `ebe8833bc chore: baseline residual docs before mes partial replan` captured residual non-task docs before implementation.
- Concurrent commit observed: `26c72dfa1 docs: record approval center todo verification` adjusted another task while this task was waiting on Git locks.
- Baseline commit: `0325b3097 chore: baseline residual qa excerpt before mes partial replan` captured the last residual non-task E2E file before implementation.
- Post-baseline status for target files: clean before RED edits.

## RED

- Backend RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProAutoScheduleAlgorithmContractTest#apply_shouldPersistBlockedIssueAndContinueSchedulableWorkOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: existing apply path threw `PRO_AUTO_SCHEDULE_PREFLIGHT_BLOCKED` / `排产工单ID=501排产失败；工单未配置工艺路线`, aborting the whole replan instead of applying the healthy work order.
- Frontend RED: `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-static.spec.js` -> FAIL, expected reason: schedule order API type did not expose `blockingIssueCount?: number`.

## GREEN

- Backend focused GREEN before final preservation-filter patch: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProAutoScheduleAlgorithmContractTest#apply_shouldPersistBlockedIssueAndContinueSchedulableWorkOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `BUILD SUCCESS`, 1 test, 0 failures, completed around 2026-08-04 20:19 +08:00.
- Frontend GREEN: `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-static.spec.js` -> PASS.
- Frontend adjacent GREEN: `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js` -> PASS.
- Frontend adjacent GREEN: `node tests/e2e/mes-pro-schedule-order-replan-skipped-selected-confirm-static.spec.js` -> PASS.
- Frontend adjacent GREEN: `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` -> PASS.
- Frontend adjacent GREEN: `node tests/e2e/mes-schedule-order-replan-single-action-static.spec.js` -> PASS.
- Frontend adjacent GREEN: `node tests/e2e/mes-pro-schedule-order-apply-replan-toast-static.spec.js` -> PASS.
- Frontend typecheck GREEN: `pnpm.cmd ts:check` -> PASS.

## Verification

- Implemented behavior:
  - `MesProAutoScheduleServiceImpl` now treats per-work-order BLOCKING issues as blocked work-order scope, applies only healthy work orders, persists returned issues, and keeps global/unattributable blocking issues fail-fast.
  - Apply cleanup, quantity sync, plan-field updates, preserved task relation sync, and EDHR completion commands filter out blocked work orders.
  - Apply summary exposes applied/blocked/skipped work-order counts.
  - Schedule order list response exposes unresolved BLOCKING issue count and latest reason by work order.
  - Frontend row rendering marks blocked rows red and displays latest blocking reason; replan apply gates only on global/unattributable blockers and confirms skipped selected rows before partial apply.
- Shared-branch commit note:
  - `2e507d526 chore: baseline dirty workspace before route product binding` includes initial backend/frontend source changes, target JUnit, new partial replan static test, and this task execution log.
  - `0cb7335da chore: baseline residual before dcc approval detail tab removal` includes later schedule-order page and partial static test changes.
  - `46e0670a7 chore: baseline concurrent residual before dcc approval tabs removal` includes later `MesProAutoScheduleServiceImpl` preservation/downstream filtering.
  - `08fa94cef chore: baseline residual before production leader tab completion` includes the two adjacent static-contract updates for the new partial-apply semantics.
- Blocked/partial verification:
  - Re-run after final backend preservation-filter patch was attempted with the same target Maven command; it remained in javac `FileDescriptor.close0` / `ClassWriter.writeClass` for more than 10 minutes without Surefire output while other `E:\IntRuoyi\IntRuoyiBackend` Maven processes were active. Only the current task Java PID was stopped; unrelated Maven processes were left running.
  - `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> BLOCKED by unrelated missing file `src/views/mes/pro/route/RouteFlowConfigPanel.vue`.
  - `node tests/e2e/mes-pro-schedule-order-usability-static.spec.js` -> BLOCKED before this task's assertions by unrelated concurrent admission quick-filter assertion.

## Blockers

- Backend final JUnit re-run pending because `yudao-module-mes` target is concurrently used by other Maven processes and this task's re-run hung in javac class writing without Surefire output.
- Current shared branch has staged and unstaged unrelated concurrent task changes; do not commit, clean, or push this task until the shared staging state is reconciled.
