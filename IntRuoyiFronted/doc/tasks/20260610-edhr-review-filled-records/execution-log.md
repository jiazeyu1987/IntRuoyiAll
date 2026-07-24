# eDHR 批次复盘已填写批记录视图执行日志

- BDD: 已完成批次复盘 -> Given 批次 `E2E-881MO090863-20260610-104136` 已完成并归档 When 用户打开复盘页 Then 页面应显示“已填写批记录”，并按工序展示已填写的只读表单内容。
- BDD: 不再默认展示原始 JSON -> Given 用户打开复盘页 When 页面加载完成 Then 页面默认不应出现大块 `<pre>` 原始 JSON。
- BDD: 关键工序抽查 -> Given 批次包含 B010、B200、B320 对应批记录 When 打开复盘页 Then 页面可看到三张表的工序信息和填写值。
- RED: 旧页面行为 -> FAIL, expected reason: 旧页面使用 `<pre>{{ formatJson(...) }}</pre>` 展示批次事件、任务事件、签名和审批 JSON，无法直接复盘已填写表单。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: Playwright real browser -> PASS，登录 `芋道源码/admin`，打开 `http://localhost:8081/mes/pro/feedback/edhr-batch-execution/review?id=32`，断言 `preCount=0`、`visibleProcessCodeCount=15`、`B010/B200/B320` 与 `E2E模拟填写-881MO090863-20260610-104136` 可见。
- REGRESSION: 批次状态时间线、签名记录、审批记录、归档版本仍在复盘页可见。

## 证据

- 截图：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\edhr-review-filled-records.png`
