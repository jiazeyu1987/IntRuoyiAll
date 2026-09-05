# Backend API Evidence

## Scope

- Add scheduled/manual Kingdee production replenishment list synchronization through existing ERP Kingdee sync runtime.

## API And Data Contract

- BDD: 自动同步金蝶生产补料单列表 -> Given 金蝶连接配置有效且当前租户已启用补料单同步, When 定时任务按水位窗口执行, Then 系统从金蝶 `PRD_FeedMtrl` 查询补料单并写入只读快照。
- API exposes `/erp/production-replenishment-list/page` and `/erp/production-replenishment-list/sync`, returning header rows with nested line details.
- Sync runtime type is `PRODUCTION_REPLENISHMENT_LIST`; handler is `kingdeeProductionReplenishmentListSyncJob`.

## Auth, Permissions, Validation, Error Behavior

- Must follow existing ERP sync admin permissions.
- Missing Kingdee field/FormId/config/tenant context must fail fast.
- Query page uses `erp:production-replenishment-list:query`; sync action uses the existing ERP sync permission boundary.
- Client maps Kingdee request/response failures to dedicated production replenishment list error codes and does not return default success.

## Required Config, Services, Fixtures, Migrations

- Requires existing Kingdee base config from `ErpKingdeeConfigService.getEffectiveProperties()`.
- Adds `KingdeeProductionReplenishmentListSyncJob`, client/service/controller, response/page VOs, and frontend API/page entry.
- Migration `IntRuoyiBackend/sql/mysql/20260905_erp_kingdee_production_replenishment_list_sync.sql` adds readonly snapshot tables, job seed, and menu/query permission.

## BDD Scenarios

- See `execution-log.md`.

## RED

- RED: `git cat-file -e origin/int_main:IntRuoyiBackend/yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/job/production/KingdeeProductionReplenishmentListSyncJob.java` -> FAIL, expected reason: base branch lacks the replenishment sync Job.
- RED: `git cat-file -e origin/int_main:IntRuoyiFronted/tests/e2e/erp-production-replenishment-list-static.spec.js` -> FAIL, expected reason: base branch lacks the frontend contract test.

## GREEN

- GREEN: `mvn --% -pl yudao-module-erp -am -Dtest=ErpKingdeeTableAutoSyncContractTest,ErpProductionReplenishmentListSchemaTest,KingdeeProductionReplenishmentListSyncJobTest,ErpKingdeeProductionReplenishmentListClientImplTest,ErpKingdeeProductionReplenishmentListServiceImplTest,ErpProductionReplenishmentListControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 21 tests.
- GREEN: `node IntRuoyiFronted\tests\e2e\erp-production-replenishment-list-static.spec.js` -> PASS.

## Contract / Integration Verification

- Contract tests cover auto-sync type registration, full-sync handler exposure, Quartz/runtime transaction boundary, controller route, client field parsing, response error handling, service upsert, tenant assignment, and schema contract.
- Live Kingdee integration was not executed because no current turn request authorized a real external-account sync run.

## Observability

- Existing sync run and watermark records must include the new sync type.

## Blockers

- No code/test blocker remains. Completion is blocked only on explicit Git commit/push authorization if full project closeout is required.
