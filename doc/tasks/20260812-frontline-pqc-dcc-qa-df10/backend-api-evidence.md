# Backend API Evidence - DF10

## Scope

- Service scope: MesFrontlinePqcContextService / MesFrontlinePqcContextServiceImpl process-page projection for active-order PQC processes.
- API contract: dedicated MesFrontlinePqcProcessRespVO response assembled for active-order/processes consumers; production-route process response model remains unchanged.

## Data Contract

- Inputs: activeOrderId.
- Required sources: active-order snapshot, locked QA regulation/version/processes, QA inspection items, task overlay, production submit candidates.
- Response fields: inspectionTypeRules, taskSummary, pqcTaskOptions.taskStatus/ruleSort/inspectionTypeRule, full published item metadata, and productionSubmitCandidates.
- Forbidden sources: management current-QA lookup, product/material/routeProcess QA inference, QA and MES route-process existence validation, formBindings, default MAIN, frontend补齐.

## Auth, Validation, Error Behavior

- This slice does not change controller auth or endpoint mapping.
- Missing required active-order / locked QA / formal process prerequisites must fail fast through existing service errors; no fallback, compatibility shim, or default-success path is introduced.

## BDD Scenarios

- BDD: DF10 process projection includes locked QA processes -> Given an active order has locked MES_QA/PUBLISHED regulation processes and QA items, When listProcessesByActiveOrder(activeOrderId) is requested, Then every locked QA process is returned with dedicated MesFrontlinePqcProcessRespVO item details and no production-route response model changes.
- BDD: DF10 task overlay does not filter QA processes -> Given a locked QA process has no existing PQC task and another process has FIRST/PATROL task options, When the projection is assembled, Then task state is NOT_CREATED for absent tasks and task options are attached without filtering or fabricating QA processes.
- BDD: DF10 avoids forbidden current QA and route validation -> Given an active order already stores the QA regulation version snapshot, When the process page projection runs, Then it uses locked snapshot readers and does not call management current-QA lookup, product/material/routeProcess QA inference, or QA-vs-MES route-process existence validation.
- BDD: DF10 production candidates belong to the selected active order -> Given production submit events include snapshot-backed and non-backed MES processes, When the PQC projection is assembled, Then only events backed by activeOrderId + routeProcessId + processId are returned in descending submit-time order, without relating any QA process to an MES process.
- BDD: DF10 response preserves section 2 contract fields -> Given a locked QA version contains inspection type rules and published QA item metadata, When the active-order process page projection is assembled, Then each QA process returns inspectionTypeRules, taskSummary, ordered PqcTaskOption rule metadata, and full published inspection item fields.
- BDD: DF10 invalid null task row fails fast -> Given the task mapper returns an invalid null task row for the selected active order, When the active-order process projection validates task overlay rows, Then it returns the formal task-identity ServiceException instead of throwing NullPointerException.

## RED

- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected reason: MesFrontlinePqcContextService lacked listProcessesByActiveOrder(Long activeOrderId), and the dedicated task option response lacked inspectionRuleKey/status.
- RED: independent-test-report.md -> FAIL, expected reason: the dedicated response omitted productionSubmitCandidates and tests did not prove candidate ownership/exclusion or formal multi-rule ordering.
- RED: independent-test-report.md -> FAIL, expected reason: the dedicated response omitted inspectionTypeRules, taskSummary, task option ruleSort/inspectionTypeRule, and complete published item fields.
- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest#listProcessesByActiveOrderIdRejectsNullTaskRecordWithServiceException" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected reason: null task row produced NullPointerException from pqcTaskIdentityText(null).

## GREEN

- GREEN: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, MesFrontlinePqcContextServiceTest 5 tests / 0 failures / 0 errors; candidate inclusion/exclusion/order, FIRST/PATROL_AM/PATROL_PM/FINAL rule order, section 2 response fields, and null task fast-fail are covered. Final passing Surefire report timestamp: 2026-08-13 16:02:55.

## Contract / Integration Verification

- GREEN: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest,MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，18 tests / 0 failures / 0 errors；正式锁定 QA 聚合与一线 PQC 投影共同覆盖。
- GREEN: MesFrontlinePqcContextServiceTest verifies getLockedVersionForOrder(dccProjectCodeId, qaRegulationId, qaRegulationVersionId) and verifies the activeOrder projection does not directly call regulation/version/process/item mappers.
- GREEN: backend-api evidence validator -> PASS.
- GREEN: git diff --check -> PASS.
- GREEN: production introduced forbidden-source scan -> PASS, no new fallback/compat/default-success/formBindings/current-DCC lookup/product-QA/material-QA/routeProcess-QA path.
- GREEN: production candidate reads are batched once before the QA-process loop; tests verify one active-order snapshot read and one production-event read.
- GREEN: static required-field scan -> PASS, dedicated response/assembler/test include inspectionTypeRules, taskSummary, ruleSort, inspectionTypeRule, itemSort, applicableInspectionTypes, firstInspectionQuantity, patrolInspectionRatio, critical, failureRule, sourceNote, sourceOriginalPage, sourceOriginalItem, sourceOriginalExcerpt, and sourceOriginalMethod.

## Observability

- Existing service exceptions remain visible to tests and callers; no new swallowed exceptions.

## Blockers

- None.
