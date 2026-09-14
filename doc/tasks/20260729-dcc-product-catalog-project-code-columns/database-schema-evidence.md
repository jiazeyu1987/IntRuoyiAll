# Database Schema Evidence

## Goal And Entities

- Add `project_name` and `project_code` to `dcc_product_catalog`.
- Backfill only rows where `data_source = 瑛泰产品` and the catalog row fully corresponds to an enabled `dcc_project_code` record.

## Engine And Migration Tool

- Engine: MySQL 8 in local Docker container `int-ruoyi-mysql`.
- Migration path: `IntRuoyiBackend/sql/mysql`.
- Test path: `IntRuoyiBackend/script/tests`.

## Data Safety

- Non-destructive additive columns.
- Backfill is restricted to exact row keys from current reviewed完全对应 set, not fuzzy matching at runtime.
- Rollback plan: clear the two new columns or drop columns before production apply if verification fails.

## BDD

- BDD: 只回填完全对应关系 -> Given 瑛泰产品目录中存在不同匹配层级，When migration runs, Then exact-match row keys get project fields and non-exact row keys stay null.

## RED / GREEN

- RED: `python -X utf8 -m pytest script\tests\test_dcc_product_catalog_database_migration.py` -> FAIL, missing migration file.
- GREEN: `python -X utf8 -m pytest script\tests\test_dcc_product_catalog_database_migration.py` -> PASS, 3 tests.

## Migration Verification

- Added migration `IntRuoyiBackend/sql/mysql/20260729_dcc_product_catalog_project_code_columns.sql`.
- Migration adds nullable `project_name` and `project_code` columns to `dcc_product_catalog`.
- Migration creates a scoped temporary table of 115 exact-match `original_row_no -> project_name/project_code` mappings.
- Migration updates only active rows where `HEX(data_source) = 'E7919BE6B3B0E4BAA7E59381'`.
- Release migration policy gate passed with dependency chain `20260513_dcc_base_schema -> 20260710_dcc_product_catalog_database -> 20260729_dcc_product_catalog_project_code_columns`.
- Local Docker MySQL apply succeeded.
- Local verification: total active `瑛泰产品` rows = 181; filled project fields = 115; selected non-exact row set with filled fields = 0.

## Blockers

- none
