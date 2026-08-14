# Backend API Evidence - M1-M2 ActiveOrder Authority and Process Targets

## Scope

- Task: `20260801-role-requirement-matrix-implementation`.
- Milestone slice: M1 activeOrderId authority.
- Service/API scope: team-leader active order add/remove/list response and frontline PQC active order list/source validation.
- Owned backend paths: `MesTeamLeaderActiveOrderServiceImpl`, `MesProcessPoolTeamLeaderController`, `MesProcessPoolActiveOrderMapper`, `MesFrontlinePqcContextServiceImpl`, active order VO/BO/DO, and targeted tests.

## Contract

- Active order creation must require `leaderUserId`, `workOrderId`, `routeId`, and `routeVersionId`.
- Active order creation must snapshot the ERP fixed work-order quantity from `MesProWorkOrderDO.quantity`; missing quantity fails fast.
- Active order identity must include route, route version, business status, and optimistic-lock version.
- Active order response must expose route, route version, ERP fixed quantity snapshot, business status, and version.
- PQC order listing and active order validation must read unified `mes_pro_process_pool_active_order` through `MesProcessPoolActiveOrderMapper`; it must not list orders through `processPoolMapper.selectActiveList`.
- Historical M1/M2 note: PQC submit dependency on `selectActiveByWorkOrderRouteProcess` was not closed by M1/M2; it is closed by the M3 QA/PQC source gate below.

## Validation

- `MesProcessPoolTeamLeaderSchemaTest` validates active order fields and migration key shape.
- `MesTeamLeaderActiveOrderServiceTest` validates add/remove fail-fast and snapshot behavior.
- `MesProcessPoolTeamLeaderControllerTest` validates request/response mapping.
- `MesFrontlinePqcContextServiceTest` validates PQC active order source switch.
- `role-requirement-matrix-real-flow.e2e.js --check` validates RRM-BLK-001..007 are removed from current SOURCE blockers.

## BDD

- BDD: M1 activeOrder authority source switch -> Given M0 is accepted under the revised gate and M1 owns RRM-BLK-001..007 When production leader and PQC read active orders Then they use the same active order authority fields and PQC no longer lists orders from `mes_pro_process_pool`.

## RED

- RED: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `NoSuchFieldException: routeId`.
- RED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL, expected reason: real E2E script did not include `ACTIVE_ORDER_AUTHORITY_SQL`.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `MesFrontlinePqcContextServiceImpl` constructor and `MesProcessPoolActiveOrderMapper` query methods were not implemented.

## GREEN

- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 13 tests.
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 6 tests.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 19 tests.

## Verification

- Verification: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS.
- Verification: authorized `pnpm e2e:role-requirement-matrix:real:check` after M1 -> EXPECTED_BLOCKED_FOR_DOWNSTREAM with 24 SOURCE blockers, 0 ENV blockers, 0 RUNTIME blockers; RRM-BLK-001..007 no longer appear.
- Verification: no plaintext passwords are recorded; command evidence uses redacted environment handling.

## Blockers

- M1 blockers RRM-BLK-001..007 are `RESOLVED_VERIFIED`.
- M2 blockers RRM-BLK-026..028 are `RESOLVED_VERIFIED`.
- Remaining blockers are downstream: M3 owns RRM-BLK-017..025, M4 owns RRM-BLK-008..016, and M5 owns RRM-BLK-029..031.
- Full real Playwright E2E is still blocked by downstream SOURCE blockers and must not be reported as PASS.

## M2 Process Target Snapshot Scope

- Milestone slice: M2 production coefficient and planned quantity snapshots.
- Service scope: active order creation freezes one process target snapshot per scheduled route process; FIFO allocation, manual allocation, report confirmation, and order-process completion read target quantity through `MesTeamLeaderOrderProcessTargetService`.
- Owned backend paths: `MesTeamLeaderActiveOrderServiceImpl`, `MesTeamLeaderFifoAllocationService`, `MesTeamLeaderReportConfirmationServiceImpl`, `MesTeamLeaderOrderProcessCompletionService`, `MesTeamLeaderOrderProcessTargetService`, `MesProcessPoolActiveOrderProcessSnapshotMapper`, `MesProAutoScheduleServiceImpl`, and targeted tests.

## M2 Contract

- Active order creation must find the effective schedule order and schedule-order-process rows for the active order route/version.
- Each process snapshot must persist active order, work order, route, route version, route process, process, ERP fixed quantity, production quantity factor, and planned quantity.
- Planned quantity must equal ERP fixed quantity multiplied by the frozen production quantity factor; mismatches fail fast.
- Allocation and completion target quantities must use the frozen process snapshot, not `MesProWorkOrderDO.quantity`.
- Auto schedule must fail fast when the route-process production factor is missing, null, or non-positive; it must not default to `1`.

## M2 Validation

- `MesProcessPoolTeamLeaderSchemaTest` validates the process snapshot DO/migration fields.
- `MesTeamLeaderActiveOrderServiceTest` validates active order creation inserts per-process target snapshots.
- `MesTeamLeaderFifoAllocationServiceTest` validates remaining quantity uses snapshot planned quantity.
- `MesTeamLeaderReportConfirmationServiceTest` validates manual allocation uses snapshot planned quantity.
- `MesTeamLeaderOrderProcessCompletionServiceTest` validates completion target quantity uses snapshot planned quantity.
- `MesProAutoScheduleContractTest` validates no default production factor path remains.
- `role-requirement-matrix-real-flow.e2e.js --check` validates RRM-BLK-026..028 are removed from current SOURCE blockers.

## M2 BDD

- BDD: M2 production coefficient snapshots -> Given M1 activeOrderId authority is accepted and a pressure pump active order uses route-process production factors When the order is joined, allocated, reported, and completed Then each process target freezes ERP quantity, production factor, and planned quantity, and no path uses a missing factor default.

## M2 RED

- RED: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesProAutoScheduleContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: missing `MesProcessPoolActiveOrderProcessSnapshotDO`, `MesProcessPoolActiveOrderProcessSnapshotMapper`, `MesTeamLeaderOrderProcessTargetService`, and `MesTeamLeaderOrderProcessTarget`.

## M2 GREEN

- GREEN: BOM encoding repair for three Java tests -> PASS, affected test files no longer start with UTF-8 BOM.
- GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesProAutoScheduleContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 25 tests.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: `node IntRuoyiFronted\tests\e2e\role-requirement-matrix-preflight-static.spec.cjs` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.

## M2 Verification

- Verification: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED_FOR_DOWNSTREAM with 21 SOURCE blockers, 0 ENV blockers, 0 RUNTIME blockers; RRM-BLK-026..028 no longer appear.
- Verification: no plaintext passwords are recorded; command evidence uses redacted environment handling.

## M3 QA/PQC API Scope

- Milestone slice: M3 QA regulation and PQC source model.
- Service/API scope: frontline PQC active order process list, PQC task snapshot projection, PQC inspection submit command, QA regulation version/item lookup, and PQC piece detail persistence.
- Owned backend paths: `MesFrontlinePqcContextServiceImpl`, `MesFrontlinePqcSubmitCommand`, `MesFrontlineRouteProcessCandidate`, `MesFrontlineDeviceAccountController`, `MesFrontlineRouteProcessRespVO`, `MesFrontlinePqcSubmitReqVO`, QA regulation DO/Mapper classes, PQC task/detail DO/Mapper classes, and targeted tests.

## M3 Contract

- PQC process context must require a unified `activeOrderId` and a pending PQC task for the selected route process.
- PQC process response must expose `activeOrderId`, `pqcTaskId`, `regulationVersionId`, `inspectionType`, `businessDate`, `shiftCode`, `roundNo`, `plannedInspectionQuantity`, and dynamic `inspectionItems`.
- PQC submit must require `activeOrderId + pqcTaskId + regulationVersionId + inspectionType + businessDate + shiftCode + roundNo`; missing or mismatched identity must fail fast.
- PQC submit must update the PQC task to `SUBMITTED` and insert piece detail rows; it must not use latest production-event lookup as the submit source.
- QA regulation source must come from the published regulation version and its item rows, not from the temporary M0 derived QC fixture.

## M3 Validation

- `MesQaPqcSchemaTest` validates QA regulation and PQC task/detail schema and mapper contracts.
- `MesFrontlinePqcContextServiceTest` validates process context, published regulation item exposure, pending task identity, submit identity, task status update, and piece detail insert behavior.
- `role-requirement-matrix-real-flow.e2e.js --check` validates RRM-BLK-017..025 are removed from current SOURCE blockers.

## M3 BDD

- BDD: M3 QA regulation and PQC source model -> Given a pressure-pump active order has a published QA regulation version and a pending PQC task When PQC opens a process and submits inspection results Then the process and submit payload use the task/regulation identity and persist piece details without relying on latest production events.

## M3 RED

- RED: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after M2 -> EXPECTED_BLOCKED_FOR_M3, expected reason: RRM-BLK-017..025 still reported missing QA regulation ownership/version, PQC task, piece detail, submit source, and frontend task snapshot fields.

## M3 GREEN

- GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesQaPqcSchemaTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 8 tests.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-qa-regulation:static` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-pqc-dynamic-form:static` -> PASS.

## M3 Verification

- Verification: `pnpm --dir IntRuoyiFronted ts:check` -> PASS.
- Verification: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- Verification: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- Verification: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED_FOR_DOWNSTREAM with 12 SOURCE blockers, 0 ENV blockers, 0 RUNTIME blockers; RRM-BLK-017..025 no longer appear.
- Verification: no plaintext passwords are recorded; command evidence uses redacted environment handling.

## M3 Blockers

- RRM-BLK-017..025 are resolved by M3 QA/PQC schema, service, controller/VO, frontend dynamic rendering, and source-gate work.
- Remaining blockers are downstream: M4 owns RRM-BLK-008..016, and M5 owns RRM-BLK-029..031.
- Full real Playwright E2E is still blocked by downstream SOURCE blockers and must not be reported as PASS.

## M4 Transfer Trace And Release Source Scope

- Milestone slice: M4 transfer/release source model.
- Service/API scope: activeOrderId transfer trace read model and eDHR release completeness source adapter.
- Owned backend paths: `MesProcessPoolActiveOrderTransferTraceDO`, `MesProcessPoolActiveOrderTransferTraceMapper`, `MesActiveOrderTransferTraceService`, `MesOrderReleaseCompletenessService`, `MesOrderReleaseCompletenessServiceImpl`, `MesProEdhrReleaseServiceImpl`, `MesPqcInspectionTaskMapper`, `MesProcessPoolWorkOrderAbnormalMapper`, and targeted tests.

## M4 Contract

- Active order transfer trace must bind active order, work order, route, route version, source type, direction, transfer/line/detail IDs, material stock, batch, item, quantity, source object, status, occurrence time, idempotency key, and source snapshot.
- Trace source types must cover `TRANSFER`, `SHIPMENT`, `REPLENISHMENT`, `RETURN`, `BATCH_TRACE`, `SCRAP`, and `REWORK`.
- Release precheck must call a formal completeness service for inspection result, deviation closed, rework closed, scrap recorded, and inventory consistency.
- Missing active order, missing PQC task, open abnormalities, open rework/scrap traces, missing material trace, frozen stock, or negative stock must produce explicit source checks; no default PASS or source-not-integrated placeholder is allowed for M4 checks.
- Existing release service tests must keep old non-M4 cases scoped by mocking the new source adapter as NOT_APPLICABLE, not by removing the dependency or swallowing missing beans.

## M4 Validation

- `MesActiveOrderTransferTraceSchemaTest` validates active order transfer trace table, fields, mapper, service, source types, and migration key shape.
- Root `MesProEdhrReleaseServiceImplTest` validates M4 release checks no longer use `buildSourceNotIntegratedItem` and the source adapter exposes all five methods.
- Existing `service/pro/batchrecord/MesProEdhrReleaseServiceImplTest` validates prior release behaviors still pass with the new source adapter dependency mocked explicitly.
- `role-requirement-matrix-real-flow.e2e.js --check` validates RRM-BLK-008..016 are removed from current SOURCE blockers.

## M4 BDD

- BDD: M4 transfer trace and start check -> Given M1-M3 are accepted and activeOrderId is the unified order identity When an active order has transfer, shipment, replenishment, return, and batch/material stock facts Then the system must trace those facts through formal activeOrderId relations and keep missing sources blocked.
- BDD: M4 release completeness source checks -> Given eDHR release precheck needs inspection, deviation, rework, scrap, and inventory checks When release precheck runs Then those checks must come from formal source adapters and must not use source-not-integrated placeholders or default PASS.

## M4 RED

- RED: `pnpm --dir IntRuoyiFronted e2e:role-matrix-transfer-start-check:static` -> FAIL, expected reason: missing formal activeOrderId transfer relation source.
- RED: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceSchemaTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL/timeout during first run, expected reason: missing M4 schema/adapter before implementation and then corrupted `target\classes` generated output blocked standard Maven from completing.
- RED: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceSchemaTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> FAIL, expected reason: existing release service Spring test lacked the newly required `MesOrderReleaseCompletenessService` bean.

## M4 GREEN

- GREEN: standard Maven from backend root -> `mvn -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceSchemaTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 21 tests, 0 failures/errors.
- GREEN: no-fork Maven diagnostic -> `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceSchemaTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> PASS, 21 tests, 0 failures/errors.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-transfer-start-check:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.

## M4 Verification

- Verification: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED_FOR_M5 with 3 SOURCE blockers, 0 ENV blockers, 0 RUNTIME blockers; RRM-BLK-008..016 no longer appear.
- Verification: no plaintext passwords are recorded; command evidence uses in-process environment variables and result artifacts redact passwords.

## M4 Blockers

- RRM-BLK-008..016 are resolved by M4 transfer trace schema, source adapter, release service integration, static contract, Maven regression, and real source-gate work.
- Remaining blockers are downstream: M5 owns RRM-BLK-029..031 route batch-record/formBindings/default MAIN separation.
- Full real Playwright E2E is still blocked by downstream M5 SOURCE blockers and must not be reported as PASS.

## M6 AC-D29 Duplicate-submit API Scope

- Milestone slice: M6 AC-D29 PQC duplicate-submit / concurrency failure path.
- Service/API scope: `MesFrontlinePqcContextServiceImpl#submitPqcInspection(...)` status transition guard for formal PQC task submission.
- Data contract: only `MesPqcInspectionTaskDO.taskStatus=PENDING` may transition to `SUBMITTED`; non-pending statuses fail fast with `PRO_FRONTLINE_PQC_TASK_STATUS_INVALID`.
- Persistence contract: rejected non-pending submissions must not call `pqcTaskMapper.updateById`, `pqcPieceDetailMapper.insertBatch`, or `processPoolEventService.createPqcInspectionEvent`.
- Error behavior: task status mismatch is a formal service exception, not a silent idempotent success, fallback, or duplicate event suppression after write.
- Migration/config impact: none; no schema, seed, permission, or runtime config change.
- Observability touchpoint: explicit service error code allows frontend/E2E to distinguish duplicate/non-pending task submission from missing task identity and identity mismatch.

## M6 AC-D29 Duplicate-submit BDD / TDD Evidence

- BDD: duplicate PQC task submit is rejected before write -> Given a task has already reached `SUBMITTED` When the same `pqcTaskId` is submitted again Then the service must reject before updating task status, inserting piece details, or creating another process-pool PQC event.
- TEST_ADDED: `MesFrontlinePqcContextServiceTest#shouldRejectAlreadySubmittedPqcInspectionTask` asserts the formal error code and no persistence/event writes.
- RED_BLOCKED: Standard and non-incremental Maven target commands could not reach target test execution because the shared Windows Maven output directory stalled or failed before tests; details are in `execution-log.md`.
- IMPLEMENTING: Added `PRO_FRONTLINE_PQC_TASK_STATUS_INVALID`, `PQC_TASK_STATUS_PENDING`, and the status guard in `requirePqcTaskIdentity(...)`; replaced submit status literal with `PQC_TASK_STATUS_SUBMITTED`.
- AUTHORIZED_CLEANUP: User authorized stopping the blocking chain and cleaning/rebuilding MES `target`; the task stopped the original blocker `57820/7728/20224`, but Maven `clean` stalled in Windows file deletion with 1081 target entries remaining, and later same-root MES Maven chains kept reappearing.
- TARGET_ISOLATION: Moved residual `yudao-module-mes\target` to `target\rrm_m6_blocked_20260803_151631` to allow a fresh Maven output directory without deleting locked files.
- TEST_FIX: Fixed a Mockito overloaded `updateById` matcher in the new failure-path test by using `any(MesPqcInspectionTaskDO.class)`.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests, 0 failures, 0 errors, BUILD SUCCESS.
- Remaining blocker: this backend failure-path slice has target GREEN, but AC-D29 and M6 still require full real E2E failure paths, read-only/permission proof, concurrency/performance gates, cleanup, and coverage acceptance before release acceptance.

## M6 AC-D34 Duplicate Terminal Review API Scope

- Milestone slice: M6 AC-D34 PQC/team-leader submission review duplicate terminal-state guard.
- Service/API scope: `MesTeamLeaderSubmissionReviewServiceImpl#reviewSubmission(...)` for `APPROVED` / `REJECTED` review creation.
- Data contract: one process-pool event may have only one active terminal review outcome; a later confirm/reject attempt for the same event must fail fast before inserting another review record.
- Concurrency contract: review creation must lock the source process-pool event and read the latest review with a current read before inserting, so concurrent requests serialize on the same event identity.
- Error behavior: duplicate terminal review returns `PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS`; it is not treated as idempotent success and does not silently append another terminal log.

## M6 AC-D34 Duplicate Terminal Review BDD / TDD Evidence

- BDD: duplicate terminal review is rejected before insert -> Given a PQC submission event already has an `APPROVED` or `REJECTED` review When another leader request confirms or rejects the same event Then the service must reject before inserting a second terminal review.
- TEST_ADDED: `MesTeamLeaderSubmissionReviewServiceTest#shouldRejectDuplicateTerminalReviewForSameSubmission` asserts the formal error code and no duplicate review insert.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: duplicate terminal review did not throw; 3 tests, 1 failure, 0 errors.
- IMPLEMENTING: Added `PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS`; `reviewSubmission(...)` is transactional, locks the event through `selectByIdForUpdate(...)`, checks latest review with `LIMIT 1 FOR UPDATE`, and rejects existing terminal reviews before insert.
- GREEN: same target Maven command -> PASS, 3 tests, 0 failures/errors, BUILD SUCCESS.
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolTeamLeaderControllerTest,ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 14 tests, 0 failures/errors, BUILD SUCCESS.
- Remaining blocker: AC-D34 remains not accepted until real page review actions, process-inspection aggregation, cleanup, broader concurrency/performance, and full M6 coverage gates pass.

## M6 AC-D35 Self-review Isolation API Scope

- Milestone slice: M6 AC-D35 PQC/team-leader submission review self-confirm isolation.
- Service/API scope: `MesTeamLeaderSubmissionReviewServiceImpl#reviewSubmission(...)` for `APPROVED` / `REJECTED` review creation.
- Data contract: a review request whose `leaderUserId` equals the source process-pool event `actualEmployeeId` must fail fast before reading latest review state for insertion or inserting a review record.
- Error behavior: self-review returns `PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN`; it is not left to frontend hiding, shared-account convention, or downstream process-inspection aggregation.
- Migration/config impact: none; no schema, seed, permission, or runtime config change.

## M6 AC-D35 Self-review Isolation BDD / TDD Evidence

- BDD: self-review is rejected before insert -> Given a PQC submission event was submitted by actual inspector `3001` When a leader review request also uses `leaderUserId=3001` Then the backend must reject and no review row is inserted.
- TEST_ADDED: `MesTeamLeaderSubmissionReviewServiceTest#shouldRejectSelfReviewWhenLeaderIsActualInspector` asserts the formal error code and no review insert.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: self-review did not throw; 4 tests, 1 failure, 0 errors.
- IMPLEMENTING: Added `PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN`; `reviewSubmission(...)` checks `leaderUserId == actualEmployeeId` before insert.
- GREEN: same target Maven command -> PASS, 4 tests, 0 failures/errors, BUILD SUCCESS.
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolTeamLeaderControllerTest,ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 15 tests, 0 failures/errors, BUILD SUCCESS.
- Remaining blocker: AC-D35 remains not accepted until real-page self-confirm action, permission/read-only proof, cleanup, broader concurrency/performance, and full M6 coverage gates pass.

## M6 AC-M21 / AC-D37 Process-inspection Aggregation API Scope

- Milestone slice: M6 AC-M21 / AC-D37 PQC process-inspection aggregation backend status gate.
- Service/API scope: `MesTeamLeaderSubmissionReviewServiceImpl#reviewSubmission(...)` and `MesPqcProcessInspectionAggregationService#aggregateApprovedPqcSubmission(...)`.
- Data contract: `mes_pro_process_pool_pqc_record` starts as `process_inspection_aggregation_status=PENDING`; only an inserted `APPROVED` review can transition the matching event record to `AGGREGATED`.
- Traceability contract: aggregated records retain `eventId`, process context, `processInspectionReviewId`, and `processInspectionAggregatedAt`; PQC task/round/regulation-version trace remains available through the process-pool event source and raw payload.
- Exclusion contract: `REJECTED` reviews are recorded but do not aggregate; missing PQC records, already aggregated records, and concurrent zero-row updates fail fast instead of silently counting completion.
- Error behavior: missing PQC record returns `PRO_PROCESS_POOL_PQC_RECORD_REQUIRED`; duplicate or concurrent aggregation returns `PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED`.
- Migration/config impact: added `20260803_mes_process_pool_pqc_process_inspection_aggregation.sql`, H2 schema columns, mapper conditional update, and explicit `PENDING` creation in `MesProcessPoolEventServiceImpl#createPqcInspectionEvent`.

## M6 AC-M21 / AC-D37 Process-inspection Aggregation BDD / TDD Evidence

- BDD: approved PQC review aggregates process-inspection evidence -> Given a formal PQC submission event has a `PENDING` process-inspection aggregation status When a team leader approves the submission Then the backend must atomically mark that PQC record as `AGGREGATED` with the review id and aggregation timestamp.
- TEST_ADDED: `MesTeamLeaderSubmissionReviewServiceTest` asserts `APPROVED` triggers aggregation and `REJECTED` does not.
- TEST_ADDED: `MesPqcProcessInspectionAggregationServiceTest` covers success, missing record, already aggregated, and concurrent zero-row update paths.
- TEST_ADDED: `MesProcessPoolSchemaTest` covers aggregation fields and migration file.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL at testCompile, expected reason: missing `MesPqcProcessInspectionAggregationService`.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests, 0 failures/errors, BUILD SUCCESS.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolPqcEventTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 4 tests, 0 failures/errors, BUILD SUCCESS.
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProcessPoolTeamLeaderControllerTest,ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineQueryTest,MesProcessPoolPqcEventTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 24 tests, 0 failures/errors, BUILD SUCCESS.
- Remaining blocker: AC-M21/AC-D37 remain not accepted until real page approval visibility, read-only verification, cleanup, broader concurrency/performance, and full M6 coverage gates pass.

## M6 AC-M21 / AC-D37 Aggregation Event-type Isolation API Scope

- Milestone slice: M6 AC-M21 / AC-D37 process-inspection aggregation event-type isolation.
- Service/API scope: `MesTeamLeaderSubmissionReviewServiceImpl#reviewSubmission(...)` on the shared `submission/review` endpoint.
- Data contract: `APPROVED` reviews aggregate only when the locked source process-pool event type is `PQC_INSPECTION`; approved `PRODUCTION_SUBMIT` reviews keep their review record but must not require or mutate a PQC record.
- Error behavior: production review success must not be converted into `PRO_PROCESS_POOL_PQC_RECORD_REQUIRED`; that error remains scoped to actual PQC aggregation calls.
- Migration/config impact: none; the existing process-inspection aggregation columns and migration remain unchanged.

## M6 AC-M21 / AC-D37 Aggregation Event-type Isolation BDD / TDD Evidence

- BDD: approved production review does not aggregate PQC process inspection -> Given the shared `submission/review` backend can review production and PQC process-pool events When an `APPROVED` review is inserted for a `PRODUCTION_SUBMIT` event Then the service must not call PQC process-inspection aggregation.
- TEST_ADDED: `MesTeamLeaderSubmissionReviewServiceTest#shouldNotAggregateApprovedProductionSubmission` asserts an approved production review inserts the review and never calls `aggregateApprovedPqcSubmission(...)`.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: approved production review still called PQC aggregation; 6 tests, 1 failure, 0 errors.
- IMPLEMENTING: `reviewSubmission(...)` now requires both `reviewStatus=APPROVED` and `eventType=PQC_INSPECTION` before calling `MesPqcProcessInspectionAggregationService`.
- GREEN: same target Maven command -> PASS, 6 tests, 0 failures/errors, BUILD SUCCESS.
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProcessPoolTeamLeaderControllerTest,ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineQueryTest,MesProcessPoolPqcEventTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 25 tests, 0 failures/errors, BUILD SUCCESS.
- Remaining blocker: AC-M21/AC-D37 remain not accepted until real page approval visibility, read-only verification, cleanup, broader concurrency/performance, and full M6 coverage gates pass.

## M6 AC-M21 / AC-D37 Aggregation Read-model API Scope

- Milestone slice: M6 AC-M21 / AC-D37 PQC process-inspection aggregation read-model and page visibility contract.
- Service/API scope: `MesProProcessPoolTimelineReadMapper.xml`, `ProcessPoolTimelineServiceImpl`, `ProcessPoolTimelineEventRespVO`, and the PQC team-leader submission page API that reuses the timeline read model.
- Data contract: approved PQC aggregation status must be read from `mes_pro_process_pool_pqc_record` and returned as `processInspectionAggregationStatus`, `processInspectionReviewId`, and `processInspectionAggregatedAt`; frontend code must not infer this state from `submissionReviewStatus`, `formBindings`, or static labels.
- Page contract: `TeamLeaderWorkbenchPage.vue` renders a PQC-only process-inspection aggregation column and exposes stable event selectors so the real E2E can approve exactly the submitted event and verify the same event returns `AGGREGATED`.
- Runtime caveat: this slice is GREEN at backend read-model, frontend static/type, and E2E-script contract levels; it is not yet full real E2E accepted because the updated code still needs to be loaded into the task runtime before a live approval/aggregation page run.

## M6 AC-M21 / AC-D37 Aggregation Read-model BDD / TDD Evidence

- BDD: aggregation status is visible after approved PQC review -> Given a PQC process-pool event has an aggregated PQC record When the PQC leader submission read model is queried Then the response and page must expose `AGGREGATED`, the review id, and the aggregation timestamp for the same event.
- TEST_ADDED: `ProcessPoolTimelineQueryTest#shouldExposePqcProcessInspectionAggregationStatus` asserts timeline service field propagation.
- TEST_ADDED: `process-pool-timeline-mapper-static.spec.cjs` asserts the mapper selects the three formal `pqc_record` aggregation columns.
- RED: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL at testCompile, expected reason: missing read/response fields.
- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> FAIL, expected reason: mapper did not select `process_inspection_aggregation_status`.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests, 0 failures/errors, BUILD SUCCESS.
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS.
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProcessPoolTeamLeaderControllerTest,ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineQueryTest,ProcessPoolTimelineFilterTest,MesProcessPoolPqcEventTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 28 tests, 0 failures/errors, BUILD SUCCESS.
- Frontend verification: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js`, `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static`, and `pnpm --dir IntRuoyiFronted ts:check` all PASS after installing worktree dependencies from the frozen lockfile.

## M6 AC-D12/D38 + AC-D27 Backend Performance API Scope

- Milestone slice: M6 daily-close and PQC piece-detail backend query-count proof.
- Service/API scope: `ProcessPoolTimelineServiceImpl#getTimelinePage(...)`, `MesTeamLeaderActiveOrderService`, and `MesFrontlinePqcContextServiceImpl#listProcessesByActiveOrder(...)`.
- Daily-close contract: timeline submission summary and active-order card reads must use bounded count/page or single active-order queries and must not load per-row detail or rebuild per-process snapshots.
- PQC piece-detail contract: frontline PQC route-process context must reuse the already bulk-loaded active-order PQC task list; it must not call `selectPendingByActiveOrderProcess(...)` for each route process.
- Status-invalid contract: stale submitted-task submission still fails fast with `PRO_FRONTLINE_PQC_TASK_STATUS_INVALID`; the bulk-list change must not downgrade stale submitted tasks to a generic missing-task error.

## M6 AC-D12/D38 + AC-D27 Backend Performance BDD / TDD Evidence

- BDD: daily-close and piece-detail performance paths do not perform hidden N+1 lookups -> Given the daily-close board and PQC piece-detail modal already have request-budget evidence When backend services assemble their source data Then target tests must prove bounded count/page or bulk reads and zero per-row/per-process task detail queries.
- TEST_ADDED: `ProcessPoolTimelineFilterTest#shouldUseCountAndPageQueriesWithoutDetailLookupsForDailyCloseSubmissionSummary`.
- TEST_ADDED: `MesTeamLeaderActiveOrderServiceTest#shouldListActiveOrdersWithSingleActiveOrderQueryForDailyClosePerformance`.
- TEST_ADDED: `MesFrontlinePqcContextServiceTest#shouldPreparePqcPieceDetailContextWithBulkQueriesOnly`.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineFilterTest,MesTeamLeaderActiveOrderServiceTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `MesFrontlinePqcContextServiceImpl` still used per-process `selectPendingByActiveOrderProcess`, causing the bulk-query test to throw `PRO_FRONTLINE_PQC_TASK_REQUIRED`.
- IMPLEMENTING: `resolvePqcTaskContext(...)` now selects pending tasks from the bulk-loaded task list with mapper-equivalent `businessDate / inspectionType / roundNo / id` ordering; `submitPqcInspection(...)` preserves stale submitted-task status validation before active-process lookup.
- TEST_FIX: existing PQC context unit tests now provide `selectListByActiveOrderId` fixtures instead of per-process pending-task stubs.
- GREEN: same target Maven command -> PASS, 23 tests, 0 failures/errors, BUILD SUCCESS.
- Static verification: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- Syntax verification: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- Remaining blocker: M6 remains not accepted until full real failure paths, permission/read-only breadth, cleanup, runtime/paging-drift evidence, concurrency/performance gate completion, and 62 AC coverage close.
