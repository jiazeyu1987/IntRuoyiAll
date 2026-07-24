# Task: DCC 受控预览增加受控章
## Goal

让 DCC 受控文件在“再次预览”场景下显示明显的 `受控` 印章，避免用户在
受控预览页看到与原始 PDF 无差别的视觉结果。

## Scope

- 检查并显式阻塞当前前端仓库中上一个未完成任务，避免两个写任务并行落在同一
  workspace。
- 在生产代码变更前创建本任务文档、执行日志和 RED 校验脚本。
- 仅修改 DCC 受控文件预览组件及其直接依赖的展示逻辑。
- 保持现有后端接口、路由、权限和下载行为不变。
- 不引入 fallback、mock 预览或静默降级逻辑。

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260516-dcc-routes-switch-auto-query/task.md`
- Status before this task: blocked.
- Impact: the previous task is paused by explicit user reprioritization and does
  not conflict with this controlled-preview stamp fix.

## Milestones

- [x] M1: Block the unfinished previous frontend task and create this task
  package before production-code edits.
- [x] M2: Record BDD scenarios and capture RED evidence for the missing
  controlled-preview stamp behavior.
- [x] M3: Implement the minimal preview-layer stamp rendering without changing
  backend contracts.
- [x] M4: Run GREEN verification and update task evidence.
- [x] M5: Commit only this task's frontend changes if verification fully
  passes.

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-preview-stamp\scripts\verify-dcc-controlled-preview-stamp.cjs`
- `pnpm exec eslint src\views\dcc\controlled-file\view\index.vue src\views\dcc\controlled-file\view\presentation.ts`

## Current Status

Superseded by `doc/tasks/20260516-dcc-controlled-view-entry-watermark/task.md`.
The visible `受控` stamp implementation and its verification evidence stay in
the current working tree and are carried forward into the broader controlled
viewer entry, watermark, and no-copy task.

## Blocker And Impact

- Blocker: superseded by the broader DCC controlled-view task.
- Impact: the stamp-only slice is no longer the final delivery unit; any
  follow-up changes must be tracked in
  `20260516-dcc-controlled-view-entry-watermark`.

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-preview-stamp\scripts\verify-dcc-controlled-preview-stamp.cjs` -> PASS
- `pnpm exec eslint src\views\dcc\controlled-file\view\index.vue src\views\dcc\controlled-file\view\presentation.ts` -> PASS
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-preview-stamp\bug-regression-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-preview-stamp\frontend-feature-evidence.md` -> PASS
