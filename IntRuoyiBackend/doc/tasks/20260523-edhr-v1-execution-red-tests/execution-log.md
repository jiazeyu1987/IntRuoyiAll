# Execution Log: eDHR V1 执行节点第一批后端 RED 测试

BDD: 按执行上下文打开时复用活动记录 -> Given 同一 `workOrderId + routeProcessId + batchRecordReportId` 已存在一条活动中的执行记录 / When 调用 `openOrCreateByContext` / Then 系统必须直接返回已有活动记录而不是重复创建新记录。

BDD: 执行上下文可以从 route-process 默认批记录绑定解析 -> Given 工艺路线工序已配置 `batchRecordReportId` / When 调用按上下文打开入口 / Then 系统必须使用该默认绑定解析批记录模板或报表上下文，不要求调用方重复传入模板主键。

BDD: 缺少默认批记录绑定时立即失败 -> Given 工艺路线工序没有配置默认 `batchRecordReportId` / When 调用按上下文打开入口 / Then 系统必须返回显式错误并阻止创建空绑定执行记录。

BDD: 提交必须满足 DCC 授权与密码签名 -> Given 当前执行记录进入提交动作且流程要求 DCC 电子签名 / When 调用提交接口但缺少授权语义或密码签名 / Then 系统必须拒绝提交，不允许沿用旧的无签名提交路径。

BDD: controller 暴露执行上下文契约 -> Given 前端需要从执行入口上下文进入 eDHR V1 页面 / When 访问执行控制器 / Then 控制器必须提供 `entry-context` 与 `open-or-create-by-context` 契约，并使用独立请求 VO 承载上下文字段。

RED: `mvn --% -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 目标测试已编译并执行，失败原因为 eDHR V1 执行上下文契约未实现：

- `MesProBatchRecordExecutionEntryContextReqVO` 不存在，controller/service 的 `entry-context` 契约缺失。
- `MesProBatchRecordExecutionOpenOrCreateByContextReqVO` 不存在，`openOrCreateByContext` 入口缺失。
- `MesProBatchRecordExecutionDO#getRouteProcessId` 不存在，执行实体尚未承载 route-process 上下文字段。
- 现有提交链路仍是 `submitBatchRecordExecution(Long id)`，未升级到带密码签名语义的提交契约。

RED: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro --task-id 20260523-edhr-v1-execution-red-tests --mode preview` -> BLOCKED, 当前仓库是 linked worktree，脚本检测到不能对 `int_main` 做 `ff-only` 预览，且主工作树与当前 worktree 仍有未提交改动，因此只记录阻塞不执行清理。
