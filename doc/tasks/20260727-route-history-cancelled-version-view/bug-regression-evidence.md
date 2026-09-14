# Bug Regression Evidence

## Bug Summary

查看老的工艺路线版本时，如果版本状态为 `CANCELLED`，前端版本工作区虽然提供“查看”，但后端关系图和工艺流程配置读取接口返回 `PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE`，页面提示“工艺路线候选版本未满足发布条件，routeVersionId=262，status=CANCELLED”。

## Expected Behavior

- `CANCELLED`、`REJECTED` 等已关闭候选版本应可从自身 `routeSnapshotJson.configSnapshots` 只读查看历史关系图和配置。
- 写入操作仍只允许 `DRAFT` 候选版本，已取消版本不得保存、提交或发布。

## Reproduction

- Reproduction command: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteProcessFlowServiceImplTest,MesProRouteFlowConfigServiceImplTest,MesProRouteScheduleConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteProcessFlowServiceImplTest,MesProRouteFlowConfigServiceImplTest,MesProRouteScheduleConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, new regression tests failed because `CANCELLED` / `REJECTED` / `SUPERSEDED` readonly read paths were rejected or returned empty schedule rows.

## Root Cause

- Frontend allowed non-`DRAFT` versions to enter readonly viewer, including `CANCELLED`.
- Backend read validation reused too-narrow candidate status sets intended for open candidates, so closed historical candidates were rejected before snapshot parsing.
- Schedule config readonly retrieval only read candidate snapshots for `DRAFT`; closed candidates have no published schedule config rows, so readonly schedule data was empty.

## Regression Tests

- `MesProRouteProcessFlowServiceImplTest#getGraph_shouldReadClosedHistoricalRouteVersionSnapshot`
- `MesProRouteProcessFlowServiceImplTest#saveGraph_shouldRejectCancelledSnapshotWriteWithoutMutatingActiveGraph`
- `MesProRouteFlowConfigServiceImplTest#getRouteFlowProcessConfigList_shouldReadSnapshotForClosedCandidateVersion`
- `MesProRouteFlowConfigServiceImplTest#saveRouteFlowConfig_shouldRejectCancelledRouteVersion`
- `MesProRouteScheduleConfigServiceTest#getConfigRespListByRouteVersionId_shouldReadCandidateScheduleSnapshotForReadonlyStatuses`
- `MesProRouteScheduleConfigServiceTest#saveConfig_shouldRejectCancelledRouteVersionWithoutWrite`
- `tests/e2e/mes-route-cancelled-version-view-static.spec.js`

## Verification

GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteProcessFlowServiceImplTest,MesProRouteFlowConfigServiceImplTest,MesProRouteScheduleConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 96 tests.

GREEN: `node --check tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS.

GREEN: `node tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS.

## Risk And Scope

- Scope is limited to readonly route-version retrieval and regression tests.
- No fallback, mock success, or exception swallowing was added.
- Write-side guards remain fail-fast and are explicitly covered for `CANCELLED`.

## Blockers And Follow-Up

- No current implementation blocker.
- Real browser E2E was not run because the fix is backend read-contract focused and no frontend production behavior changed; static frontend contract covers the viewer handoff.
