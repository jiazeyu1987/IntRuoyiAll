# Task: 工艺路线关联物品支持点击跳转

## Goal

在 MES 工艺路线详情弹窗的“关联产品”页签中，将关联物品编码做成可点击链接；用户点击后跳转到 MES 物料产品页面，并自动打开对应物料的详情。

## Scope

- 先创建当前前端任务文档，再开始生产代码修改。
- 严格按 BDD + TDD 先补失败验证，再做最小实现。
- 仅修改工艺路线关联产品列表、物料产品页跳转承接逻辑、必要的前端验证脚本与证据文档。
- 保持现有后端接口、物料详情弹窗、工艺路线详情弹窗和视觉样式不变。
- 不引入 fallback 文案、伪跳转或额外测试专用控件。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260518-dcc-process-detail-stage-name-normalization/task.md`
- Status before this task: blocked / on hold.
- Impact: previous task was explicitly paused due priority switch before any production code change, so it does not block this new route-product navigation task.

## Milestones

- [x] M1: Create task package and record the navigation target before code edits.
- [x] M2: Add RED verification for clickable route-product item navigation.
- [x] M3: Implement the smallest route-to-item navigation flow.
- [x] M4: Run targeted verification and update evidence.
- [ ] M5: Commit only task-scoped files after required verification passes.

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-route-product-item-link\scripts\verify-route-product-item-link.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- `npx.cmd --yes --package @playwright/cli playwright-cli -s=route-product-item-link-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-route-product-item-link\scripts\verify-route-product-item-link-e2e.mjs`

## Current Status

Completed. Route-product item codes now navigate to `MesMdItem`, and the item page auto-opens the target item detail from query state.

## Final Verification Result

- PASS: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-route-product-item-link\scripts\verify-route-product-item-link.mjs`
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli -s=route-product-item-link-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-route-product-item-link\scripts\verify-route-product-item-link-e2e.mjs`

## Blocker And Impact

- Blocking reason: none.
- Impact: none.
