# Task: DCC 受控文件详情页文案乱码修复

## Goal

修复 DCC 受控文件详情页里的占位乱码文案，确保纸质发放确认区域和未知用户兜底文案都显示为规范简体中文。

## Scope

- 把详情页表格中 `??? / ????` 占位文案改为准确中文。
- 把未知确认人兜底文案中的 `??#` 改为规范的用户标识格式。
- 不改业务行为，不引入 fallback 分支。

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260517-dcc-paper-distribution-audit-fields-frontend/task.md`
- Status before this task:
  code delivery completed, but task-scoped commit was blocked by unrelated in-progress changes already recorded in that task.
- Impact:
  this task can proceed independently because问题点在同一详情页的残留文案，不依赖前一个任务的未完成提交。

## Milestones

- [x] M1: Create this task package and record the regression target.
- [x] M2: Add a failing regression check for the placeholder copy.
- [x] M3: Fix the DCC detail page copy and helper fallback text.
- [x] M4: Run targeted verification and update evidence.
- [ ] M5: Commit only task-scoped files if the repo state allows a clean task-only commit (blocked by pre-existing dirty worktree).

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-controlled-file-detail-copy-fix\scripts\verify-dcc-controlled-file-detail-copy-fix.mjs`
- `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- `python C:\Users\BJB110\.codex\skills\clear-frontend-copy\scripts\scan_frontend_copy.py --root D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\detail --format markdown`

## Current Status

Code fix and verification are complete. The remaining blocker is a clean task-only commit because the repository already contains pre-existing uncommitted DCC detail-page changes from earlier work.

## Blockers And Impact

- Blocker: existing uncommitted DCC detail-page feature changes in the same repository/worktree.
- Impact: the current fix is verified, but a task-only commit cannot be made safely without also touching pre-existing work.

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-controlled-file-detail-copy-fix\scripts\verify-dcc-controlled-file-detail-copy-fix.mjs` -> PASS
- `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS
