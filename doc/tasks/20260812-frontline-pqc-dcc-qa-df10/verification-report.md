# DF10 Verification Report

## Scope

- Backend service projection and locked QA aggregate boundary: MesFrontlinePqcContextService, MesFrontlinePqcContextServiceImpl, MesFrontlinePqcProcessRespVO, MesQaInspectionRegulationService, their implementations and focused tests.
- The dedicated PQC converter in MesFrontlineDeviceAccountController was migrated off removed compatibility setters. Frontend, schema, production-route response model, mappers, supervisor state, and shared business data were not modified.

## Results

- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, missing activeOrderId projection method and task option rule/status fields.
- RED: independent-test-report.md -> FAIL, production submit candidates and formal multi-rule ordering were not covered.
- RED: independent-test-report.md -> FAIL, missing section 2 fields inspectionTypeRules, taskSummary, task option ruleSort/inspectionTypeRule, and complete published item metadata.
- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest#listProcessesByActiveOrderIdRejectsNullTaskRecordWithServiceException" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, null task row produced NullPointerException from pqcTaskIdentityText(null).
- GREEN: target Maven command -> PASS, MesFrontlinePqcContextServiceTest 5 tests / 0 failures / 0 errors; it now covers candidate inclusion/exclusion/descending order, one batched snapshot/event read, FIRST/PATROL_AM/PATROL_PM/FINAL ordering, section 2 response fields, and null task fast-fail. Final passing Surefire report timestamp: 2026-08-13 16:02:55.
- GREEN: backend-api evidence validator -> PASS.
- GREEN: git diff --check -> PASS.
- GREEN: production introduced forbidden-source scan -> PASS.
- GREEN: required-field static scan -> PASS.
- GREEN: UTF-8 validation for task.md, execution-log.md, backend-api-evidence.md, and verification-report.md -> PASS.
- GREEN: final target Maven command with MesFrontlinePqcContextServiceTest and MesQaInspectionRegulationServiceTest -> PASS at 2026-08-14 01:57:19, 18 tests / 0 failures / 0 errors / 0 skipped.
- GREEN: QA locked aggregate is now read through MesQaInspectionRegulationService#getLockedVersionForOrder; the activeOrder projection no longer duplicates regulation/version/process/item mapper aggregation.
- GREEN: bug-regression evidence validator -> PASS after exact GREEN:/Verification markers were recorded.

## Notes

- Projection reads the active order locked DCC/QA/QA version snapshots through the QA service aggregate boundary.
- QA service validates locked QA regulation/version ownership and returns complete rules/processes/items/equipment for PUBLISHED or RETIRED versions without current-version or enabled-DCC dependency.
- It returns production submit candidates only when activeOrderId + routeProcessId + processId is backed by the selected active order's process snapshot; this ownership check is independent from QA processes.
- It builds task options through MesFrontlinePqcTaskOverlay.fromExpectedTasks, preserving formal rule-key order.
- It returns inspectionTypeRules, taskSummary, full PqcTaskOption rule metadata, and complete published QA item metadata required by section 2.
- The dedicated item response uses the published QA item contract directly; compatibility-only acceptanceStandard/processInspectionMethod aliases are not retained.
- It rejects invalid null task rows with the formal task identity ServiceException instead of a NullPointerException.
- It does not call current-DCC QA lookup, product/material inference, formBindings, or QA-vs-MES route-process existence validation.
- NOT_CREATED task options do not fabricate an inspectionRuleKey.

## Final Status

ready_for_closeout：全部目标验证通过，等待主管独立复验与集成；未提交、未合并、未删除 worktree。
