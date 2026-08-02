# Database Schema Evidence - M0-M2 Fixtures and ActiveOrder Schemas

## Data

- Goal: prepare local M0 test fixtures for the role requirement matrix real E2E preflight in tenant `1 / 芋道源码`.
- Affected entities: `system_users`, `system_user_role`, `dcc_electronic_signature_authorization`, `dcc_electronic_signature_image`, `mes_pro_work_order`, `mes_wm_batch`, `mes_wm_material_stock`, `mes_wm_transfer`, `mes_wm_transfer_line`, `mes_wm_transfer_detail`, `mes_qc_template`, `mes_qc_indicator`, `mes_qc_template_indicator`, `mes_qc_template_item`.
- Created or updated: six role accounts, six role bindings to `super_admin`, six electronic signature authorizations/images, one RRM work order, two RRM transfer fixtures, one IPQC QC template, three initial indicators, one derived temporary QA template, 49 derived inspection indicators, 49 template-indicator links, and template-product links.
- Sensitive data handling: account password values are not written to this evidence file; `real:check` evidence redacts passwords.

## Migration

- Migration tool: none. This was a local data fixture write against existing MySQL schema; no DDL, indexes, constraints, or production schema files were changed.
- Schema verification: `DESCRIBE` and `SHOW INDEX` were run before writes for all touched tables with Chinese text handled through `python -X utf8`.
- Fixture prefix: `RRM-20260801-`.
- Formal source caveat: these fixtures do not create the formal activeOrderId/QA regulation schema required by M1-M4.
- Derived QA source: `bpm_form_template_version` `32 / 过程检验记录 V3.0` source file `过程检验记录.docx`; the fixture parses 49 inspection rows into `mes_qc_template_indicator`.

## Safety

- Scope limited to tenant `1` after explicit user authorization for this local test run.
- No rows were deleted.
- Existing user roles were preserved; `super_admin` was added for local test coverage only.
- Existing pressure pump route/version/process/batch-record bindings were read but not modified.
- Electronic signature fixture rows reuse an existing local test image file metadata; no historical signature records were forged.
- QC/IPQC fixture rows are task-prefixed and documented as local preflight fixtures, not formal QA regulation versions.
- The derived QA template `RRM-20260801-QA-REG-PP-V21` is local temporary test data. It does not modify route V21, batch-record bindings, form-slot bindings, or published FormCenter templates.
- Source row `组装Ⅲ` has no exact V21 route process match and remains recorded as unmatched; no packaging process was silently substituted.

## Rollback

Rollback is task-scoped and can be performed by deleting or reverting rows with `creator/updater = 'codex-rrm'` or `RRM-20260801-` codes, then removing added `system_user_role` rows for role `1` from the six selected users if no longer needed. Password rollback would require a secure credential decision from the user because old password hashes were not recorded in task documents.

## BDD

BDD: M0 authorized local tenant fixture setup -> Given the user authorized local tenant `芋道源码`, six selected accounts, and pressure pump route V21 When local M0 fixtures are prepared Then ENV/RUNTIME preflight data for tenant/accounts/permissions/signatures/order/route/transfer/QC exists without claiming formal SOURCE blockers are solved.

BDD: M0 derived QA regulation fixture -> Given pressure pump route V21 MAIN batch-record bindings and `PROCESS_INSPECTION / 过程检验记录 V3.0` When the source Word table is parsed Then a temporary QC template captures the source inspection methods, source quantities, and user-authorized temporary patrol coefficient without claiming formal QA regulation ownership/version is implemented.

## RED

RED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL, expected reason: real E2E preflight did not yet require the explicit local baseline tenant authorization token.

RED: `pnpm e2e:role-requirement-matrix:real:check` -> FAIL, expected reason: user-authorized tenant `芋道源码` was still rejected by the ENV tenant guard before the explicit authorization token existed.

RED: derived QA regulation data validation query -> FAIL, expected reason: template `RRM-20260801-QA-REG-PP-V21` did not exist and therefore could not cover the 49 source inspection rows from `过程检验记录 V3.0`.

## GREEN

GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS.

GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS.

GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS.

GREEN: database verification query -> PASS; six users have enabled authorizations and active signature images, roles include `super_admin`, work order `980008`, transfers `1,2`, QC template `5`, and indicators `5,6,7` exist.

GREEN: derived QA regulation database verification -> PASS; template `6 / RRM-20260801-QA-REG-PP-V21` exists, links product `902149`, has 49 derived method rows, and every method row records a temporary first-inspection quantity plus patrol coefficient marker.

GREEN: `pnpm e2e:role-requirement-matrix:real:check` with redacted password env and explicit local tenant authorization -> EXPECTED_BLOCKED with 31 SOURCE blockers and no ENV/RUNTIME blockers.

## Verification

- Tenant: `芋道源码`, `status=0`, not deleted.
- Accounts: `liuyueyue`, `lvyujie`, `sunxiaoqing`, `shangmengying`, `huzonggang`, `zhengxiaofang`.
- Signature image IDs: `22`, `23`, `24`, `25`, `26`, `27`.
- Route: `922119 / RT000028 / 球囊扩张压力泵`; active version `448 / V21`.
- Process inspection binding: route processes `928609` and `928610` have `PROCESS_INSPECTION` / `过程检验记录`.
- Work order: `980008 / RRM-20260801-PP-MO-001`.
- Transfer IDs: `1,2`.
- QC template: `5 / RRM-20260801-IPQC-PRESSURE-PUMP`.
- Derived QA template: `6 / RRM-20260801-QA-REG-PP-V21`, 49 derived inspection methods from `过程检验记录 V3.0`.
- Temporary PQC defaults: first-inspection quantity uses source quantity when present or `5`; patrol coefficient uses `0.05`.
- Latest real preflight evidence: `doc/tasks/20260801-role-requirement-matrix-implementation/role-requirement-matrix-real-e2e-evidence.md`.

## Blockers

- M0 is accepted under the revised 2026-08-02 gate because those 31 SOURCE gaps are structured and assigned to M1-M5; the gaps still block their owner milestones until formally implemented.
- The local QC template fixture is not a formal QA regulation/version model.
- The derived QA template fixture is not a formal QA regulation/version model and does not solve QA ownership, immutable published version, PQC task identity, regulation snapshot, or piece-detail model blockers.
- The local work order and transfer fixtures are not an activeOrderId relation source.

## M1 ActiveOrder Authority Schema

### Data

- Entity: `mes_pro_process_pool_active_order`.
- Added authority fields: `route_id`, `route_version_id`, `erp_fixed_quantity_snapshot`, `business_status`, and `version`.
- DO alignment: `MesProcessPoolActiveOrderDO` now exposes `routeId`, `routeVersionId`, `erpFixedQuantitySnapshot`, `businessStatus`, and `@Version Integer version`.

### Migration

- Migration file: `IntRuoyiBackend/sql/mysql/20260802_mes_process_pool_active_order_authority.sql`.
- Migration adds the authority fields, performs fail-fast backfill checks before `NOT NULL`, drops the old `uk_mes_pp_active_order` when it contains `leader_user_id`, and adds the new unique key on `(tenant_id, work_order_id, route_id, route_version_id, deleted)`.
- Static `real:check` now reads both the original P1 active-order table migration and the M1 authority migration so the old key is not reported when the new migration formally drops/replaces it.

### Safety

- No running database data repair was executed in this slice.
- Existing legacy rows are not guessed into route/version/quantity: the migration contains explicit precondition checks and fails fast if a row lacks formal route, route version, or ERP fixed quantity source.
- No fallback, mock, default route version, default quantity, or silent downgrade was introduced.

### Rollback

- Rollback strategy is migration-level: restore from the pre-migration database backup or revert the M1 migration before applying to shared environments.
- The migration intentionally does not include a destructive runtime rollback branch inside application code.

### BDD

- BDD: M1 active order authority schema -> Given an active order is promoted to the cross-role authority source When it is created or checked by PQC Then it must persist route, route version, ERP fixed quantity snapshot, business status, optimistic lock version, and a cross-role unique key.

### RED

- RED: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `NoSuchFieldException: routeId`.
- RED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL, expected reason: real E2E script did not include `ACTIVE_ORDER_AUTHORITY_SQL`.

### GREEN

- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 19 tests.
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS.

### Verification

- Verification: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS.
- Verification: authorized `pnpm e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED_FOR_DOWNSTREAM with 24 SOURCE blockers, 0 ENV blockers, 0 RUNTIME blockers; `uk_mes_pp_active_order` no longer appears as a blocker.

### Blockers

- RRM-BLK-001..007 are resolved by M1 schema/service/source-switch work.
- RRM-BLK-026..028 are resolved by M2 process-target snapshot schema/service/source-switch work.

## M2 ActiveOrder Process Target Snapshot Schema

### Data

- Entity: `mes_pro_process_pool_active_order_process_snapshot`.
- Goal: freeze per-active-order, per-route-process target quantities for production allocation, report confirmation, and process completion.
- Added persistence model: `MesProcessPoolActiveOrderProcessSnapshotDO` with `activeOrderId`, `workOrderId`, `routeId`, `routeVersionId`, `routeProcessId`, `processId`, `erpFixedQuantitySnapshot`, `productionQuantityFactorSnapshot`, and `plannedQuantitySnapshot`.

### Migration

- Migration file: `IntRuoyiBackend/sql/mysql/20260802_mes_process_pool_active_order_process_snapshot.sql`.
- Migration tool: release SQL migration with metadata `dependsOn=20260802_mes_process_pool_active_order_authority`.
- Schema creates a tenant-scoped unique key on `(tenant_id, active_order_id, route_process_id, process_id, deleted)`.
- Schema creates a lookup index on `(tenant_id, work_order_id, route_process_id, process_id)`.

### Safety

- No running database data repair was executed in this slice.
- Existing legacy rows are not guessed into target quantities; active order creation must read effective schedule-order-process snapshots and fail fast when source quantity/factor/planned quantity is missing or inconsistent.
- No fallback, mock, default production factor, default planned quantity, or silent downgrade was introduced.

### Rollback

- Rollback strategy is migration-level: restore from the pre-migration database backup or revert the M2 migration before applying to shared environments.
- Application code intentionally does not include a runtime fallback to `MesProWorkOrderDO.quantity` when the process target snapshot is missing.

### BDD

- BDD: M2 active order process target schema -> Given an active order is created for a route/version with scheduled process factors When production allocation, manual confirmation, and process completion run Then they read the frozen process target snapshot and fail fast if it is missing or non-positive.

### RED

- RED: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesProAutoScheduleContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: missing process snapshot DO/mapper/target service classes.

### GREEN

- GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesProAutoScheduleContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 25 tests.
- GREEN: `node IntRuoyiFronted\tests\e2e\role-requirement-matrix-preflight-static.spec.cjs` -> PASS.

### Verification

- Verification: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- Verification: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- Verification: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED_FOR_DOWNSTREAM with 21 SOURCE blockers, 0 ENV blockers, 0 RUNTIME blockers; `activeOrderProductionQuantityFactorSnapshot`, `activeOrderPlannedQuantitySnapshot`, and `defaultProductionQuantityFactorInAutoSchedule` no longer appear.

### Blockers

- RRM-BLK-026..028 are resolved by M2 schema/service/source-switch work.
- Remaining schema/source blockers are downstream: M3 owns QA/PQC schema gaps, M4 owns transfer/release relation gaps, and M5 owns route configuration separation gaps.

## M3 QA Regulation And PQC Task Schema

### Data

- Entity group: formal QA inspection regulation, regulation version, regulation item, PQC inspection task, and PQC inspection piece detail.
- Goal: replace M0 temporary QC template evidence with formal, queryable source models for M3 QA regulation ownership, published version identity, PQC task identity, and piece-detail persistence.
- Added persistence models: `MesQaInspectionRegulationDO`, `MesQaInspectionRegulationVersionDO`, `MesQaInspectionRegulationItemDO`, `MesPqcInspectionTaskDO`, and `MesPqcInspectionPieceDetailDO`.
- Added mappers: `dal/mysql/qa/regulation/*` and `dal/mysql/pro/processpool/pqc/*`.

### Migration

- Migration files: `IntRuoyiBackend/sql/mysql/20260802_mes_qa_inspection_regulation.sql` and `IntRuoyiBackend/sql/mysql/20260802_mes_pqc_inspection_task.sql`.
- Migration tool: release SQL migration files under the standard backend `sql/mysql` directory.
- QA regulation schema separates regulation master, immutable published version identity, and item rows.
- PQC task schema persists active order, work order, route/version/process, regulation version, inspection type, business date, shift code, round number, planned/actual quantity, status, and submitter.
- PQC piece detail schema persists per-task item/piece results so submitted inspection evidence can be reconstructed.

### Safety

- No running database data repair was executed in this slice.
- Existing temporary QC fixtures remain documented as M0 preflight data only; M3 code does not use them as the formal QA regulation source.
- Missing published regulation, missing pending PQC task, or mismatched submit identity fails fast; the implementation does not write default task identity, default quantity, default pass, or mock detail rows.
- No fallback, mock, default QA regulation, default PQC task, default inspection item, or silent downgrade was introduced.

### Rollback

- Rollback strategy is migration-level: restore from the pre-migration database backup or revert the M3 migrations before applying to shared environments.
- Application code intentionally does not include a runtime fallback from M3 formal QA/PQC tables to the M0 derived QC template.

### BDD

- BDD: M3 QA/PQC schema -> Given a pressure-pump process has a published QA regulation version and a pending PQC task When PQC context and submit run Then the task identity and piece detail rows come from formal QA/PQC tables and fail fast when missing.

### RED

- RED: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after M2 -> EXPECTED_BLOCKED_FOR_M3, expected reason: missing QA regulation ownership/version model, PQC task model, PQC piece detail model, production-event-independent PQC submit source, and dynamic PQC frontend source.

### GREEN

- GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesQaPqcSchemaTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 8 tests.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-qa-regulation:static` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-pqc-dynamic-form:static` -> PASS.

### Verification

- Verification: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- Verification: `pnpm --dir IntRuoyiFronted ts:check` -> PASS.
- Verification: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- Verification: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED_FOR_DOWNSTREAM with 12 SOURCE blockers, 0 ENV blockers, 0 RUNTIME blockers; `qaRegulationOwnership`, `qaRegulationVersionModel`, `pqcTaskModel`, `pqcPieceDetailModel`, `selectActiveByWorkOrderRouteProcess`, `hardcodedPqcInspectionItems`, `defaultPqcInspectionType`, `defaultPqcInspectionQuantity`, and `defaultPqcScrapQuantity` no longer appear.

### Blockers

- RRM-BLK-017..025 are resolved by M3 schema/service/frontend source-switch work.
- Remaining schema/source blockers are downstream: M4 owns transfer/release relation gaps, and M5 owns route configuration separation gaps.
