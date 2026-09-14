# DF07 Backend API Evidence

## Scope

- Service scope: MesQaInspectionRegulationService locked QA version process reader.
- Changed behavior: read QA-owned process rows by dccProjectCodeId + qaRegulationId + qaRegulationVersionId from an active order locked snapshot.
- Out of scope: product/material inference, MES route process existence validation, current QA lookup, item/equipment aggregation, frontend, SQL/schema, permissions.

## API Contract And Data Contract

- Contract method: getLockedVersionProcessesForOrder(Long dccProjectCodeId, Long qaRegulationId, Long qaRegulationVersionId).
- Input contract: all three IDs must point to an existing DCC project code, a QA regulation owned by that DCC project code, and a QA version owned by that regulation.
- Version contract: PUBLISHED and RETIRED are readable for locked historical orders; DRAFT or unrelated versions fail fast.
- Output contract: returns MesQaInspectionRegulationProcessDO rows from the locked QA version only.

## Auth, Permissions, Validation, And Error Behavior

- No new endpoint or permission was added in DF07.
- Missing DCC project code fails with QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID.
- Missing QA regulation fails with QA_INSPECTION_REGULATION_NOT_EXISTS.
- QA regulation outside the locked DCC project fails with QA_INSPECTION_REGULATION_DCC_PROJECT_INVALID.
- Missing or unrelated QA version fails with QA_INSPECTION_REGULATION_VERSION_NOT_EXISTS.
- Non-PUBLISHED and non-RETIRED QA version fails with QA_INSPECTION_REGULATION_VERSION_NOT_PUBLISHED.
- Empty process snapshot fails with QA_INSPECTION_REGULATION_SNAPSHOT_INVALID.

## Required Config, Services, Fixtures, And Migrations

- Required existing services/mappers: DCC project code mapper, QA regulation mapper, QA regulation version mapper, QA regulation process mapper.
- Required fixtures: mocked DCC project, regulation, version, and process rows in MesQaInspectionRegulationServiceTest.
- Migrations: none.
- Runtime services: none.

## BDD Scenarios

- BDD: locked QA version process list success -> Given an order locked DCC/QA/version triple with a PUBLISHED or RETIRED QA version and QA-owned process rows; When the backend reads the locked process list; Then it returns only that QA version process list without item/equipment aggregation.
- BDD: locked QA version rejects invalid ownership -> Given the locked QA regulation or version belongs to another DCC project/regulation; When the backend reads the locked process list; Then it fails fast and does not return default success.
- BDD: locked QA version rejects unsupported status -> Given the QA version is DRAFT; When the backend reads the locked process list; Then it fails fast with the version status error.

## RED

- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, in isolated RED worktree with only the DF07 test patch applied, MesQaInspectionRegulationServiceImpl had no getLockedVersionProcessesForOrder(Long, Long, Long) method and testCompile failed.

## GREEN

- GREEN: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, MesQaInspectionRegulationServiceTest 12 tests / 0 failures / 0 errors / 0 skipped, BUILD SUCCESS.

## Contract Or Integration Verification

- git diff --check -> PASS.
- Production diff forbidden scan -> PASS, no product/material/formBindings/selectEnabledList/fallback/兼容/兜底/默认成功/routeProcess/MesRouteProcess/itemEquipment/equipment.
- Independent verifier report -> PASS, no route-process existence validation, no product/material inference, no item/equipment assembly.

## Observability Touchpoints

- No new logs or metrics were added because DF07 exposes service-level fail-fast behavior only and adds no runtime endpoint/job.
- Failure remains observable through existing ServiceException error codes and target unit tests.

## Blockers And Downstream Skill Needs

- No DF07 implementation blocker remains.
- Downstream consumers DF08/DF10 must call this locked-version method instead of getCurrent or product/material/route-derived lookups.
