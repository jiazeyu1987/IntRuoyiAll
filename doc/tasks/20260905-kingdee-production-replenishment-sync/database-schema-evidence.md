# Database Schema Evidence

## Data Change Goal

- Persist Kingdee production replenishment list headers and line details per tenant, with stable source document keys and idempotent upsert behavior.

## Database Engine And Migration Tool

- MySQL migration scripts under `IntRuoyiBackend/sql/mysql`.

## Schema / Migration / Fixture Changes

- BDD: 租户化补料单快照落库 -> Given 同一金蝶补料单在不同租户同步, When 主表和明细表写入, Then 唯一键必须包含 `tenant_id` 且服务显式设置当前租户 ID。
- Adds `erp_kingdee_production_replenishment_list` for header snapshots.
- Adds `erp_kingdee_production_replenishment_list_item` for line snapshots.
- Adds tenant-scoped source unique keys, bill/material/production-order indexes, Quartz job seed, menu seed, and query permission seed.

## Data Safety Analysis

- New tables only unless existing schema already has a formal replenishment snapshot table.
- No destructive migration.

## Rollback / Recovery Plan

- Drop new task-owned tables or disable new scheduled job seed if migration has not been released.

## BDD Scenarios

- See `execution-log.md`.

## RED

- RED: `git cat-file -e origin/int_main:IntRuoyiBackend/yudao-module-erp/src/test/java/cn/iocoder/yudao/module/erp/kingdeeautosync/ErpProductionReplenishmentListSchemaTest.java` -> FAIL, expected reason: base branch lacks the replenishment schema contract.

## GREEN

- GREEN: `mvn --% -pl yudao-module-erp -am -Dtest=ErpKingdeeTableAutoSyncContractTest,ErpProductionReplenishmentListSchemaTest,KingdeeProductionReplenishmentListSyncJobTest,ErpKingdeeProductionReplenishmentListClientImplTest,ErpKingdeeProductionReplenishmentListServiceImplTest,ErpProductionReplenishmentListControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 21 tests.

## Migration Verification

- Java schema test confirms dedicated table names, source keys, tenant fields, unique keys, Job seed, menu component, and permission token in `20260905_erp_kingdee_production_replenishment_list_sync.sql`.
- No destructive migration is present; migration creates new readonly snapshot tables and seeds Job/menu rows idempotently.

## Blockers

- No schema blocker remains in static verification. Live database migration execution was not performed in this turn.
