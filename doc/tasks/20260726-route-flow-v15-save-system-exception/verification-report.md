# Verification Report

## Summary

已修复路线流转关系图保存失败时的重复“系统异常”提示链路。内部保存 API 关闭 axios 自动错误提示，`RouteFlowGraphDesigner` 不再在 rethrow 前重复 toast，由 `RouteFormContent.handleSubmitRequest` 统一展示一次可读错误。同时已移除草稿普通保存后的隐式“立即提交发布”确认，保存后仍停留在 DRAFT 草稿编辑上下文。针对用户复报的单条“系统异常”，已修复后端 DRAFT BATCH 表单绑定快照保存后读回被当前工序设置覆盖的问题。

## Changed Files

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/vo/flowconfig/MesProRouteFlowProcessConfigSaveReqVO.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteFlowConfigServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteFlowConfigServiceImplTest.java`
- `IntRuoyiFronted/src/api/mes/pro/route/index.ts`
- `IntRuoyiFronted/src/api/mes/pro/route/flowconfig.ts`
- `IntRuoyiFronted/src/views/mes/pro/route/RouteEditPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/route/RouteFormContent.vue`
- `IntRuoyiFronted/src/views/mes/pro/route/RouteFlowGraphDesigner.vue`
- `IntRuoyiFronted/tests/e2e/mes-route-draft-save-stays-editable-static.spec.js`
- `IntRuoyiFronted/tests/e2e/mes-route-candidate-save-publish-prompt-static.spec.js`
- `IntRuoyiFronted/tests/e2e/mes-route-edit-unsaved-candidate-discard-static.spec.js`
- `IntRuoyiFronted/tests/e2e/mes-route-flow-graph-only-static.spec.js`
- `IntRuoyiFronted/tests/e2e/mes-route-flow-save-error-single-toast-static.spec.js`
- `docs/backend-development.md`
- `docs/frontend-development.md`
- `docs/experience-index.md`

## Verification Commands

- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest#getRouteFlowProcessConfigList_shouldReadSavedDraftBatchSnapshotBeforeCurrentBindings" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> RED before fix, expected `FB-DRAFT-SAVED`, got `FB-LIVE`.
- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest#getRouteFlowProcessConfigList_shouldReadSavedDraftBatchSnapshotBeforeCurrentBindings" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 34 tests, 0 failures.
- `node tests/e2e/mes-route-flow-save-error-single-toast-static.spec.js` -> PASS.
- `node tests/e2e/mes-route-draft-save-stays-editable-static.spec.js` -> PASS.
- `node tests/e2e/mes-route-candidate-save-publish-prompt-static.spec.js` -> PASS.
- `node tests/e2e/route-batch-record-save-contract-static.spec.js` -> PASS.
- `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js` -> PASS.
- `node tests/e2e/mes-route-edit-unsaved-candidate-discard-static.spec.js` -> PASS.
- `node tests/e2e/mes-route-flow-graph-only-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.
- `git diff --check -- <task files>` -> PASS, with CRLF warnings only.
- `rg -n "保存系统异常重复提示|前端保存链路重复错误提示门禁|save error single toast|草稿保存后仍可修改|前端草稿保存与提交发布解耦门禁" docs/experience-index.md docs/frontend-development.md` -> PASS.
- `rg -n "草稿 BATCH 快照显式保存|batchRecordBindingSnapshotExplicit|草稿保存系统异常|草稿 BATCH 快照读写对称边界" docs/experience-index.md docs/backend-development.md` -> PASS.
- Non-target legacy contract note: `node tests/e2e/mes-production-config-candidate-gate-static.spec.js` is currently blocked before assertions by historical `yudao-ui-admin-vue3/...` path references, so it was not used as current task verification.

## Backend Draft Snapshot Check

- Regression: `getRouteFlowProcessConfigList_shouldReadSavedDraftBatchSnapshotBeforeCurrentBindings`.
- Result: PASS after adding an explicit `batchRecordBindingSnapshotExplicit` marker to saved DRAFT BATCH snapshots and reading that snapshot before current bindings only for DRAFT versions.
- Adjacent coverage: full `MesProRouteFlowConfigServiceImplTest` PASS confirms PENDING_APPROVAL / READY_TO_PUBLISH still read current process settings instead of legacy snapshots.

## Real Page Check

- Entry: `http://127.0.0.1:8081/mes/pro/route/edit/922119?tab=flow&routeVersionId=361&routeVersionNo=V15&routeVersionStatus=DRAFT`.
- Method: Playwright logged into the local frontend, loaded the V15 flow page, intercepted `/admin-api/mes/pro/route-process-flow/validate` with a business 500 response, clicked the visible save button, and asserted one visible save error toast.
- Result: PASS, visible save error count = 1.
- Data safety: The intercepted verification did not write V15 data.

## Draft Save Check

- Entry: `http://127.0.0.1:8081/mes/pro/route/edit/922119?tab=flow&routeVersionId=361&routeVersionNo=V15&routeVersionStatus=DRAFT`.
- Method: Playwright logged into the local frontend, loaded the V15 flow page, intercepted `/admin-api/mes/pro/route-process-flow/validate` and `/admin-api/mes/pro/route-process-flow/save` with success responses, clicked the visible save button, and monitored `/admin-api/mes/pro/route-version/submit-publish`.
- Result: PASS, validate/save were intercepted once each, submit-publish request count was 0, and the page still showed `当前查看：草稿版本 V15`.
- Data safety: Both save endpoints were intercepted, so V15 was not modified during this verification.

## Notes

- Earlier direct API and one real save check had already advanced V15 `graphVersion`; after that, further real write checks were stopped and replaced with interception to avoid additional data changes.
- The current backend at `48081` was not restarted in this turn. Per `docs/local-runtime.md`, real-page validation of the backend fix requires confirming the owning PID/worktree and rebuilding or restarting the correct runtime; this report does not claim the running backend has loaded the new Java code.
- Frontend `8081` checks remain valid for the previously fixed duplicate-toast and draft-save/publish decoupling behavior.
- User-requested int_main E2E with `芋道源码/admin` was blocked before login: `8081` is the `E:\IntRuoyi` frontend, but `48081` is currently served by a Jar under `D:\IntRuoyiWorktree\edhr-release-dossier-e2e-20260726`. Current rules require fail-fast for worktree occupancy of `48081`, so no real login/save write was performed and no int_main backend-loaded claim is made.
- User-requested runtime restoration was also blocked: PID 57744 still belongs to the unrelated worktree Jar. No process was stopped and no replacement int_main backend was started because the current rules prohibit force-stopping that worktree process.

## Closeout Status

- Implementation and verification are complete.
- Cleanup preview/apply completed after the latest backend follow-up documentation update, with no deletions, blockers, or warnings.
- Task remains `ready_for_closeout`, not `completed`, because commit/push is blocked by the shared dirty/ahead worktree state.
- Additional user-requested int_main real E2E remains blocked until `48081` is restored to the `E:\IntRuoyi` int_main backend runtime.
