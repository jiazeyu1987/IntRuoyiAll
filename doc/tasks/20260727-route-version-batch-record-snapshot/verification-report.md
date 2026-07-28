# Verification Report

## Scope

- 验证后续新生成或重新发布的路线版本快照会写入当前工序配置中的 `configSnapshots.batchUseConfigs[*].formBindings`。
- 验证历史 V15 不被本任务回补或直接修改。
- 验证发布投影可继续消费快照中的动态表单绑定。

## Passed Checks

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

## RED Evidence

- Temporarily restoring the old candidate snapshot copy path caused `MesProRouteVersionWorkflowServiceTest#createCandidate_shouldRefreshCurrentConfigSnapshotAsDraft` to fail.
- Failure reason: expected refreshed snapshot with `FORM_BINDING_COPY_1`, but actual candidate snapshot kept stale `batchUseConfigs: []`.

## Known Exclusions

- `MesProEdhrBatchExecutionServiceTest` was excluded from test compile because it currently has unrelated compile errors against `WorkbenchReleaseSummary`.
- `MesProRouteVersionAndCopyTest` was excluded from the broad version regression rerun because it currently fails from unrelated missing `MesProRouteOwnerPermissionService` mock setup.

## Result

- PASS for task-owned behavior: future candidate/new version snapshot generation now refreshes current process batch-record form bindings into the version snapshot.
- PASS for user policy: no historical V15 data backfill was performed.

