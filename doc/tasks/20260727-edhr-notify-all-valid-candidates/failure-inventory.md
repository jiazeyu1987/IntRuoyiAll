# MES Full Regression Failure Inventory

Baseline command: `mvn -pl yudao-module-mes test`

Baseline result: 2509 tests, 58 failures, 78 errors, 31 skipped, 41 failing suites.

## T2 Static Contract And Path Drift

| Suite | First observed root signal |
| --- | --- |
| `MesProBatchRecordVersionPhaseTwoMigrationContractTest` | draft reupload contract no longer matches the real import call |
| `MesProEdhrFormFillLogMenuContractTest` | hard-coded stale frontend root `E:\IntRuoyi\yudao-ui-admin-vue3` |
| `MesProEdhrTemplateConfigMenuRemovalContractTest` | shared `DesignerWrapper` contract mismatch |

## T3 Schema, Spring Context And H2 Isolation

| Suite | First observed root signal |
| --- | --- |
| `MesBatchRecordBaseSchemaTest` | runtime schema missing `mes_pro_batch_record_execution.batch_record_definition_id` |
| `BalloonProcessDeviceMappingImportServiceImplTest` | Spring bean dependency injection failure |
| `MesProBatchRecordReportRenameServiceImplDbTest` | Spring bean dependency injection failure |
| `MesProBatchRecordReportServiceImplDbTest` | H2 unique key collision on active BATCH route flow config |
| `ThirdPartyFeedbackImportServiceImplDbTest` | Spring bean dependency injection failure |
| `IntGyRouteMarkdownImportServiceImplDbTest` | Spring bean dependency injection failure |

## T4 Route And eDHR Contracts

| Suite | First observed root signal |
| --- | --- |
| `MesProRouteVersionAndCopyTest` | missing `routeOwnerPermissionService` test dependency |
| `MesProBatchRecordExecutionArchiveServiceImplTest` | archive expectation drift, expected 0 but observed 2 |
| `MesProEdhrBatchExecutionLegacyProcessTest` | current batch-record binding validation rejects legacy test setup |
| `MesProEdhrBatchExecutionServiceTest` | attachment owner snapshot validation and adjacent expectation drift |
| `MesProEdhrBatchExecutionTaskGateTest` | task gate expectation drift |
| `MesProEdhrRehearsalReadinessServiceTest` | missing process-form fill rule now correctly blocks rehearsal |
| `MesProEdhrWorkTaskLegacyProcessTest` | tenant prerequisite missing and removed `calculateDueTime` reflection contract |
| `MesProBatchRecordExecutionFieldAuditQueryExportServiceTest` | Spring Context repaired; responsibility export still accepts `EVIDENCE_MISSING` / `BLOCKED` evidence instead of failing fast |
| `MesProRouteProcessServiceImplBatchRecordBindingTest` | unnecessary strict Mockito stubbing |
| `MesProRouteServiceImplDisplayFieldsTest` | missing `platformAdapter` test dependency |
| `MesProRouteServiceImplTest` | route behavior expectation drift |

## T5 Batch Record Parser, Layout And Route Generation

| Suite | First observed root signal |
| --- | --- |
| `MesProBatchRecordReportJsonBuilderTest` | expected BOOLEAN but current output is STRING |
| `MesProBatchRecordReportLayoutCalibratorTest` | calibrated column/index expectation drift |
| `MesProBatchRecordReportShapeRulesTest` | expected shape count 44 but observed 68 |
| `MesProBatchRecordRouteCandidateGovernanceTest` | candidate rebuild deletes active route-use config |
| `MesProBatchRecordRouteGenerationCodeRuleTest` | generated route still writes legacy `nextProcessId` chain |

## T6 Scheduling And Schedule Order

| Suite | First observed root signal |
| --- | --- |
| `MesProTaskGanttWorkOrderCodeContractTest` | preview row conversion does not receive work-order code |
| `MesProAutoScheduleAlgorithmContractTest` | test setup lacks frozen route version |
| `MesProAutoScheduleContractTest` | missing `routeVersionMapper` test dependency |
| `MesProScheduleOrderAdmissionTest` | strict stubbing uses obsolete one-argument workstation lookup |
| `MesProScheduleOrderFourRiskContractTest` | expected progress 170 but current capped value is 100 |
| `MesProScheduleOrderNoDefaultConfigContractTest` | no-default-config contract drift |
| `MesProScheduleOrderServiceImplTest` | expected NORMAL but current result is CAPACITY_MISSING |

## T7 Authoritative Fixture Consumers

| Suite | Required prerequisite |
| --- | --- |
| `Sheet1MachineryProcessExcelParserTest` | authoritative Excel fixture |
| `FullWordTableInventoryProbeTest` | authoritative Word `.doc` fixture |
| `MesProBatchRecordJingxiTableStructureVerificationTest` | authoritative Word `.doc` fixture |
| `MesProBatchRecordRouteARecognizerTest` | authoritative Word `.doc` fixture |
| `MesProBatchRecordRouteFRecognizerTest` | authoritative Word `.doc` fixture |
| `TmpPrintBatchRecordTableTest` | authoritative Word `.doc` fixture |
| `Sheet1RouteExcelImportServiceImplDbTest` | authoritative Excel fixture after Spring dependency repair |
| `Sheet1RouteExcelImportServiceImplTest` | authoritative Excel fixture |
| `Sheet1RouteExcelParserTest` | authoritative Excel fixture |

## Inventory Gate

- All 41 failing suites are mapped exactly once.
- No suite is assigned to a skip/exclusion task.
- Fixture-dependent suites remain blocked until source and integrity are confirmed.
- Current unrelated frontend modifications are not task-owned; T2 must not edit those files without ownership coordination.

## Current Remaining Gate（2026-07-28）

- User scope change: Sheet1 Excel 真实样本覆盖“不需要覆盖这个”，缺失 `sheet1-route-balloon-catheter.xlsx` 不再作为本任务验收前置。
- Scope adjustment: 删除 `Sheet1RouteExcelImportServiceImplTest`、`Sheet1RouteExcelImportServiceImplDbTest`、`Sheet1RouteExcelTestFixtures` 以及 parser 的真实 fixture 用例；保留 `Sheet1RouteExcelParserTest` 4 个合成 fail-fast/契约测试。
- Latest command: `mvn -pl yudao-module-mes test`
- Latest result: `PASS`，2026-07-28 08:53:25 +08:00；2530 tests、0 failures、0 errors、18 skipped，`BUILD SUCCESS`。
- Remaining implementation/test gate: 无。
- Remaining closeout gate: 无；cleanup preview/apply、最终提交和推送均已完成。
