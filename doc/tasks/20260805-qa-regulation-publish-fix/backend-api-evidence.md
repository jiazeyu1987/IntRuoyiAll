# Backend API Evidence

## Endpoint Scope

- Endpoint: `POST /admin-api/mes/pro/route-product/save-qa-regulation-route-by-item`.
- Controller: `MesProRouteProductController#saveQaRegulationRouteProductByItem`.
- Service: `MesProRouteProductService#saveQaRegulationRouteProductByItem`.
- Runtime issue: screenshot error showed the frontend called the correct endpoint, but 48081 returned `NoResourceFoundException` / `请求地址不存在`.

## API And Data Contract

- Request body uses `MesProRouteProductByItemSaveReqVO` with `itemId` and `routeId`.
- Success writes the product-route binding for QA regulation scope only.
- Failure must be fail-fast: missing route, no ACTIVE route version, or permission failure must return explicit backend errors.
- Product-side `saveRouteProductByItem` semantics remain unchanged and still guard enabled route maintenance.

## Auth Permission Validation

- Permission is `mes:qc-template:update`.
- QA path must not call product-side `validateRouteNotEnable`.
- QA path must require the selected route to exist and have an ACTIVE route version.
- Login-state probe used an intentionally invalid route ID and received business validation `工艺路线不存在`, proving the route is mapped without writing a real binding.

## BDD Scenarios

- BDD: QA binds published route -> Given a QA user selects a published route with ACTIVE version When calling the QA endpoint Then the product-route binding is saved and route scope can be reloaded.
- BDD: QA route missing -> Given a QA user calls the endpoint with a non-existing route When the request reaches backend Then backend returns a business validation error instead of 404.
- BDD: Product route maintenance unaffected -> Given product maintenance uses `saveRouteProductByItem` When an enabled route is selected Then product-side enabled-route guard still applies.

## RED Evidence

- RED: Runtime log at `2026-08-05 20:16:16` -> FAIL, `/admin-api/mes/pro/route-product/save-qa-regulation-route-by-item` returned `NoResourceFoundException: No static resource ...`, matching the screenshot `请求地址不存在`.
- RED: Existing 48081 runtime jar inspection -> FAIL, old `backend-runtime-control-20260805-172627.jar` nested MES module did not contain `save-qa-regulation-route-by-item` in `MesProRouteProductController.class`.

## GREEN Evidence

- GREEN: Source and compiled target class inspection -> PASS, `MesProRouteProductController.class`, `MesProRouteProductService.class`, and `MesProRouteProductServiceImpl.class` contain the QA endpoint/method.
- GREEN: Current 48081 running jar inspection -> PASS, `backend-runtime-control-20260805-team-leader-employee-profile-hotpatch-20260805-203537.jar` nested MES module contains `save-qa-regulation-route-by-item` and QA service methods.
- GREEN: `http://127.0.0.1:48081/actuator/health` -> PASS, status `UP`.
- GREEN: Login-state API probe -> PASS, `POST /admin-api/mes/pro/route-product/save-qa-regulation-route-by-item` with invalid routeId returned code `1040501000`, message `工艺路线不存在`, and not `请求地址不存在`.

## Verification

- Verification: runtime jar class inspection -> PASS, current 48081 jar contains Controller, Service interface, and ServiceImpl QA route binding methods.
- Verification: health probe -> PASS, `http://127.0.0.1:48081/actuator/health` returned `UP`.
- Verification: login-state API probe -> PASS, invalid route request reached service validation and returned `工艺路线不存在`, proving the endpoint is mapped.
- Verification: no real route binding was written by the probe because it used an invalid route ID and failed before persistence.

## Runtime Notes

- Standard `restart-int-ruoyi-local.ps1 -Component backend` was attempted but Maven package timed out.
- Maven PID was diagnosed with `jcmd Thread.print`; it was stuck in `IncrementalBuildHelper.beforeRebuildExecution -> WinNTFileSystem.delete0`.
- Only the task-owned Maven/restart PIDs were stopped; unrelated worktree Java processes were not stopped.
- A QA route hotpatch jar was generated for inspection, but the active 48081 process is currently the newer combined hotpatch jar that already contains the QA endpoint.

## Blockers

- Full AC-M09 `MesQaInspectionRegulationServiceTest` remains blocked by the shared Maven target issue recorded in `verification-report.md`.
- No database migration was required for this runtime endpoint registration fix.
