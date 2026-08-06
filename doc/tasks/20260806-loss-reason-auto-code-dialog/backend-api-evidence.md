# Scope

后端新增生产组长工序损耗原因接口 `/mes/pro/process-pool/team-leader/loss-reasons` 改为不接收手工原因编码，由服务端按当前 `routeProcessId` 自动生成唯一损耗原因编码并默认启用。非目标范围：不调整班组不良原因接口、历史异常原因保存接口、损耗原因编辑接口、权限判断或数据库 schema。

## Contract

- Request VO：`MesTeamLeaderLossReasonSaveReqVO` 只保留 `routeProcessId` 和 `reasonName`。
- Service BO：`MesTeamLeaderLossReasonSaveReqBO` 只保留 `leaderUserId`、`routeProcessId` 和 `reasonName`。
- Controller：新增接口只构造 `leaderUserId`、`routeProcessId`、`reasonName`，不读取 `reqVO.getReasonCode()`。
- Service：`createLossReason()` 通过 `generateLossReasonCode(routeProcess.getId())` 写入 `reasonCode`，并设置 `enabled(Boolean.TRUE)`。
- Data contract：编码格式为 `LOSS-<routeProcessId>-<###>`，若计数候选重复则递增直到唯一。

## Validation

- 必填校验保留 `leaderUserId`、`routeProcessId`、`reasonName`。
- 新增保存校验不再要求 `reasonCode`，避免前端隐藏字段后触发服务端缺字段错误。
- 权限仍通过 `requireAuthorizedRouteProcess()` / `assertCanMaintainRouteProcess()` 校验维护范围。
- 不新增 fallback、默认成功、异常吞掉或 schema 变更。

## BDD

- BDD: 新增损耗原因服务端生成编号 -> Given create 请求只有 `routeProcessId` 和 `reasonName`, When 服务端保存损耗原因, Then 服务端生成 `LOSS-<routeProcessId>-<###>` 编码并保存启用状态。
- BDD: 新增损耗原因不接受手工编号 -> Given 请求 VO/BO 不包含 `reasonCode`, When Controller 调用 Service, Then 保存链路不读取客户端 `reasonCode`。
- BDD: 编辑已有损耗原因不重编编号 -> Given 已有损耗原因存在, When 调用 update 接口, Then 仅更新名称、启用状态和备注，不重新生成编码。

## RED / GREEN

- RED: `node tests/e2e/team-leader-loss-reason-auto-code-dialog-static.spec.cjs` -> FAIL, 旧 Controller/VO/BO/create payload 仍存在新增手工 `reasonCode` 契约。
- GREEN: `node tests/e2e/team-leader-loss-reason-auto-code-dialog-static.spec.cjs` -> PASS, 静态合同确认新增 VO/BO 移除 `reasonCode`、Controller 不传手工编码、Service 生成编码且校验不要求 `reasonCode`。

## Verification

- Contract verification：`node tests/e2e/team-leader-loss-reason-auto-code-dialog-static.spec.cjs` -> PASS。
- Adjacent frontend/API verification：`node tests/e2e/team-leader-process-config-unified-static.spec.cjs` -> PASS。
- Adjacent role tab verification：`node tests/e2e/production-leader-function-tabs-static.spec.js` -> PASS。
- Type verification：`pnpm ts:check` -> PASS。
- Whitespace verification：`git diff --check b9a752088^ b9a752088 -- <task-owned paths>` -> PASS。
- Maven standard verification：未叠加执行；检查到同模块 Maven PID `47148/49960` 正在运行其它任务 `ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineFilterTest,ProcessPoolTimelineQueryTest`，按共享 `target` 门禁记录环境阻塞。

## Observability

- 新增与编辑审计继续通过 `TeamMaintenanceAuditSupport.insertAudit(...)` 记录 `CREATE_LOSS_REASON` / `UPDATE_LOSS_REASON`。
- 未改日志、权限注解或异常码链路。

## Blockers

- 标准 Maven 编译/JUnit 未完成：当前 `E:\IntRuoyi\IntRuoyiBackend` 同模块 `yudao-module-mes` 已有其它任务 Maven 进程写目标目录，禁止并发叠加。后续空闲时建议复跑 `mvn -pl yudao-module-mes -am "-DskipTests" compile`。
