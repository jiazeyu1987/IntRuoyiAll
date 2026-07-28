# Bug Regression Evidence

## Bug Summary

- Fusion branch real E2E failed when creating the task-owned eDHR batch after copying, editing, publishing, and enabling route `CODX-VFC-20260727`.
- Failure: `/mes/pro/edhr-batch-execution/open-or-create failed: 批记录附件负责人配置无效：batchRecordAttachmentOwners`.
- Expected behavior: copied task-owned route ACTIVE and candidate-published snapshots preserve the source ACTIVE route-level `configSnapshots.batchRecordAttachmentOwners` array, so batch creation continues to validate the formal frozen route snapshot without downgrade or default success.

## Reproduction

- Real path reproduction: `node tests\e2e\edhr-visual-fill-config-real-flow.e2e.js` -> FAIL before fix, result file `IntRuoyiFronted\test-results\edhr-visual-fill-config-real-flow\result.json`.
- Isolated RED: `mvn "-Dtest=MesProRouteServiceImplTest#copyRoute_shouldRefreshActiveVersionWithCompleteConfigSnapshotAfterChildConfigsCopied" "-Dsurefire.failIfNoSpecifiedTests=false" test -pl yudao-module-mes -am` -> FAIL, copied route snapshot did not include `batchRecordAttachmentOwners`.

## Root Cause

- `copyRoute` created the target V1 route version and then refreshed its complete snapshot after child configs were copied.
- `refreshRouteVersionSnapshot` preserved route-level attachment owners only by reading the target route version's existing snapshot.
- The target V1 was newly created, so it had no `batchRecordAttachmentOwners`; the source ACTIVE route's formal attachment owner snapshot was never inherited.

## Regression Test

- Updated `MesProRouteServiceImplTest#copyRoute_shouldRefreshActiveVersionWithCompleteConfigSnapshotAfterChildConfigsCopied`.
- The test now seeds source ACTIVE route snapshot `configSnapshots.batchRecordAttachmentOwners` and asserts the copied target ACTIVE snapshot contains all four attachment owner entries with original candidate users.

## RED / GREEN

- RED: target Maven command failed with `batchRecordAttachmentOwners` null in copied route snapshot.
- GREEN: same target Maven command passed after `copyRoute` inherited reusable route-level config snapshots from the source ACTIVE version and retained strict JSON array validation.

## Regression Scope

- Backend focused integration regression passed: `Tests run: 321, Failures: 0, Errors: 0`.
- Frontend static contracts and `pnpm ts:check` passed.
- Real E2E passed on slot 2 (`8083` / `48083`) and cleanup restored the report config, voided the task batch, and deleted the task route.

## Verification

- `mvn "-Dtest=MesProRouteServiceImplTest,MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -pl yudao-module-mes -am` -> PASS, `Tests run: 57, Failures: 0, Errors: 0`.
- `mvn "-Dtest=MesProEdhrWorkTaskServiceImplTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrProcessFormPermissionRuleServiceImplTest,MesProRouteFlowConfigServiceImplTest,MesProEdhrRehearsalReadinessServiceTest,MesProRouteServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -pl yudao-module-mes -am` -> PASS, `Tests run: 321, Failures: 0, Errors: 0`.
- Six frontend static contracts -> PASS.
- `pnpm ts:check` -> PASS.
- `mvn -pl yudao-server -am "-DskipTests" package` -> PASS.
- `node tests\e2e\edhr-visual-fill-config-real-flow.e2e.js` -> PASS.

## Blockers

- None remaining for this regression after the route-copy snapshot fix.
- Prior E2E blocker was caused by copied route snapshot missing `batchRecordAttachmentOwners`; it is resolved without changing backend validation.

## Risk

- Low. The fix only carries existing formal route-level attachment owner snapshot from source ACTIVE route to the copied target route when copying a route.
- No fallback, mock, API-only write path, validation relaxation, or default attachment owner inference was introduced.
