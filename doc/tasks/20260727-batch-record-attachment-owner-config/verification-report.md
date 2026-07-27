# Verification Report

## Summary

- Backend fix: route snapshot rebuild now preserves existing `configSnapshots.batchRecordAttachmentOwners`.
- Regression added: `MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners`.
- Current data finding: route `922119` still has no owner snapshot in ACTIVE `V14` or DRAFT `V15`, so that exact route remains blocked until configured/published.

## Commands

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, owners array null.
- GREEN: same target test with `MAVEN_OPTS='-Xmx1024m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=128m'` -> PASS, 1 test.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldSerializeCurrentBatchRecordBindingsFromProcessSettings+buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners,MesProRouteBatchRecordAttachmentOwnerServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests.
- COMPILE: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS.

## Data Check

- Backend health: `http://127.0.0.1:48081/actuator/health` -> `UP`.
- Read-only DB query: route `922119` versions `V14 ACTIVE` and `V15 DRAFT` both returned `has_attachment_owners=0`, `owner_count=NULL`.

## Status

- Code verification: PASS.
- Current screenshot data: BLOCKED pending authorized route attachment-owner configuration and publish/refresh.
