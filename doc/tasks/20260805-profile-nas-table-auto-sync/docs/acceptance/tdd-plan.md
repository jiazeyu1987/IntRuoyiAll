# NAS 表格自动同步 TDD Plan

## Purpose and Scope

按 strict TDD 为数据库、后端 API/Job、前端入口和真实 E2E 规划 RED/GREEN 顺序。生产代码变更前必须先写能失败的目标测试或静态契约。

## Evidence Reviewed

- `docs/system/*.md` 本任务设计。
- 现有前端 `tests/e2e/*static.spec.js` 以 Node 静态合同锁定页面契约。
- 现有后端 `script/tests/test_*_sql.py` 校验 SQL 迁移。
- 现有 JobHandler 和 NAS 服务代码。

## TDD Sequence

1. RED 数据库迁移测试：断言新增表、唯一键、Job handler、release metadata 存在。
2. RED 后端静态/单元测试：断言 Controller 路径、权限、JobHandler、plan service 方法、NAS 上传方法存在。
3. RED 前端静态合同：断言配置内部 tab label、组件导出、API wrapper、权限边界和错误展示。
4. GREEN 实现 schema、后端、前端。
5. GREEN 运行目标测试并补回归。
6. 真实 E2E 在 worktree slot 7 成对运行态验证。

## RED Commands

- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_erp_nas_table_auto_sync_sql.py`
- `mvn -pl yudao-module-erp -am "-Dtest=ErpNasTableSyncContractTest" test`
- `node IntRuoyiFronted/tests/e2e/profile-nas-table-auto-sync-static.spec.js`

## Expected Failures

- SQL 测试初始失败：`20260805_erp_nas_table_auto_sync.sql` 不存在。
- 后端合同初始失败：Controller、Service、JobHandler、DO/Mapper 或 NAS 上传方法不存在。
- 前端合同初始失败：`NAS表格自动同步` tab、API wrapper 和组件不存在。

## GREEN Commands

- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_erp_nas_table_auto_sync_sql.py`
- `mvn -pl yudao-module-erp -am "-Dtest=ErpNasTableSyncContractTest" test`
- `node IntRuoyiFronted/tests/e2e/profile-nas-table-auto-sync-static.spec.js`
- `pwsh -NoProfile -File scripts/preflight/branch-runtime-port-guard.ps1`

## Refactor Checks

- 不将业务配置写入 `infra_job.handler_param`。
- 不用 NAS 下载方法冒充上传。
- 不新增第二套调度器。
- 不新增与配置页签不同的前端权限边界。

## Evidence Log Template

- `BDD: <scenario> -> Given/When/Then`
- `RED: <command> -> FAIL, <expected reason>`
- `GREEN: <command> -> PASS`
- `REGRESSION: <command> -> PASS or BLOCKED with exact reason`

## Test Blockers

- Maven reactor 因无关 target 损坏阻塞时，需记录并使用符合 worktree 规则的隔离验证方案。
- pnpm/node_modules 缺失时必须在目标 worktree 前端目录运行 `pnpm install --frozen-lockfile`，不得复制依赖。
