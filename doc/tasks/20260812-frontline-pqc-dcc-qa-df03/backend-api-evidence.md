# Backend API Evidence

## Scope

DF03 owns the MES route to DCC project code binding API under /mes/pro/route/dcc-project-binding, plus service, mapper, VO, error code, and backend tests.

## Contract

- GET returns the current route-DCC binding and latest relation version for a route.
- PUT saves or rebinds a route to one enabled DCC project code using expectedVersion CAS.
- DELETE removes the current binding using expectedVersion and writes a deleted tombstone with the next monotonically increasing version.
- Missing route, missing/disabled DCC project code, and stale expectedVersion fail fast.
- The API never infers DCC project code from product, QA regulation, formBindings, route name, process, or operation list.

## Validation

- Route existence is checked before reading or writing the relation.
- DCC project code must exist and have status ENABLE before save/rebind.
- Relation rows use TenantBaseDO to align with the C00 tenant_id schema.
- DELETE authorization stays on route update permission only; DCC query permission is not required for unbind.

## BDD: route DCC binding

Given a route has no current DCC binding, When a user saves an enabled DCC project code with expectedVersion 0, Then the service creates version 1 and returns the bound project code.

## BDD: stale version rejection

Given the route relation is already at version 3, When a user saves or deletes with expectedVersion 2, Then the service rejects the request and keeps the existing relation unchanged.

## BDD: unbind tombstone

Given the route relation is version 3, When a user unbinds with expectedVersion 3, Then the current row is closed and a deleted tombstone version 4 is inserted.

## RED:

mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesRouteDccProjectBindingServiceTest,MesRouteDccProjectBindingControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL. Expected reason: PRO_ROUTE_DCC_PROJECT_INVALID was missing, so disabled DCC project code rejection could not compile.

## GREEN:

mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesRouteDccProjectBindingServiceTest,MesRouteDccProjectBindingControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS. Result: 10 tests, 0 failures, 0 errors, BUILD SUCCESS.

## Verification

- Service tests cover initial bind, stale rebind rejection, rebind next version, disabled DCC project rejection, unbind tombstone, latest unbound version, and missing route.
- Controller tests cover GET, PUT, and DELETE delegation with expectedVersion.
- git diff --check passed with only LF/CRLF working-copy warnings.

## Blockers

None for DF03 backend scope.
