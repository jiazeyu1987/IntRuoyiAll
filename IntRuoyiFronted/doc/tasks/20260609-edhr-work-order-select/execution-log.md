# eDHR 批次执行工单选择改造执行日志

## 2026-06-09

- BDD: 选择有效未冻结工单 -> Given 用户打开 eDHR 批次执行的打开/创建弹窗 When 用户输入工单号关键字 Then 系统只查询已确认且未临时冻结的生产工单，并以下拉方式让用户选择。
- BDD: 防止无效工单提交 -> Given 用户未选择下拉中的有效工单 When 点击确认 Then 前端阻止提交并提示必须选择有效生产工单。
- BDD: 后端阻止冻结工单 -> Given 调用方绕过前端直接传入未确认或临时冻结工单 When 调用 openOrCreate Then 后端 fail fast，不创建 eDHR 批次执行。
- RED: `node tests\e2e\edhr-batch-work-order-select-static.spec.js` -> FAIL, expected reason: `BatchExecutionListPage.vue` 仍使用工单ID手填输入框，没有远程工单选择器。
- GREEN: `node tests\e2e\edhr-batch-work-order-select-static.spec.js` -> PASS。
- GREEN: Playwright 真实前端只读验证 -> PASS，测试租户打开“打开/创建”弹窗，前端请求 `/mes/pro/work-order/page?pageNo=1&pageSize=20&status=1&temporaryFrozen=false`，未提交创建。
- REGRESSION: `pnpm e2e:edhr:batch-execution:check` -> PASS。
- REGRESSION: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
