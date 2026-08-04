# Verification Report

## Scope

- Backend partial replan behavior, issue persistence, and schedule order list issue summary.
- Frontend replan apply gating, blocked-row red state, and visible blocking reason.

## Commands

- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProAutoScheduleAlgorithmContractTest#apply_shouldPersistBlockedIssueAndContinueSchedulableWorkOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - RED: FAIL, existing behavior aborted the whole apply on one attributable blocked work order.
  - GREEN before final downstream-filter patch: PASS, `BUILD SUCCESS`, 1 test, 0 failures.
  - Final re-run after downstream-filter patch: BLOCKED, javac stayed in `FileDescriptor.close0` / `ClassWriter.writeClass` for more than 10 minutes with no Surefire output while other same-module Maven processes were active.
- `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-static.spec.js` -> PASS.
- `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js` -> PASS.
- `node tests/e2e/mes-pro-schedule-order-replan-skipped-selected-confirm-static.spec.js` -> PASS.
- `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` -> PASS.
- `node tests/e2e/mes-schedule-order-replan-single-action-static.spec.js` -> PASS.
- `node tests/e2e/mes-pro-schedule-order-apply-replan-toast-static.spec.js` -> PASS.
- `pnpm.cmd ts:check` -> PASS.
- `node --check tests/e2e/mes-pro-schedule-order-partial-replan-blockers-real-readonly.e2e.js` -> PASS.
- `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-real-readonly.e2e.js` with local Chrome and local test credential env -> BLOCKED: logged into `测试租户/aoteman` on `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`; the first 74 schedule orders had no unresolved blocking issue, so blocked-row red state and reason visibility could not be verified. Evidence output recorded `mesWriteRequestCount=0`, `pageErrorCount=0`, `consoleErrorCount=0`.
- `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-real-readonly.e2e.js` with local Chrome and user-authorized `芋道源码/admin` env -> BLOCKED: logged into `芋道源码/admin`; the schedule-order list returned 47 rows with no unresolved blocking display row, and the read-only issues endpoint returned `BLOCKING` total 0 / unresolved 0 / unresolved with `workOrderId` 0. Evidence output recorded `mesWriteRequestCount=0`, `pageErrorCount=0`, `consoleErrorCount=0`.
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> BLOCKED by unrelated missing `RouteFlowConfigPanel.vue`.
- `node tests/e2e/mes-pro-schedule-order-usability-static.spec.js` -> BLOCKED by unrelated admission quick-filter assertion.

## Results

- Backend implementation is present in current HEAD through shared-branch baseline commits and covers partial application, issue persistence, summary counts, and blocked-work-order exclusion from downstream apply syncs.
- Frontend implementation is present in current HEAD and passes the focused partial-blocker contract, adjacent replan contracts, and TypeScript check.
- Real E2E reached the local login and schedule-order list with no page/console/MES-write errors in both tested identities, but remains BLOCKED by missing unresolved blocking issue fixture in the selected tenants. The existing 881MO write-flow script was intentionally not run because it can apply replan changes to non-task-owned data.
- Final backend regression re-run remains pending due concurrent Maven/Windows compile hang; task should not be marked completed until it passes or the blocker is explicitly accepted.

## Final Status

in_progress
