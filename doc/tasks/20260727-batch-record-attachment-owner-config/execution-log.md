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
- AUTHORIZED DATA REPAIR: user replied `授权`; scope limited to local `int_main`, tenant `1 / 芋道源码/admin`, route `922119`, active route version `V15/id=361`, and existing batch `900000000876`.
- GREEN: `node doc\tasks\20260727-batch-record-attachment-owner-config\runtime-artifacts\configure-route-attachment-owners.cjs` -> PASS after script auth/idempotency fix; `V15/id=361` is `ACTIVE`, has 4 `batchRecordAttachmentOwners`, and no publish action was needed on rerun. Artifact: `runtime-artifacts/configure-route-attachment-owners.json`.
- DB: schema check -> `mes_pro_edhr_batch_execution.route_snapshot_json` is JSON, `mes_pro_route_version.route_snapshot_json` exists; target batch before repair had `owner_count=NULL`, `batchUseConfigCount=14`, `flowNodeCount=14`; ACTIVE `V15/id=361` had owner count `4`.
- DB REPAIR: backed up original `900000000876` frozen snapshot to `runtime-artifacts/batch-900000000876-route-snapshot-before-repair.json`; restored the original snapshot after an initial script row-count ordering mistake, then reran the protected update copying `$.configSnapshots.batchRecordAttachmentOwners` directly from ACTIVE `V15/id=361`. Evidence: `restoreRows=1`, `repairRows=1`, `afterOwnerType=ARRAY`, `afterOwnerCount=4`, artifact `runtime-artifacts/batch-900000000876-route-snapshot-repair.json`.
- GREEN: final DB check -> batch `900000000876` `route_snapshot_json.configSnapshots.batchRecordAttachmentOwners` type `ARRAY`, count `4`; active `V15/id=361` owner count `4`.
- GREEN: `node doc\tasks\20260727-batch-record-attachment-owner-config\runtime-artifacts\verify-open-create-confirm.cjs` -> PASS, clicked `打开/创建`, selected work order `881MO090889`, route `922119`, batch `34126020001`, clicked `确认`, `/open-or-create` returned code `0`, and page reached `/mes/pro/feedback/edhr-batch-execution/detail?id=900000000876` with no visible `批记录附件负责人配置无效`. Artifact refreshed: `runtime-artifacts/edhr-open-create-confirm-existing.json`, screenshot refreshed: `runtime-artifacts/edhr-open-create-confirm-existing.png`.
- GREEN: `node doc\tasks\20260727-batch-record-attachment-owner-config\runtime-artifacts\verify-edhr-page-after-restart.cjs` -> PASS after repair; list page still loads, first rows include V15 batch `900000000878` and repaired V14 batch `900000000876`, `blockedCount=0`, no global owner error, no console/page errors.
- GREEN: project-experience-consolidation -> PASS, merged the reusable JSON-type/frozen-snapshot repair lesson into `docs/backend-development.md#edhr-批次任务配置来源门禁` and updated `docs/experience-index.md`; no new long-term experience document was created.

## Blockers

- No product/E2E blocker remains for the reported confirm-button path.
- Closeout blocker only: the shared `int_main` working tree contains unrelated dirty files; no commit/push was performed in this step.
