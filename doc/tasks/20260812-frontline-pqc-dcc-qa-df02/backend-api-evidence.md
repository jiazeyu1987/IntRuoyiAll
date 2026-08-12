# Backend API Evidence - DF02

## Scope

DF02 only: ActiveOrderSnapshotResolver resolves activeOrderId to the same-tenant effective active-order route snapshot and locked QA snapshot. It does not expose a controller contract, does not write database rows, and does not update shared mapper implementations.

## Contract

- Input: activeOrderId
- Output: current-tenant active-order route snapshot and QA locked snapshot.
- Failure: nonexistent, removed, cross-tenant illegal reference, missing route snapshot, or missing QA snapshot fails fast.
- Persistence: read-only; no database writes.

## BDD

- BDD: 选择订单解析路线 -> Given a valid active order with route and QA snapshots, When the resolver requires the effective snapshot, Then it returns only the server-side locked values.
- BDD: 非法订单统一失败 -> Given an invalid, removed, cross-tenant, or incomplete active order, When the resolver requires the effective snapshot, Then it fails without leaking cross-tenant existence or writing data.

## Verification

- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineActiveOrderSnapshotResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, resolver class missing.
- GREEN: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineActiveOrderSnapshotResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 5 tests run, 0 failures, 0 errors.
- Static check: resolver source scan found no insert, update, delete, FOR UPDATE, workOrder+route active-order lookup, product_id, formBindings, routeProcessId, or processId references.

## Validation

- backend-api-delivery validator self-test: PASS.
- backend-api-delivery evidence validator: PASS.

## Blockers

- none
