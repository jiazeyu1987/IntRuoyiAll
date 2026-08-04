# Bug Regression Evidence

## Bug Summary

审批中心待办菜单徽标显示存在待办任务，但进入待办页签后列表显示为空或数量为 0。

## Expected Behavior

未设置模块或关键词过滤时，待办页签列表必须展示同一用户同一正式聚合口径下的待办任务；如果过滤后无结果，过滤条件必须在页面上可见。模块列表接口失败时必须显示真实错误，不得被后续列表请求静默覆盖成 “0 个模块”。

## Reproduction

- Screenshot symptom: 左侧“待办”徽标有数量，页面列表区域显示“暂无审批任务”，页面元信息显示 `0 个模块`。
- RED: `node tests/e2e/approval-center-route-filter-visible-static.spec.js` before the fix failed because route `moduleCode` / `keyword` query state was not synchronized into visible quick-filter controls.
- Backend inconsistent provider case is represented by `ApprovalCenterServiceImplTest#getTaskPageFailsFastWhenProviderReportsTodoTotalWithoutFirstPageRows`.

## Root Cause

- Backend provider page aggregation accepted an inconsistent first page where provider `total > 0` but `list` was empty, allowing adapter/query defects to surface as a false empty list.
- Frontend route query parameters could continue to affect list requests while quick-filter controls looked empty, so users could see an empty list without visible active filters.
- Frontend `loadModules()` swallowed module API failures, allowing later list refreshes to leave page module count looking like a valid zero instead of a load error.

## Regression Test

- Added backend JUnit: `ApprovalCenterServiceImplTest#getTaskPageFailsFastWhenProviderReportsTodoTotalWithoutFirstPageRows`.
- Added frontend static contract: `IntRuoyiFronted/tests/e2e/approval-center-route-filter-visible-static.spec.js`.
- Re-ran adjacent approval-center static contracts for pagination/page-area behavior.

## RED

RED: `node tests/e2e/approval-center-route-filter-visible-static.spec.js` -> FAIL before fix, expected reason: missing `syncApprovalQuickFilterStateFromQuery` and hidden route filters were not reflected in visible quick-filter controls.

RED: Backend service-fix-reverted JUnit attempt -> BLOCKED by Maven/Javac stall before Surefire. No backend business failure was claimed from the stalled run; the added regression is verified by subsequent GREEN runs.

## GREEN

GREEN: `mvn.cmd -pl yudao-module-bpm -am "-Dtest=ApprovalCenterServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.

GREEN: Detached worktree rerun of the same Maven command at `D:\IntRuoyiWorktree\approval-center-todo-verify-20260804` -> PASS at 2026-08-04T19:16:56+08:00, `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.

GREEN: `node tests/e2e/approval-center-route-filter-visible-static.spec.js` -> PASS.

GREEN: `node tests/e2e/approval-center-pagination-preserve-page-static.spec.js`, `node tests/e2e/approval-center-fill-list-area-static.spec.js`, and `node tests/e2e/approval-center-pagination-event-payload-static.spec.js` -> PASS.

## Verification

- `git diff --check` on task-owned source/test files passed earlier in the task flow.
- Target backend JUnit passed in a clean detached worktree, proving the committed HEAD content is not relying on dirty main-worktree files.
- The detached worktree was removed after verification and `Test-Path` returned `False`.
- ESLint is not recorded as passing in this closeout because two scoped `pnpm exec eslint` attempts hung without output and were stopped.

## Risk And Scope

范围限定在统一审批中心待办列表、provider 聚合分页一致性、路由查询状态同步和模块加载错误传播；不调整模块业务审批处理页，不引入 fallback。

## Blockers

- Backend RED command could not produce a clean failing Surefire result because Maven stalled before the test phase during the manual revert attempt.
- Final commit/push closeout is blocked until unrelated ahead commits on `int_main` are handled by their owning task or explicitly approved for push.

