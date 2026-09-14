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
- Real E2E syntax GREEN: `node --check tests/e2e/mes-pro-schedule-order-partial-replan-blockers-real-readonly.e2e.js` -> PASS.
- Fixture E2E locator RED: `$env:MES_PARTIAL_REPLAN_E2E_PASSWORD=<local-test-credential>; $env:PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH='C:\Program Files\Google\Chrome\Application\chrome.exe'; node tests\e2e\mes-pro-schedule-order-partial-replan-blockers-real-fixture.e2e.js` -> FAIL after creating task-owned issue `19254`. The filtered page visibly rendered source work order `881MO098538`, the red blocking reason `E2E_PARTIAL_REPLAN_BLOCKER_20260804154148 自动重排局部阻断红行验证`, zero page/console errors, and only the two expected MES writes, but the script waited for hidden schedule-order code `SCH-881MO098538-20260707-0001` inside the row. Failure cleanup resolved issue `19254` through the real calendar UI.
- Fixture E2E locator root cause: the `芋道源码/admin` user column settings hide the “排产工单号” column while keeping “来源生产工单号” visible. The fixture now requires `erpWorkOrderCode` during real candidate discovery and locates the filtered row by that visible business key rather than by a hidden column.
- Fixture E2E syntax GREEN: `node --check tests\e2e\mes-pro-schedule-order-partial-replan-blockers-real-fixture.e2e.js` -> PASS.
- Fixture E2E whitespace GREEN: `git diff --check -- IntRuoyiFronted/tests/e2e/mes-pro-schedule-order-partial-replan-blockers-real-fixture.e2e.js doc/tasks/20260804-mes-partial-replan-blockers/execution-log.md` -> PASS; Git only warned that LF will be normalized to CRLF on next checkout.
- User-authorized fixture E2E GREEN: local `8081/48081`, Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe`, `芋道源码/admin`, task-owned issue path -> PASS. Evidence: schedule order `SCH-881MO098538-20260707-0001`, source work order `881MO098538`, work order ID `925867`, issue date `2026-08-31`, created issue `19255`, visible reason `阻断：E2E_PARTIAL_REPLAN_BLOCKER_20260804160047 自动重排局部阻断红行验证`, red background `rgb(255, 241, 240)`, expected MES writes exactly `POST /admin-api/mes/pro/auto-schedule/issues` and `PUT /admin-api/mes/pro/auto-schedule/issues/resolve`, unexpected MES writes `0`, page errors `0`, console errors `0`, cleanup `resolved-via-ui-and-row-cleared`.
- Fixture cleanup read-only GREEN: authenticated final API check for `workOrderId=925867&severity=BLOCKING` found task marker issues `19252, 19253, 19254, 19255`; unresolved task marker issue count `0`.
- User-requested E2E rerun GREEN on 2026-08-05: local `8081/48081`, Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe`, `芋道源码/admin`, task-owned fixture -> PASS. Evidence: schedule order `SCH-881MO098538-20260707-0001`, source work order `881MO098538`, work order ID `925867`, issue date `2026-08-31`, created issue `19256`, visible reason `阻断：E2E_PARTIAL_REPLAN_BLOCKER_20260804162805 自动重排局部阻断红行验证`, red background `rgb(255, 241, 240)`, expected MES writes exactly `POST /admin-api/mes/pro/auto-schedule/issues` and `PUT /admin-api/mes/pro/auto-schedule/issues/resolve`, unexpected MES writes `0`, page errors `0`, console errors `0`, cleanup `resolved-via-ui-and-row-cleared`.
- User-requested E2E cleanup read-only GREEN on 2026-08-05: authenticated final API check for `workOrderId=925867&severity=BLOCKING` found task marker issues `19252, 19253, 19254, 19255, 19256`; unresolved task marker issue count `0`.
- User challenge on 2026-08-05: prior E2E evidence did not cover the screenshot path where `芋道源码/admin` selected schedule orders, clicked `开始重排`, then clicked `确认应用重排` and the progress stayed at `90%`. Acknowledged previous evidence only covered task-owned blocker fixture red-row/reason/cleanup, not full-select apply.
- BDD: Full-select replan apply must not stall at skipped-row confirmation -> Given `芋道源码/admin` selects all current-page selectable schedule orders and preview contains per-work-order blockers or skipped rows, When the user confirms the replan start date once, Then the page must apply all schedulable work orders without opening a second blocking confirmation, blocked work orders remain visible with reasons, and progress must leave `90%` after apply completes.
- RED: `node tests/e2e/mes-pro-schedule-order-replan-skipped-selected-confirm-static.spec.js` -> FAIL, expected reason: old implementation still imported `ElMessageBox` and required a blocking skipped-row confirm dialog.
- RED: `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-static.spec.js` -> FAIL, expected reason: old apply path still awaited `confirmSkippedSelectedReplanRows(freshPreview)` before calling apply.
- GREEN: replaced skipped-selected modal confirmation with non-blocking `ElNotification`; `notifySkippedSelectedReplanRows(freshPreview)` now informs the user that skipped/blocked rows remain while continuing directly to `replanApply`.
- GREEN: `node tests/e2e/mes-pro-schedule-order-replan-skipped-selected-confirm-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-schedule-order-replan-apply-timeout-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-schedule-order-apply-replan-toast-static.spec.js` -> PASS.
- GREEN: `pnpm.cmd ts:check` -> PASS.
- Syntax/whitespace GREEN: `node --check tests/e2e/mes-pro-schedule-order-full-select-replan-admin-real.e2e.js` -> PASS; `git diff --check -- IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue IntRuoyiFronted/tests/e2e/mes-pro-schedule-order-replan-skipped-selected-confirm-static.spec.js IntRuoyiFronted/tests/e2e/mes-pro-schedule-order-partial-replan-blockers-static.spec.js IntRuoyiFronted/tests/e2e/mes-pro-schedule-order-full-select-replan-admin-real.e2e.js` -> PASS with LF-to-CRLF warnings only.
- Runtime note: local frontend `8081` returned HTTP 200. Local backend `48081` was initially unavailable and standard restart was blocked by concurrent backend restart Maven processes; after stopping only this task's failed restart process and leaving unrelated concurrent processes running, a task-owned backend process was started from existing independent runtime jar `output/runtime/int_main/backend-runtime-control-20260805-090357.jar`. Health check returned `UP` before E2E.
- Real full-select apply E2E GREEN: `node tests/e2e/mes-pro-schedule-order-full-select-replan-admin-real.e2e.js` on local `8081/48081`, Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe`, `芋道源码/admin`, start date `2026-08-06` -> PASS. The script selected 12 current-page selectable schedule orders and observed target requests `POST /admin-api/mes/pro/schedule-order/preflight`, `POST /admin-api/mes/pro/auto-schedule/replan/preview`, and `POST /admin-api/mes/pro/auto-schedule/replan/apply`.
- Real full-select apply E2E evidence: preflight returned `code=0`, summary `passCount=7`, `warnCount=5`, `blockedCount=0`; preview returned `code=0`, summary `workOrderCount=12`, `generatedTaskCount=490`, `preservedTaskCount=10`, `blockingIssueCount=1`, `appliedWorkOrderCount=11`, `blockedWorkOrderCount=1`, `skippedWorkOrderCount=0`, `shortageCount=319`; apply returned HTTP `200`, business `code=0`, same summary. Progress snapshots moved `36% -> 61% -> 72% -> 81% -> 85% -> 0%`, `confirmDialogVisible=false` throughout, final `dateDialogVisible=false`, `pageErrors=[]`, `consoleErrors=[]`.
- Real full-select apply E2E artifacts: `doc/tasks/20260804-mes-partial-replan-blockers/artifacts/full-select-replan-admin-e2e-2026-08-05T02-23-40-841Z.json` and `.png`. Final screenshot shows success toast `应用重排成功：应用工单 11 个，标记阻断 1 个，跳过 0 个，正式排程已更新，新增任务 490 个，删除任务 490 个，保留任务 6 个。`
- Artifact hygiene GREEN: full-select E2E JSON artifacts were reduced to request/response summaries and runtime `calendarContextToken` / `idempotencyKey` values were redacted; `rg` for the raw observed token/idempotency prefixes under the artifact directory returned no matches.
- Experience consolidation GREEN: merged the reusable lesson into `docs/e2e-rules.md#MES 手动重排全选应用完成门禁` and added routing keywords to `docs/experience-index.md`. Rule: full-select manual replan E2E must observe the real `preflight / preview / apply` requests and final UI convergence; fixture red-row verification, preview summary, or intermediate progress cannot be used as full-select apply evidence.
- User follow-up on 2026-08-05: skipped/blocked notification should not show excessive details; it should only show the affected work order and the blocked reason.
- BDD: Concise skipped-row blocker notification -> Given selected schedule orders include rows blocked from this replan preview, When the non-blocking notification is shown, Then each listed item shows only the work order and blocked reason, and does not show product code, product name, or other detail fields.
- RED: `node tests/e2e/mes-pro-schedule-order-replan-skipped-selected-confirm-static.spec.js` -> FAIL, expected reason: `SkippedSelectedReplanRow` still carried `productCode/productName` and the notification rendered `(${productCode} / ${productName})`.
- GREEN: simplified `notifySkippedSelectedReplanRows` to render `工单：<code>；原因：<reason>` only, removed product fields from the skipped-row notification model, and kept the notice non-blocking.
- GREEN: `node tests/e2e/mes-pro-schedule-order-replan-skipped-selected-confirm-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-schedule-order-apply-replan-toast-static.spec.js` -> PASS.
- GREEN: `node --check tests/e2e/mes-pro-schedule-order-full-select-replan-admin-real.e2e.js` -> PASS.
- GREEN: `pnpm.cmd ts:check` -> PASS.
- Whitespace GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue IntRuoyiFronted/tests/e2e/mes-pro-schedule-order-replan-skipped-selected-confirm-static.spec.js` -> PASS; Git only warned LF will be normalized to CRLF.

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
  - Re-run on 2026-08-05 with the same target Maven command no longer hung; it reached `yudao-module-mes` `testCompile` and failed before Surefire because unrelated `MesQaInspectionRegulationServiceTest` references missing `MesQaInspectionRegulationProjectStatusRespVO` getters: `getProductId`, `getConfigured`, `getRegulationId`, `getLifecycleStatus`, and `getRegulationCode`. Target `MesProAutoScheduleAlgorithmContractTest` did not start.
  - Real page readonly E2E: `$env:MES_PARTIAL_REPLAN_E2E_PASSWORD=<local-test-credential>; $env:PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH='C:\Program Files\Google\Chrome\Application\chrome.exe'; node tests\e2e\mes-pro-schedule-order-partial-replan-blockers-real-readonly.e2e.js` -> BLOCKED. Browser logged into `http://127.0.0.1:8081` as `测试租户/aoteman`, used backend `http://127.0.0.1:48081`, and returned `mesWriteRequestCount=0`, `pageErrorCount=0`, `consoleErrorCount=0`. Blocker: 测试租户前 74 条排产工单没有未解决阻断 issue，无法验证阻断工单红行和原因展示.
  - User-authorized admin readonly E2E: same readonly script with `MES_PARTIAL_REPLAN_E2E_TENANT=芋道源码` and `MES_PARTIAL_REPLAN_E2E_USERNAME=admin` -> BLOCKED because the tenant initially had no unresolved blocking display row. This data precondition was closed by adding and running the task-owned fixture E2E above, which creates and resolves its own `BLOCKING` issue through real pages.
  - Write-type real E2E was not run against `mes-schedule-order-replan-881mo090863-real-flow.e2e.js` because that script can click `开始重排` and apply changes to an existing non-task-owned schedule order. Per `docs/e2e-rules.md`, this requires task-owned traceable data or explicit approval.
  - `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> BLOCKED by unrelated missing file `src/views/mes/pro/route/RouteFlowConfigPanel.vue`.
  - `node tests/e2e/mes-pro-schedule-order-usability-static.spec.js` -> BLOCKED before this task's assertions by unrelated concurrent admission quick-filter assertion.

## Blockers

- Backend final JUnit re-run pending because the 2026-08-05 rerun now fails in unrelated MES QA regulation `testCompile` before the target Surefire test can start.
- Real E2E red-row verification is no longer blocked: user-authorized `芋道源码/admin` fixture created task-owned issues `19255` and `19256` in separate real-path runs, verified the blocked row and reason, and closed each issue through the UI. Final read-only cleanup found `0` unresolved task marker issues for work order `925867`.
- Real full-select apply E2E is no longer blocked: `芋道源码/admin` current-page full-select apply path now completes with observed apply request and final UI success; the prior 90% stall was caused by the skipped-row second confirmation blocking the confirmed apply flow.
- Current shared branch has staged and unstaged unrelated concurrent task changes; do not commit, clean, or push this task until the shared staging state is reconciled.
