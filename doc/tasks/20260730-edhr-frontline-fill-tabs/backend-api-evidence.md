# Backend API Evidence

## Scope

- Endpoint: `GET /admin-api/mes/pro/feedback/frontline/device-account/processes`
- Service: `MesFrontlineDeviceAccountContextService`
- Formal binding source: login user posts -> workstation workers -> route process workstations -> enabled routes -> workstation machinery.

## Contract

- The login account only receives route processes reachable from its enabled user posts and workstation bindings.
- Route processes are matched by `routeId + workstationId`; route-level authorization alone must not attach another workstation's device.
- One process may return multiple rows for multiple devices.
- A workstation without machinery still returns one process row with `deviceId = null`.
- Missing formal bindings remain fail-fast through the existing frontline error codes; no permission expansion, mock, or default success is allowed.

## Authorization And Validation

- Login user must exist, be enabled, and have formal post assignments.
- Workstation worker rows must bind those posts to enabled workstations.
- Route processes must explicitly reference those workstations and belong to enabled routes.
- Missing referenced master data is treated as an invalid formal context.

## BDD

- Given a login user post is bound to enabled workstations and enabled route processes, when the process endpoint is opened, then only those route/workstation processes are returned.
- Given one route process workstation has multiple machinery bindings, when context loads, then all devices are returned for that process.
- Given one authorized workstation has no machinery binding, when context loads, then the process remains available with a null device.
- Given the same route has different process workstations, when context loads, then each process receives only the device from its own workstation.

## RED

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineWorkstationPostRouteBindingSourceTest,MesFrontlineDeviceAccountContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: FAIL at test compilation because `MesFrontlineWorkstationPostRouteBindingSource` does not exist.

## Required Services And Data

- Existing tables only: `system_users`, `system_user_post`, `mes_md_workstation_worker`, `mes_md_workstation`, `mes_pro_route_process`, `mes_pro_route`, `mes_md_workstation_machine`, `mes_dv_machinery`.
- No schema migration is required.
- Real E2E uses task-owned, traceable, removable local fixture rows.

## Observability

- Existing REST business error responses remain visible to the frontend.
- E2E records the process endpoint response, browser console errors, and fixture cleanup result.

## Status

- RED confirmed.
- GREEN: targeted backend tests passed, 7 tests with no failures or errors.
- Static frontend contract and TypeScript checks passed.
- Real-path E2E pending runtime reload and local fixture setup.
