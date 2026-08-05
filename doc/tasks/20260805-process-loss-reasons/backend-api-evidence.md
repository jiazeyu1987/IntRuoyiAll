# Backend API Evidence

## Scope

AC-D04 后端范围包含生产组长损耗原因维护接口、路线“工序开始”授权解析、员工报工运行配置和报工提交校验。

## Contract

- `GET /mes/pro/process-pool/team-leader/loss-reasons/page` 返回当前登录生产组长通过“工序开始”获得权限的路线工序及共享 LOSS 原因。
- `POST /mes/pro/process-pool/team-leader/loss-reasons` 新增绑定到 `routeProcessId` 的 LOSS 原因。
- `PUT /mes/pro/process-pool/team-leader/loss-reasons/{id}` 修改原因名称、备注或启用状态。
- `DELETE /mes/pro/process-pool/team-leader/loss-reasons/{id}` 停用原因，不允许进入新报工。
- 报工运行配置 `runtimeConfig.defectReasons` 只返回当前 `routeProcessId + LOSS + enabled=true` 的原因。
- 报工提交保存 `lossReasonId`、`lossReasonCodeSnapshot`、`lossReasonNameSnapshot`，并拒绝禁用、删除或跨工序原因。

## Validation

- 权限解析来自 ACTIVE 工艺路线版本快照中的 `routeStartProductionLeaders`，支持 USER/USERS/ROLE。
- LOSS 原因新增时 `leaderUserId=null`，避免以最后编辑组长作为数据所有者。
- 报工提交先执行身份与工序授权，再校验损耗原因，失败时不写入报工、记录本或工序池事件。

## BDD

- BDD: 生产组长只能看到有权限工序 -> Given “工序开始”配置生产组长 When 打开维护列表 Then 只返回该路线下路线工序。
- BDD: 多个生产组长共享同一工序损耗原因 -> Given 同一路线配置多个组长 When 任一组长维护 Then 其他组长看到同一 `routeProcessId` 数据。
- BDD: 禁用或跨工序原因被拒绝 -> Given 原因不属于当前 `routeProcessId` 或未启用 When 提交报工 Then 后端拒绝且不写入。
- BDD: 历史报工保留快照 -> Given 使用原因完成报工 When 后续修改或停用原因 Then 历史报工仍保留快照字段。

## RED / GREEN

- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderRuntimeConfigServiceTest,MesFrontlineRuntimeConfigServiceTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProFrontlineFeedbackSubmitServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 旧后端缺 routeProcess 共享 LOSS 原因、提交校验和快照字段。
- GREEN: 同命令 -> PASS, Tests run: 19, Failures: 0, Errors: 0, Skipped: 0。

## Verification

- `MesProcessPoolTeamLeaderSchemaTest#processLossReasonSchemaMustBeRouteProcessSharedAndSnapshotFeedbackHistory` 覆盖 routeProcess 共享唯一约束和报工快照字段。
- `MesTeamLeaderRuntimeConfigServiceTest#shouldBindLossReasonToRouteProcessSharedByRouteStartProductionLeaders` 覆盖 LOSS 原因不绑定组长个人。
- `MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_returnsEnabledLossReasonsByRouteProcessWithoutLeaderOwnership` 覆盖报工下拉来自后端配置。
- `MesProFrontlineFeedbackSubmitServiceTest#shouldRejectDisabledOrCrossRouteProcessLossReasonBeforeWritingAnyRecord` 覆盖禁用/跨工序原因拒绝。

## Blockers

- 真实写入型 Playwright E2E 缺生产组长/员工账号与任务自有样本数据前置，不能宣称真实用户路径通过。
