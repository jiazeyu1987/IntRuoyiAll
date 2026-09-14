# DF10 Independent Test Report

## Objective

Independently verify whether DF10 satisfies the supervisor dev-plan.md task DF10, test-plan.md case TC-DF10-BACKEND-PROJECTION, and E:\IntRuoyi\doc\tasks\20260811-frontline-pqc-dcc-qa-agent-design\interface-contracts.md section 2. The gate specifically checks that the active-order-only backend projection returns the dedicated frontline PQC process response from the locked active-order DCC/QA/version snapshot, all locked QA processes and complete published item fields, task overlay, and production submit candidates, without N+1 reads, management current-QA lookup, product/material QA inference, QA-to-MES route-process validation, fallback, compatibility behavior, default success, or formBindings.

## Evidence Reviewed

- Worktree rules: D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df10\AGENTS.md.
- Required root rules: E:\IntRuoyi\docs\backend-development.md, E:\IntRuoyi\docs\task-closeout-rules.md, and E:\IntRuoyi\docs\powershell-encoding.md.
- Independent verification skill: C:\Users\BJB110\.codex\skills\independent-verification-gate\SKILL.md and references\verification-report.md.
- Supervisor requirements: doc\tasks\20260812-frontline-pqc-dcc-qa-delivery-supervision\dev-plan.md DF10 and test-plan.md TC-DF10-BACKEND-PROJECTION.
- Contract source: E:\IntRuoyi\doc\tasks\20260811-frontline-pqc-dcc-qa-agent-design\interface-contracts.md, section 2.
- DF10 task evidence: task.md, execution-log.md, verification-report.md, and backend-api-evidence.md.
- Production diff: MesFrontlinePqcContextService.java, MesFrontlinePqcContextServiceImpl.java, and MesFrontlinePqcProcessRespVO.java.
- Test diff: MesFrontlinePqcContextServiceTest.java.
- Current Git status and diff scope for D:\IntRuoyiWorktree\20260812-frontline-pqc-dcc-qa-df10.

## Requirement Coverage

| Requirement | Evidence | Result |
| --- | --- | --- |
| Scope limited to DF10-owned backend service/VO/test plus task docs | git status --short --branch shows only the three DF10 production files, MesFrontlinePqcContextServiceTest.java, and task docs changed | PASS |
| Dedicated activeOrderId projection | MesFrontlinePqcContextService#listProcessesByActiveOrder(Long activeOrderId) and implementation added | PASS |
| Locked active-order DCC/QA/version source | resolveLockedQaProcessSource(activeOrder) reads active-order dccProjectCodeId, qaRegulationId, and qaRegulationVersionId; current-QA lookup is not added to the new projection | PASS |
| All locked QA processes/items returned | Service reads version processes and items and keeps QA processes visible even when no pending task exists | PARTIAL |
| Complete published item fields | Contract requires full published item fields including itemSort, applicableInspectionTypes, firstInspectionQuantity, patrolInspectionRatio, critical, failureRule, sourceNote, and source-original fields. Static scan found these fields absent from MesFrontlinePqcProcessRespVO, the projection assembler, and the DF10 test | FAIL |
| inspectionTypeRules | Contract requires top-level inspectionTypeRules. Static scan found inspectionTypeRules absent from the response VO, assembler, and test | FAIL |
| taskSummary | Contract requires PqcTaskSummaryVO with state/totalCount/pendingCount/submittedCount/confirmedCount/cancelledCount. Static scan found taskSummary absent | FAIL |
| pqcTaskOptions | Response includes pqcTaskOptions, and the test asserts FIRST/PATROL_AM/PATROL_PM order | PASS |
| ruleSort and inspectionTypeRule inside task options | Contract requires ruleSort and full inspectionTypeRule for each option. Static scan found both absent from response VO, assembler, and test | FAIL |
| inspectionRuleKey and task status | Response and task options include inspectionRuleKey; top-level taskStatus is present. Tests assert PENDING and NOT_CREATED | PASS |
| productionSubmitCandidates | Response includes candidates; assembler reads active-order process snapshots and production events once, filters by snapshot membership, and sorts DESC; test asserts include/exclude and shared candidates | PASS |
| Batch reads / no N+1 for new projection | New projection reads tasks, equipment, production snapshots, and production events outside the QA-process loop. Test verifies one snapshot read and one production-event read | PASS |
| No management current QA lookup in new projection | Added-line forbidden scan did not find selectByDccProjectCodeId; test verifies it is never called | PASS |
| No product/material QA inference | Added-line forbidden scan found no precise product/material-to-QA inference pattern | PASS |
| No QA-to-MES route-process existence validation | Production candidates use route/process snapshot ownership only; added-line scan found no qaProcessId relationship to MES routeProcessId/processId | PASS |
| No fallback/compat/default-success/formBindings | Added-line forbidden scan passed for fallback, compat, default-success, formBindings, and catch blocks | PASS |
| Test coverage for the missing contract fields | MesFrontlinePqcContextServiceTest.java has no assertions for inspectionTypeRules, taskSummary, ruleSort, inspectionTypeRule, or complete published item fields | FAIL |

## Verification Commands

- mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
  - PASS: Tests run: 4, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS; finished at 2026-08-13T12:48:23+08:00.
- python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260812-frontline-pqc-dcc-qa-df10\backend-api-evidence.md
  - PASS: Backend API evidence is valid.
- git diff --check
  - PASS with LF-to-CRLF advisory warnings only; no whitespace errors.
- Precise added-production-line forbidden scan for fallback, compatibility, default success, formBindings, current-QA lookup, product/material QA inference, QA-to-MES route-process validation, and catch blocks.
  - PASS: no precise forbidden token in added production lines.
- Static required-field scan across MesFrontlinePqcProcessRespVO.java, MesFrontlinePqcContextServiceImpl.java, and MesFrontlinePqcContextServiceTest.java.
  - FAIL evidence: inspectionTypeRules, taskSummary, ruleSort, inspectionTypeRule, itemSort, applicableInspectionTypes, firstInspectionQuantity, patrolInspectionRatio, critical, failureRule, sourceNote, sourceOriginalPage, sourceOriginalItem, sourceOriginalExcerpt, and sourceOriginalMethod are absent.

## Findings

1. [P1] The dedicated response still omits mandatory contract fields. Section 2 requires top-level inspectionTypeRules and taskSummary, plus task-option ruleSort and inspectionTypeRule. MesFrontlinePqcProcessRespVO.java defines inspectionItems, pqcTaskOptions, and productionSubmitCandidates, but it has no fields for these required contract objects.
2. [P1] The projection does not return complete published QA item fields. The contract says runtime item response must preserve the full published item contract. The current PqcInspectionItem / MesFrontlinePqcInspectionItem projection omits itemSort, applicableInspectionTypes, firstInspectionQuantity, patrolInspectionRatio, critical, failureRule, sourceNote, and source-original fields, so downstream consumers cannot rely on the published item contract.
3. [P1] The DF10 tests do not cover the missing response contract. The Maven test now covers candidates, candidate exclusion, descending candidate order, FIRST/PATROL_AM/PATROL_PM task-option order, and NOT_CREATED/PENDING statuses. It still has no assertions for inspectionTypeRules, taskSummary, ruleSort, inspectionTypeRule, or complete published item fields.

## Residual Risks

- The Maven target is green, but it only proves the four current unit tests; it does not prove the full section 2 response contract.
- The candidate regression from the first independent review appears addressed: production candidates are now present, filtered by active-order process snapshot membership, and sorted descending.
- The current gate did not run frontend, controller, database, or E2E verification because DF10 is scoped to backend projection and the requested command set was backend/static.
- LF-to-CRLF warnings remain informational and did not fail git diff --check.

## Decision

FAIL

Mandatory contract fields are still missing from the dedicated DF10 response and from test coverage. Passing Maven, the evidence validator, git diff --check, and forbidden-source scans cannot substitute for the absent inspectionTypeRules, taskSummary, ruleSort, inspectionTypeRule, and complete published item field contract.

## Follow-Up Actions

1. Extend MesFrontlinePqcProcessRespVO and the assembler to include top-level inspectionTypeRules and taskSummary.
2. Add ruleSort and inspectionTypeRule to each pqcTaskOptions entry, using the locked version rule order and full rule object.
3. Preserve the complete published item field set in process and task item projections, including item sorting/applicability, quantity/ratio, critical/failure/source metadata, and source-original fields.
4. Add DF10 tests that fail without those fields and assert the exact contract shape, then rerun the Maven target, backend evidence validator, forbidden-source scan, and git diff --check.
