# Database Schema Evidence

## Data Change Goal

为 DCC NAS 未受控文件处理增加可查询、可审计的未受控文件明细表 `dcc_nas_control_audit_file`，并为后续 `import-selected` 增加处理任务快照字段，支撑“扫描 -> 选择 -> 本地写入 -> 归类/待处理”的严格 TDD 实现。

## Affected Entities

- Runtime migration: `IntRuoyiBackend/sql/mysql/20260803_dcc_nas_control_audit_file.sql`
- Runtime migration: `IntRuoyiBackend/sql/mysql/20260803_dcc_nas_uncontrolled_import_task_snapshot.sql`
- Test schema: `IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql`
- Persistence model: `DccNasControlAuditFileDO`
- Persistence model: `DccControlledFileNasTransferTaskDO`
- Persistence model: `DccControlledFileNasTransferTaskItemDO`
- Mapper: `DccNasControlAuditFileMapper`
- Tests: `DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails`
- Tests: `DccBaseSchemaTest#mysqlSchemaShouldSupportNasUncontrolledImportTaskSnapshots`
- SQL contract: `IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py`
- SQL contract: `IntRuoyiBackend/script/tests/test_dcc_nas_uncontrolled_import_task_snapshot_sql.py`

## Database Engine And Migration Tool

- Database engine: MySQL / InnoDB / utf8mb4.
- Migration style: existing ordered SQL files under `IntRuoyiBackend/sql/mysql`.
- Test fixture strategy: mirror required table into DCC module `create_tables.sql` and validate with JUnit plus Python SQL static contract.

## Schema Changes

- Add `dcc_nas_control_audit_file` as a non-destructive `CREATE TABLE IF NOT EXISTS`.
- Store path identity: `task_id`, `nas_share_name`, `root_path`, `normalized_relative_path`, `path_hash`, `file_name`.
- Store source snapshot: `file_size`, `modified_at`, `source_signature`.
- Store independent statuses: `control_status`, `classification_status`, `download_status`, `archive_status`.
- Store classification snapshot and reasons: project code, taxonomy id, level1-5, reason.
- Store local write/archive error separation: `local_relative_path`, `local_write_error_code`, `local_write_error`, `archive_error_code`, `archive_error`.
- Store DCC archive link: `controlled_file_id`.
- Keep BaseDO columns and `tenant_id`.
- Do not add a unique key on `path_hash`; repeated scans of the same NAS path must remain auditable, while duplicate processing is controlled by import task state and idempotency.
- Extend `dcc_controlled_file_nas_transfer_task` additively with `audit_task_id`, `idempotency_key` and `request_hash` for `NAS_UNCONTROLLED_IMPORT` idempotency and canonical request hashing; allow `template_category_id` and `effective_date` to be nullable for this source type so uncontrolled imports do not depend on legacy NAS transfer task defaults.
- Extend `dcc_controlled_file_nas_transfer_task_item` additively with `audit_file_id`, `source_signature`, recognition snapshot fields, `classification_candidates_json_snapshot`, binary-collated `local_relative_path`, local-write status/error fields, archive status/error fields, and formal archive metadata snapshot fields for category, directory, project code, taxonomy, change type, file name, file number, version, effective date and remark.
- Extend `dcc_nas_control_audit_file` additively with `selected_import_task_id` and `selected_import_task_item_id` so duplicate active bindings can be queried and rejected by service tests.

## Data Safety Analysis

- Migration is additive only; no existing table or data is dropped, truncated, deleted, or rewritten.
- Existing `dcc_nas_control_audit_task` remains unchanged.
- Existing NAS transfer task and task item tables are extended additively; no existing source type or existing NAS transfer row is rewritten.
- Binary collation is required for NAS and local relative path fields where path identity matters.
- Tenant-scoped indexes are required so later service code cannot query cross-tenant audit details.
- SQL contract asserts no `DROP TABLE`, `TRUNCATE TABLE`, `DELETE FROM`, `UNIQUE KEY`, or `UNIQUE INDEX` in the additive migration.

## Rollback Or Recovery Plan

- Before runtime release, rollback is to omit the additive migration from the release package.
- After runtime release, rollback requires an explicit follow-up migration and data retention decision; this task does not perform destructive rollback.

## BDD Scenarios

BDD: 未受控扫描明细可审计持久化 -> Given NAS audit 扫描发现未受控文件 When 系统保存 audit task Then 每个未受控文件都有 `dcc_nas_control_audit_file` 明细、source signature、初始识别/下载/归档状态和 tenant-scoped path hash 索引。

BDD: Import-selected task snapshots are schema-backed -> Given selected audit files will be locked into an import task When the backend creates `NAS_UNCONTROLLED_IMPORT` Then task header stores audit task, idempotency key and canonical request hash, task items store audit file/source signature/recognition/local path snapshots, and audit files expose current import task/item binding for duplicate-selection checks.

## RED Command And Expected Failure

- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails" test` -> expected FAIL, `dcc_nas_control_audit_file` table and test method do not exist yet.
- RED: `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q` -> expected FAIL, SQL static contract file and migration do not exist yet.
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportNasUncontrolledImportTaskSnapshots" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> expected FAIL, `20260803_dcc_nas_uncontrolled_import_task_snapshot.sql` and aligned DO/test schema fields do not exist yet.

## GREEN Command And Passing Result

- GREEN: `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q` -> PASS，2 passed in 3.18s。
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportNasUncontrolledImportTaskSnapshots" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。\n- GREEN: `mvn -f IntRuoyiBackend\\pom.xml -pl yudao-module-dcc -am -rf :yudao-module-dcc "-Dmaven.resources.skip=true" "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportNasUncontrolledImportTaskSnapshots" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS，验证 M24 正式归档快照字段与 nullable task header 约束。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_uncontrolled_import_task_snapshot_sql.py IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q` -> PASS，4 passed in 1.49s。

## Migration Verification

- Migration file starts with the required release metadata marker and depends on `20260730_dcc_nas_control_audit`.
- Runtime migration creates `dcc_nas_control_audit_file` with source identity, source signature, independent classification/download/archive statuses, classification snapshot, local/archive error separation, controlled-file link, BaseDO columns and tenant scope.
- Test schema mirrors the required table contract for DCC module tests.
- JUnit schema test confirms binary collation for NAS/local relative paths and tenant-scoped indexes on task, path hash and status fields.
- SQL static contract confirms the migration is additive and does not use a unique path hash key that would block repeated audit evidence.
- Import task snapshot migration starts with release metadata and depends on `20260803_dcc_nas_control_audit_file`.
- Import task snapshot schema test confirms idempotency index, audit-file lookup index, audit/import binding fields, binary-collated local relative path, nullable uncontrolled-import task header fields, formal archive metadata snapshot fields and DO field alignment.
- SQL static contract confirms import snapshot migration is additive and has no destructive DDL/DML or uniqueness constraint that would silently turn conflicts into schema failures.
- Evidence validator: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\database-schema-evidence.md` -> PASS，`Database schema evidence is valid.`

## Blockers

- None for M7 detail schema slice or M15 import task snapshot schema slice.
