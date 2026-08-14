# DF06 Independent Test Report

## Gate Decision

PASS

DF06 active-order QA version lock is independently verified. No blocking defects were found in the DF06 implementation scope.

## Scope Verified

- New active-order creation locks `dccProjectCodeId`, `qaRegulationId`, and `qaRegulationVersionId` from the formal route-DCC relationship and the DCC-owned published QA regulation.
- Removed active-order reactivation validates and preserves the existing QA lock snapshot instead of recalculating against the current QA version.
- PQC task identity uses `activeOrderId + regulationVersionId + qaProcessId + inspectionRuleKey + businessDate`.
- Canonical rule keys `FIRST`, `PATROL_AM`, `PATROL_PM`, and `FINAL` remain separate; AM/PM patrol tasks are not collapsed by `inspectionType=PATROL`.
- QA process/task generation does not validate QA processes against MES route processes; DF06-created QA tasks leave `routeProcessId` and `processId` unset.
- No product/material/formBindings/selectEnabledList fallback inference is present in DF06 touched production changes.

## Requirement-to-Evidence Checklist

| Requirement | Evidence | Result |
|---|---|---|
| Route -> DCC -> QA lock path | `MesTeamLeaderActiveOrderServiceImpl` uses `MesRouteDccProjectBindingMapper.selectCurrentByRouteId`, then DCC project status, then QA regulation by DCC; tests include work-order product differs and legacy QA route fields ignored. | PASS |
| Active order snapshot fields | `MesProcessPoolActiveOrderDO` has `dccProjectCodeId`, `qaRegulationId`, `qaRegulationVersionId`; `MesQaPqcSchemaTest` confirms SQL columns. | PASS |
| Removed reactivation keeps lock | `validateRemovedQaLockSnapshot` is called before reactivation; test coverage includes removed reactivation using the existing snapshot. | PASS |
| Four rule keys | `parseCanonicalInspectionRules` requires exactly `FIRST/PATROL_AM/PATROL_PM/FINAL`; task tests assert task rule keys in that order. | PASS |
| PQC task identity | Mapper `selectByQaIdentity` now keys by `inspectionRuleKey` and `businessDate`; schema postflight defines the matching unique key. | PASS |
| No QA/MES route coupling for DF06-created tasks | `buildPqcTask` sets `qaProcessId` only; test helper asserts `routeProcessId` and `processId` are null. Existing old route-process mapper methods have no production call sites in the scan. | PASS |
| No fallback/default-success inference | Forbidden scan for `selectEnabledList(`, `productMasterId`, `material`, `formBindings`, `fallback`, `兼容`, `兜底`, `默认成功` found no matches in DF06 touched production files. | PASS |

## Commands Run

- `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 33 tests, 0 failures, 0 errors, 0 skipped.
- `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest,MesTeamLeaderActiveOrderErpPlannedStartTest,MesTeamLeaderActiveOrderManualSortTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 39 tests, 0 failures, 0 errors, 0 skipped.
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df06/backend-api-evidence.md` -> PASS, Backend API evidence is valid.
- `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 7 tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check` -> PASS; only LF-to-CRLF working-copy warnings were reported.
- Forbidden scans:
  - `rg -n "selectEnabledList\(|productMasterId|material|Material|formBindings|fallback|兼容|兜底|默认成功|mock success|default success|graceful degradation|fallback" <DF06 touched production files>` -> PASS, no matches.
  - `rg -n "selectPendingByActiveOrderProcess|selectByIdentity\(" IntRuoyiBackend/yudao-module-mes/src/main/java IntRuoyiBackend/yudao-module-mes/src/test/java` -> PASS for DF06 scope; old route-process lookup methods are only declared, not called.
  - `rg -n "getRouteProcessId\(\)|getProcessId\(\)|assertNull\(|routeProcessId\(|processId\(" MesTeamLeaderActiveOrderServiceTest.java` -> PASS; DF06 PQC task assertions confirm null route/process fields.

## Non-Blocking Notes

- `MesPqcInspectionTaskDO` and `MesPqcInspectionTaskMapper` still contain legacy `routeProcessId/processId` fields and old route-process lookup methods. They are not used by DF06 task generation and no production call site was found, so this does not block DF06. A later cleanup task can remove or quarantine the legacy API after confirming older flows no longer depend on it.
- `doc/tasks/20260812-frontline-pqc-dcc-qa-df06/evidence/backend-api-validation-summary.json` and `evidence/backend-api-validation-report.md` are not present. The worker-documented validator contract uses `backend-api-evidence.md), and that validator passed, so this is informational only.

## Blockers

None.

