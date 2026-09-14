# DF06 Verification Report

## Scope

- DF06 active-order QA version lock and PQC task generation only.
- No frontend, DF07+, supervisor state, merge, push, or runtime service operation was performed.

## Implementation Verification

- New active-order creation resolves work order route -> formal route-DCC binding -> enabled DCC project code -> DCC-owned QA regulation -> current PUBLISHED QA version.
- Active-order insert stores dccProjectCodeId, qaRegulationId, qaRegulationVersionId in the same transaction before process snapshots and PQC task creation.
- Removed active-order reactivation validates existing DCC/QA lock snapshots and preserves process snapshots plus existing task history.
- PQC task identity now uses activeOrderId + regulationVersionId + qaProcessId + inspectionRuleKey + businessDate.
- Canonical rule keys FIRST, PATROL_AM, PATROL_PM, FINAL are parsed from QA inspectionTypeRulesJson; PATROL_AM and PATROL_PM stay separate tasks even though both use inspectionType=PATROL.
- QA processes remain QA-owned. DF06 does not map QA process IDs to MES routeProcessId/processId.

## Commands

- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, missing DF06 helpers/identity alignment and later one route snapshot fixture expectation mismatch.
- GREEN: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 33, Failures: 0, Errors: 0, Skipped: 0.
- REGRESSION: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest,MesTeamLeaderActiveOrderErpPlannedStartTest,MesTeamLeaderActiveOrderManualSortTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 39, Failures: 0, Errors: 0, Skipped: 0.
- VALIDATOR: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df06/backend-api-evidence.md -> PASS.
- STATIC: git diff --check -> PASS.
- STATIC: rg narrow forbidden scan for selectEnabledList/productMasterId/formBindings/fallback/兼容/兜底/默认成功 in DF06 touched production files -> PASS, no matches.

## Notes

- Broad scan for routeProcess/qa words produced one reviewed false positive where routeProcesses and qaProcesses are adjacent independent constructor parameters, not a QA-to-route mapping.
- Two adjacent test files were updated only because the service constructor dependency changed and Maven test compilation covers all module tests.
- No blockers remain for DF06 independent verification.
