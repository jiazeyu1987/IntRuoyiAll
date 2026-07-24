# Task: DCC 发放方式前端改造

## Goal

在 `yudao-ui-admin-vue3` 的 `DCC下发` 页面中补齐“发放方式”前端配置能力，让用户可以为每条分发规则配置：

- `PUBLIC_FOLDER`
- `PAPER`

本任务只交付前端第二阶段能力：API 类型、页面交互、保存 payload 和列表回显，不改视觉基调，不补纸质签收闭环页面。

## Scope

- 为前端分发规则类型增加 `distributionMedium`。
- 在 `DCC下发` 页增加“发放方式”列，并支持行内选择。
- 保存时把 `distributionMedium` 一起提交给后端。
- 回显已存在规则的 `distributionMedium`。
- 保持现有部门选择、启用开关、提示文案和布局风格基本不变。
- 不在本任务中改 `DCC培训` 页，也不改详情页分发展示。

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260516-dcc-upload-name-version-linkage/task.md`
- Status before this task: completed.
- Impact: no unfinished latest frontend task blocks this delivery.

## Repository Context Risk

- The frontend repository currently has unrelated dirty changes, including:
  - `src/api/dcc/controlledFile/workflow.ts`
  - `src/views/dcc/controlled-file/upload/index.vue`
  - `src/views/dcc/controlled-file/training/**`
- This task avoided reverting or sweeping in those unrelated edits.

## Milestones

- [x] M1: Create this frontend task package before production-code edits.
- [x] M2: Record BDD scenarios and RED evidence for the missing medium selector.
- [x] M3: Implement API typing and page-level medium editing / payload support.
- [x] M4: Run targeted frontend verification and update evidence.
- [ ] M5: Commit only frontend files produced by this task if verification fully passes.

## Expected Verification

- `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-distribution-medium-frontend\frontend-feature-evidence.md`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-distribution-medium-frontend run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-distribution-medium-frontend\scripts\verify-dcc-distribution-medium-frontend.mjs`

## Current Status

Completed for frontend code delivery. The `DCC下发` page now exposes a
delivery-medium column, defaults existing rules to `PUBLIC_FOLDER` when the
backend response omits the field, and includes `distributionMedium` in the save
payload. Real-page verification is green after the paired backend runtime was
rebuilt and restarted.

## Blocker And Impact

- Blocker: a task-scoped frontend commit is not yet safe because the repository
  already contains unrelated in-progress DCC upload / training changes in other
  files and task directories.
- Impact: the frontend slice is implemented and verified, but a clean
  task-only commit still requires a narrower or cleaner write set.

## Final Verification Result

- RED:
  - source-level check -> FAIL,
    `missing_distribution_medium_in:D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/src/views/dcc/controlled-file/distribution/index.vue|...CategoryDepartmentRulesSection.vue|...categories/governance.ts|...fileCategories.ts`
  - `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> FAIL initially due
    repository Node heap exhaustion before rerunning with larger heap.
- GREEN:
  - `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-distribution-medium-frontend run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-distribution-medium-frontend\scripts\verify-dcc-distribution-medium-frontend.mjs` -> PASS
- Real path result:
  - category: `产品技术要求`
  - headers include: `发放方式`
  - first row medium label: `公盘目录`
  - request payload: `{"departmentId":103,"distributionMedium":"PUBLIC_FOLDER","active":true}`
  - backend response row: `{"id":6,"categoryId":1,"departmentId":103,"distributionMedium":"PUBLIC_FOLDER","active":true}`

## Cleanup Keep

- `doc/tasks/20260517-dcc-distribution-medium-frontend/frontend-feature-evidence.md`
- `doc/tasks/20260517-dcc-distribution-medium-frontend/scripts/verify-dcc-distribution-medium-frontend.mjs`
