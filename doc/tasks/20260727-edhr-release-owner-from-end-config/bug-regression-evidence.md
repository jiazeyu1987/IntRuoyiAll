# Bug Regression Evidence

## Bug Summary

放行负责人没有从工艺路线“工序结束 > 放行责任人”解析。前端显示通用 `stageOwnerRole`，后端正式放行授权读取 `CLOSE` 关闭负责人，导致展示、电子签名授权和放行审批任务口径不一致。

## Expected Behavior

- 工作台放行摘要展示路线级 `RELEASE_APPROVE` 配置。
- 放行提交只允许 `RELEASE_APPROVE` 候选人签名。
- 只配置 `CLOSE` 时不能越权放行。
- 未配置时明确显示 `放行责任人未配置`，不回退到“执行人”。

## Reproduction

- Frontend static reproduction: `node tests\e2e\edhr-release-owner-label-static.spec.js`.
- Backend regression reproduction: target JUnit for workbench release owner fields and close-owner-only submit.

## Root Cause

- `BatchExecutionDetailPage.vue` used `stageOwnerRole` as the release-stage owner fallback.
- `MesProEdhrReleaseServiceImpl.requireReleaseOwner()` selected route `CLOSE` assignment rule instead of `TASK_TYPE_RELEASE_APPROVE`.
- `EdhrBatchWorkbenchRespVO.WorkbenchReleaseSummary` had no release-owner-specific fields, so the UI had no authoritative owner label to display.

## Regression Tests

- `MesProEdhrBatchExecutionServiceTest#workbench_resolvesReleaseOwnerLabelFromRouteReleaseUserRule`
- `MesProEdhrBatchExecutionServiceTest#workbench_resolvesReleaseOwnerLabelFromRouteReleaseRoleGroupRule`
- `MesProEdhrBatchExecutionServiceTest#workbench_marksReleaseOwnerMissingWhenRouteReleaseRuleAbsent`
- `MesProEdhrReleaseServiceImplTest#submitReleasesDirectlyWhenRouteReleaseRoleMemberSigns`
- `MesProEdhrReleaseServiceImplTest#submitRejectsWhenOnlyRouteCloseOwnerIsConfigured`
- `MesProEdhrReleaseServiceImplTest#submitRejectsWhenRouteReleaseRoleHasNoEnabledMembers`
- `tests/e2e/edhr-release-owner-label-static.spec.js`

## RED

- RED: Backend RED failed at test compilation because `WorkbenchReleaseSummary` did not expose release owner getters.
- RED: Frontend RED failed because `releaseOwnerConfigured` was absent from the workbench response type.

## GREEN

- GREEN: Backend target JUnit PASS, 9 tests.
- GREEN: Frontend static contracts PASS.
- GREEN: `pnpm ts:check` PASS.
- GREEN: route end-node release-owner static contracts PASS.

## Verification

- Verification confirms route-level `RELEASE_APPROVE` is used for workbench owner display and formal release authorization.

## Risk And Regression Scope

- Release owner source is now route-level `RELEASE_APPROVE`; `CLOSE` remains available only for batch close authorization.
- Existing `stageOwnerRole` remains for non-release stages.
- No database migration or data rewrite was introduced.

## Blockers

- Real Playwright page verification is pending safe backend runtime reload.
- Final Maven rerun for the newly added empty-role regression test is pending a free shared repository build slot; concurrent Maven processes caused timeout without a test failure.
