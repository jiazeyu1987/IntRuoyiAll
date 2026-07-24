# 执行日志：eDHR 批次模板预览入口

## 2026-06-26

- 初始化任务：根据用户说明，在 eDHR 批次执行列表的 `复盘` 右边新增 `模板` 入口，并新增左侧全量模板工序、右侧模板说明表格的只读页面。
- BDD: 模板入口可见 -> Given 用户打开 eDHR 批次执行列表 When 查看某行批次操作 Then 在 复盘 右边可看到 模板 按钮。
- BDD: 模板页展示全量模板工序 -> Given 当前批次详情中存在多个带 batchRecordReportId 的任务 When 打开模板页 Then 左侧按工序顺序展示全部带模板任务，而不是只显示已填写工序。
- BDD: 模板页显示单元格用途 -> Given 用户选中某张模板 When 右侧渲染模板表格 Then 可填写和签名单元格直接显示 文字/数字/日期/日期时间/勾选/签名/附件 等中文用途提示与关键规则。
- BDD: 模板规则缺失明确报错 -> Given 所选模板缺少有效布局或单元格规则 When 模板页加载 Then 页面明确显示错误，不静默降级成空模板。
- RED: `node tests/e2e/edhr-batch-template-preview-static.spec.js` -> FAIL，断言 `必须新增批次模板预览页 BatchExecutionTemplatePage.vue` 失败，确认模板页与模板说明组件尚未实现。
- CHANGE：新增共享模板规则工具 `src/views/mes/pro/batchrecordtemplate/batchRecordTemplateRules.ts`，统一单元格类型中文映射、默认控件映射、规则归一化、附件规则清洗与合并单元格解析。
- CHANGE：`src/views/mes/pro/batchrecordtemplate/index.vue` 改为复用共享模板规则工具，避免新模板说明页再复制一套类型字典与 merge 解析逻辑。
- CHANGE：`src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue` 新增 `模板` 按钮和 `openTemplate(row)` 跳转。
- CHANGE：`src/router/modules/remaining.ts` 新增隐藏路由 `MesProEdhrBatchExecutionTemplate`，路径 `/mes/pro/feedback/edhr-batch-execution/template`。
- CHANGE：新增 `src/views/mes/pro/edhr-batch/BatchExecutionTemplatePage.vue`，先调用 `getEdhrBatchExecution(id)` 读取批次详情，再从 `detail.tasks` 中筛出全部 `batchRecordReportId` 非空任务，按 `routeProcessSort -> batchRecordSort -> id` 排序；选中任务后并行调用 `BatchRecordReportApi.getCellRules(reportId)` 与 `BatchRecordReportApi.getSignatureCellMarkers(reportId)`，并按 `reportId` 缓存。
- CHANGE：新增 `src/views/mes/pro/edhr/components/EdhrExecutionTemplateGuide.vue`，按原模板表格渲染静态文本，并在可填写/签名单元格内直接显示中文用途与规则摘要；缺少模板布局或规则时 fail-fast 报错。
- GREEN: `node tests/e2e/edhr-batch-template-preview-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-batch-history-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-inline-signature-cells-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-batch-review-summary-labels-static.spec.js` -> PASS
- BLOCKER: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> FAIL，当前仓本地 `node_modules` 缺少 `@volar/typescript/lib/quickstart/runTsc`，`vue-tsc` 无法启动，属于工具链前置条件缺失，非本次模板改动引入。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-batch-template-preview\frontend-feature-evidence.md` -> PASS
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-edhr-batch-template-preview --mode preview` -> READY，默认 keep `task.md` / `execution-log.md`，delete 为 `frontend-feature-evidence.md`。
