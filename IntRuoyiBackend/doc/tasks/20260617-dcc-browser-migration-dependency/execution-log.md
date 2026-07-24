# 执行日志: 修复 DCC 浏览索引迁移依赖元数据

- `BDD: 迁移依赖 ID 可解析 -> Given 20260617 DCC 浏览索引迁移依赖 20260513 DCC 基础表, When 发布迁移策略门禁扫描 sql/mysql, Then dependsOn 应引用已存在的 migrationId 20260513_dcc_base_schema 并通过依赖解析。`
- 范围：只修正 `sql/mysql/20260617_dcc_browser_performance_indexes.sql` 首行 `release-migration` 元数据；不执行数据库写入。
RED: python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql -> FAIL, dependsOn missing migration '20260513_dcc_base_schema.sql' for migrationId '20260617_dcc_browser_performance_indexes'
- 修复：将 `dependsOn=20260513_dcc_base_schema.sql` 改为 `dependsOn=20260513_dcc_base_schema`，与 `release_migration_manifest.py` 的 `migrationId = path.stem` 规则一致。
GREEN: python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql -> PASS, status=passed, migrationCount=147
GREEN: python -X utf8 -m pytest script\tests\test_release_migration_policy_gate.py -q -> PASS, 6 passed
GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260617-dcc-browser-migration-dependency\database-schema-evidence.md -> PASS
GREEN: task-closeout-cleanup -> PASS，preview/apply 均无删除项；保留 task.md、execution-log.md 与 database-schema-evidence.md。
