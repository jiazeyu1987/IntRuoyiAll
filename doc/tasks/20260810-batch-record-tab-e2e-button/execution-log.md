# Execution Log

## User Intent

- 用户要求在截图红框位置新增 E2E 按钮。
- 点击后从当前选中的批记录测试 Tab 内，从前到后依次执行 E2E；后续 E2E 要依据前一个 E2E 的结果，所以必须顺序执行。

## BDD

- BDD: 当前 Tab E2E 顺序执行 -> Given 用户选择测试租户并停留在任一批记录测试内部 Tab, When 点击顶部 E2E 按钮, Then 页面按该 Tab 完整行集合从前到后逐个启动 Playwright E2E，后一项启动前必须等待前一项正式终态。
- BDD: E2E 结果依赖传递 -> Given 第 N 项 E2E 已返回正式终态, When 第 N+1 项 E2E 启动, Then 第 N+1 项的测试数据上下文包含第 N 项 executionId、状态和摘要信息。
- BDD: E2E 与现有测试互斥 -> Given 单行测试、测试全部或 E2E 正在执行, When 用户再次点击任一测试入口, Then 页面阻止重入并保持行级历史归属不串行。

## Milestone Updates

- M1 规则与现状定位：读取 frontend-feature-delivery 技能、前端/E2E/任务/编码规则，定位目标页面为 IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue。
- RED: node tests/e2e/edhr-batch-record-test-tab-e2e-static.spec.cjs -> FAIL, expected reason: 当前页面 5 个 Tab 顶部尚未提供 data-edhr-batch-record-test-e2e-button。
