# 任务：DCC NAS 转移后端异步任务化

## Goal

把 `/dcc/controlled-files/nas-transfer` 从“同步递归扫描并逐文件导入”的长耗时接口改成“创建持久化后台任务 + 分轮执行 + 状态查询”的长期稳定方案，确保大目录转移对前端可追踪、可恢复、可观测，并避免用户因 30 秒客户端超时误判失败。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-dcc\src\main\java\cn\iocoder\yudao\module\dcc\controller\admin\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-dcc\src\main\java\cn\iocoder\yudao\module\dcc\service\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-dcc\src\main\java\cn\iocoder\yudao\module\dcc\dal\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-dcc\src\test\java\cn\iocoder\yudao\module\dcc\service\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-dcc-nas-transfer-async-task\**`

## Non-Scope

- 不重做 DCC 受控文件普通上传/审批链路
- 不引入 mock 成功、同步超时后静默后台继续且前端无状态的降级方案
- 不修改与 NAS 转移无关的 showroom、infra runtime control、MES 逻辑

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-company-save-revision-mismatch-fix\task.md`
- Status before this task: `Completed on 2026-05-23`
- Impact on this task: 上一任务已完成且范围在 `showroom` 模块；本任务可在 `dcc` 模块独立推进，但不得混入其相关文件。

## Milestones

- [x] M1：核对同仓前置任务状态并建立本任务文档、执行日志。
- [ ] M2：梳理现有同步 NAS 转移链路与仓库内可复用的异步任务模型。
- [ ] M3：先补 RED，锁定“创建任务 / 查询状态 / 继续执行”的后端契约与持久化需求。
- [ ] M4：实现最小表结构、任务服务、启动恢复、状态查询与控制器改造。
- [ ] M5：运行定向测试与 schema 验证，回写证据并完成 closeout 预览。

## Expected Verification

- `mvn --% -pl yudao-module-dcc -am -Dtest=DccControlledFileNasTransferServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -pl yudao-module-dcc -am -Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileFinalizationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python -m pytest script/tests/test_dcc_nas_transfer_task_sql.py -q`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-dcc-nas-transfer-async-task\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-dcc-nas-transfer-async-task\database-schema-evidence.md`

## Current Status

Completed on 2026-05-23. 后端异步任务化、任务表结构、恢复调度、定向测试和真实运行态验证均已完成；本地运行库已应用 `sql/mysql/20260523_dcc_nas_transfer_task.sql`，真实前端路径已创建并轮询任务 `id=1`。

## Final Verification Result

- PASS: `mvn --% -pl yudao-module-dcc -am -Dtest=DccControlledFileNasTransferServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- PASS: `mvn --% -pl yudao-module-dcc -am -Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileFinalizationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- PASS: `python -m pytest script/tests/test_dcc_nas_transfer_task_sql.py -q`
- PASS: 本地运行库已执行 `sql/mysql/20260523_dcc_nas_transfer_task.sql`
- PASS: 真实运行态状态接口验证。`GET /admin-api/dcc/controlled-files/nas-transfer/tasks/1` 返回 `code=0`，`status=RUNNING`，`selectedNasPaths=[\"1. QMS documents/5.STM实验室规程\"]`，`createdFileCount=381`，`remainingPendingCount=278`，`failedFileCount=0`
- PASS: 真实前端链路联调。`http://127.0.0.1:8081/system/nas` 创建任务 `id=1` 后立即进入任务态，不再等待同步长请求完成

## Blockers

- 无当前任务阻塞
