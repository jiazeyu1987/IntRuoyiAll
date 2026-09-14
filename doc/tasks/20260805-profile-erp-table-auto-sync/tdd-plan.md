# TDD Plan

## Purpose and Scope

以任务专用合同测试驱动“ERP表格自动同步”配置落地，先 RED 证明当前缺接口/表/页面，再实现最小正式方案转 GREEN。

## TDD Sequence

1. 新增后端合同测试 `ErpKingdeeTableAutoSyncContractTest`，断言 controller、service、DO、mapper、job、类型映射和权限边界。
2. 新增 SQL 静态测试 `test_erp_kingdee_table_auto_sync_sql.py`，断言配置表、唯一键、dispatcher job 和 release metadata。
3. 新增前端静态合同 `profile-erp-table-auto-sync-static.spec.js`，断言配置页签、组件、API wrapper 和非 NAS 混用边界。
4. 运行 RED 命令，记录预期失败。
5. 实现后端配置 API、调度服务、dispatcher job、SQL 和前端页签。
6. 运行 GREEN 命令和相邻回归。

## RED Commands

- `mvn -pl yudao-module-erp -am "-Dtest=cn.iocoder.yudao.module.erp.kingdeeautosync.ErpKingdeeTableAutoSyncContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_erp_kingdee_table_auto_sync_sql.py`
- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js`

## GREEN Commands

- `mvn -pl yudao-module-erp -am "-Dtest=cn.iocoder.yudao.module.erp.kingdeeautosync.ErpKingdeeTableAutoSyncContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_erp_kingdee_table_auto_sync_sql.py`
- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js`

## Refactor Checks

- `git diff --check`
- `pnpm ts:check`
- `scripts\preflight\branch-runtime-port-guard.ps1`
