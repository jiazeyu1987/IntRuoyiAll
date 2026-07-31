# Database Schema Evidence

## Goal And Entities

- Target entity: `dcc_product_catalog`.
- Goal: apply project name/code columns and exact-match backfill to the user-requested `芋道源码` target runtime database.

## Engine And Migration Tool

- Engine: MySQL 8, Docker container `int-ruoyi-mysql`, database `ruoyi-vue-pro`, host port `23306`.
- Migration: `IntRuoyiBackend/sql/mysql/20260729_dcc_product_catalog_project_code_columns.sql`.

## Data Safety

- Additive nullable columns.
- Backfill uses stable `original_row_no` and `HEX(data_source)` guard.
- No fuzzy matching runs in the target database.
- Rollback: set `project_name/project_code` to NULL for the exact Yingtai row scope, then drop the two additive columns only if rollback is explicitly approved.

## BDD

- BDD: 芋道源码产品目录回填 -> Given target database has DCC product catalog rows, When migration runs, Then only exact-match Yingtai row keys receive project fields.

## RED / GREEN

- RED: `127.0.0.1:3306` configured root connection -> FAIL with MySQL `1045`; no write executed.
- RED: Chinese literal tenant comparison -> FAIL with MySQL `1267`; query was replaced with HEX equality.
- GREEN: formal migration applied to Docker `23306/ruoyi-vue-pro` -> PASS.

## Migration Verification

- Tenant precondition: `system_tenant` contains `芋道源码`.
- Schema: `project_name` and `project_code` exist with `utf8mb4_unicode_ci`.
- Data: 181 active Yingtai catalog rows; 115 rows have both project fields; selected non-exact rows have zero filled project fields.
- The formal migration was re-run idempotently; no separate manual update SQL was used.

## Blockers

- none
