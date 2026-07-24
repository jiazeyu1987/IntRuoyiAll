BDD: 上传页提交前预览使用受控阅读器 -> Given 用户在上传页选择一个 PDF 并完成 preview upload When 页面显示提交前预览 Then 页面必须使用统一受控 viewer 渲染 PDF bytes 并显示可见水印，而不是原始 iframe 预览

BDD: 文件浏览页点击文件名称或文件编号进入受控阅读页 -> Given 用户在文件浏览页看到可预览文件 When 用户点击文件名称或文件编号 Then 页面必须打开 `viewer=1` 受控阅读页而不是直接打开原始 preview 二进制 URL

BDD: 我的文件页点击文件名称或文件编号进入受控阅读页 -> Given 用户在我的文件页看到可预览文件 When 用户点击文件名称或文件编号 Then 页面必须打开 `viewer=1` 受控阅读页而不是直接打开原始 preview 二进制 URL

BDD: 审批任务页点击文件标题或文件编号进入受控阅读页 -> Given 用户在审批任务列表看到带关联受控文件的审批项 When 用户点击文件标题或文件编号 Then 页面必须打开 `viewer=1` 受控阅读页并保持原审批入口不丢

BDD: 受控阅读页阻止常见复制路径 -> Given 用户打开 DCC 受控阅读页 When 用户尝试右键、复制快捷键、剪切、全选或拖拽 Then 页面必须阻止这些复制相关交互且仍保持 PDF.js canvas-only 阅读能力

RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-view-entry-watermark\scripts\verify-dcc-controlled-view-entry-watermark.cjs` -> FAIL, `missing:mine-file-number-column`, proving my-files still lacked the required clickable file-number entry.

GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-view-entry-watermark\scripts\verify-dcc-controlled-view-entry-watermark.cjs` -> PASS, proving upload/browser/mine/approval-tasks/viewer source contracts are now wired to the unified controlled viewer.

GREEN: `pnpm -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 exec eslint src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/view/index.vue src/views/dcc/controlled-file/view/presentation.ts src/views/dcc/controlled-file/upload/index.vue src/views/dcc/controlled-file/upload/submitter.ts src/views/dcc/controlled-file/browser/index.vue src/views/dcc/controlled-file/mine/index.vue src/views/dcc/controlled-file/approval-tasks/index.vue src/views/dcc/controlled-file/detail/index.vue doc/tasks/20260516-dcc-controlled-view-entry-watermark/scripts/verify-dcc-controlled-view-entry-watermark.mjs` -> PASS.

RED: `pnpm -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 ts:check` -> FAIL, repository-wide syntax errors remain in untouched generated file `src/types/auto-components.d.ts`.

RED: earlier real Playwright reruns exposed multiple live blockers in sequence:
- missing backend watermark metadata exposure
- final approval stuck in `FINALIZING`
- missing PDF.js worker asset
- runtime OAuth2 token-cache failure after fresh login

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-entry-watermark run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-view-entry-watermark\scripts\verify-dcc-controlled-view-entry-watermark.mjs` -> PASS, created controlled file `10`, verified unified viewer entry from `upload-preview`, `mine`, `approval-tasks`, `detail`, and `browser`, and confirmed badge/overlay plus copy-guard behavior in the protected viewer.
