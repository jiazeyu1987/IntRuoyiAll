BDD: 识别表单使用单页紧凑布局 -> Given 用户上传 Word 并看到识别出的本地表单预览, When 预览组件渲染表格, Then 行高、字体和有效列宽受限，表单以紧凑方式呈现。
BDD: 空单元格显示可填写占位符 -> Given 识别结果中存在空单元格, When 预览组件渲染表格, Then 空单元格显示明确的文字占位符，提示该位置可填写。
BDD: 已提交模板沿用同一约束 -> Given 用户提交识别候选并回到模板列表, When 打开 `查看版式`, Then 只读预览沿用同一套紧凑布局与空单元格占位符。

RED: `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 show HEAD:src/views/mes/pro/batchrecordtemplate/TemplateLayoutPreview.vue` -> previous committed preview did not parse `displayConstraints`, used hard-coded size values, and rendered a static placeholder string instead of consuming blank-cell placeholder metadata.

GREEN: `pnpm exec eslint src/views/mes/pro/batchrecordtemplate/TemplateLayoutPreview.vue src/views/mes/pro/batchrecordtemplate/index.vue src/api/mes/pro/batchrecordtemplate/index.ts` -> PASS.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-batch-record-single-page-layout-constraints\scripts\verify-batch-record-single-page-layout-constraints.mjs` -> PASS, using the real DOC `RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`, the live page showed placeholder text `请填写`, import-preview placeholder cell metrics `fontSize=11 height=33.1875`, drawer placeholder cell metrics `fontSize=12 height=35.375`, successfully committed the candidate batch, returned to `tab=list`, opened the list drawer preview, and deleted the generated templates.
