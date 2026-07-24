# 执行记录

## BDD

- BDD: 历史页只显示已归档批记录 -> Given 系统存在不同状态的 eDHR 批次, When 用户进入历史批记录页签, Then 页面只请求并显示状态为已归档的批次。
- BDD: 点击历史批次显示工序 -> Given 用户在历史批记录页选中一条已归档批次, When 右侧详情加载完成, Then 用户看到该批次下有批记录执行的工序列表。
- BDD: 点击工序显示模板表格 -> Given 右侧工序列表包含 B010, When 用户点击 B010, Then 右侧显示该工序对应的已填写模板表格，不显示原始 JSON。
- BDD: 切换工序只显示当前表格 -> Given 用户正在查看某一道工序的模板表格, When 用户点击另一道工序, Then 右侧切换为新工序的单张模板表格。

## RED / GREEN / REGRESSION

- RED: `node tests/e2e/edhr-batch-history-static.spec.js` -> FAIL，缺少 eDHR 历史批记录页面 `BatchRecordHistoryPage.vue`。
- GREEN: `node tests/e2e/edhr-batch-history-static.spec.js` -> PASS，页面、路由、已归档过滤、复盘接口和只读模板组件检查通过。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: Playwright 真实只读 E2E -> PASS，登录 `测试租户/aoteman`，打开 `/mes/pro/feedback/edhr-batch-history`，分页接口返回 3 条已归档批次，选中批次后显示 15 个工序，页面无默认 `<pre>` JSON。
- REGRESSION: 现有 `eDHR批次执行` 页面保留原列表、打开/创建、详情、复盘、归档、下载操作，仅新增顶部页签跳转。
