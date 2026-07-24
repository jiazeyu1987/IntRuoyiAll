# Execution Log: ERP schema repair

BDD: ERP base schema exists -> Given `yudao-module-erp` is enabled and the backend connects to the configured MySQL database, When an ERP purchase-order API queries ERP tables, Then the database has all required `erp_*` base tables and the global handler does not return the missing ERP schema message.

BDD: Kingdee sync table exists -> Given Kingdee purchase-order synchronization is enabled, When the sync service records a source mapping, Then `erp_kingdee_purchase_order_sync_record` exists with the source-key unique constraint.

## Evidence

- M1/M2: Completed. Previous backend task documents were checked before this repair. Pre-existing CRM/BPM/MES working-tree changes are unrelated to this repair and were left untouched.
- M3 RED: `node doc/tasks/20260512-erp-schema-repair/scripts/validate-erp-schema.cjs` -> FAIL, missing `sql/mysql/20260512_erp_base_schema.sql`.
- M4 GREEN: `node doc/tasks/20260512-erp-schema-repair/scripts/generate-erp-base-schema.cjs` -> PASS, generated `sql/mysql/20260512_erp_base_schema.sql` with 33 ERP base tables.
- M4 GREEN: updated `sql/mysql/20260512_erp_kingdee_purchase_order_sync.sql` to use `CREATE TABLE IF NOT EXISTS` and idempotent menu insert.
- M5 GREEN: `java -cp "C:\Users\BJB110\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" doc\tasks\20260512-erp-schema-repair\scripts\MysqlSchemaRunner.java "jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true" root 123456 sql\mysql\20260512_erp_base_schema.sql sql\mysql\20260512_erp_kingdee_purchase_order_sync.sql` -> PASS, imported and verified 34 ERP tables in the live database.
- M5 GREEN: `mvn -pl yudao-module-erp -am "-Dtest=ErpKingdeePurchaseOrderSyncRecordMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- M5 GREEN: `node doc/tasks/20260512-erp-schema-repair/scripts/validate-erp-schema.cjs` -> PASS, all ERP DO tables covered by the two MySQL schema files.
- M6 GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260512-erp-schema-repair\database-schema-evidence.md` -> PASS.
- M6 GREEN: repeated `MysqlSchemaRunner.java` against `127.0.0.1:23306/ruoyi-vue-pro` -> PASS, SQL is idempotent and still verifies 34 ERP tables.
