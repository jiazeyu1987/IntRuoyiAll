# Backend API Evidence

## Scope
- Read-model mapper used by `/mes/pro/process-pool/timeline/*` and `/mes/pro/process-pool/team-leader-workbench/*`.
- Populate the actual employee name and FIFO allocation state from formal tenant-scoped data.

## API And Data Contract
- API response shape is unchanged.
- `actualEmployeeUserName` comes from `system_users.nickname` by `tenant_id + actual_employee_id`.
- FIFO status and summary come from an aggregated read subquery over formal output fragments and allocation lines.
- One process-pool event remains one timeline/workbench row.

## Authorization And Failure Behavior
- Existing controller permissions remain unchanged.
- No fallback, default-success response, swallowed exception, or write behavior is introduced.
- Missing employee master data leaves the display name null; missing FIFO allocation remains explicit `PENDING`.

## Validation
- Tenant identity is part of every employee, fragment, allocation, and event join.
- The mapper aggregates one-to-many allocation lines before joining the timeline so one event remains one row.
- Missing optional display data remains explicit instead of being replaced by mock or default-success values.

## Required Fixtures And Schema
- Existing `system_users`.
- Existing `mes_pro_process_pool_quantity_fragment`.
- Existing `mes_pro_process_pool_fifo_allocation_line`.
- No schema migration is required.

## BDD
- BDD: tenant-scoped employee and FIFO projection -> Given a tenant-scoped process-pool event references an actual employee and has FIFO output allocation lines, When the timeline or team-leader workbench reads the event, Then it returns the formal employee nickname and an aggregated `PENDING` / `PARTIAL` / `ALLOCATED` FIFO state without duplicating the event row.
- BDD: cross-tenant identifiers remain isolated -> Given another tenant has the same user or event identifiers, When the read model joins names and allocations, Then only rows with the event tenant are used.

## TDD Evidence
- RED: `node yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> FAIL because the mapper did not project `actual_employee.nickname` and still returned null employee/FIFO fields.
- GREEN: `node yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS.
- Regression command: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTimelineQueryTest,ProcessPoolTimelineFilterTest,ProcessPoolTimelineDateFilterTest,ProcessPoolTimelineContentSummaryTest,ProcessPoolTimelineTraceabilityTest,ProcessPoolTeamLeaderWorkbenchServiceTest,MesProcessPoolTeamLeaderWorkbenchControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests.

## Verification
- Static mapper contract independently rerun on July 30, 2026: PASS.
- Focused timeline and team-leader Maven regression: PASS, 11 tests with 0 failures and 0 errors.
- Full task backend regression: PASS, 58 tests with 0 failures and 0 errors.

## Observability
- Existing backend request logging and SQL error propagation remain unchanged.

## Blockers
- None for the mapper contract.
