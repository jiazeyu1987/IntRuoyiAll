# Task: DCC 受控查看入口、水印与防复制联动

## Goal

让 IntRuoyi DCC 受控文件在上传页提交前预览、文件浏览、我的文件、审批任务、
详情页 viewer 模式五个入口都统一进入受控阅读页，并在阅读页显示可追溯水印、
统一的 badge/overlay，以及阻止常见复制路径，不再直接暴露原始预览二进制入口。

## Scope

- 在前端仓库开始生产代码修改前创建本任务目录并记录前序任务状态。
- 承接 `20260516-dcc-controlled-preview-stamp` 已存在的 RED/GREEN 证据和工作树改动。
- 只改 DCC 受控文件前端页面、共享 viewer、前端 API helper 和任务级验证脚本。
- 不改审批流业务语义，不引入 OnlyOffice、通用文档预览或 fallback 预览链路。
- 若真实前端验证缺少可预览数据或权限，必须 fail fast 并记录阻塞。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-dcc-special-position-display-names/task.md`
- Status before this task: completed.
- Related frontend task: `doc/tasks/20260516-dcc-controlled-preview-stamp/task.md`
- Handling decision: the stamp-only task is explicitly superseded by this broader task and its evidence is reused here.

## Milestones

- [x] M1: Confirm previous task state, supersede the stamp-only task, and create this task package.
- [x] M2: Record BDD scenarios and RED evidence for the missing unified controlled-view behavior.
- [x] M3: Implement the shared watermarked no-copy viewer and upload-page protected preview.
- [x] M4: Wire browser, mine, approval-tasks, and detail entry points to the unified viewer route.
- [x] M5: Run GREEN verification, update evidence, and assess scoped commit readiness.

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-view-entry-watermark\scripts\verify-dcc-controlled-view-entry-watermark.cjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-entry-watermark run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-view-entry-watermark\scripts\verify-dcc-controlled-view-entry-watermark.mjs`
- `pnpm -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 exec eslint src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/view/index.vue src/views/dcc/controlled-file/view/presentation.ts src/views/dcc/controlled-file/upload/index.vue src/views/dcc/controlled-file/upload/submitter.ts src/views/dcc/controlled-file/browser/index.vue src/views/dcc/controlled-file/mine/index.vue src/views/dcc/controlled-file/approval-tasks/index.vue src/views/dcc/controlled-file/detail/index.vue doc/tasks/20260516-dcc-controlled-view-entry-watermark/scripts/verify-dcc-controlled-view-entry-watermark.mjs`
- `pnpm -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 ts:check`

## Current Status

Completed for the functional task scope. The shared controlled viewer,
upload-page protected preview, and browser/mine/approval-tasks/detail entry
rewiring are all live and verified through a real browser path.

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-view-entry-watermark\scripts\verify-dcc-controlled-view-entry-watermark.cjs` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-entry-watermark run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-view-entry-watermark\scripts\verify-dcc-controlled-view-entry-watermark.mjs` -> PASS
- `pnpm -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 exec eslint src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/view/index.vue src/views/dcc/controlled-file/view/presentation.ts src/views/dcc/controlled-file/upload/index.vue src/views/dcc/controlled-file/upload/submitter.ts src/views/dcc/controlled-file/browser/index.vue src/views/dcc/controlled-file/mine/index.vue src/views/dcc/controlled-file/approval-tasks/index.vue src/views/dcc/controlled-file/detail/index.vue doc/tasks/20260516-dcc-controlled-view-entry-watermark/scripts/verify-dcc-controlled-view-entry-watermark.mjs` -> PASS
- Real browser result:
  - created controlled file id: `10`
  - verified entries: `upload-preview`, `mine`, `approval-tasks`, `detail`, `browser`
  - watermark badge and overlay were visible in the unified viewer
  - copy shortcut and context-menu blocking both held in the viewer

## Blocker And Impact

- Blocker 1: repository-wide `ts:check` still fails in untouched generated file
  `src/types/auto-components.d.ts`.
- Impact: this task's owned DCC files are verified and ready for scoped
  commit, but repo-wide type health remains a separate issue outside this task.

## Cleanup Keep

- `doc/tasks/20260516-dcc-controlled-view-entry-watermark/bug-regression-evidence.md`
- `doc/tasks/20260516-dcc-controlled-view-entry-watermark/frontend-feature-evidence.md`
- `doc/tasks/20260516-dcc-controlled-view-entry-watermark/scripts/verify-dcc-controlled-view-entry-watermark.cjs`
- `doc/tasks/20260516-dcc-controlled-view-entry-watermark/scripts/verify-dcc-controlled-view-entry-watermark.mjs`
