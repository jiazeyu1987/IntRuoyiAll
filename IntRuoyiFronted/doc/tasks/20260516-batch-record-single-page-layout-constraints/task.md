# Task: 电子批记录单页布局与占位约束

## Goal

让当前电子批记录识别出来的本地表单预览更紧凑，尽量在一个浏览器页面里显示完，并让空单元格显示明确的文字占位符，提示这些位置可填写。

## Scope

- 调整 `TemplateLayoutPreview.vue` 的渲染方式，消费后端返回的紧凑布局约束。
- 在导入页预览和列表抽屉预览中统一显示空单元格占位符。
- 不修改 DCC 页面，不改其他无关前端模块，不引入 fallback。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-dcc-full-chain-real-e2e/task.md`
- Status before this task: blocked by user reprioritization.
- Impact: the paused DCC full-chain E2E task is unrelated and does not block this electronic batch record layout work.

## BDD Scenarios

- BDD: 识别表单使用单页紧凑布局 -> Given 用户上传 Word 并看到识别出的本地表单预览, When 预览组件渲染表格, Then 行高、字体和有效列宽受限，表单以紧凑方式呈现。
- BDD: 空单元格显示可填写占位符 -> Given 识别结果中存在空单元格, When 预览组件渲染表格, Then 空单元格显示明确的文字占位符，提示该位置可填写。
- BDD: 已提交模板沿用同一约束 -> Given 用户提交识别候选并回到模板列表, When 打开 `查看版式`, Then 只读预览沿用同一套紧凑布局与空单元格占位符。

## Milestones

1. [x] M1: 创建任务包并记录 BDD 场景。
2. [x] M2: 记录前端 RED 基线与目标验证点。
3. [x] M3: 落前端预览约束渲染并接入后端约束字段。
4. [x] M4: 使用真实 DOC 路径复跑前端 E2E 并验证单页约束与占位符。
5. [x] M5: 更新任务证据并提交当前任务相关文件。

## Expected Verification

- 真实 Word 导入后，预览区域中的单元格字体和高度明显受限。
- 空单元格可见文字占位符。
- 已提交模板的 `查看版式` 预览保持同样约束。

## Current Status

Completed for implementation and verification. 电子批记录本地模板预览现在会读取后端返回的紧凑显示约束，并在导入页预览和列表抽屉预览中对空单元格显示可填写占位符，且当前任务相关前端文件已整理为可独立提交范围。

## Final Verification Result

- `pnpm exec eslint src/views/mes/pro/batchrecordtemplate/TemplateLayoutPreview.vue src/views/mes/pro/batchrecordtemplate/index.vue src/api/mes/pro/batchrecordtemplate/index.ts` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-batch-record-single-page-layout-constraints\scripts\verify-batch-record-single-page-layout-constraints.mjs` -> PASS
