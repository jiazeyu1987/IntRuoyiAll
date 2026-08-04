# Execution Log

## User Intent

用户反馈：审批中心“待办”页签里，左侧徽标显示待办数量 128，但列表区域显示为空/数量为 0。

## Baseline

- Branch: `int_main`
- Existing dirty workspace baseline commit: `6f9ed0e83 chore: baseline existing workspace changes`
- Shared baseline anomaly: 本任务源码、测试和初始任务文档后续被 `1bd808f30 chore: baseline int_main remaining before rrm M6 merge` 混入共享分支基线提交；该提交还包含 DCC、BPM policy、process pool 等其它任务文件。按共享分支并发基线门禁记录事实，不 amend、不 reset、不回滚他人改动。
- Current local branch later advanced to `e9388400e feat(mes): continue role requirement M6 gates`，`origin/int_main..HEAD` 仍包含其它任务 ahead 提交，收尾推送需单独确认边界。

## BDD Scenarios

- BDD: 待办徽标与待办列表一致 -> Given 当前用户存在全局待办任务总数大于 0, When 用户打开 `/approval-center/todo` 且未设置模块或关键词过滤, Then 列表接口必须返回非空当前页数据并且 total 与徽标统计口径一致。
- BDD: 待办查询状态可见且可恢复 -> Given 待办页签 URL 或快速过滤中存在模块/关键词过滤, When 过滤导致列表为空, Then 页面查询控件必须展示对应过滤状态，不能在控件空白时偷偷按过滤条件查询。

## RED / GREEN Evidence

- RED: `node tests/e2e/approval-center-route-filter-visible-static.spec.js` -> FAIL before frontend fix, expected reason: source lacked `syncApprovalQuickFilterStateFromQuery` and route query filters were not synchronized into visible quick-filter controls.
- RED: backend manual RED attempt by temporarily reverting the service consistency guard and running `mvn.cmd -pl yudao-module-bpm -am "-Dtest=ApprovalCenterServiceImplTest#getTaskPageFailsFastWhenProviderReportsTodoTotalWithoutFirstPageRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BLOCKED, Maven/Javac stalled before Surefire; no business failure was claimed.
- GREEN: `mvn.cmd -pl yudao-module-bpm -am "-Dtest=ApprovalCenterServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS at 2026-08-04T16:50:12+08:00, `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
- GREEN: isolated verification worktree `D:\IntRuoyiWorktree\approval-center-todo-verify-20260804`, HEAD `b59f5baf4`, command `mvn.cmd -pl yudao-module-bpm -am "-Dtest=ApprovalCenterServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS at 2026-08-04T19:16:56+08:00, `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, total time `07:32 min`.
- GREEN: `node tests/e2e/approval-center-route-filter-visible-static.spec.js` -> PASS, `approval center route filters are visible in quick-filter controls`.
- GREEN: `node tests/e2e/approval-center-pagination-preserve-page-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/approval-center-fill-list-area-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/approval-center-pagination-event-payload-static.spec.js` -> PASS.
- BLOCKED: `pnpm exec eslint --ext .js,.ts,.vue src/views/approval-center/index.vue tests/e2e/approval-center-route-filter-visible-static.spec.js` and retry with explicit `--ext=.js --ext=.ts --ext=.vue` both exceeded 60s with no output and had to be stopped by task-owned process chain. This is recorded as a tool/runtime blocker, not a pass.

## Implementation Notes

- Backend `ApprovalCenterServiceImpl` now wraps provider page responses with `requireConsistentPage(...)`; when first page reports `total > 0` but returns an empty list, it throws `APPROVAL_ADAPTER_PAGE_INCONSISTENT` instead of letting the UI show a false empty state.
- Backend `ApprovalCenterServiceImplTest` adds `getTaskPageFailsFastWhenProviderReportsTodoTotalWithoutFirstPageRows`.
- Frontend `loadModules()` rethrows errors after setting visible load error state, so module API failures are not silently overwritten as “0 个模块”.
- Frontend adds `syncApprovalQuickFilterStateFromQuery()` and invokes it before list loading in both full refresh and route-query refresh paths.

## Worktree Verification

- Read `docs\worktree-restrictions.md` and matching `docs\worktree-memory.md#主工作区-maven-target-冲突时的隔离验证-worktree-门禁`.
- Created detached verification worktree under allowed root: `D:\IntRuoyiWorktree\approval-center-todo-verify-20260804`.
- Did not start frontend/backend services and did not use ports; no slot registration was required.
- Removed via `git worktree remove --force D:\IntRuoyiWorktree\approval-center-todo-verify-20260804`; final `Test-Path=False`.

## Remaining Blockers

- Commit/push closeout is not yet safe from this task context because `origin/int_main..HEAD` contains unrelated ahead commits `b59f5baf4` and `e9388400e` from other tasks. Pushing now would publish unrelated task work; rewriting/resetting is forbidden.

