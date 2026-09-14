# 20260806 commit frontend backend and merge int_main

## Task Goal

在 `E:\IntRuoyi` 的 `int_main` 上完成用户授权的融合：先保留既有脏工作区为独立基线，再融合 `origin/int_main` 与 `origin/codex/replan-current-route-after-feedback`，确保重排逻辑按“剩余未完成部分读取当前最新工艺路线”生效，并推送到 `origin/int_main`。

## 经验门禁

- `docs/powershell-memory.md#脏工作区基线门禁`：脏工作区必须先用独立基线提交保留，不能 reset 或混入当前融合提交。
- `docs/powershell-memory.md#脏工作区功能分支融合增量门禁`：融合远端功能分支前先用 merge-base 计算真实分支增量，再与未暂存文件求交集，避免误判或混入并行改动。
- `docs/worktree-restrictions.md` 与 `docs/branch-runtime-ports.md`：融合、提交和推送前后运行 branch runtime port guard。

## Milestones

- [x] 读取 Git、PowerShell、worktree、端口、后端、前端、数据库、E2E 和 closeout 门禁。
- [x] 保存融合前脏工作区基线提交。
- [x] 合并 `origin/int_main` 并解决冲突。
- [x] 合并 `origin/codex/replan-current-route-after-feedback`。
- [x] 验证重排逻辑和冲突相关静态合同。
- [x] 保存融合后的并行残余基线。
- [x] 运行 closeout cleanup preview/apply。
- [x] 提交任务记录、推送 `int_main` 并记录 post-push 状态。

## Expected Verification

- `scripts\preflight\branch-runtime-port-guard.ps1`
- `git diff --check -- IntRuoyiBackend IntRuoyiFronted`
- `node IntRuoyiFronted/tests/e2e/mes-process-pool-team-leader-static.spec.js`
- `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` in `IntRuoyiFronted`
- `node IntRuoyiFronted/tests/e2e/team-leader-workbench-static.spec.cjs`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation+replanPreview_shouldNotReserveFeedbackProtectedRouteProcessCapacityWithoutLineKey" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `git push origin int_main`
- `git status --short --branch`

## Current Status

completed

Implementation, required verification, cleanup preview/apply, task-record commit, and required `origin/int_main` push are complete for the replan fusion. This final record includes additional task-outside baseline commits created during push closeout; the shell post-push check verifies no local ahead state after this record is pushed.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，融合保留当前工艺路线重排语义，并用 merge-base 增量检查和独立基线避免并行改动污染。
- `是否存在临时补丁或绕过`：否。
