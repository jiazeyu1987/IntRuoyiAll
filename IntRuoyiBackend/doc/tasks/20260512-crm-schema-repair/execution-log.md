# Execution Log: CRM schema repair

BDD: CRM base schema exists -> Given `yudao-module-crm` is enabled and the backend connects to MySQL, When CRM APIs query `crm_*` tables, Then every CRM DO table exists and the global handler does not return the missing CRM schema message.

## Evidence

- M1: Completed. Previous backend task `doc/tasks/20260512-enable-crm-module/task.md` is completed and committed.
- M2: Completed. This task document and execution log were created before schema changes.
- M3 RED: `node doc/tasks/20260512-crm-schema-repair/scripts/validate-crm-schema.cjs` -> FAIL, expected reason: missing `sql\mysql\20260512_crm_base_schema.sql`.
- M3 RED: `mvn "-Dflatten.skip=true" -pl yudao-module-crm -am "-Dtest=CrmBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `CRM MySQL schema file must exist`.
- M4 GREEN: `node doc/tasks/20260512-crm-schema-repair/scripts/generate-crm-base-schema.cjs` -> PASS, generated `sql\mysql\20260512_crm_base_schema.sql` with 19 CRM tables.
- M4 GREEN: `node doc/tasks/20260512-crm-schema-repair/scripts/validate-crm-schema.cjs` -> PASS, 19 CRM DO tables covered by the schema file.
- M4 GREEN: `mvn "-Dflatten.skip=true" -pl yudao-module-crm -am "-Dtest=CrmBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `CrmBaseSchemaTest` ran 1 test with 0 failures, 0 errors, and 0 skipped.
- M5 GREEN: `java -cp "C:\Users\BJB110\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" doc\tasks\20260512-crm-schema-repair\scripts\MysqlCrmSchemaRunner.java "jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true" root 123456 sql\mysql\20260512_crm_base_schema.sql` -> PASS, imported and verified 19 CRM tables in the live local MySQL database.
- M5 GREEN: repeated `MysqlCrmSchemaRunner.java` against `127.0.0.1:23306/ruoyi-vue-pro` -> PASS, SQL is idempotent and still verifies 19 CRM tables.
- M6 GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260512-crm-schema-repair\database-schema-evidence.md` -> PASS.
- M6 GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260512-crm-schema-repair\bug-regression-evidence.md` -> PASS.
