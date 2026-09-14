# Execution Log

## 2026-07-30

- 用户需求：将 `MES工序` 改成 `标准模板列表`。
- 范围判断：本次按用户可见入口/页面标题重命名处理，保留 `/mes/pro/mes-process` 路由、`mes:pro-mes-process:query` 权限、资源池接口和表格内 `MES工序名称 / MES工序编码` 数据列口径。
- 使用技能：`clear-frontend-copy`，用于规范中文标题；保留 `MES` 作为必要业务术语。
- BDD: 标准模板列表入口 -> Given 管理员进入 `MES 系统 > 生产管理`, When 查看原 `MES工序` 同级入口, Then 该入口显示为 `标准模板列表`，位置仍在 `工序设置` 和 `工艺流程` 之间。
- BDD: 标准模板列表页面 -> Given 管理员打开 `/mes/pro/mes-process`, When 页面加载完成, Then 页面标题显示 `标准模板列表`，表格仍展示 MES 工序、设备、执行工序和批记录工序名称。
- COPY SCAN: `python -X utf8 C:\Users\BJB110\.codex\skills\clear-frontend-copy\scripts\scan_frontend_copy.py --root IntRuoyiFronted --format json` -> TIMEOUT，前端全量历史文案扫描超过当前任务范围。
- COPY SCAN: `python -X utf8 C:\Users\BJB110\.codex\skills\clear-frontend-copy\scripts\scan_frontend_copy.py --root IntRuoyiFronted\src\views\mes\pro\mes-process --format json` -> PASS，扫描 1 个文件；标题从 mixed language 问题中移除，剩余 `MES工序名称 / MES工序编码` 为保留的数据列业务术语，`共 {{ total }} 条` 为模板插值误报。
- RED: 更新静态合同后运行 `node tests/e2e/mes-pro-mes-process-readonly-static.spec.js` -> FAIL，预期失败：`页面标题必须显示为标准模板列表`。
- 实施：`IntRuoyiFronted/src/views/mes/pro/mes-process/index.vue` 的 doc-alert 标题改为 `【生产】标准模板列表`。
- 实施：`IntRuoyiBackend/sql/mysql/20260730_mes_process_readonly_catalog_menu.sql` 将 `system_menu.id=5718` 名称改为 `标准模板列表`，`5719` 改为 `标准模板列表查询`，均使用 UTF-8 HEX 写入。
- GREEN: `node tests/e2e/mes-pro-mes-process-readonly-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-route-resource-orphan-static.spec.js` -> PASS。
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260512_mes_base_schema.sql --sql-file IntRuoyiBackend\sql\mysql\20260709_mes_route_process_flow_graph.sql --sql-file IntRuoyiBackend\sql\mysql\20260709_mes_route_flow_config_unification.sql --sql-file IntRuoyiBackend\sql\mysql\20260730_mes_process_readonly_catalog_menu.sql --output doc\tasks\20260730-rename-mes-process-standard-template-list\migration-policy-gate.json` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check` -> PASS，仅有 CRLF 提示，无空白错误。
- APPLY DB: Docker API 当前不可用；改用 Python `pymysql` 直连本机 MySQL 端口，只更新 `system_menu.id IN (5718,5719)` 的名称并校验 HEX -> PASS。
- DB VERIFY: `5718` 名称为 `标准模板列表`，HEX 为 `E6A087E58786E6A8A1E69DBFE58897E8A1A8`；`5719` 名称为 `标准模板列表查询`，HEX 为 `E6A087E58786E6A8A1E69DBFE58897E8A1A8E69FA5E8AFA2`。
- RUNTIME CHECK: 前端 `http://127.0.0.1:8081/` -> HTTP 200。
- RUNTIME BLOCKER: 后端 `http://127.0.0.1:48081/actuator/health` 当前拒绝连接；项目标准脚本 `restart-int-ruoyi-local.ps1 -Component backend` 打包阶段失败于本任务外未跟踪文件 `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProMesProcessCatalogSchemaTest.java`，该文件引用不存在的 `cn.iocoder.yudao.module.mes.dal.dataobject.pro.mesprocess.*` 包。
- EXPERIENCE: 按 `project-experience-consolidation` 检查，已有 `docs/frontend-development.md#动态菜单页签重命名门禁` 和 `docs/database-rules.md#中文菜单名称-ascii-安全迁移门禁` 覆盖本次经验，不新增长期经验文档。
- EXPERIENCE VERIFY: `rg -n --no-mmap "动态菜单页签重命名|中文菜单名称 ASCII" docs\frontend-development.md docs\database-rules.md docs\experience-index.md` -> PASS。
