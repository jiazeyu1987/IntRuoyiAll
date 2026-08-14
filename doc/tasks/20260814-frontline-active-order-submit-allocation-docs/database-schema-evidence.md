# P5 本地测试库 Schema 迁移证据

## Scope

- Engine: MySQL 8.0.39 (`int-ruoyi-mysql`, local test database on port 23306)
- Tenant data scope: schema-only migration; no tenant business rows are modified by the migration
- Official migration: `IntRuoyiBackend/sql/mysql/20260814_mes_frontline_selected_initial_allocation.sql`
- Target table: `mes_pro_process_pool_report_allocation`

## BDD

BDD: Given the local test schema still requires `review_id` and only documents FIFO/MANUAL, when the official migration is applied, then `review_id` is nullable and `allocation_mode` formally documents `FRONTLINE_SELECTED/FIFO/MANUAL/SYSTEM`.

## Data

- No production or shared remote data is in scope.
- The migration changes metadata only; the P5 fixture uses task-owned rows in fixed test tenant 122 and removes them by exact IDs.

## RED Evidence

RED: the pre-migration schema query fails the nullable/comment acceptance contract as expected.

- Pre-migration `review_id`: `IS_NULLABLE=NO`, comment `班组长复核记录ID`.
- Pre-migration `allocation_mode`: `varchar(32) NOT NULL`, comment `分配方式：FIFO/MANUAL`.
- Schema backup: `e2e-artifacts/schema-before-migration.sql`.

## Migration Safety

- The official migration fails fast if the target table is absent.
- The official migration verifies `review_id` is nullable before returning.
- The DDL is limited to two column definitions and creates no compatibility branch or fallback.
- The migration is idempotent for the target definitions.

## Rollback Evidence

Rollback is documented but intentionally not executed because P5 requires the formal schema to remain installed. Before rollback, all rows whose `review_id IS NULL` must be removed through their owning task cleanup. The schema-only rollback is:

```sql
ALTER TABLE `mes_pro_process_pool_report_allocation`
  MODIFY COLUMN `review_id` bigint NOT NULL COMMENT '班组长复核记录ID',
  MODIFY COLUMN `allocation_mode` varchar(32) NOT NULL COMMENT '分配方式：FIFO/MANUAL';
```

## Current Status

`green_verified`; the official migration remains installed in the local test schema.

GREEN: official migration exited 0; `review_id` is `bigint NULL`, `allocation_mode` is `varchar(32) NOT NULL` with comment `分配方式：FRONTLINE_SELECTED/FIFO/MANUAL/SYSTEM`, and the temporary migration procedure count is 0.

## Verification

- Host and container copies had identical SHA-256 `bf0eb2c25f967dc724312a5e5473e8cec1927a1e248745839651478f644a2f78` before execution.
- Migration command exit code: 0.
- Post-migration `information_schema.columns` query exit code: 0.
- Database schema evidence validator exit code: 0 after GREEN evidence update.

## Blockers

None.
