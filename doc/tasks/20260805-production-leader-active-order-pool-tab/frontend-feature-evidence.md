# Frontend Feature Evidence

## Feature

- Feature goal: “新增活跃订单” dialog uses one remote searchable Element Plus `el-select` labeled `订单号`.
- Non-goals: no transfer association entry in the add flow; existing transfer trace remains read-only and historical data is preserved.
- Owned files: `TeamLeaderWorkbenchPage.vue`, `teamLeader.ts`, active-order static contracts, RRM real-flow script, and team-leader real-flow syntax.

## Acceptance

- The select displays production work order code and binds selected `workOrderId`.
- Free text without selecting a real candidate is blocked with `请选择订单号` and no add request is sent.
- Successful add calls `addTeamLeaderActiveOrder({ workOrderId })`, closes the dialog, clears form state, and reloads the active-order list.
- Candidate search errors clear options, clear selection, and show an explicit error.
- Eligible candidate options display a green “符合要求” marker and remain first because the frontend preserves backend candidate order.
- The production leader tab header displays de-duplicated responsible route names from formal process-config `routeName` rows.
- Route ID, route version ID, and transfer ID inputs are removed from the add dialog and request type.

## BDD

- BDD: 选择订单号候选加入 -> Given 生产组长打开新增活跃订单弹窗 When 输入工单编号并选择下拉候选 Then 前端只提交候选对应 `workOrderId`。
- BDD: 未选择候选阻塞 -> Given 生产组长只输入自由文本 When 点击加入活跃订单 Then 前端提示“请选择订单号”且不发起新增写请求。
- BDD: 候选绿色展示 -> Given 订单号候选包含 `eligible=true` When 下拉渲染候选 Then 该候选显示绿色“符合要求”标记，不符合候选显示原因。
- BDD: 负责路线页签展示 -> Given 正式工序配置接口返回当前生产组长负责的路线工序 When 生产组长任一模块页签栏渲染 Then 页签右侧显示去重后的工艺路线名称，不使用表单槽位、活跃订单、路线编码或路线 ID 推断名称。
- BDD: 调拨追溯只读拆分 -> Given 已有正式调拨关联数据 When 查看活跃订单追溯 Then 页面只读展示追溯表格，不通过新增弹窗写入补数据。

## RED

- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL, initial active-order tab/add-dialog contract was missing before implementation.
- RED: RRM/static contracts were updated to reject old `routeId` / `routeVersionId` / `transferIds` add payload and require candidate selection.
- RED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL, stale adjacent contract still required route/version/transfer fields after the add request was narrowed.
- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL, dropdown options lacked eligibility template and green “符合要求” marker.
- RED: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL, active-order maintenance lacked candidate eligibility marker.
- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL, production module tab strips lacked `data-production-leader-responsible-routes`.
- RED: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL, the workbench page lacked the responsible-route header marker.

## GREEN

- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> PASS.
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: `node --check tests/e2e/team-leader-workbench-real-flow.e2e.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/production-leader-active-order-pool-tab-static.spec.js IntRuoyiFronted/tests/e2e/team-leader-workbench-static.spec.cjs doc/tasks/20260805-production-leader-active-order-pool-tab/task.md doc/tasks/20260805-production-leader-active-order-pool-tab/execution-log.md` -> PASS.

## Verification

- Loading state: `activeOrderCandidateLoading` is bound to the remote select.
- Error state: `activeOrderCandidateError` is rendered and `ElMessage.error` surfaces candidate search failure.
- Eligibility state: `TeamLeaderActiveOrderCandidateRespVO` exposes `eligible` / `ineligibleReason`; `el-option` renders `team-leader-workbench__active-order-candidate.is-eligible` with green `#16a34a`.
- Responsible-route state: `productionResponsibleRouteNames` reads only `processConfigRows.value[].routeName`, de-duplicates route names, and renders the list in `data-production-leader-responsible-routes` beside every production module tab strip.
- Empty/free-text state: `requirePositiveNumber(activeOrderForm.workOrderId, '请选择订单号')` blocks submission before API call.
- Request contract: `TeamLeaderActiveOrderAddReqVO` contains only `workOrderId`.
- E2E preflight: `node tests/e2e/team-leader-workbench-real-flow.e2e.js` -> BLOCKED because required `TLW_*` variables are absent; generated blocker evidence in `IntRuoyiFronted/test-results/team-leader-workbench-real-flow/result.json`.

## Blockers

- Full write-type Playwright E2E is blocked by missing `TLW_FRONTEND_URL`, `TLW_BACKEND_URL`, `TLW_TENANT`, `TLW_USERNAME`, `TLW_PASSWORD`, and task-owned production order/process/device/signature fixture IDs.
- Because required real E2E is blocked, the task is not marked completed and no commit/push is performed under project rules.
