# Verification Report

## Summary

- Backend fix: route snapshot rebuild now preserves existing `configSnapshots.batchRecordAttachmentOwners`.
- Regression added: `MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners`.
- Real E2E list-page result: the batch execution list loads on local `int_main` without the global red `批记录附件负责人配置无效` alert/toast.
- Real E2E confirm-button result: selecting work order `881MO090889`, route `922119`, batch `34126020001`, then clicking `确认` still returns business code `1040271050` because the existing batch frozen route snapshot lacks `batchRecordAttachmentOwners`.
- Current data finding: route `922119` still has no owner snapshot in ACTIVE `V14` or DRAFT `V15`, and existing batch `900000000876` uses ACTIVE `V14`, so that exact business data remains blocked until authorized configuration/publish/data repair.

## Commands

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, owners array null.
- GREEN: same target test with `MAVEN_OPTS='-Xmx1024m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=128m'` -> PASS, 1 test.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldSerializeCurrentBatchRecordBindingsFromProcessSettings+buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners,MesProRouteBatchRecordAttachmentOwnerServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests.
- COMPILE: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS.
- E2E LIST: `node doc\tasks\20260727-batch-record-attachment-owner-config\runtime-artifacts\verify-edhr-page-after-restart.cjs` -> PASS; artifacts: `runtime-artifacts/edhr-batch-execution-after-restart.json`, `runtime-artifacts/edhr-batch-execution-after-restart.png`.
- E2E CONFIRM: `node doc\tasks\20260727-batch-record-attachment-owner-config\runtime-artifacts\verify-open-create-confirm.cjs` -> BLOCKED on real data; `/open-or-create` returned business code `1040271050`, message `批记录附件负责人配置无效：batchRecordAttachmentOwners`; artifacts: `runtime-artifacts/edhr-open-create-confirm-existing.json`, `runtime-artifacts/edhr-open-create-confirm-existing.png`.

## Data Check

- Backend health: `http://127.0.0.1:48081/actuator/health` -> `UP`.
- Read-only DB query: route `922119` versions `V14 ACTIVE` and `V15 DRAFT` both returned `has_attachment_owners=0`, `owner_count=NULL`.
- Runtime ownership: frontend `8081` belongs to `E:\IntRuoyi\IntRuoyiFronted` Vite; backend `48081` belongs to `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`. This verification ran on `int_main`, not an additional worktree.

## Status

- Code verification: PASS.
- List-page E2E: PASS.
- Confirm-button E2E on current screenshot data: BLOCKED pending authorized route attachment-owner configuration and publish/refresh, plus repair/recreation of the existing batch frozen snapshot as appropriate.
