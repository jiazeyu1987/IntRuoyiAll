# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: clicking confirm in the eDHR open/create dialog can fail with `批记录附件负责人配置无效：batchRecordAttachmentOwners`.
- Expected behavior: when a route version already has a valid `configSnapshots.batchRecordAttachmentOwners` snapshot, route snapshot refresh/build paths must preserve it so eDHR batch execution can resolve special-node attachment fillers from the frozen snapshot.

## Reproduction

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: FAIL, `batchRecordAttachmentOwners` was missing from the regenerated snapshot and the regression assertion hit a null owners array.

## Root Cause

- `MesProRouteServiceImpl#buildCompleteRouteConfigSnapshots` rebuilt current route snapshots from route/process configuration but did not carry forward the existing `configSnapshots.batchRecordAttachmentOwners` array from the route version snapshot.
- Runtime special-node filler resolution is intentionally strict and reads owners from the frozen route snapshot; dropping that array turns a valid saved configuration into `PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID`.

## Regression Test

- Added `MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners`.
- The test verifies that a route version with four saved attachment-owner entries still has those entries after current snapshot generation.

## GREEN Verification

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners" "-Dsurefire.failIfNoSpecifiedTests=false" test` with bounded `MAVEN_OPTS` -> PASS, 1 test.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldSerializeCurrentBatchRecordBindingsFromProcessSettings+buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners,MesProRouteBatchRecordAttachmentOwnerServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests.
- COMPILE: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS.

## Risk And Scope

- Scope is backend route snapshot generation only.
- No fallback, inferred users, default empty owners, or swallowed validation was introduced.
- Existing invalid/missing route data still fails fast; route `922119` was confirmed missing owner snapshots and requires authorized business configuration.

## Blockers And Follow-Up

- BLOCKER: route `922119` ACTIVE `V14` and DRAFT `V15` do not contain `configSnapshots.batchRecordAttachmentOwners`; no data write was performed without authorization.
