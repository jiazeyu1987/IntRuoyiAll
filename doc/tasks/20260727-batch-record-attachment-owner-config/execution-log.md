# Execution Log

## User Intent

- Bug: clicking confirm in the "open or create eDHR batch execution" dialog fails with `批记录附件负责人配置无效：batchRecordAttachmentOwners`.

## BDD / TDD

- BDD: valid batch record attachment owners should not block batch execution confirm -> Given a route BATCH configuration with a valid attachment owner list, When the user confirms opening or creating an eDHR batch execution, Then the backend accepts the owner configuration and creates or opens the execution instead of rejecting the field key.

## Commands And Evidence

- GREEN: experience-preflight -> PASS, read task, backend, PowerShell, bug-regression, backend-api, and experience-index rules before implementation.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: generated current route snapshot dropped `configSnapshots.batchRecordAttachmentOwners`; test errored because owners array was null.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners" "-Dsurefire.failIfNoSpecifiedTests=false" test` with `MAVEN_OPTS='-Xmx1024m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=128m'` -> PASS, Tests run: 1, Failures: 0, Errors: 0.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldSerializeCurrentBatchRecordBindingsFromProcessSettings+buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners,MesProRouteBatchRecordAttachmentOwnerServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` with reduced Maven JVM memory -> PASS, Tests run: 5, Failures: 0, Errors: 0.
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` with reduced Maven JVM memory -> PASS.
- INFO: first post-fix GREEN attempt without reduced Maven JVM memory failed at MES compile with JVM native memory allocation error (`G1 virtual space`); rerun with bounded `MAVEN_OPTS` passed.
- INFO: read-only runtime check -> backend `http://127.0.0.1:48081/actuator/health` returned `UP`.
- BLOCKER: read-only DB check for route `922119` -> ACTIVE `V14` and DRAFT `V15` both have `has_attachment_owners=0`, `owner_count=NULL`; current route data still lacks the required batch record attachment owner configuration.

## Blockers

- Real route `922119` must be configured and published, or otherwise formally migrated, before the current screenshot path can succeed on that exact data. No data write was performed because the route is not task-owned test data.
