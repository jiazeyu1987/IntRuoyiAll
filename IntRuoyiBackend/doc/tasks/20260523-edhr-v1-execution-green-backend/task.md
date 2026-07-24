# 任务：eDHR V1 执行节点后端 GREEN

## Goal

在 `yudao-module-mes` 范围内完成 eDHR V1 执行节点第一批后端 GREEN，实现并通过当前 RED 对应能力：

- `entry-context`
- `open-or-create-by-context`
- execution 实体/VO/mapper 上下文字段（至少 `routeProcessId`、`batchRecordReportId`）
- 提交链路改为签名感知路径，复用 DCC 电子签名授权 + 当前密码校验语义，但签名记录必须落 MES 自有实体

## Scope

- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\controller\admin\pro\batchrecord\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\dal\dataobject\pro\batchrecord\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\dal\mysql\pro\batchrecord\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\controller\admin\pro\batchrecord\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\test\resources\sql\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\pom.xml`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\doc\tasks\20260523-edhr-v1-execution-green-backend\**`

## Non-Scope

- 不修改前端仓
- 不回滚并行改动
- 不复用 `DccSignatureVerificationService`
- 不把签名记录写入 DCC `controlled_file_signature`

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\doc\tasks\20260523-edhr-v1-execution-red-tests\task.md`
- Status before this task: `Completed on 2026-05-23`
- Impact: RED 契约与失败证据已完成记录，本任务可直接在同一契约上做 GREEN。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro`
- Current state: 存在并行未提交改动
- Impact: 仅在本任务负责的 MES 后端文件范围内增量修改，不回滚他人改动。

## Milestones

- [x] M1: 复核 RED 测试、已有实现、DCC 授权/密码校验可复用边界。
- [x] M2: 先运行定向 RED 命令，确认当前失败面。
- [x] M3: 补齐上下文 VO、controller/service 契约与 execution 上下文字段。
- [x] M4: 实现 `entry-context`、`open-or-create-by-context`、签名感知提交与 MES 自有签名记录。
- [x] M5: 运行最小 GREEN/回归测试并回写证据、状态、风险。

## Expected Verification

- `mvn --% -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 若新增独立签名服务测试，则运行对应定向测试命令
- 结果必须指向 GREEN 或明确的剩余真实阻塞，不允许 mock 成功或静默降级

## Current Status

Completed on 2026-05-23. 已完成 eDHR V1 执行节点第一批后端 GREEN：补齐上下文入口/打开契约、执行上下文字段、运行态快照字段、MES 自有签名记录服务，并通过定向 GREEN/回归测试。

## Final Verification Result

- `mvn --% -pl yudao-module-mes -am -Dtest=MesBatchRecordBaseSchemaTest,MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionSignatureServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro --task-id 20260523-edhr-v1-execution-green-backend --mode preview` -> BLOCKED，linked worktree 无法对 `int_main` 做 ff-only 预览，且主工作树及当前 worktree 均存在未提交改动。
