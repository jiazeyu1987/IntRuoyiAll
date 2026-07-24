# 任务：eDHR V1 执行节点 M3 标准化快照后端

## Goal

在 `yudao-module-mes` 范围内把 `executionSnapshotJson` 从原始报表 JSON 升级为前端可编辑的标准化结构，并通过最小后端回归。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\controller\admin\pro\batchrecord\vo\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\doc\tasks\20260523-edhr-v1-execution-m3-snapshot-backend\**`

## Non-Scope

- 不修改前端仓
- 不回滚并行改动
- 不做 E2E
- 不追求对所有 JMReport 组件的完整语义还原

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\doc\tasks\20260523-edhr-v1-execution-green-backend\task.md`
- Status before this task: `Completed on 2026-05-23`
- Impact: 执行上下文、活动态、列表契约、签名路径已完成，本任务在其基础上补 M3 标准化快照。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro`
- Current state: 存在并行未提交改动
- Impact: 仅在本任务负责的 MES 后端文件范围内增量修改，不回滚他人改动。

## Milestones

- [x] M1: 创建任务文档并记录 M3 BDD/RED 目标。
- [x] M2: 阅读 JMReport JSON 结构并补标准化快照 RED 测试。
- [x] M3: 实现标准化 `executionSnapshotJson` 与必要响应字段。
- [x] M4: 运行最小后端回归并记录证据、风险。

## Expected Verification

- `mvn --% -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionSignatureServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed on 2026-05-23. 已完成 M3 标准化快照后端：`open-or-create-by-context` 生成前端可编辑的标准化 `executionSnapshotJson`，并通过定向后端回归。

## Final Verification Result

- `mvn --% -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionSignatureServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
