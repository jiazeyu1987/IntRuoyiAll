# Execution Log

## User Intent

- 后续新生成或重新发布的路线版本必须把当前工序批记录配置写入 `configSnapshots.batchUseConfigs[*].formBindings`。
- 历史 V15 不强制回补。
- 新版本生成后通过快照核对确认工序不再显示“未配置”。

## BDD / TDD

- BDD: 后续路线版本快照保留批记录表单绑定 -> Given 当前工序配置存在有效批记录表单绑定，When 新路线版本生成或重新发布并构建完整路线快照，Then 对应 `batchUseConfigs[*]` 必须包含该工序的 `formBindings`，且绑定字段保持可读。
- BDD: 历史 V15 不回补 -> Given 历史 V15 快照可能缺少绑定，When 执行本任务修复，Then 不直接修改历史 V15 数据，只保证后续版本生成链路。
- BDD: 发布投影保留批记录表单绑定 -> Given 候选版本快照包含 `formBindings`，When 候选版本发布投影到当前路线，Then 批记录绑定仍归属对应工序配置，不被投影流程丢弃。

## Commands And Evidence

### Preflight

- Read `docs/backend-development.md`, `docs/task-closeout-rules.md`, `docs/powershell-memory.md`, and bug-regression skill instructions.
- Current branch: `int_main`.
- Existing shared worktree has unrelated dirty files; current task must not modify or stage them.

### Implementation

- Updated `MesProRouteVersionWorkflowServiceImpl#createCandidate` so future candidate versions call `MesProRouteService#buildCurrentRouteSnapshotJson(routeId, activeVersionId)` and persist a fresh snapshot built from current process configuration.
- Updated `MesProRouteServiceImpl#createDraftCandidateRouteVersion` so route-edit generated draft candidates use `buildCompleteRouteConfigSnapshots(routeId, activeVersionId)` instead of copying `extractConfigSnapshots(activeVersion)`.
- Updated `MesProRouteVersionWorkflowServiceTest#createCandidate_shouldRefreshCurrentConfigSnapshotAsDraft` to prove stale active snapshots are not copied forward when current process settings contain `formBindings`.
- Updated `MesProRouteVersionPlatformAdapterTest#createCandidate_shouldRegisterPlatformDraftRefAfterNativeCandidateInserted` to match the refreshed-snapshot contract.
- Historical V15 snapshots were not modified or backfilled.

### RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersionWorkflowServiceTest#createCandidate_shouldRefreshCurrentConfigSnapshotAsDraft" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test` -> FAIL, expected reason: with the old `active.getRouteSnapshotJson()` copy path, the candidate snapshot kept stale `batchUseConfigs: []` instead of refreshed `formBindings`.
- Initial target command without the test compile exclusion was blocked before executing the target assertion by unrelated compile errors in `MesProEdhrBatchExecutionServiceTest`; the focused RED above used only the unrelated compile exclusion and no production fallback.

### GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersionWorkflowServiceTest#createCandidate_shouldRefreshCurrentConfigSnapshotAsDraft" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersionWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test` -> PASS, Tests run: 16, Failures: 0, Errors: 0.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldSerializeCurrentBatchRecordBindingsFromProcessSettings" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test` -> PASS, Tests run: 5, Failures: 0, Errors: 0.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPlatformAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test` -> PASS, Tests run: 3, Failures: 0, Errors: 0.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteVersion*Test,MesProRouteVersion*ImplTest,!MesProRouteVersionAndCopyTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java" test` -> PASS, Tests run: 79, Failures: 0, Errors: 0.
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS.

### Regression Note

- Full route-version pattern including `MesProRouteVersionAndCopyTest` still fails because `MesProRouteVersionAndCopyTest` has no mock injected for existing `MesProRouteOwnerPermissionService`, causing unrelated `NullPointerException` in create/copy route paths.
- The same broad run also revealed the now-fixed `MesProRouteVersionPlatformAdapterTest` expectation drift; after syncing its mock to the refreshed snapshot contract, the platform adapter test passed.

## Root Cause Notes

- 当前工序设置页面读取实时工序配置。
- 路线版本流程图读取版本快照中的 `batchUseConfigs`。
- 候选版本生成链路曾直接复制 active 版本的 `routeSnapshotJson`；当 active 快照历史上缺失或陈旧时，后续候选版本继续继承空 `batchUseConfigs`，因此流程图显示“未配置”。
- 路线编辑触发草稿候选版本时也曾复用 `extractConfigSnapshots(activeVersion)`，存在同类 stale snapshot 继承风险。
- 正式修复点是版本快照生成/保存链路，不是表单配置本身，也不是前端展示层。

## Verification

- 已完成 RED/GREEN、相邻发布投影回归、平台适配回归和 MES 模块编译验证。
- 证据详见 `verification-report.md` 与 `bug-regression-evidence.md`。

## Blockers

- 本任务实现和验证已完成。
- 最终提交/推送受共享工作区既有大量无关 dirty 文件阻塞；未擅自提交或推送本任务外改动。
