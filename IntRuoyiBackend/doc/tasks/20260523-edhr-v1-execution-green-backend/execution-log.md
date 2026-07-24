# Execution Log: eDHR V1 执行节点后端 GREEN

BDD: 按执行上下文打开时复用活动记录 -> Given 同一 `tenantId + workOrderId + taskId + routeProcessId + workstationId + batchCode` 已存在活动执行记录 / When 调用 `openOrCreateByContext` / Then 系统必须直接返回已有记录而不是重复创建。

BDD: 执行入口按默认绑定解析批记录上下文 -> Given `routeId + processId` 默认绑定到一条 `batchRecordReportId` 且 route-process 已配置默认批记录报表 / When 调用 `entry-context` / Then 系统必须返回 `routeProcessId`、`batchRecordReportId`、批次等执行入口上下文，不要求前端先调 route-process 查询接口。

BDD: 缺少默认批记录绑定时立即失败 -> Given route-process 未配置默认 `batchRecordReportId` / When 调用 `open-or-create-by-context` / Then 系统必须显式报错并阻止创建空绑定执行记录。

BDD: 提交必须满足签名授权与密码校验 -> Given 当前执行记录仍处于可提交状态 / When 提交请求缺少电子签名授权或密码校验失败 / Then 系统必须拒绝提交；When 授权开启且密码校验通过 / Then 系统必须写入 MES 自有签名记录并完成提交。

BDD: 运行态快照唯一来源于 executionSnapshotJson -> Given 新旧字段并存 / When 创建或打开执行记录 / Then 运行态必须以 `executionSnapshotJson` 为主，旧 `sheetLayoutJson/metaJson` 仅作为迁移来源。

RED: `mvn --% -pl yudao-module-mes -am -Dtest=MesBatchRecordBaseSchemaTest,MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionSignatureServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 初次扩展测试后编译失败点集中在缺少上下文 VO、缺少签名服务/实体/mapper、提交请求缺少 password/comment、执行 DO 缺少上下文字段与 executionSnapshotJson。

GREEN: `mvn --% -pl yudao-module-mes -am -Dtest=MesBatchRecordBaseSchemaTest,MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionSignatureServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS

GREEN: 已实现 `entry-context` / `open-or-create-by-context` 控制器与服务契约，默认从 `routeProcess.batchRecordReportId` 解析批记录报表绑定，并在缺少绑定时 fail-fast。

GREEN: 已为 execution 聚合新增 `routeProcessId`、`taskId`、`workstationId`、`batchRecordReportId`、`executionSnapshotJson` 字段；运行态快照由模板旧字段迁移生成。

GREEN: 已新增 MES 自有执行签名服务与签名记录表，提交时复用 `DccElectronicSignatureAuthorizationService` + `AdminUserService#isPasswordMatch` 语义，不写 DCC `controlled_file_signature`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro --task-id 20260523-edhr-v1-execution-green-backend --mode preview` -> BLOCKED, linked worktree 无法对 `int_main` 做 ff-only 预览，且主工作树与当前 worktree 均有未提交改动。
