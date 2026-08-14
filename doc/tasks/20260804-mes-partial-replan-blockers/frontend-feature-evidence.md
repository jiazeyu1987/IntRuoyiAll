# Frontend Feature Evidence

## Feature Goal

排产工单自动重排确认时，局部阻断不再禁用整批应用；阻断工单在列表中红色提示，并可查看最新阻断原因。全选重排出现跳过/阻断工单时，非阻塞通知只显示工单和阻断原因，不展开产品或其它细节。

## Non-Goals

- 不改变全局日历 token、无选择范围、冻结/完成/取消等现有不可应用门禁。
- 不新增 mock 数据或静默成功。

## Entry Points

- `IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue`
- `IntRuoyiFronted/src/api/mes/pro/task/autoSchedule/index.ts`
- `IntRuoyiFronted/src/api/mes/pro/scheduleorder/index.ts`

## API States

- Summary includes applied, blocked, and skipped counts.
- Schedule order row includes `blockingIssueCount` and `latestBlockingIssueMessage`.

## BDD Scenarios

- BDD: Mixed replan scope applies healthy orders -> Given selected rows include schedulable and blocked work orders, When the user confirms apply, Then the UI notifies skipped rows without blocking and applies the schedulable remainder.
- BDD: Blocked orders visible in list -> Given a row has unresolved blocking issues, When the schedule order list renders, Then the row is red and exposes the latest blocking reason.
- BDD: Concise skipped-row blocker notification -> Given selected rows include work orders blocked from the preview, When the non-blocking notification appears, Then each item shows only the work order and blocked reason.

## Acceptance

- Acceptance: local frozen/finished/canceled/no-scope gates remain blocking.
- Acceptance: global or unattributable replan blockers still disable direct apply.
- Acceptance: attributable work-order blockers do not disable applying healthy selected work orders.
- Acceptance: skipped/blocked notification rows do not carry or render product code, product name, or extra detail fields.

## RED

- RED: `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-static.spec.js` -> FAIL, expected reason: schedule order API type lacked `blockingIssueCount?: number`.
- RED: `node tests/e2e/mes-pro-schedule-order-replan-skipped-selected-confirm-static.spec.js` -> FAIL, expected reason: skipped-row notification model still carried `productCode/productName` and rendered product details.

## GREEN

- GREEN: `node tests/e2e/mes-pro-schedule-order-partial-replan-blockers-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-schedule-order-replan-skipped-selected-confirm-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-schedule-order-replan-single-action-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-schedule-order-apply-replan-toast-static.spec.js` -> PASS.
- GREEN: `node --check tests/e2e/mes-pro-schedule-order-full-select-replan-admin-real.e2e.js` -> PASS.
- GREEN: `pnpm.cmd ts:check` -> PASS.

## UI Checks

- Blocked schedule order rows receive `schedule-order-pool__row--blocked`.
- Latest blocking reason is visible through `schedule-order-pool__blocking-reason` and tooltip content.
- Replan action only locks on global/unattributable blockers; attributable blockers continue directly to apply after `notifySkippedSelectedReplanRows(freshPreview)`.
- Skipped/blocked notification item text is limited to `工单：<code>；原因：<blocked reason>`.
- Apply success message includes applied, blocked, and skipped work-order counts when backend summary provides them.

## Verification

- Verification: focused partial-blocker static contract, adjacent replan static contracts, and TypeScript check passed.
- Verification: broader pool/usability static contracts are blocked by unrelated concurrent changes listed below.

## Blockers

- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` is blocked by unrelated missing `src/views/mes/pro/route/RouteFlowConfigPanel.vue`.
- `node tests/e2e/mes-pro-schedule-order-usability-static.spec.js` is blocked before this task's assertions by an unrelated admission quick-filter assertion from concurrent work.
