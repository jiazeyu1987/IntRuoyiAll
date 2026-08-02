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
