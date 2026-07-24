# Task: CRM schema repair

## Goal

Fix `[CRM 系统 yudao-module-crm - 表结构未导入]` by providing and importing the missing MySQL `crm_*` base tables required by `yudao-module-crm`.

## Scope

- Identify the missing CRM table root cause from the runtime error and local SQL inventory.
- Add a repeatable MySQL CRM base schema script under `sql/mysql/`.
- Validate the schema against CRM DO `@TableName` declarations and fields.
- Import the schema into the configured local MySQL database when available.
- Do not suppress the missing-table exception.
- Do not add fallback behavior or mock success responses.

## Milestones

- [x] M1: Previous backend task state checked before starting.
- [x] M2: Task documentation created before schema changes.
- [x] M3: RED validation records missing CRM schema.
- [x] M4: CRM base schema script and validation tooling added.
- [x] M5: Local database schema imported and verified or exact blocker recorded.
- [x] M6: Evidence updated and task finalized.
- [x] M7: Task changes committed separately after verification passes.

## Expected Verification

- `node doc/tasks/20260512-crm-schema-repair/scripts/validate-crm-schema.cjs`
- `node doc/tasks/20260512-crm-schema-repair/scripts/generate-crm-base-schema.cjs`
- `mvn "-Dflatten.skip=true" -pl yudao-module-crm -am "-Dtest=CrmBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `java -cp "C:\Users\BJB110\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" doc\tasks\20260512-crm-schema-repair\scripts\MysqlCrmSchemaRunner.java "<jdbc-url>" root 123456 sql\mysql\20260512_crm_base_schema.sql`

## Current Status

Completed. Previous CRM enablement task `20260512-enable-crm-module` is completed and committed. Runtime reached CRM module code, but the local SQL inventory lacked `CREATE TABLE ... crm_` DDL, causing the global missing-table handler to return the CRM schema-not-imported message. This task added `sql/mysql/20260512_crm_base_schema.sql`, validated it against 19 CRM DO tables, and imported it into the configured local MySQL database.
