# Verification Report

## Scope

- 将同级入口和页面标题从 `MES工序` 改为 `标准模板列表`。
- 保留现有路由 `/mes/pro/mes-process`、组件名、权限 `mes:pro-mes-process:query`、资源池接口和表格数据列口径。

## Commands

- `node tests/e2e/mes-pro-mes-process-readonly-static.spec.js` -> RED 后 GREEN。
- `node tests/e2e/mes-pro-route-resource-orphan-static.spec.js` -> PASS。
- `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py ... 20260730_mes_process_readonly_catalog_menu.sql` -> PASS。
- `pnpm ts:check` -> PASS。
- `git diff --check` -> PASS，仅有 CRLF 提示。
- Python `pymysql` 本机数据库更新与 HEX 核验 -> PASS。

## Evidence

- `IntRuoyiFronted/src/views/mes/pro/mes-process/index.vue` 的标题为 `【生产】标准模板列表`。
- `IntRuoyiBackend/sql/mysql/20260730_mes_process_readonly_catalog_menu.sql` 使用 `E6A087E58786E6A8A1E69DBFE58897E8A1A8` 写入 `标准模板列表`，使用 `E6A087E58786E6A8A1E69DBFE58897E8A1A8E69FA5E8AFA2` 写入 `标准模板列表查询`。
- 本机数据库 `system_menu.id=5718` 已更新为 `标准模板列表`，`system_menu.id=5719` 已更新为 `标准模板列表查询`。
- 表格列仍保留 `MES工序名称`、`MES工序编码`、`执行工序`、`批记录工序名称`，没有误改业务数据术语。

## Runtime Blocker

- 本机前端 `8081` 可访问。
- 本机后端 `48081` 当前未监听；标准后端重启脚本在 Maven testCompile 阶段被本任务外未跟踪测试草稿 `MesProMesProcessCatalogSchemaTest.java` 阻塞，该草稿引用不存在的独立 MES 工序目录包。
- 因此本次未完成真实浏览器页面复验；代码、迁移、类型检查和本机数据库菜单名均已验证。
