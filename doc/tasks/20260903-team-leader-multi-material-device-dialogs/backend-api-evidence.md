# Scope
生产组长报工事件和修改提交链路支持逐物料数量，同时保留设备参数所属设备身份。

## Contract
- `ProcessPoolTimelineEventRespVO` 暴露 `materialDetails`，每项包含物料 ID、编码、名称、完成数量和损耗数量。
- `ProcessPoolTimelineServiceImpl` 从正式事件 payload 投影 `materialDetails`。
- `ProcessPoolProductionReportCorrectionReqVO` 与 service command 接收 `materialDetails`。
- 修改提交按物料 ID 更新完成数量和损耗数量，未知物料或缺少正式物料上下文时 fail fast。
- 设备参数提交继续要求 `deviceId` 和 `parameterCode`，不因多设备展示丢失身份。

## Validation
- 请求 VO、command、service 均有逐物料字段。
- service 层只更新正式 payload 已存在的物料明细，不构造默认物料。
- 缺少上下文或未知物料 ID 会抛出明确业务错误。

## BDD
BDD: 修改报工逐物料数量 -> Given 报工事件 payload 含多条 `materialDetails` When 生产组长提交修改物料完成/损耗数量 Then 后端按 materialId 更新对应行并记录差异。

BDD: 缺物料上下文失败 -> Given 修改请求携带 `materialDetails` 但事件 payload 缺正式物料上下文 When 提交修改 Then 后端 fail fast，不创建默认物料或静默成功。

## RED
RED: `node IntRuoyiFronted\tests\e2e\team-leader-multi-material-device-dialogs-static.spec.cjs` -> FAIL，API 类型缺少 `ProcessPoolTimelineMaterialDetailVO`。

## GREEN
GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProcessPoolProductionReportCorrectionContractTest,MesProcessPoolProductionReportCorrectionServiceTest,MesFrontlineProcessMaterialServiceTest,MesProRouteFlowConfigServiceImplTest,MesProRouteServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，80 tests, 0 failures, 0 errors。

GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，生成 `yudao-server\target\yudao-server-exec.jar`。

## Verification
- worktree 后端通过 `scripts\runtime\start-branch-backend.ps1 -Slot 11` 启动在 `48092`。
- `/actuator/health` 返回 `status=UP`。
- 真实 E2E 后续只通过真实前端操作弹框，后端/API 只作为只读健康和结果证据。

## Blockers
- 真实前端 E2E 缺少本轮临时登录变量 `TLW_USERNAME` / `TLW_PASSWORD`。
