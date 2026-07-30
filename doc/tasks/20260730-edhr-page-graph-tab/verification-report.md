# Verification Report

## Scope

- 新增 eDHR “批记录页面关系图”共享页签、隐藏路由和独立页面。
- 页面节点代表页面或业务入口，不代表工艺路线工序；不写回工艺路线配置。

## Verification Results

- PASS: `node tests/e2e/edhr-batch-page-graph-tab-static.spec.js`
- PASS: `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- PASS: `pnpm ts:check`

## Contract Checks

- 页签：`EdhrBatchRecordTabs.vue` 已加入 `批记录页面关系图` 和 `pageGraph` 路由映射。
- 路由：`remaining.ts` 已加入 `/mes/pro/feedback/edhr-batch-page-graph`，权限复用 `mes:pro-edhr-batch-execution:query`。
- 页面：`BatchPageGraphPage.vue` 提供 `data-edhr-page-graph`、`data-edhr-page-node`、`data-edhr-page-edge` 稳定选择器。
- 节点：包含生产工单、生产填写、PQC填写、工序池、班组长复核、FIFO分配、EDHR审核副本、正式批记录、归档、MES工序/班组设置。
- 边界：未确认正式路由的节点显示 `待接入` 并使用禁用状态，不执行假跳转。

## Closeout Status

- Current status: `ready_for_closeout`。
- Blocker: 当前工作区存在其它任务文档改动 `doc/tasks/20260730-route-admin-list-layout-unification/`，为避免混入非本任务改动，未执行最终 closeout commit / push。
