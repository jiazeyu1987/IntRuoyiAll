# Database Schema Evidence

## Data Change Goal

- 删除 DCC 产品目录中 `data_source = 子公司产品` 的数据来源。
- 新装库不再通过 `20260710_dcc_product_catalog_database.sql` 初始化子公司来源行。
- 已运行库通过 `20260729_dcc_product_catalog_remove_subsidiary_source.sql` 精确删除子公司来源行。

## Affected Entities

- `dcc_product_catalog`
- `data_source`
- `original_row_no`
- 唯一键：`uk_dcc_product_catalog_source_row (data_source, original_row_no)`

## Database Engine And Migration Tool

- Engine: MySQL / InnoDB。
- Migration location: `IntRuoyiBackend/sql/mysql/` release SQL。
- Migration metadata uses `-- release-migration: ...`.

## Schema And Data Changes

- Updated `20260710_dcc_product_catalog_database.sql` to remove all 32 initial seed rows whose value tuple starts with `('子公司产品',`.
- Added `20260729_dcc_product_catalog_remove_subsidiary_source.sql`.
- Cleanup migration runs:
  - `SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;`
  - `DELETE FROM dcc_product_catalog WHERE HEX(data_source) = 'E5AD90E585ACE58FB8E4BAA7E59381';`
- The cleanup SQL does not update or delete `瑛泰产品` rows.

## Data Safety Analysis

- User explicitly requested deletion of subsidiary-source data.
- Delete scope is constrained by exact UTF-8 hex comparison on `data_source`.
- No `DROP TABLE`, `TRUNCATE TABLE`, broad update, or fallback/default-success path was introduced.
- `瑛泰产品` seed row count remains 181.

## Rollback Or Recovery Plan

- Restore deleted subsidiary-source rows from the pre-migration database backup or the original 20260710 seed file after reconciling any user edits.
- The rollback is manual by design because the requested operation is a destructive source removal.

## BDD Scenarios

- BDD: DCC 产品目录仅保留瑛泰来源 -> Given DCC 产品目录初始化或迁移后存在产品目录数据 When 查询数据来源 Then 不应再出现 `子公司产品`，且 `瑛泰产品` 数据仍保留。

## RED

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_product_catalog_remove_subsidiary_source.py` -> FAIL, expected reason: cleanup migration did not exist.

## GREEN

- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_product_catalog_database_migration.py IntRuoyiBackend\script\tests\test_dcc_product_catalog_remove_subsidiary_source.py` -> PASS, 4 passed.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductCatalogServiceImplTest,DccProductCatalogRegistrationExpiryCompareServiceTest,DccProductCatalogControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, BUILD SUCCESS; 17 product catalog tests run, 0 failures, 0 errors.

## Migration Verification

- `test_product_catalog_migration_declares_global_table_and_seed_rows` verifies no `('子公司产品',` remains in the initial seed and exactly 181 `('瑛泰产品',` rows remain.
- `test_product_catalog_cleanup_migration_removes_subsidiary_source_only` verifies the cleanup migration exists, depends on the project-code column migration, deletes only the hex-encoded subsidiary source, and contains no update/drop/truncate.

## Blockers

- No blocker remains for the scoped implementation.
- Real database execution was not performed because the task scope was repository migration/code delivery, not direct local or remote DB mutation.

