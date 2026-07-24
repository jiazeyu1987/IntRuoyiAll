# Task: 电子批记录双页签导入分析

## Goal

将前端 `电子批记录` 页面改造为双页签结构：`模板列表` 与 `文件解析导入`。首期只支持 `.doc/.docx` 文件，在本地页内完成上传、解析结果展示、候选勾选、只读版式预览与提交后回列表。

## Scope

- 在前端仓库创建任务文档并记录 BDD/TDD 证据。
- 替换当前电子批记录页对 `batchrecordreport` 的主流程依赖，改接本地模板 `page/delete/import/parse/import/commit` 接口。
- 新增只读版式预览组件与候选分析页签状态。
- 保留现有 `mode=designer` 分支文件但不再作为主流程入口。
- 不做图片导入、不做页内编辑、不做手工改单元格。

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260516-dcc-upload-approval-persistence-real-e2e/task.md`
- Status before this task: blocked by backend runtime prerequisite failure.
- Impact: that DCC real E2E blocker is independent and does not block this MES
  page implementation.

## BDD Scenarios

- BDD: 默认展示模板列表页签 -> Given 用户进入 `/mes/pro/batch-record-template`, When 页面初始化, Then 默认显示 `模板列表` 页签并加载本地模板列表。
- BDD: Word 文件解析展示候选 -> Given 用户切到 `文件解析导入` 页签并上传有效 `.doc/.docx`, When 前端调用本地 `parse` 接口, Then 页面展示导入摘要、候选表列表和右侧只读版式预览。
- BDD: 候选勾选后提交成功回列表 -> Given 用户已获得解析候选, When 勾选候选并提交, Then 前端调用本地 `commit` 接口、清空导入状态、切回 `模板列表` 并刷新列表。
- BDD: 无导入权限时不可进入导入页签 -> Given 用户缺少 `mes:pro-batch-record-template:import` 权限, When 用户打开页面或手动带 `tab=import`, Then 前端隐藏导入页签并强制回退到 `模板列表`。
- BDD: 不支持扩展名时快速失败 -> Given 用户在导入页签选择非 `.doc/.docx` 文件, When 前端校验文件, Then 页面显示明确错误且不调用后端解析接口。

## Milestones

1. [x] M1: 创建任务包并记录 BDD 场景。
2. [x] M2: 先写失败的前端类型检查或目标验证基线。
3. [x] M3: 落本地模板 API、双页签页面、只读预览与权限控制。
4. [x] M4: 运行前端静态校验与真实路径验证并记录 GREEN。
5. [ ] M5: 仅提交当前任务相关前端改动。

## Expected Verification

- 页面默认打开 `模板列表`，可切换到 `文件解析导入`。
- 上传 `.doc/.docx` 后出现导入摘要、候选列表、右侧只读版式预览。
- 清空勾选后提交会被明确阻止。
- 提交成功后自动回列表且新模板出现在顶部。
- 无导入权限时看不到导入页签并且 `tab=import` 被回退。
- 无删除权限时看不到删除操作。

## Current Status

Implemented with one unrelated verification blocker. 目标页签、Word 解析展示、只读版式预览、提交回列表、查看版式与删除清理的真实路径均已跑通；但全量 `pnpm ts:check` 被仓库内现有 `src/types/auto-components.d.ts` 非法声明阻塞，尚未处理该与本任务无关的生成文件基线问题。

## Final Verification Result

- `pnpm exec eslint src/views/mes/pro/batchrecordtemplate/index.vue src/views/mes/pro/batchrecordtemplate/TemplateLayoutPreview.vue src/api/mes/pro/batchrecordtemplate/index.ts` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-electronic-batch-record-import-analysis-tabs\scripts\verify-electronic-batch-record-import-analysis-tabs.mjs` -> PASS
- `node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> BLOCKED by unrelated tracked file `src/types/auto-components.d.ts`
