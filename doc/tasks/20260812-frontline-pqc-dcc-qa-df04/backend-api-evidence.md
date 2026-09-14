# DF04 Backend API Evidence

## Scope

- Service component: `DccProjectResolver`.
- Contract: `requireEnabledByRoute(routeId)` returns the formal DCC project identity for one route.
- No controller or external endpoint is added in DF04.

## Data Contract

- Input: positive `routeId` from the active-order route snapshot.
- Formal relation: current `mes_pro_route_dcc_project_binding` rows for the route.
- Output: `dccProjectCodeId`, `projectCode`, and `projectName` from the referenced DCC project.
- Dependencies: existing `MesRouteDccProjectBindingMapper`, `DccProjectCodeMapper`, and tenant context.

## Auth And Tenant Boundary

- The resolver runs inside an authenticated tenant context and requires `TenantContextHolder.getRequiredTenantId()`.
- The relation and referenced DCC project must both belong to that tenant.
- Cross-tenant and missing rows share the same invalid-reference behavior; no tenant interceptor bypass is used.

## Validation And Error Behavior

- Missing or invalid route identity: stable missing-binding error.
- No current relation: stable missing-binding error.
- Multiple current relations: stable ambiguous-binding error.
- Deleted/cross-tenant relation or invalid project id: stable invalid-reference error.
- Missing, deleted, disabled, or cross-tenant DCC project: stable invalid-reference error.
- Exceptions are not swallowed and no default project is returned.

## Required Services And Migrations

- Consumes the C00 route-DCC schema and DF03 relation mapper already merged into `int_main`.
- No schema, configuration, external service, or fixture migration is added by DF04.

## BDD Scenarios

- BDD: unique formal project -> Given exactly one formal relation to an enabled same-tenant project, When resolving the route, Then return that project identity.
- BDD: missing formal relation -> Given no formal relation, When resolving, Then fail without querying DCC projects.
- BDD: ambiguous formal relation -> Given duplicate formal relations, When resolving, Then fail without choosing a row.
- BDD: invalid formal reference -> Given a deleted/cross-tenant relation or missing/disabled/deleted/cross-tenant project, When resolving, Then fail with one invalid-reference semantic.
- BDD: no inference -> Given product/material/QA/form/process data that appears to match, When the formal relation is missing, Then do not consult those sources.

## RED Evidence

- RED: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineDccProjectResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL during test compilation because `DccProjectResolver` did not exist.
- Expected reason: the formal resolver contract was not implemented.

## GREEN Evidence

- GREEN: target Maven command -> PASS, 10 tests / 0 failures / 0 errors.
- Combined DF02/DF03/DF04 regression command: PASS.
- Static forbidden-inference scan: no product/material/productMaster/formBindings/QA/routeName/process or enabled-list scan references in the resolver.

## Verification

- Target and prerequisite regression tests passed.
- `git diff --check` passed.
- The forbidden-inference scan passed.

## Observability

- This pure read component surfaces stable service error codes to the calling transaction.
- It creates no audit record and performs no writes, consistent with DF04 scope.

## Blockers And Downstream

- No DF04 blocker remains.
- DF06 may consume the resolver after supervisor review and fast-forward merge.
