# Active Order Null WorkOrder Regression Evidence

## Bug Summary

- 用户反馈：加入活跃订单池时页面提示 `请求参数不正确:不能为null`；截图复现为用户已在“订单号”框输入完整生产工单编号 `881MO093613`，但未点击下拉候选。
- 期望行为：新增活跃订单只允许提交正式生产工单候选对应的 `workOrderId`；若用户已输入完整订单号且精确命中候选，则提交前自动解析候选；未命中、清空或搜索失败时前端提示 `请选择订单号`，不得调用 `/active-order/add`，后端不得收到 `workOrderId=null`。

## Expected

- 只输入完整订单号且精确命中候选时，前端必须解析出候选 `workOrderId` 再发起新增写请求。
- 未命中真实候选时，前端必须 fail fast，提示 `请选择订单号` 并阻止新增写请求。
- 已选择真实候选时，新增请求体只能包含该候选对应的 `workOrderId`。

## Reproduction

- RED: `workdir=IntRuoyiFronted; node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，新增弹窗缺少 `@change="handleActiveOrderCandidateChange"` 与 `@clear="handleActiveOrderCandidateClear"`，不能证明提交前绑定了真实候选。
- RED: `workdir=IntRuoyiFronted; node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL，提交仍直接使用 `requirePositiveNumber(activeOrderForm.workOrderId, '请选择订单号')`，缺少候选级校验函数。
- RED: `workdir=IntRuoyiFronted; node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，旧修复仍缺少 `activeOrderCandidateKeyword` 与按订单号精确解析候选的提交前路径。
- RED: `workdir=IntRuoyiFronted; node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL，旧修复仍要求点击候选，不能覆盖截图中的“完整输入后直接提交”路径。

## Root Cause

- 前端只校验 `activeOrderForm.workOrderId` 是正数，没有保存并校验“当前值来自远程候选列表中的真实选项”。
- 第一轮修复要求点击候选，但 Element Plus 远程下拉在用户只输入完整订单号时不会更新 `v-model`；若运行态仍走旧提交链路，就会继续把空 `workOrderId` 发到后端。
- 静态合同只覆盖 payload 字段收缩，没有锁定完整订单号输入的精确候选解析行为，因此截图路径仍可能落到后端校验层。

## Fix

- 在 `TeamLeaderWorkbenchPage.vue` 增加 `activeOrderSelectedCandidate`，通过 `@change` 记录真实候选，通过 `@clear`、空搜索、搜索失败和候选刷新失配清除选择。
- 新增 `requireSelectedActiveOrderCandidateWorkOrderId()`；提交 `/active-order/add` 前必须同时满足表单 `workOrderId`、已选候选和当前候选列表一致，否则抛出 `请选择订单号` 并阻止 API 调用。
- 增加 `activeOrderCandidateKeyword` 与 `resolveActiveOrderCandidateByKeyword()`；用户只输入完整订单号时，提交前先用当前候选精确匹配 `workOrderCode`，未命中则即时调用候选搜索接口，精确命中后才提交对应 `workOrderId`。
- 未改后端 `@NotNull workOrderId`，没有引入兜底、默认值或兼容旧字段。

## Regression Test

- 更新 `production-leader-active-order-pool-tab-static.spec.js`，要求远程下拉绑定 change/clear 事件，并要求提交走候选校验函数。
- 更新 `team-leader-workbench-static.spec.cjs`，要求 `addTeamLeaderActiveOrder` 只接收 `requireSelectedActiveOrderCandidateWorkOrderId()` 返回值，并验证该函数会在未选真实候选时抛 `请选择订单号`。
- 更新同两个静态合同，要求保存输入的订单号关键字、按 `workOrderCode` 精确匹配候选，并在提交前 `await requireSelectedActiveOrderCandidateWorkOrderId()`。

## Verification

- GREEN: `workdir=IntRuoyiFronted; node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `workdir=IntRuoyiFronted; node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `workdir=IntRuoyiFronted; pnpm ts:check` -> PASS。
- GREEN: `workdir=E:\IntRuoyi; git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/production-leader-active-order-pool-tab-static.spec.js IntRuoyiFronted/tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS，仅 CRLF working-copy 提示。

## Blockers And Follow-Up

- BLOCKED: `workdir=IntRuoyiFronted; node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL，当前缺少 PQC 过程检验汇集稳定选择器 `data-pqc-process-inspection-aggregation`，不属于本次活跃订单空值修复。
- BLOCKED: `workdir=IntRuoyiFronted; node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> FAIL，当前 PQC 组长切换后提交看板多维筛选重置链路合同失败，不属于本次活跃订单空值修复。
- BLOCKED: 写入型真实 Playwright E2E 仍缺少任务自有 `TLW_*` 测试租户、账号、工单、工序、设备和签名夹具；未使用 mock、自由输入、隐藏字段或 API-only 替代。
