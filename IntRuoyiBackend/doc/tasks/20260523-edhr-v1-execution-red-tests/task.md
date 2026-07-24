# 任务：eDHR V1 执行节点第一批后端 RED 测试

## Goal

在不修改生产代码的前提下，为 eDHR V1 执行节点补第一批后端 failing tests，锁定以下核心行为缺口：

- 按上下文 `openOrCreateByContext` 可复用现有活动记录
- 能从 `route-process.batchRecordReportId` 解析默认批记录绑定
- 缺少默认批记录绑定时 fail-fast
- 提交必须满足 DCC 授权语义与密码签名
- controller 暴露 `entry-context` / `open-or-create-by-context` 契约

## Scope

- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\controller\admin\pro\batchrecord\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\doc\tasks\20260523-edhr-v1-execution-red-tests\**`

## Non-Scope

- 不改任何 `src/main` 生产代码
- 不补真实实现
- 不扩展前端、E2E 或运行时联调

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\doc\tasks\20260523-nas-transfer-failure-report-backend\task.md`
- Status before this task: `Completed on 2026-05-23`
- Impact: 上一任务已完成且写入完成态，本任务可独立在 MES 测试范围内继续。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro`
- Current state: 仓库存在并行用户改动
- Impact: 本任务只增量修改 MES 后端测试与任务文档，不回滚其他人的更改。

## Milestones

- [x] M1: 检查上一任务状态、定位已放行文档与现有执行测试基线。
- [x] M2: 创建本任务文档与执行日志，先记录 BDD 场景和 RED 范围。
- [x] M3: 在现有 service/controller 测试基础上补第一批 failing backend tests。
- [x] M4: 运行定向 RED 命令，确认失败原因属于功能未实现或接口不存在。
- [x] M5: 回写失败证据、更新状态并汇总修改文件。

## Expected Verification

- `mvn --% -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 失败原因应明确指向 eDHR V1 执行节点接口、方法、VO 或约束尚未实现，而不是语法错误、编码问题或环境缺失。

## Current Status

Completed on 2026-05-23. 已在现有 service/controller 测试基础上补第一批 eDHR V1 执行节点 RED 测试，定向 Maven 运行确认失败集中在缺失 VO、缺失上下文接口、缺失执行上下文字段，未出现语法错误或环境阻塞。

## Final Verification Result

- `mvn --% -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL
- 失败点 1：`MesProBatchRecordExecutionEntryContextReqVO` / `MesProBatchRecordExecutionOpenOrCreateByContextReqVO` 不存在，说明 `entry-context` 与 `open-or-create-by-context` 契约尚未实现。
- 失败点 2：`MesProBatchRecordExecutionDO#getRouteProcessId` 不存在，说明执行实体尚未承载 route-process / 默认批记录绑定上下文。
- 失败点 3：`MesProBatchRecordExecutionService` 仍停留在旧的 `submitBatchRecordExecution(Long id)` 提交路径，尚未切换到可承载 DCC 密码签名语义的提交契约。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro --task-id 20260523-edhr-v1-execution-red-tests --mode preview` -> BLOCKED，linked worktree 不能对 `int_main` 做 `ff-only` 预览，且主工作树与当前 worktree 均有未提交改动。
