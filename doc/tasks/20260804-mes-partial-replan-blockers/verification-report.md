# Verification Report

## Scope

- Backend partial replan behavior, issue persistence, and schedule order list issue summary.
- Frontend replan apply gating, blocked-row red state, and visible blocking reason.

## Commands

- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProAutoScheduleAlgorithmContractTest#apply_shouldPersistBlockedIssueAndContinueSchedulableWorkOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - RED: FAIL, existing behavior aborted the whole apply on one attributable blocked work order.
  - GREEN before final downstream-filter patch: PASS, `BUILD SUCCESS`, 1 test, 0 failures.
  - Final re-run after downstream-filter patch: BLOCKED, javac stayed in `FileDescriptor.close0` / `ClassWriter.writeClass` for more than 10 minutes with no Surefire output while other same-module Maven processes were active.
  - Final re-run on 2026-08-05: BLOCKED before Surefire in `yudao-module-mes` `testCompile` by unrelated `MesQaInspectionRegulationServiceTest` compile errors for missing `MesQaInspectionRegulationProjectStatusRespVO` getters: `getProductId`, `getConfigured`, `getRegulationId`, `getLifecycleStatus`, and `getRegulationCode`.
- `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-static.spec.js` -> PASS.
- `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js` -> PASS.
- `node tests/e2e/mes-pro-schedule-order-replan-skipped-selected-confirm-static.spec.js` -> PASS.
- `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` -> PASS.
- `node tests/e2e/mes-schedule-order-replan-single-action-static.spec.js` -> PASS.
- `node tests/e2e/mes-pro-schedule-order-apply-replan-toast-static.spec.js` -> PASS.
- `pnpm.cmd ts:check` -> PASS.
- `node --check tests/e2e/mes-pro-schedule-order-partial-replan-blockers-real-readonly.e2e.js` -> PASS.
- `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-real-readonly.e2e.js` with local Chrome and local test credential env -> BLOCKED: logged into `测试租户/aoteman` on `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`; the first 74 schedule orders had no unresolved blocking issue, so blocked-row red state and reason visibility could not be verified. Evidence output recorded `mesWriteRequestCount=0`, `pageErrorCount=0`, `consoleErrorCount=0`.
- `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-real-readonly.e2e.js` with local Chrome and user-authorized `芋道源码/admin` env -> BLOCKED before fixture setup: logged into `芋道源码/admin`; the schedule-order list returned 47 rows with no unresolved blocking display row, and the read-only issues endpoint returned `BLOCKING` total 0 / unresolved 0 / unresolved with `workOrderId` 0. Evidence output recorded `mesWriteRequestCount=0`, `pageErrorCount=0`, `consoleErrorCount=0`.
- `node --check tests\e2e\mes-pro-schedule-order-partial-replan-blockers-real-fixture.e2e.js` -> PASS.
- `git diff --check -- IntRuoyiFronted/tests/e2e/mes-pro-schedule-order-partial-replan-blockers-real-fixture.e2e.js doc/tasks/20260804-mes-partial-replan-blockers/execution-log.md` -> PASS; only LF-to-CRLF normalization warnings were emitted.
- `node tests\e2e\mes-pro-schedule-order-partial-replan-blockers-real-fixture.e2e.js` with local Chrome and user-authorized `芋道源码/admin` -> PASS: schedule order `SCH-881MO098538-20260707-0001`, source work order `881MO098538`, work order ID `925867`, issue date `2026-08-31`, created issue `19255`, visible reason `阻断：E2E_PARTIAL_REPLAN_BLOCKER_20260804160047 自动重排局部阻断红行验证`, first-cell background `rgb(255, 241, 240)`, expected MES mutation requests exactly `POST /admin-api/mes/pro/auto-schedule/issues` and `PUT /admin-api/mes/pro/auto-schedule/issues/resolve`, unexpected MES mutation count `0`, page error count `0`, console error count `0`, cleanup `resolved-via-ui-and-row-cleared`.
- Final read-only API cleanup check for `workOrderId=925867&severity=BLOCKING` -> PASS: task marker issues `19252, 19253, 19254, 19255` are all resolved; unresolved task marker issue count `0`.
- 2026-08-05 user-requested rerun: `node tests\e2e\mes-pro-schedule-order-partial-replan-blockers-real-fixture.e2e.js` with local Chrome and user-authorized `芋道源码/admin` -> PASS: schedule order `SCH-881MO098538-20260707-0001`, source work order `881MO098538`, work order ID `925867`, issue date `2026-08-31`, created issue `19256`, visible reason `阻断：E2E_PARTIAL_REPLAN_BLOCKER_20260804162805 自动重排局部阻断红行验证`, first-cell background `rgb(255, 241, 240)`, expected MES mutation requests exactly `POST /admin-api/mes/pro/auto-schedule/issues` and `PUT /admin-api/mes/pro/auto-schedule/issues/resolve`, unexpected MES mutation count `0`, page error count `0`, console error count `0`, cleanup `resolved-via-ui-and-row-cleared`.
- 2026-08-05 final read-only API cleanup check for `workOrderId=925867&severity=BLOCKING` -> PASS: task marker issues `19252, 19253, 19254, 19255, 19256` are all resolved; unresolved task marker issue count `0`.
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> BLOCKED by unrelated missing `RouteFlowConfigPanel.vue`.
- `node tests/e2e/mes-pro-schedule-order-usability-static.spec.js` -> BLOCKED by unrelated admission quick-filter assertion.

## Results

- Backend implementation is present in current HEAD through shared-branch baseline commits and covers partial application, issue persistence, summary counts, and blocked-work-order exclusion from downstream apply syncs.
- Frontend implementation is present in current HEAD and passes the focused partial-blocker contract, adjacent replan contracts, and TypeScript check.
- Real E2E is PASS on the user-authorized `芋道源码/admin` task-owned fixture, including the 2026-08-05 rerun: it creates one attributable `BLOCKING` issue through the calendar UI, verifies the schedule-order row is red and exposes the reason, then closes the issue through the UI and verifies the row is cleared. Only the two expected MES write APIs were called in each run.
- Final backend regression re-run remains pending because the latest attempt is blocked by unrelated MES QA regulation testCompile errors before the target Surefire test starts; task should not be marked completed until it passes or the blocker is explicitly accepted.

## Final Status

blocked
