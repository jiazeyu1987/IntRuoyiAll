# Task: MES schema import

## Goal

Fix the MES frontend "系统异常" errors after MES routes were enabled by importing the required MES MySQL table structures into the local `ruoyi-vue-pro` database.

## Affected Endpoints

- `GET /admin-api/mes/md/item/page`
- `GET /admin-api/mes/md/item-type/simple-list`
- `GET /admin-api/mes/home-statistics/summary`
- `GET /admin-api/mes/home-statistics/work-order-status`
- `GET /admin-api/mes/home-statistics/production-trend`

## Milestones

- [x] M1: Task document and BDD/TDD evidence log created before schema changes.
- [x] M2: Reproduce missing MES tables with a real MySQL schema check.
- [x] M3: Add a MySQL MES schema migration artifact for the tables used by the affected pages.
- [x] M4: Apply the schema to the local MySQL database without fallback or mock data.
- [x] M5: Verify MES APIs return business success and prepare only current task changes for commit.

## Expected Verification

- RED MySQL table check proves MES tables are missing.
- GREEN MySQL table check proves required MES tables exist.
- Mapper-equivalent SQL checks prove the affected queries execute with `deleted` and `tenant_id` filters.
- Runtime API checks prove the affected MES APIs return `code=0`.

## Current Status

Completed. The local MySQL database now contains the MES tables required by the reported material item and MES home paths, and authenticated API verification returns `code=0` for all affected endpoints.

## Data Safety

The migration must be additive only: create missing MES tables if they do not exist. It must not drop, truncate, overwrite, or mock existing data.
