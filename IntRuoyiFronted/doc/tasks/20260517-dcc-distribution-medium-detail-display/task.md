# Task: DCC 分发方式详情展示

## Goal

在 `DCC受控文件详情` 页的“分发状态”表格中展示每条分发记录的发放方式，让前面已经打通的
`distributionMedium` 能从配置页延伸到读侧详情展示。

## Scope

- 为前端 `ControlledFileDistributionStatusVO` 增加 `distributionMedium`。
- 在详情页分发状态表格中增加“发放方式”列。
- 增加 `distributionMedium` 的前端标签映射。
- 保持现有详情页布局、分发状态标签、接收人展示不变。
- 不在本任务中改训练页、浏览页或纸质签收闭环。

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260517-dcc-distribution-medium-frontend/task.md`
- Status before this task: completed for code delivery.
- Impact: medium config/save is already green, so this task focuses only on
  detail read-side display.

## Milestones

- [x] M1: Create this frontend task package before production-code edits.
- [x] M2: Record BDD scenarios and RED evidence for missing medium display.
- [x] M3: Implement type + detail-table display support.
- [x] M4: Run targeted verification and update evidence.
- [ ] M5: Commit only task-scoped files if verification fully passes.

## Expected Verification

- `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-distribution-medium-detail-display\frontend-feature-evidence.md`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-distribution-medium-detail-display run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-distribution-medium-detail-display\scripts\verify-dcc-distribution-medium-detail-display.mjs`

## Current Status

Completed for frontend code delivery. The detail distribution-status table now
shows a readable delivery-medium column, and real detail-page verification is
green.

## Blocker And Impact

- Blocker: a task-scoped frontend commit is not yet safe because the repository
  still contains unrelated in-progress DCC upload / training changes in other
  files and task directories.
- Impact: the detail-display slice is implemented and verified, but a clean
  task-only commit still needs a narrower or cleaner write set.

## Final Verification Result

- RED:
  - source-level check -> FAIL,
    `missing_detail_distribution_medium_in:workflow.ts|detail/index.vue|detail/presentation.ts`
- GREEN:
  - `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-distribution-medium-detail-display run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-distribution-medium-detail-display\scripts\verify-dcc-distribution-medium-detail-display.mjs` -> PASS
- Real detail result:
  - controlled file id: `46`
  - title: `DCC-TRAIN-1778985397847-文件`
  - first medium: `PUBLIC_FOLDER`
  - first distribution row contains: `研发部门 / 公盘目录 / 已阅读`

## Cleanup Keep

- `doc/tasks/20260517-dcc-distribution-medium-detail-display/frontend-feature-evidence.md`
- `doc/tasks/20260517-dcc-distribution-medium-detail-display/scripts/verify-dcc-distribution-medium-detail-display.mjs`
