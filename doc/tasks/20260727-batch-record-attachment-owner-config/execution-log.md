# Execution Log

## User Intent

- Bug: clicking confirm in the "open or create eDHR batch execution" dialog fails with `批记录附件负责人配置无效：batchRecordAttachmentOwners`.
- Authorization: user replied `授权` after being asked to allow configuring route `922119` under local `芋道源码/admin` and handling the existing batch `900000000876` frozen snapshot / rebuild issue. Scope is limited to local `int_main`, route `922119`, draft version `V15`, active/version snapshot needed for future opens, and existing batch `900000000876`.

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
- E2E: `node doc\tasks\20260727-batch-record-attachment-owner-config\runtime-artifacts\verify-edhr-page-after-restart.cjs` -> PASS, local `int_main` frontend `http://localhost:8081`, tenant/user label `芋道源码/admin`, page title visible, no visible global `批记录附件负责人配置无效` alert/toast. Artifact: `runtime-artifacts/edhr-batch-execution-after-restart.json`, screenshot: `runtime-artifacts/edhr-batch-execution-after-restart.png`.
- E2E: `node doc\tasks\20260727-batch-record-attachment-owner-config\runtime-artifacts\verify-open-create-confirm.cjs` -> BLOCKED as expected on real data, clicked `打开/创建`, selected work order `881MO090889`, route `922119`, batch `34126020001`, and clicked `确认`; `/admin-api/mes/pro/edhr-batch-execution/open-or-create` returned HTTP 200 with business code `1040271050` and message `批记录附件负责人配置无效：batchRecordAttachmentOwners`. Artifact: `runtime-artifacts/edhr-open-create-confirm-existing.json`, screenshot: `runtime-artifacts/edhr-open-create-confirm-existing.png`.
- INFO: runtime ownership -> frontend port `8081` is `E:\IntRuoyi\IntRuoyiFronted` Vite, backend port `48081` is `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`; verification was on `int_main`, not an additional worktree.

## Blockers

- Real route `922119` and existing batch `900000000876` must receive a valid frozen `batchRecordAttachmentOwners` configuration through an authorized UI configuration/publish flow or a formally approved data repair. No route/batch data write was performed because the data is not task-owned test data.
