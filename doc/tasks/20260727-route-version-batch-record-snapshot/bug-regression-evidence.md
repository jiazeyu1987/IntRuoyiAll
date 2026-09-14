# Bug Regression Evidence

## Bug Summary

- 历史 V15 的批记录表单配置在工序设置中存在，但版本快照里的 `configSnapshots.batchUseConfigs[*].formBindings` 缺失，导致按版本快照展示的工序批记录表单显示“未配置”。
- 用户确认口径：历史 V15 不强制回补；后续新生成或重新发布的版本必须把当前工序配置写入版本快照。

## Expected Behavior

- Given 当前工序配置存在批记录表单绑定。
- When 后续候选版本、新生成版本或重新发布链路构建版本快照。
- Then `configSnapshots.batchUseConfigs[*].formBindings` 必须包含对应工序的表单绑定，后续按快照展示时不再显示“未配置”。

## Reproduction

- Regression test: `MesProRouteVersionWorkflowServiceTest#createCandidate_shouldRefreshCurrentConfigSnapshotAsDraft`
- Test setup creates a stale active route snapshot with empty `batchUseConfigs` and a current refreshed snapshot containing `FORM_BINDING_COPY_1`.

## Root Cause

- `MesProRouteVersionWorkflowServiceImpl#createCandidate` used `active.getRouteSnapshotJson()` directly, so stale or incomplete active snapshots were copied into future candidate versions.
- `MesProRouteServiceImpl#createDraftCandidateRouteVersion` used `extractConfigSnapshots(activeVersion)`, so route-edit generated drafts could also inherit stale active snapshot content.
- The existing form configuration itself was not the root cause; the broken boundary was version snapshot generation and persistence.

## Regression Test

- Updated `MesProRouteVersionWorkflowServiceTest#createCandidate_shouldRefreshCurrentConfigSnapshotAsDraft` to require `routeService.buildCurrentRouteSnapshotJson(routeId, activeVersionId)` and assert that `FORM_BINDING_COPY_1` appears in the candidate snapshot.
- Updated `MesProRouteVersionPlatformAdapterTest#createCandidate_shouldRegisterPlatformDraftRefAfterNativeCandidateInserted` to reflect the refreshed snapshot dependency.

## RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersionWorkflowServiceTest#createCandidate_shouldRefreshCurrentConfigSnapshotAsDraft" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test` -> FAIL, stale active snapshot copied forward with empty `batchUseConfigs`.
- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersionWorkflowServiceTest#createCandidate_shouldRefreshCurrentConfigSnapshotAsDraft" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test`
- Result: FAIL with old copy behavior.
- Expected reason: candidate snapshot should equal refreshed snapshot containing `FORM_BINDING_COPY_1`; actual snapshot copied stale active JSON with empty `batchUseConfigs`.

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersionWorkflowServiceTest#createCandidate_shouldRefreshCurrentConfigSnapshotAsDraft" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test` -> PASS, refreshed candidate snapshot contains `FORM_BINDING_COPY_1`.
- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersionWorkflowServiceTest#createCandidate_shouldRefreshCurrentConfigSnapshotAsDraft" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test`
- Result: PASS; Tests run: 1, Failures: 0, Errors: 0.
- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersionWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test`
- Result: PASS; Tests run: 16, Failures: 0, Errors: 0.
- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldSerializeCurrentBatchRecordBindingsFromProcessSettings" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test`
- Result: PASS; Tests run: 1, Failures: 0, Errors: 0.
- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test`
- Result: PASS; Tests run: 5, Failures: 0, Errors: 0.
- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPlatformAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test`
- Result: PASS; Tests run: 3, Failures: 0, Errors: 0.
- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersion*Test,MesProRouteVersion*ImplTest,!MesProRouteVersionAndCopyTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test`
- Result: PASS; Tests run: 79, Failures: 0, Errors: 0.
- `mvn -pl yudao-module-mes -am "-DskipTests" compile`
- Result: PASS.

## Risk And Regression Scope

- Scope is limited to future snapshot generation and candidate version creation.
- No fallback, mock-success path, or historical V15 data rewrite was introduced.
- Existing incomplete historical snapshots still display according to their persisted snapshot state unless a new version is generated or republished.

## Verification

- Target regression, workflow class, current snapshot builder, publish projection, platform adapter, version-related regression subset, and MES compile checks passed.
- The only excluded failures are recorded as unrelated blockers.

## Blockers And Follow-Up

- Full broad route-version run including `MesProRouteVersionAndCopyTest` is blocked by unrelated missing `MesProRouteOwnerPermissionService` mock setup.
- Full target test compile without excluding `MesProEdhrBatchExecutionServiceTest` is blocked by unrelated compile errors against `WorkbenchReleaseSummary`.
- Final commit/push is blocked by shared workspace dirty state containing many task-unowned changes.
