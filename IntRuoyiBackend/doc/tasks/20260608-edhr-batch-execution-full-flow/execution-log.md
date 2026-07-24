# eDHR 批次执行后端执行日志

## 2026-06-08

- BDD: 创建批次执行 -> Given 测试租户存在真实工单、产品、工艺路线和默认批记录绑定 When 用户创建 eDHR 批次执行 Then 后端生成批次主记录和按路线排序的工序任务。
- BDD: 缺默认批记录阻塞关闭 -> Given 任一必需工序缺默认批记录 When 用户关闭批次 Then 后端返回明确阻塞项，不跳过工序。
- BDD: 多人签名校验 -> Given 同一单张执行记录缺复核或审批签名 When 用户关闭批次 Then 后端拒绝关闭。
- BDD: 归档只允许关闭后生成 -> Given 批次未关闭 When 生成批次归档 Then 后端 fail fast。

- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest test` -> FAIL，预期原因：`MesProEdhrBatchExecutionService`、Controller、VO、DO、Mapper、错误码尚不存在，测试编译失败。
- RED: 后端 controller 测试 -> FAIL，同一次 testCompile 已覆盖 `MesProEdhrBatchExecutionControllerTest`，失败原因为 controller 与 VO 尚不存在。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest test` -> PASS，6 个 service 场景通过，覆盖创建、幂等、阻塞、打开任务、关闭签名校验、归档前置。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionControllerTest test` -> PASS，2 个 controller 合约场景通过，覆盖 endpoint 和权限码。
- GREEN: `mvn -pl yudao-module-mes '-Dtest=MesProEdhrBatchExecutionServiceTest,MesProEdhrBatchExecutionControllerTest,MesProEdhrBatchExecutionArchiveControllerTest' test` -> PASS，10 个批次执行 service/controller/归档 controller 场景通过。
- REGRESSION: `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionArchiveContractTest,MesProBatchRecordExecutionSignatureServiceTest' test` -> PASS，81 个现有单张 eDHR 执行、签名、归档测试通过。

## 2026-06-09

- BDD: 批次打开工序任务携带生产任务与工作站上下文 -> Given 批次工序来自目标工艺路线且工单有唯一同工序生产任务 When 用户点击打开填写 Then 单张 eDHR 执行记录必须写入 `taskId` 和 `workstationId`，主数据追溯不再因 TASK/WORKSTATION 缺失阻塞。
- RED: 真实 Playwright 单表提交 -> FAIL, expected reason: `openTask` 未传递 `taskId/workstationId`，`domain-trace/verify` 返回 `TASK_REQUIRED` 与 `WORKSTATION_REQUIRED`。
- GREEN: `mvn -pl yudao-module-mes '-Dtest=MesProEdhrBatchExecutionServiceTest,MesProEdhrBatchExecutionControllerTest,MesProEdhrBatchExecutionArchiveControllerTest' test` -> PASS，15 个批次执行 service/controller/归档 controller 场景通过，新增覆盖缺生产任务上下文 fail fast、同工序任务精确传参、单任务工单确定性传参。
- REGRESSION: `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionArchiveContractTest,MesProBatchRecordExecutionSignatureServiceTest' test` -> PASS，81 个现有单张 eDHR 执行、签名、归档测试通过。
- GREEN: `mvn -pl yudao-server -am '-Dmaven.test.skip=true' package` -> PASS，后端 worktree jar 成功打包并在 `48081` 启动，健康检查 HTTP 200。
- E2E: Playwright 真实测试租户 -> PASS，批次 `EDHR-BATCH-122-FULL-0609020810` 关闭后状态 `40`，`task_total=21`、`task_approved_count=15`、`blocked_count=0`，批次归档 `SEALED`。
- RED: `git commit -m "任务: 实现eDHR批次执行后端闭环"` -> FAIL, expected reason: 后端提交门禁要求 `sql/mysql/` 变更必须配套 `script/tests/` 下的 SQL contract test。
- GREEN: `python -X utf8 -m pytest script/tests/test_edhr_batch_execution_schema_sql.py -q` -> PASS，4 个迁移 SQL contract 测试通过，覆盖批次表、工序任务、签名、归档、权限、租户包 JSON fail-fast 与禁止静默覆盖。
- BDD: 融入后 int_main 后端支撑完整 E2E -> Given 合并后的后端主目录 jar 在 `48081` 启动且连接本机测试库 When 前端真实用户完成批次创建、15 张单表签名/追溯/提交/审批、批次关闭、归档、下载、打印和复盘 Then 后端状态闭环为归档终态且不产生阻塞项。
- GREEN: `mvn -pl yudao-server -am '-Dmaven.test.skip=true' package` -> PASS，合并后的后端主目录 jar 重新打包并启动在 `48081`，`/actuator/health` 返回 HTTP 200。
- GREEN: Playwright 融入后完整 E2E -> PASS，测试租户批次 `EDHR-BATCH-122-MAIN-0609013248` 最终 `status=40`，`task_total=21`、`task_approved_count=15`、`blocked_count=0`，归档 ID `2`、`archive_status=SEALED`、PDF `EDHR-BATCH-122-MAIN-0609013248-edhr-final.pdf` 下载成功，打印窗口和复盘页通过前端验证。
