# DF06 Backend API Evidence

## Scope

- Service scope: active-order creation/reactivation and PQC inspection task generation in MesTeamLeaderActiveOrderService.
- Data contract: active order locks dccProjectCodeId, qaRegulationId, qaRegulationVersionId; PQC task uses inspectionRuleKey as formal rule identity.
- No controller/API path change is owned by DF06.

## Contract

- New active orders resolve route -> route-DCC project code -> DCC-owned QA regulation -> current PUBLISHED QA version.
- New active orders persist DCC/QA snapshot fields before process snapshot and task generation in the same transaction.
- Removed-row reactivation validates existing locked snapshot and preserves historical task rows.
- PQC tasks are generated from QA rule keys. PATROL_AM and PATROL_PM may share inspectionType=PATROL, but they are separate task identities.

## Auth, Validation, And Error Behavior

- Existing active-order service validation and tenant-scoped mappers remain authoritative.
- Missing route-DCC, missing QA, unpublished version, invalid QA process identity, missing inspection items, and invalid quantity rules fail fast with existing MES service errors.
- No fallback, mock success, or product/material inference is introduced.

## Required Config, Services, Fixtures, Migrations

- Requires C00 schema for active-order QA snapshot columns and PQC task inspectionRuleKey.
- Requires DF03 route-DCC binding mapper and DF05 DCC-owned QA relation.
- Unit fixtures are in MesTeamLeaderActiveOrderServiceTest.

## BDD Scenarios

- BDD: 新建活跃订单锁定 QA 快照 -> Given 工单的当前生产路线存在正式 route-DCC 关系且 DCC 项目代码下存在已发布 QA 规程, When 班组长把工单加入活跃订单池, Then 新活跃订单写入 dccProjectCodeId、qaRegulationId、qaRegulationVersionId，并在同一事务继续生成工序快照和 PQC 任务。
- BDD: 上午下午巡检不合并 -> Given QA 发布版本包含 FIRST、PATROL_AM、PATROL_PM、FINAL 规则且 PATROL_AM/PATROL_PM 共用 inspectionType=PATROL 检验项目, When 系统生成 PQC 任务, Then 生成四条任务并分别写入 inspectionRuleKey=FIRST/PATROL_AM/PATROL_PM/FINAL。
- BDD: 重新激活保留旧快照 -> Given 旧活跃订单已被移除且已有 DCC/QA 锁定快照, When 用户重新加入同一工单路线版本, Then 系统只恢复原订单，不重新锁定当前 QA 版本，不重建历史任务。

## RED

- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, compile failure from missing DF06 rule helpers and mapper identity alignment after RED test was added.
- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, final pre-GREEN rerun failed with one route snapshot fixture expectation mismatch.

## GREEN

- GREEN: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 33, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

## Contract Or Integration Verification

- Active-order insert captures dccProjectCodeId, qaRegulationId, qaRegulationVersionId before process snapshots and PQC tasks.
- Removed active-order reactivation validates and preserves the existing locked DCC/QA snapshot and does not rebuild tasks.
- PQC task identity uses activeOrderId, regulationVersionId, qaProcessId, inspectionRuleKey, and businessDate; PATROL_AM and PATROL_PM remain separate tasks sharing inspectionType=PATROL.
- QA process identity remains QA-owned: routeProcessId/processId are not used to map or validate QA processes.
- REGRESSION: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest,MesTeamLeaderActiveOrderErpPlannedStartTest,MesTeamLeaderActiveOrderManualSortTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 39, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- REGRESSION: git diff --check -> PASS.
- REGRESSION: narrow forbidden scan for selectEnabledList/productMasterId/formBindings/fallback/兼容/兜底/默认成功 -> PASS, no matches in DF06 touched production files.
- Backend API evidence validator -> PASS, Backend API evidence is valid.

## Observability

- Existing audit insert remains for active-order add/reactivate/remove operations.

## Blockers

- None currently.
