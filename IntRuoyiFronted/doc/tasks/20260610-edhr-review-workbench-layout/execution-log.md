# 执行日志

BDD: 顶部证据区保留 -> Given 一个已完成 eDHR 批次, When 打开复盘页, Then 顶部仍可看到批次时间线、签名、审批、归档信息。

BDD: 左侧显示有批记录工序 -> Given 批次有 15 张已填写批记录, When 查看“已填写批记录”, Then 左侧显示 15 个有批记录的工序。

BDD: 点击 B010 查看产品信息表 -> Given 用户打开复盘工作台, When 点击左侧 `B010 吹球囊成型`, Then 右侧显示 `产品信息` 模板表格和该表填写值。

BDD: 点击 B320 切换末道工序 -> Given 用户正在查看 B010, When 点击左侧 `B320 球囊测漏及全检`, Then 右侧切换为 B320 对应模板表格且页面只显示 1 张模板表格。

RED: `rg -n "el-collapse|expandedExecutionKeys|edhr-batch-review__collapse" src\views\mes\pro\edhr-batch\BatchExecutionReviewPage.vue` -> FAIL，当前页面仍使用 `el-collapse` 纵向展示多张表，不支持左工序右表格。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

REGRESSION: Playwright 真实页面只读验证 `http://localhost:8081/mes/pro/feedback/edhr-batch-execution/review?id=32` -> PASS，左侧工序 `processCount=15`，点击 `B010` 后右侧 `templateSheetCount=1` 且可见 `产品信息` / `产品名称` / `E2E模拟填写-881MO090863-20260610-104136`，点击 `B320` 后当前高亮切换为 `B320` 且右侧仍只显示 1 张模板表格。截图：`output/playwright/edhr-review-workbench-layout.png`。
