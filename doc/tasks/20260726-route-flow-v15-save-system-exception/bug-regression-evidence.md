# Bug Regression Evidence

## Bug Summary

保存“球囊扩张压力泵”草稿版本 V15 的流转关系图时，页面会连续显示多条“系统异常”。另外，草稿普通保存成功后会继续弹“草稿已保存，是否立即提交发布？”，用户确认后会把 DRAFT 草稿推进审批/发布并导致该草稿不可继续编辑。

2026-07-26 用户复报：点击保存时仍提示单条“系统异常”。继续排查后确认，前端重复提示治理和草稿保存/发布解耦之外，还存在后端 DRAFT 草稿 BATCH 表单绑定快照保存后读回不对称的问题。

## Expected Behavior

- 若保存成功，页面只显示保存成功，不出现系统异常。
- 若任一内部保存接口失败，页面只显示一次来自外层保存入口的可读错误。
- 若普通保存草稿成功，只保存当前 DRAFT 草稿，不弹立即提交发布确认，不调用 submit-publish；用户仍可继续修改同一草稿版本。
- 若保存链路存在业务校验失败，必须返回可解释错误，不应只显示通用“系统异常”。
- 不通过吞异常、默认成功或隐藏后端错误来减少提示数量。

## Reproduction

- 页面路径：`/mes/pro/route/edit/922119?tab=flow&routeVersionId=361&routeVersionNo=V15&routeVersionStatus=DRAFT`。
- 数据对象：`RT000028 / 球囊扩张压力泵 / routeId=922119 / draft routeVersionId=361 / V15`。
- 真实页面普通保存当前不再复现后端 500；为避免继续推进 V15 `graphVersion`，使用 Playwright 拦截 `/admin-api/mes/pro/route-process-flow/validate` 返回业务 500，验证失败提示数量。
- 草稿保存解耦复现：静态合同先断言 `RouteEditPage.handleSaved` 不得调用保存后发布确认；修复前失败，因为普通保存成功后仍会隐式进入提交发布确认。
- 复报后端复现：直接调用 BATCH 表单槽位保存接口返回 code 0，但随后读取 `/mes/pro/route/flow-config?routeId=922119&useType=BATCH&routeVersionId=361` 时目标工序仍返回空 `formBindings`，说明显式保存的 DRAFT 草稿快照未被读取。

## Root Cause

保存按钮走聚合链路：`RouteFormContent.handleSubmitRequest` -> `submitForm` -> `RouteFlowGraphDesigner.validateBeforeSubmit` -> `RouteFlowGraphDesigner.saveFromParent`。当内部接口返回业务 500 时，原实现会在三层重复提示：

- axios response interceptor 自动 `ElMessage.error`。
- `RouteFlowGraphDesigner` catch 后 `message.error(...)` 并重新抛出。
- `RouteFormContent.handleSubmitRequest` 最外层 catch 再次 `message.error(...)`。

草稿不可继续编辑的根因是保存和发布被耦合在同一成功回调中：`RouteFormContent.submitForm` 通过 `promptRouteVersionSubmit` 成功载荷驱动 `RouteEditPage.handleSaved`，随后 `confirmSubmitRouteCandidateVersionAfterSave` 弹出立即提交发布确认；这会把普通保存变成潜在的发布动作。

复报单条“系统异常”的后端根因是 DRAFT 草稿 BATCH 表单槽位快照读写不对称：保存链路会把草稿配置写入 `routeSnapshotJson.configSnapshots.batchUseConfigs`，但 `MesProRouteFlowConfigServiceImpl#getRouteVersionSnapshotFlowProcessConfigList` 对可读候选版本使用 `CURRENT_PROCESS_SETTINGS` 读取当前工序设置，导致已显式保存的 DRAFT 草稿 `formBindings` 被当前配置覆盖或读回为空。修复通过 `batchRecordBindingSnapshotExplicit` 标记区分“显式保存过的 DRAFT 草稿快照”和 legacy 候选快照，只让前者优先于当前工序设置。

## Regression Test

- Added: `IntRuoyiFronted/tests/e2e/mes-route-flow-save-error-single-toast-static.spec.js`。
- The test locks API wrappers to support `ignoreErrorMessage` options and asserts `RouteFlowGraphDesigner` does not toast before rethrow.
- Added: `IntRuoyiFronted/tests/e2e/mes-route-draft-save-stays-editable-static.spec.js`。
- Updated: `IntRuoyiFronted/tests/e2e/mes-route-candidate-save-publish-prompt-static.spec.js` to lock the new behavior that save keeps the draft editable and explicit submit remains separate.
- Added: `MesProRouteFlowConfigServiceImplTest#getRouteFlowProcessConfigList_shouldReadSavedDraftBatchSnapshotBeforeCurrentBindings`。
- The backend test locks the rule that explicitly saved DRAFT BATCH snapshots are read before current process bindings, while adjacent lifecycle statuses keep their existing current-setting behavior.

## RED

- RED: `node tests/e2e/mes-route-flow-save-error-single-toast-static.spec.js` -> FAIL。
- Expected reason: `validateRouteProcessFlowGraph` did not support `ignoreErrorMessage` request options, and child save methods still contained duplicate toast paths.
- RED: `node tests/e2e/mes-route-draft-save-stays-editable-static.spec.js` -> FAIL。
- Expected reason: `RouteEditPage.handleSaved` still invoked the save-after-submit confirmation path.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest#getRouteFlowProcessConfigList_shouldReadSavedDraftBatchSnapshotBeforeCurrentBindings" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL。
- Expected reason: DRAFT BATCH 草稿已保存快照被当前工序设置覆盖，期望 `FB-DRAFT-SAVED`，实际 `FB-LIVE`。

## GREEN

- GREEN: `node tests/e2e/mes-route-flow-save-error-single-toast-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-draft-save-stays-editable-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-candidate-save-publish-prompt-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest#getRouteFlowProcessConfigList_shouldReadSavedDraftBatchSnapshotBeforeCurrentBindings" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 34 tests, 0 failures。
- Playwright interception: simulated `/route-process-flow/validate` business 500 on the V15 page and asserted exactly one visible save error toast.
- Playwright interception: simulated successful `/route-process-flow/validate` and `/route-process-flow/save` on the V15 page and asserted submit-publish request count stayed 0 and the page remained in draft V15 context.

## Verification

- `node tests/e2e/route-batch-record-save-contract-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-edit-unsaved-candidate-discard-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-flow-graph-only-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-draft-save-stays-editable-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-flow-save-error-single-toast-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-candidate-save-publish-prompt-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。
- `git diff --check -- <task files>` -> PASS, with only CRLF warnings.

## Risk

- The save-error change affects frontend error presentation only; it does not change backend validation, persistence, permissions, or request payload semantics.
- The draft-save change removes an implicit frontend publish prompt from the normal save path; explicit submit/publish logic remains available through its dedicated route-version action.
- Existing API callers keep default behavior unless they explicitly pass request options.
- The backend change is deliberately limited to DRAFT route versions with an explicit BATCH binding snapshot marker; PENDING_APPROVAL / READY_TO_PUBLISH continue to read current process settings per existing tests.

## Blockers

- Final commit/push is blocked by the current shared workspace state: `int_main...origin/int_main [ahead 20]` and unrelated dirty files exist outside this task.
- Runtime load is not complete: 48081 was not restarted in this turn, so the running page cannot be claimed to have loaded the backend fix until the owning runtime is rebuilt/restarted under `docs/local-runtime.md`.
