# 任务：eDHR V1 Runtime Gate 后端契约修复

## Goal

在 `yudao-module-mes` 范围内修复当前 eDHR V1 runtime gate 评审发现的两个放行前契约问题：

- `executionSnapshotJson.snapshotVersion` 必须与系统设计文档冻结值一致；
- 旧 `legacy-create-from-template` HTTP 入口必须从对外接口面移除，避免再生成与 V1 执行页不兼容的旧记录。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\controller\admin\pro\batchrecord\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\controller\admin\pro\batchrecord\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\**`
- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\doc\tasks\20260523-edhr-v1-runtime-gate-backend-fixes\**`

## Non-Scope

- 不修改前端产品代码
- 不回滚并行 worktree 改动
- 不引入 fallback 或兼容分支
- 不扩展审批、追踪、PDF 归档

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\doc\tasks\20260523-edhr-v1-tenant122-data-explorer\task.md`
- Status before this task: `Completed on 2026-05-23`
- Impact: 前一任务已完成真实数据盘点；本任务继续处理 runtime gate 的后端契约收口。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro`
- Current state: 存在当前 eDHR 节点未提交改动
- Impact: 仅在本任务负责的 MES 后端和任务文档范围内增量修改，不回滚他人改动。

## Milestones

- [x] M1: 创建任务文档并记录 BDD/RED 目标
- [x] M2: 先改测试，让 `snapshotVersion` 和 legacy HTTP 入口断言失败
- [x] M3: 实现最小后端修复并让测试转绿
- [x] M4: 复跑定向回归、真实 API 验证并记录结果

## Expected Verification

- `mvn --% -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionSignatureServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 真实 API：
  - `GET http://127.0.0.1:48083/admin-api/mes/pro/batch-record-execution/entry-context`
  - `POST http://127.0.0.1:48083/admin-api/mes/pro/batch-record-execution/open-or-create-by-context`
  - `GET http://127.0.0.1:48083/admin-api/mes/pro/batch-record-execution/get?id=<id>`

## Current Status

Completed on 2026-05-23. 已完成 runtime gate 后端契约修复：`executionSnapshotJson.snapshotVersion` 对齐到文档冻结值 `EDHR_EXECUTION_V1`，`legacy-create-from-template` 已从公开 HTTP 接口面移除，并通过定向回归与真实 API 验证。

## Final Verification Result

- `mvn --% -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> RED then GREEN
- `mvn --% -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionSignatureServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `mvn --% -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS
- 真实 API 验证：
  - `POST /admin-api/mes/pro/batch-record-execution/legacy-create-from-template` -> not exposed
  - `GET /admin-api/mes/pro/batch-record-execution/entry-context` -> PASS
  - `POST /admin-api/mes/pro/batch-record-execution/open-or-create-by-context` -> PASS
  - `GET /admin-api/mes/pro/batch-record-execution/get?id=8` -> PASS, `status = 1`, `snapshotVersion = EDHR_EXECUTION_V1`
