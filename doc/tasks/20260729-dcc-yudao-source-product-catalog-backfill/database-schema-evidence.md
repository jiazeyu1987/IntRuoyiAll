# Database Schema Evidence

## Goal And Entities

- Target entity: `dcc_product_catalog`.
- Goal: apply project name/code columns and exact-match backfill to the user-requested `芋道源码` target runtime database.

## Engine And Migration Tool

- Engine: MySQL, target to be confirmed before write.
- Migration: `IntRuoyiBackend/sql/mysql/20260729_dcc_product_catalog_project_code_columns.sql`.

## Data Safety

- Additive nullable columns.
- Backfill uses stable `original_row_no` and `HEX(data_source)` guard.
- No fuzzy matching runs in the target database.

## BDD

- BDD: 芋道源码产品目录回填 -> Given target database has DCC product catalog rows, When migration runs, Then only exact-match Yingtai row keys receive project fields.

## RED / GREEN

- RED: pending
- GREEN: pending

## Migration Verification

- pending

## Blockers

- none

