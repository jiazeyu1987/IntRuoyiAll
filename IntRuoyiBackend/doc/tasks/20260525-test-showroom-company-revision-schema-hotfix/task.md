# 任务：修复测试服展厅公司版本表缺列

## 任务目标

- 修复测试服后端查询 `showroom_company_revision.display_name_snapshot` 报 `Unknown column` 的问题。
- 只执行既有展厅版本中心 schema 迁移中缺失的非破坏性 `ADD COLUMN`，不修改业务数据、不同步数据库、不切换租户或账号绕过。

## 非目标

- 不修改正式服。
- 不执行全量数据库同步。
- 不执行展厅版本中心 backfill 数据回填，除非用户后续明确要求。
- 不删除、改名或重建任何表、索引、数据。

## 前置任务检查

- 最近同仓任务 `20260525-tenant-yudao-to-yingtai-copy` 当前因租户复制方案和用户确认缺失处于阻塞状态。
- 该任务是本机租户数据复制预研，与本次测试服 schema 缺列热修无共享写入范围；本次只修测试服 `showroom_company_revision` 表缺列。

## 里程碑

- [x] M1：建立任务记录并确认前置任务不共享写入范围。
- [x] M2：只读确认测试服缺失列和既有迁移脚本。
- [x] M3：执行最小 schema 修复并验证列存在。
- [x] M4：通过真实测试服前端/API 验证原报错消失。
- [x] M5：记录证据、执行 closeout 预览，并按策略提交本任务文档。

## BDD 场景

- BDD: 测试服展厅公司版本查询不再缺列 -> Given 测试服已部署引用 `display_name_snapshot` 的后端代码, When 查询或打开展厅公司版本相关页面, Then `showroom_company_revision` 应包含 `display_name_snapshot`、`display_name_en_snapshot`、`company_type_snapshot`，查询不再报 Unknown column。

## 预期验证

- RED：只读查询测试服 `information_schema.columns`，确认缺失目标列。
- GREEN：执行既有 schema 迁移对应的 3 个 `ADD COLUMN` 后，再次查询列存在。
- GREEN：测试服真实前端/API 访问触发原查询路径，不再出现该 SQLSyntaxErrorException。
- `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260525-test-showroom-company-revision-schema-hotfix/database-schema-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260525-test-showroom-company-revision-schema-hotfix/bug-regression-evidence.md`

## Cleanup Keep

- `doc/tasks/20260525-test-showroom-company-revision-schema-hotfix/database-schema-evidence.md`
- `doc/tasks/20260525-test-showroom-company-revision-schema-hotfix/bug-regression-evidence.md`

## Current Status

completed

## 回滚策略

- 本次只新增 nullable 列，不写入业务数据。若需回滚，可在确认无新代码依赖后执行：
  - `ALTER TABLE showroom_company_revision DROP COLUMN display_name_snapshot;`
  - `ALTER TABLE showroom_company_revision DROP COLUMN display_name_en_snapshot;`
  - `ALTER TABLE showroom_company_revision DROP COLUMN company_type_snapshot;`
- 回滚需另行确认，因为当前已部署代码依赖这些列，直接回滚会重新触发 Unknown column。

## 当前状态

- 状态：completed
- 已完成：
  - 已定位既有迁移脚本 `sql/showroom/20260523_showroom_version_center_schema.sql` 包含目标列。
  - 已建立任务记录。
  - RED 确认测试服缺失 3 个快照列，直接查询 `display_name_snapshot` 报 `ERROR 1054`。
  - 已在测试服执行最小 DDL：新增 `display_name_snapshot`、`display_name_en_snapshot`、`company_type_snapshot`，并补建缺失的 `showroom_version_bundle` 空表。
  - GREEN 确认 3 个列存在且直接查询不再报错。
  - GREEN 通过测试服真实 API 与 Playwright 前端路径验证 `/showroom/company` 不再出现 `Unknown column`。
  - closeout 预览保留 task、execution-log、schema evidence、bug evidence，仅清理一次性 Playwright helper。
- 后续阻塞（本任务非目标）：
  - `/showroom/version-center/history/detail` 已不再报 SQL 缺列，但因本次明确不执行 backfill，仍返回 `SHOWROOM_VERSION_CENTER_NOT_READY` 或 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND`。若要版本中心完整可用，需要单独批准并执行展厅版本中心 readable bundle 数据回填。
