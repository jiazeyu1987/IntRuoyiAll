# Backend API Evidence

## Endpoint Scope

- Endpoint: `POST /mes/pro/process-pool/team-leader/work-order/abnormal/report`.
- Controller: `MesProcessPoolTeamLeaderController#markAndReportWorkOrderAbnormal`.
- Service: `MesWorkOrderAbnormalReportServiceImpl#markAndReport`.

## API Contract And Data Contract

- Request VO keeps only `workOrderId` and `abnormalDescription`.
- Service BO keeps only `workOrderId`, `markerUserId`, and `abnormalDescription`.
- Persisted abnormal record leaves route process, process, source event, and abnormal reason columns unset for this path.

## Auth Permissions Validation Error Behavior

- Permission remains `mes:pro-process-pool-team-leader:abnormal`.
- Login user remains the marker/reporter source.
- Validation requires `workOrderId`, login `markerUserId`, and nonblank `abnormalDescription`; it no longer requires abnormal reason.

## Required Config Services Fixtures Migrations

- No schema or migration change required; existing nullable abnormal detail columns are left available for other historical data.

## BDD Scenarios

- BDD: 后端异常上报接口不要求工序和原因 -> Given 请求体只有 `workOrderId` 与 `abnormalDescription` / When 调用 `work-order/abnormal/report` / Then 后端按登录用户标记并上报，工序和异常原因字段保持空值。

## RED GREEN Evidence

- RED: BLOCKED - `mvn -pl yudao-module-mes -am "-Dtest=MesWorkOrderAbnormalReportServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before target tests due existing active-order add compile errors for missing `getRouteId/getRouteVersionId/getTransferIds`.
- GREEN: substitute - `node tests/e2e/work-order-abnormal-minimal-report-static.spec.js` -> PASS, covers backend VO, BO, controller method and service validation source contracts.

## Verification

- Verification: source-level contract passed via node tests/e2e/work-order-abnormal-minimal-report-static.spec.js.
ode tests/e2e/work-order-abnormal-minimal-report-static.spec.js.
- Verification: Maven target command remains blocked before task-owned JUnit because of existing active-order add compile errors.

## Contract Integration Verification

- Backend Maven target verification remains blocked by pre-existing compile errors unrelated to abnormal reporting.

## Observability Touchpoints

- No new logging or error swallowing was introduced.

## Blockers And Downstream Skills

- Backend verification requires the existing active-order add contract compile blocker to be fixed or isolated before JUnit can run.