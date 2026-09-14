# Frontend Feature Evidence

## Feature Goal And Non-goals

- Goal: “订单异常上报”只展示订单号和异常说明，并只提交 `workOrderId` 与 `abnormalDescription`。
- Non-goals: 不改活跃订单池维护、班组配置里的工序异常原因维护、报工复核筛选或 PQC 多维筛选。

## Requirements And Acceptance

- Acceptance: 用户上报异常时不需要填写工序ID。
- Acceptance: 用户上报异常时不需要选择异常原因。
- Acceptance: 请求 payload 不包含 `routeProcessId`、`processId`、`sourceEventId`、`abnormalReasonCode`。

## UI Entry Points And Owned Files

- Entry: 生产组长工作台“异常”页签，“订单异常上报”模块。
- Owned frontend files: `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`、`src/api/mes/pro/processpool/teamLeader.ts`。
- Focused contract: `tests/e2e/work-order-abnormal-minimal-report-static.spec.js`。

## API Contracts And Data States

- Frontend request type: `WorkOrderAbnormalReportReqVO` keeps only `workOrderId` and `abnormalDescription`.
- The UI still selects from active order options to resolve the formal `workOrderId`, but the user-facing label is “订单号” rather than the internal active-order concept.

## BDD Scenarios

- BDD: 订单异常上报只填订单号和异常说明 -> Given 生产组长进入“异常”页签 / When 选择订单号并填写异常说明后提交 / Then 页面不展示工序ID和异常原因，提交 payload 只包含订单号对应的 `workOrderId` 与 `abnormalDescription`。

## RED GREEN Evidence

- RED: `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> FAIL，旧页面没有“订单号”字段，仍展示旧异常上报字段。
- GREEN: `node tests/e2e/work-order-abnormal-minimal-report-static.spec.js` -> PASS。
- REGRESSION BLOCKED: `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> FAIL at existing PQC multi-filter reset assertion, outside this task-owned change.

## Responsive Accessibility Loading Empty Error Permission

- No layout expansion was introduced; removed two form rows from the existing Element Plus form.
- Required validation remains on order number and abnormal description.
- Permission and endpoint are unchanged: `mes:pro-process-pool-team-leader:abnormal`.

## Verification

- Verification: focused static contract passed with node tests/e2e/work-order-abnormal-minimal-report-static.spec.js.
ode tests/e2e/work-order-abnormal-minimal-report-static.spec.js.
- Verification: broad regression static contract remains blocked by unrelated PQC multi-filter assertion.

## Blockers And Follow-up Skills

- Existing large static contract has an unrelated PQC multi-filter failure; this task uses the dedicated static contract per frontend static contract isolation gate.