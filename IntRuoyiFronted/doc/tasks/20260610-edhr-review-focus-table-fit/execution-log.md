# 执行日志

BDD: 聚焦已填写批记录 -> Given 用户打开 eDHR 批次复盘页, When 页面加载完成, Then 不显示顶部工具条、批次状态时间线、签名记录、审批记录和归档版本。

BDD: 表格完整压缩显示 -> Given 用户点击左侧任一有批记录工序, When 右侧显示模板表格, Then 表格宽度适配右侧容器且不出现横向滚动条。

RED: 代码检查 -> FAIL，当前 `BatchExecutionReviewPage.vue` 仍渲染 `edhr-batch-review__toolbar` / `edhr-batch-review__summary-grid`，`EdhrExecutionReadonlyForm.vue` 仍设置 `min-width: 960px` 和 `overflow: auto`。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

REGRESSION: Playwright 真实页面只读验证 `http://localhost:8081/mes/pro/feedback/edhr-batch-execution/review?id=32` -> PASS，`toolbarCount=0`、`summaryGridCount=0`，复盘页内不再显示 `批次状态时间线`、`签名记录`、`审批记录`、`归档版本`；左侧 `processCount=15`，右侧 `templateSheetCount=1`，表格容器 `sheetClientWidth=830`、`sheetScrollWidth=830`，无横向滚动。截图：`output/playwright/edhr-review-focus-table-fit.png`。
