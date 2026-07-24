# Task: DCC 上传页历史文件名称与版本号联动

## Goal

在 DCC 受控文件上传页中，用户先选择文件类型后，“文件名称”支持从历史上传记录中下拉选择，也允许不选继续手输；当用户选择某个历史文件名称时，“版本号”输入框自动显示该同名文件当前版本号，并保持可编辑以继续填写更高新版本。

## Scope

- 先明确阻塞并暂停当前最新未完成前端任务，避免跨任务混改。
- 在本任务包中记录 BDD、RED/GREEN 证据和最终验证结果。
- 调整上传页表单交互，让“文件名称”支持历史选项与自由输入并存。
- 接入后端历史文件名称查询接口，并在类别切换、名称选择、清空时保持状态一致。
- 保持现有上传页样式、审批路线预览、PDF 预览上传和严格版本校验不变。
- 使用 Playwright 走真实前端路径做最终验收；若真实数据缺失则按 fail-fast 原则记录阻塞。

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260516-dcc-four-real-approvers-e2e/task.md`
- Status before this task: blocked by explicit user reprioritization.
- Impact: the previous task is intentionally paused and does not block this
  upload-linkage delivery.

## Milestones

- [x] M1: Block the previous unfinished frontend task and create this task
  package before production-code edits.
- [x] M2: Record BDD scenarios and RED evidence for the missing dropdown and
  version autofill behavior.
- [x] M3: Implement the minimal upload-form linkage and API integration.
- [x] M4: Run targeted frontend verification plus real Playwright validation
  and update evidence.
- [x] M5: Commit only frontend files produced by this task if verification
  fully passes.

## Expected Verification

- `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-name-version-linkage run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-upload-name-version-linkage\scripts\verify-dcc-upload-name-version-linkage.mjs`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-upload-name-version-linkage\frontend-feature-evidence.md`

## Current Status

Completed. The upload form now exposes historical file-name suggestions,
auto-fills the current version on selection, and keeps that version editable.
Closeout preview has been reviewed and only task-scoped frontend files remain
for commit.

## Final Verification Result

- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-name-version-linkage-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-upload-name-version-linkage\scripts\verify-dcc-upload-name-version-linkage.mjs` -> PASS
- Real path result: category `产品技术要求`, historical file `DCC-FULL-CHAIN-1778939065187-文件`, auto-filled version `1.0`

## Cleanup Keep

- doc/tasks/20260516-dcc-upload-name-version-linkage/frontend-feature-evidence.md
- doc/tasks/20260516-dcc-upload-name-version-linkage/scripts/verify-dcc-upload-name-version-linkage.mjs
