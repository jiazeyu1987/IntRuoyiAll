# Execution Log：排产工单工单编码支持跳转生产工单

- `2026-06-30 任务创建`：建立前端任务文档，准备按严格 TDD 为排产工单工单编码补齐生产工单跳转。
- `BDD: 排产工单工单编码可跳到生产工单详情 -> Given 用户在排产工单列表看到一条已关联生产工单的排产记录 / When 点击该行工单编码 / Then 页面跳转到生产工单页，并自动按该工单编码筛选且打开目标工单详情。`
- `BDD: 缺少工单编码时不渲染假链接 -> Given 某排产工单行没有有效工单编码 / When 页面渲染该列 / Then 页面只显示占位文本，不渲染可点击链接。`
- `RED: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-workorder-link-static.spec.js -> FAIL`，当前排产工单“工单编码”仍是普通文本，缺少 `openScheduleWorkOrder(row)` 链接跳转实现。
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-workorder-link-static.spec.js -> PASS`
- `GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-schedule-order-workorder-link\frontend-feature-evidence.md -> PASS`
