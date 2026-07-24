# 任务: 修复 DCC 浏览索引迁移依赖元数据

## 任务目标

修复 `sql/mysql/20260617_dcc_browser_performance_indexes.sql` 的 `release-migration` 依赖声明，使发布迁移策略门禁能识别已有基础迁移 `20260513_dcc_base_schema`。

本任务只修正迁移元数据，不变更表结构 SQL，不执行数据库写入。

## 经验门禁

- 项目级发布门禁要求构建发布包前必须通过迁移策略门禁。
- 缺少迁移 manifest 或依赖链证据必须 fail fast；不得绕过门禁继续发布。
- 本任务仅修正 `dependsOn` 的 migrationId 格式，不引入 fallback、降级或跳过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。迁移系统以文件 stem 作为 `migrationId`，依赖必须使用不带 `.sql` 的 migrationId。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 迁移依赖 ID 可解析 -> Given 20260617 DCC 浏览索引迁移依赖 20260513 DCC 基础表, When 发布迁移策略门禁扫描 sql/mysql, Then dependsOn 应引用已存在的 migrationId 20260513_dcc_base_schema 并通过依赖解析。`

## 里程碑

- [x] 创建任务文档。
- [x] 复现迁移门禁失败。
- [x] 修正迁移元数据。
- [x] 运行迁移策略门禁验证。
- [x] 更新证据与状态。

## 当前状态

已完成。

## Current Status

completed

## Cleanup Keep

- `doc/tasks/20260617-dcc-browser-migration-dependency/database-schema-evidence.md`

## 最终验证

- `RED: python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql -> FAIL, dependsOn missing migration '20260513_dcc_base_schema.sql' for migrationId '20260617_dcc_browser_performance_indexes'`
- `GREEN: python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql -> PASS, status=passed, migrationCount=147`
- `GREEN: python -X utf8 -m pytest script\tests\test_release_migration_policy_gate.py -q -> PASS, 6 passed`
- `GREEN: python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260617-dcc-browser-migration-dependency\database-schema-evidence.md -> PASS`
- `GREEN: task-closeout-cleanup -> PASS`，无删除项，保留迁移证据。
