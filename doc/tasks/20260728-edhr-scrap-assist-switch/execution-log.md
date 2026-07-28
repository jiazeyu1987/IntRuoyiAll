# Execution Log: 20260728-edhr-scrap-assist-switch

## User Intent
- 用户反馈：选择红框里的“张可莹”候选时应切换到损耗单，现在显示错误“eDHR 批次缺少唯一批记录路线”。
- 用户授权：可以在 worktree 里修复，然后融合进 int_main。

## Preconditions
- 已读取 `docs/worktree-restrictions.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 已读取 `bug-regression-fix-loop` 技能和 `references/bug-contract.md`。
- 已读取 `docs/frontend-development.md`、`docs/backend-development.md`、`docs/e2e-rules.md`、`docs/branch-runtime-ports.md`、`docs/worktree-memory.md`。
- int_main 初始存在脏改动，按项目规则创建基线提交 `3fb50fa6`。
- 创建 worktree `D:\IntRuoyiWorktree\20260728-edhr-scrap-assist-switch`，分支 `codex/20260728-edhr-scrap-assist-switch`。
- 运行 `reserve-worktree-slot.ps1` 登记 slot `10`，前端 `8091`，后端 `48091`。
- 读取 `docs/experience-index.md` 后命中前端切换填写人、后端切换填写人快照、worktree 融合门禁；已把本次 FormCenter 槽位导航经验沉淀到 `docs/frontend-development.md` 和 `docs/experience-index.md`。

## BDD Scenarios
- BDD: 表单槽位候选切换到损耗单 -> Given 当前 eDHR 工序同时存在批处理表单候选和工艺路线表单槽位候选 When 用户在切换填写人弹窗中选择“张可莹 / 损耗单”候选 Then 系统应切换到损耗单对应填写链路 And 不得报“eDHR 批次缺少唯一批记录路线”。

## RED/GREEN Evidence
- RED: `node tests\e2e\edhr-switch-filler-formcenter-slot-static.spec.js` -> FAIL，断言“切换到损耗单等 FormCenter 表单槽位时，必须先走表单槽位详情页分支，不能先要求 executionId。”
- GREEN: `node tests\e2e\edhr-switch-filler-formcenter-slot-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-switch-filler-selectability-static.spec.js` -> PASS。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\edhr-work-task-formcenter-navigation-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\edhr-dynamic-form-card-preview-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS。
- REGRESSION: `pnpm ts:check` -> first FAIL because current worktree lacked `node_modules` / `cross-env`; after `pnpm install --frozen-lockfile` completed and `node_modules\.bin\vite.cmd` + `cross-env.cmd` existed, rerun `pnpm ts:check` -> PASS。
- REGRESSION: `git diff --check` -> PASS with LF/CRLF warning only.

## Implementation Notes
- `ExecutionPage.vue` now detects `opened.formCenterInstanceId && opened.formTemplateId` before the traditional `executionId` guard, and routes to `/mes/pro/feedback/edhr-batch-execution/detail` with `openRouteForm=1` for FormCenter route-form slots.
- The route query carries `batchTaskId`, backend-confirmed `workTaskId`, selected `assistUserId`, and selected filler display name so the selected loss-form filler context is not lost.
- `BatchExecutionDetailPage.vue` now parses `assistUserId` from the route query for the matching auto-open task and passes it into the second `openEdhrBatchTask` call.
- No backend fallback, exception swallowing, mock success, or default-success behavior was added.

## Integration Notes
- `git fetch origin int_main` completed.
- `origin/int_main` is not an ancestor of current `codex/20260728-edhr-scrap-assist-switch` because this branch starts from baseline commit `3fb50fa6` that contains unrelated dirty-worktree preservation.
- Implementation commit on source branch: `b4700d39 fix: route assist filler form slots to detail drawer`.
- Created clean branch `codex/20260728-edhr-scrap-assist-switch-clean` from `origin/int_main`.
- Cherry-picked task implementation commit onto clean branch; after latest rebase onto `origin/int_main` commit `7d59f3bf`, the clean implementation commit is `5e87b3ef fix: route assist filler form slots to detail drawer`.
- Re-ran on clean branch: target static contracts, adjacent loss/dynamic-form contracts, bug-regression validator, and `pnpm ts:check` all PASS.
- `branch-runtime-port-guard.ps1` initially failed because the port registry still recorded source branch `codex/20260728-edhr-scrap-assist-switch` for this path after switching to the clean branch.
- Updated only the current worktree registry entry branch to `codex/20260728-edhr-scrap-assist-switch-clean`, keeping slot `10`, frontend `8091`, backend `48091`; removed failed-update temp file from `D:\IntRuoyiWorktree\.ports`.
- `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS on clean branch.
- Rebase conflict in `docs/frontend-development.md` and `docs/experience-index.md` was resolved by keeping both upstream eDHR current-process highlight gates and this task's FormCenter slot navigation gate.
- Latest rebase onto `origin/int_main` commit `7d59f3bf` completed without new conflicts.
- After rebase, `git log --oneline origin/int_main..HEAD` showed only the clean implementation commit `5e87b3ef fix: route assist filler form slots to detail drawer` and this task's integration-record commit.
- Verification rerun after rebase:
  - `node tests\e2e\edhr-switch-filler-formcenter-slot-static.spec.js` from `IntRuoyiFronted` -> PASS.
  - `node tests\e2e\edhr-switch-filler-selectability-static.spec.js` from `IntRuoyiFronted` -> PASS.
  - `node tests\e2e\edhr-work-task-formcenter-navigation-static.spec.js` from `IntRuoyiFronted` -> PASS.
  - `node tests\e2e\edhr-loss-form-open-action-static.spec.js` from `IntRuoyiFronted` -> PASS.
  - `node tests\e2e\edhr-dynamic-form-card-preview-static.spec.js` from `IntRuoyiFronted` -> PASS.
  - `node tests\e2e\edhr-batch-detail-assist-preview-switch-static.spec.js` from `IntRuoyiFronted` -> PASS.
  - `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` from repo root -> PASS.
  - `pnpm ts:check` from `IntRuoyiFronted` -> PASS.
  - `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260728-edhr-scrap-assist-switch\bug-regression-evidence.md` -> PASS.
  - `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS.
  - `git diff --check` -> PASS.
- `git push origin HEAD:int_main` -> PASS; `origin/int_main` advanced to `59dc8a50`.
- `git fetch origin int_main` plus `git rev-parse HEAD` / `git rev-parse origin/int_main` confirmed both at `59dc8a50`.
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-edhr-scrap-assist-switch --mode preview` -> BLOCKED: main worktree `E:\IntRuoyi` is dirty and cannot receive ff-only merge. Cleanup keep list preserved `task.md`, `execution-log.md`, `verification-report.md`, and `bug-regression-evidence.md`; delete list was empty.
