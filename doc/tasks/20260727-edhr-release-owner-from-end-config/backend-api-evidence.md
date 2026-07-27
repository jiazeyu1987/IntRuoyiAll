# Backend API Evidence

## Scope

- Service scope: `MesProEdhrBatchWorkbenchServiceImpl` and `MesProEdhrReleaseServiceImpl`.
- Response contract: `EdhrBatchWorkbenchRespVO.WorkbenchReleaseSummary` now includes `releaseOwnerConfigured`, `releaseOwnerSourceType`, and `releaseOwnerLabel`.
- Authorization contract: formal release submit validates the current user against route-level `RELEASE_APPROVE` candidates.

## Contract

- `USER` source displays the enabled admin user nickname.
- `ROLE_GROUP` source displays the enabled role name plus `（角色成员均可放行）`.
- Missing route `RELEASE_APPROVE` returns `releaseOwnerConfigured=false` and `releaseOwnerLabel=放行责任人未配置` in workbench summaries.
- Submit release fails fast with `PRO_EDHR_RELEASE_OWNER_INVALID` when the actor is not in the `RELEASE_APPROVE` candidate snapshot.
- Invalid `ROLE_GROUP` configuration with no enabled members fails fast with `PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY` before password validation.
- Blank user nicknames and blank role names are invalid; numeric IDs are not used as display fallbacks.

## BDD

- BDD: Route release user owner -> Given a route-level `RELEASE_APPROVE` USER rule When workbench is queried Then the release summary labels that user's nickname.
- BDD: Route release role owner -> Given a route-level `RELEASE_APPROVE` ROLE_GROUP rule When workbench is queried Then the release summary labels the role and notes role members can release.
- BDD: Role member release submit -> Given an actor belongs to the `RELEASE_APPROVE` role candidate pool When submit signs release Then release succeeds.
- BDD: Close owner blocked -> Given only `CLOSE` is configured When the close owner submits release Then release fails before password validation.
- BDD: Empty role candidate pool blocked -> Given `RELEASE_APPROVE` points to a role with no enabled members When any user submits release Then candidate resolution fails before password validation.

## Validation

- Validation covers response fields, source labels, candidate snapshots, authorization failure, and no-migration constraints.

## RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#workbench_resolvesReleaseOwnerLabelFromRouteReleaseUserRule+workbench_resolvesReleaseOwnerLabelFromRouteReleaseRoleGroupRule+workbench_marksReleaseOwnerMissingWhenRouteReleaseRuleAbsent,MesProEdhrReleaseServiceImplTest#submitRejectsWhenOnlyRouteCloseOwnerIsConfigured" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected because `WorkbenchReleaseSummary` lacked the new release owner getters.

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#workbench_resolvesReleaseOwnerLabelFromRouteReleaseUserRule+workbench_resolvesReleaseOwnerLabelFromRouteReleaseRoleGroupRule+workbench_marksReleaseOwnerMissingWhenRouteReleaseRuleAbsent,MesProEdhrReleaseServiceImplTest#submitReleasesDirectlyWhenOwnerSignsAndDhrPassesAndExternalSourcesAreNotYetIntegrated+submitReleasesDirectlyBeforeBatchCloseWhenOwnerSignsAndDhrEvidenceIsComplete+submitReleasesDirectlyWhenRouteReleaseRoleMemberSigns+submitRejectsWhenCurrentUserIsNotRouteReleaseOwner+submitRejectsWhenOwnerSignaturePasswordIsMissingOrInvalid+submitRejectsWhenOnlyRouteCloseOwnerIsConfigured" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests.
- The Surefire reports at `2026-07-27 17:49` record 3 workbench tests and 6 release tests with 0 errors and 0 failures.
- Isolated `javac` compilation of the final `MesProEdhrBatchWorkbenchServiceImpl` source and `MesProEdhrReleaseServiceImplTest` source -> PASS.

## Verification

- Verification confirms workbench owner labels and formal release authorization both use route-level `RELEASE_APPROVE`.

## Config And Migrations

- No database migration.
- No route configuration data repair.
- No fallback, compatibility shim, mock success, or swallowed exception added.

## Blockers

- Real browser verification is blocked until the shared local backend process on `48081` is safely rebuilt/restarted with this task's backend changes.
- Final Maven rerun including `submitRejectsWhenRouteReleaseRoleHasNoEnabledMembers` timed out while unrelated Maven builds were concurrently active in the same repository; no failing test result was produced.
