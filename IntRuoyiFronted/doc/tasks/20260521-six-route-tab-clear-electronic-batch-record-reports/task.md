# Task: 六路识别页签增加一键清空电子批记录报表按钮

## Goal

在 `报表管理 -> 报表设计器 -> 六路识别` 页签中增加一个“清空电子批记录报表”按钮。用户确认后，前端调用新的后端批量删除接口，清空 `电子批记录` 文件夹下所有报表，并刷新当前列表与删除结果提示。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\report\jmreport\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\mes\pro\batchrecordreport\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\report-management-six-route-page.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-six-route-tab-clear-electronic-batch-record-reports\**`

## Non-Scope

- 不改动 `报表设计器` iframe 页内布局。
- 不新增测试专用按钮或隐藏入口。
- 不做与本次按钮无关的视觉重构。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-bilingual-tabs-real-e2e\task.md`
- Status before this task: `Completed with backend runtime blocker on 2026-05-21`
- Impact: 上一同仓库任务已完成并已记录 blocker，不阻塞本次报表页签功能交付。

## Milestones

1. 建立任务文档并确认六路识别页签结构、API 封装与现有测试方式。
2. 先写失败前端源码契约测试，覆盖批量删除 API 和按钮入口。
3. 最小实现按钮、确认弹窗、删除调用、结果提示与列表刷新。
4. 跑源码契约、类型检查、真实页面回归与 closeout preview，完成任务范围提交。

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\report-management-six-route-page.test.mjs`
- `pnpm --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-six-route-tab-clear-electronic-batch-record-reports\frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-six-route-tab-clear-electronic-batch-record-reports --mode preview`

## Current Status

Completed on 2026-05-22 after explicit user return to this report-management request.

## Completed Work

1. 在六路识别工具栏新增 `清空电子批记录报表` 危险操作按钮。
2. 新增前端 API `deleteAllGeneratedReports()` 对接 `DELETE /mes/pro/batch-record-report/delete-all`。
3. 新增确认弹窗、删除数量提示和删除后列表刷新。
4. 扩展源码契约测试，并补齐真实 Playwright 闭环脚本。

## Final Verification

- PASS: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\report-management-six-route-page.test.mjs`
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 ts:check`
- PASS: Playwright 真实链路完成“登录 -> 清空 -> A 路生成 -> 再次清空 -> 列表归零”
- PASS: screenshot `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\six-route-clear-button-20260521.png`
