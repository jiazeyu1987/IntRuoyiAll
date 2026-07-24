# 执行日志：MES 报工列表合并筛选

BDD: 合并正式报工筛选 -> Given 用户打开 MES 正式报工列表 / When 查看筛选栏 / Then 原先报工编号、报工单号、报工类型、生产工单、产品物料、报工人、记录人、状态、报工时间等平铺筛选合并为一个筛选类型下拉和一个动态筛选值控件。
BDD: 筛选类型决定输入控件 -> Given 用户选择不同筛选类型 / When 筛选值区域渲染 / Then 编号和单号显示文本输入，类型和状态显示字典下拉，生产工单、产品物料、报工人、记录人显示原业务选择器，报工时间显示日期范围。
BDD: 查询参数保持兼容 -> Given 用户选择某个筛选类型并输入筛选值 / When 点击搜索 / Then 仅对应的旧查询参数被提交，其他旧筛选参数被清空，路由传入 feedbackId/status 仍可设置对应旧查询参数。

RED: node tests/e2e/mes-feedback-merged-filter-static.spec.js -> FAIL, AssertionError: 正式报工筛选必须通过 feedbackFilterFields 集中声明可选筛选类型。
GREEN: node tests/e2e/mes-feedback-merged-filter-static.spec.js -> PASS
GREEN: node tests/e2e/mes-feedback-tracking-static.spec.js -> PASS
GREEN: NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check -> PASS

GREEN: frontend-feature-evidence-validation -> PASS
GREEN: task-closeout-cleanup-preview -> PASS, delete only frontend-feature-evidence.md

GREEN: task-closeout-cleanup-apply -> PASS, removed temporary frontend-feature-evidence.md

