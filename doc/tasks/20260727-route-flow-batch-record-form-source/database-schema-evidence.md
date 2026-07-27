# Database Schema Evidence

## Data Change Goal

- 将 `mes_pro_route_version.route_snapshot_json` 从 `TEXT` 扩展为 `MEDIUMTEXT`，支持路线候选快照同时保存正式 `batchRecordReports`、独立 `formBindings` 和 `batchRecordAttachmentOwners`。

## Affected Entities

- MySQL 表：`mes_pro_route_version`
- 字段：`route_snapshot_json`
- 迁移文件：`IntRuoyiBackend/sql/mysql/20260727_mes_route_version_snapshot_mediumtext.sql`
- 基础 schema：`IntRuoyiBackend/sql/mysql/20260613_mes_smart_scheduling_t1_schema.sql`

## Data Safety

- 迁移只做字段容量扩展，不删除、不截断、不重写业务数据。
- 迁移先检查表和字段存在性，缺失时 `SIGNAL SQLSTATE '45000'` fail fast。
- 仅当当前类型不是 `mediumtext`、`longtext` 或 `json` 时执行 `MODIFY COLUMN`。

## Rollback Or Recovery

- 容量扩展为向前兼容变更。若必须回滚，应先确认所有 `route_snapshot_json` 长度不超过 `TEXT` 上限，再在维护窗口手动降回 `TEXT`；本任务不执行自动降级，避免截断快照。

## BDD

- BDD: 大路线候选快照可保存 -> Given 路线草稿包含正式批记录、表单槽位和工序开始配置，When 保存候选快照，Then `route_snapshot_json` 容量不得因超过 64KB 导致保存失败。
- BDD: 缺 schema 前置必须 fail fast -> Given 迁移目标表或字段不存在，When 执行迁移，Then 迁移直接失败并报告缺失对象，不得静默成功。

## Verification

- RED: 保存 V19 草稿正式批记录绑定时，`route_snapshot_json` 原 `TEXT` 容量不足，候选快照超过 64KB 导致保存失败。
- GREEN: `docker exec int-ruoyi-mysql ... SHOW COLUMNS FROM mes_pro_route_version LIKE "route_snapshot_json"` -> `mediumtext`。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_route_version_snapshot_mediumtext_sql.py -q` -> PASS，`3 passed`。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteVersionLifecycleSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。

## Blockers

- None.

