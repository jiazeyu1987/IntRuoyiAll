# Frontend Feature Evidence

## Feature

生产组长工作台新增“损耗原因维护”标准列表区域，按后端授权返回的路线工序展示，独立列显示损耗原因，并通过操作面板执行新增、修改、删除。

## Acceptance

- 生产组长只能看到后端按“工序开始”授权返回的路线工序。
- 多个组长共享同一 `routeProcessId` 下的损耗原因数据。
- 新增、修改、删除通过正式后端接口执行。
- 报工下拉读取 `deviceState.runtimeConfig.defectReasons`，不得使用固定前端列表。

## BDD

- BDD: 标准列表维护损耗原因 -> Given 生产组长有路线工序权限 When 打开工作台 Then 可见 `data-loss-reason-standard-list`、独立损耗原因列和操作面板。
- BDD: 报工下拉来自后端配置 -> Given 后端返回运行配置 When 员工报工 Then 下拉从 `runtimeConfig.defectReasons` 计算。

## RED / GREEN

- RED: `node IntRuoyiFronted\tests\e2e\process-loss-reason-maintenance-static.spec.cjs` -> FAIL, 旧前端缺损耗原因维护区域、独立列、API wrapper 和报工 `lossReasonId` payload。
- GREEN: `node IntRuoyiFronted\tests\e2e\process-loss-reason-maintenance-static.spec.cjs` -> PASS, `PASS: process loss reason maintenance static contract is wired`。
- GREEN: `pnpm.cmd ts:check` -> PASS。

## Verification

- 静态合同断言 `data-team-leader-loss-reason-tab`、`data-loss-reason-standard-list`、`data-loss-reason-column`、`data-loss-reason-operation-panel`、新增/修改/删除文案均存在。
- 静态合同断言 `getTeamLeaderLossReasonPage`、`createTeamLeaderLossReason`、`updateTeamLeaderLossReason`、`deleteTeamLeaderLossReason` 使用正式 `/loss-reasons` 接口。
- 静态合同断言报工提交 payload 包含 `lossReasonId`，且下拉来自 `runtimeConfig.defectReasons`。

## Blockers

- 真实 Playwright 写入型 E2E 缺两个生产组长账号、一个员工账号和可清理样本数据，未执行真实 UI 增删改和跨账号可见性验收。
