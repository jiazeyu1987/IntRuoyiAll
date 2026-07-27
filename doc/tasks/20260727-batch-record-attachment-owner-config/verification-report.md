# Verification Report

## Summary

- Backend fix: route snapshot rebuild now preserves existing `configSnapshots.batchRecordAttachmentOwners`.
- Regression added: `MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners`.
- Real E2E list-page result: the batch execution list loads on local `int_main` without the global red `批记录附件负责人配置无效` alert/toast.
- Authorized route data repair: route version `V15/id=361` is ACTIVE and has 4 valid `batchRecordAttachmentOwners`.
- Authorized batch data repair: existing batch `900000000876` frozen route snapshot was repaired from ACTIVE `V15`; final DB check shows `batchRecordAttachmentOwners` JSON type `ARRAY` and count `4`.
- Real E2E confirm-button result: selecting work order `881MO090889`, route `922119`, batch `34126020001`, then clicking `确认` now returns code `0` and opens `/mes/pro/feedback/edhr-batch-execution/detail?id=900000000876` with no visible owner-config error.

## Commands

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, owners array null.
- GREEN: same target test with `MAVEN_OPTS='-Xmx1024m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=128m'` -> PASS, 1 test.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldSerializeCurrentBatchRecordBindingsFromProcessSettings+buildCurrentRouteSnapshotJson_shouldPreserveExistingBatchRecordAttachmentOwners,MesProRouteBatchRecordAttachmentOwnerServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests.
- COMPILE: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS.
- E2E LIST: `node doc\tasks\20260727-batch-record-attachment-owner-config\runtime-artifacts\verify-edhr-page-after-restart.cjs` -> PASS; artifacts: `runtime-artifacts/edhr-batch-execution-after-restart.json`, `runtime-artifacts/edhr-batch-execution-after-restart.png`.
- CONFIG CHECK: `node doc\tasks\20260727-batch-record-attachment-owner-config\runtime-artifacts\configure-route-attachment-owners.cjs` -> PASS; `V15/id=361` is ACTIVE and has 4 owner configs; artifact: `runtime-artifacts/configure-route-attachment-owners.json`.
- DB REPAIR: protected local SQL repair for batch `900000000876` -> PASS; `restoreRows=1`, `repairRows=1`, final owner JSON type `ARRAY`, count `4`; artifacts: `runtime-artifacts/batch-900000000876-route-snapshot-before-repair.json`, `runtime-artifacts/batch-900000000876-route-snapshot-repair.json`.
- E2E CONFIRM: `node doc\tasks\20260727-batch-record-attachment-owner-config\runtime-artifacts\verify-open-create-confirm.cjs` -> PASS; `/open-or-create` returned business code `0`, reached detail page; artifacts: `runtime-artifacts/edhr-open-create-confirm-existing.json`, `runtime-artifacts/edhr-open-create-confirm-existing.png`.

## Data Check

- Backend health: `http://127.0.0.1:48081/actuator/health` -> `UP`.
- Read-only DB query after repair: batch `900000000876` returned owner JSON type `ARRAY`, owner count `4`; active route version `V15/id=361` returned owner count `4`.
- Runtime ownership: frontend `8081` belongs to `E:\IntRuoyi\IntRuoyiFronted` Vite; backend `48081` belongs to `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`. This verification ran on `int_main`, not an additional worktree.

## Status

- Code verification: PASS.
- List-page E2E: PASS.
- Confirm-button E2E on current screenshot data: PASS.
- Task status: ready_for_closeout; cleanup/commit/push not performed because unrelated dirty files are present in the shared `int_main` working tree.
