# 任务：展厅产品管理增加一键卖点

## Goal

在 `showroom/product` 产品管理页增加一个 `一键卖点` 批量入口，支持按当前筛选范围一次性补齐产品当前版本缺失的中文核心卖点和英文核心卖点。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\ProductListTable.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\showroom-admin\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-list.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-frontend.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\showroom-product-toolbar-layout.spec.js`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-batch-selling-points\**`

## Non-Scope

- 不改动 `showroom/product` 以外的业务页面。
- 不新增 mock 数据、默认成功提示或前端 fallback。
- 不改动现有 `一键讲解 / 一键语音 / 一键封面` 的既有入口语义。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-cover-skip-503-errors\task.md`
- Status before this task: `Completed`
- Impact: 前一任务已完成且仅为前端影响记录，不阻塞本次继续交付 showroom 产品管理新入口。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在 showroom、DCC、排程日历等并行未提交改动。
- Impact: 本任务仅允许修改 showroom 产品管理批量卖点入口、定向测试与本任务文档，不覆盖无关在途改动。

## Milestones

1. 建立任务文档并确认当前产品管理工具栏、批量动作和状态展示契约。
2. 先补 RED，锁定“一键卖点按钮可见、事件接线正确、调用专用接口”的前端可观察行为。
3. 最小实现工具栏按钮、交互确认、加载态、接口调用与结果反馈。
4. 跑定向源码回归、静态检查与必要的真实页面验证。
5. 回写证据并执行 closeout preview。

## Expected Verification

- `node --test scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs`
- `node tests/e2e/showroom-product-toolbar-layout.spec.js`
- `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue src/views/showroom-admin/index.vue src/api/showroom-admin/index.ts scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs tests/e2e/showroom-product-toolbar-layout.spec.js --format stylish`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-batch-selling-points\frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-product-batch-selling-points --mode preview`

## Current Status

Completed on 2026-05-22.

## Completed Work

- 已定位产品管理批量能力入口落在 `ProductListTable.vue` 与 `showroom-admin/index.vue`。
- 已确认当前前端仅存在 `一键讲解 / 一键语音 / 一键封面`，不存在 `一键卖点` 接口接线。
- 已确认列表卖点状态当前读取 `displayRevision.fields`，因此本次必须明确批量卖点生成的真实链路与反馈方式。
- 已在产品管理工具栏中新增 `一键卖点` 按钮，并接入独立 loading 状态与确认弹窗。
- 已为前端 API 层新增 `batchGenerateProductSellingPoints(...)` 契约和卖点批量响应类型。
- 已补齐源码断言、工具栏回归和真实入口验证脚本。

## Verification Result

- PASS: `node --test scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs`
- PASS: `node tests/e2e/showroom-product-toolbar-layout.spec.js`
- PASS: `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue src/views/showroom-admin/index.vue src/api/showroom-admin/index.ts scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs tests/e2e/showroom-product-toolbar-layout.spec.js --format stylish`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-batch-selling-points run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-batch-selling-points\scripts\verify-showroom-product-batch-selling-points.mjs`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-batch-selling-points\frontend-feature-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-product-batch-selling-points --mode preview`

## Notes

- `pnpm ts:check` 仍失败，但当前仅剩仓内既有、与本任务无关的类型问题：
  - `src/views/showroom-admin/narration/NarrationWorkspace.vue`
  - `src/views/showroom-frontstage/shared/payload.ts`
