# 任务：展厅产品 Codex CLI 讲解稿与双语语音生成

## 目标

在 `展厅 / 产品管理` 的产品详情里新增讲解稿文本框与 `生成讲解稿` 按钮，点击后用当前配置的 Codex CLI 根据产品基础资料生成中文讲解稿；并提供 `生成语音` 入口，基于当前中文讲解稿生成中文语音和翻译后的英文语音。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\showroom-admin\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\ProductListTable.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\product\ProductDetailDialog.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\product\contracts.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-frontend.test.mjs`
- 需要时补充的产品详情/列表定向测试
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-codex-bilingual-narration\**`

## Non-Scope

- 不重做展厅后台整体布局。
- 不新增独立的产品讲解页面路由。
- 不为了测试额外添加 fake 控件、mock 数据或 fallback 文案。

## 前置任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-test-tenant-login-baseline\task.md`
- 启动前状态：已完成。
- 影响：可以独立开展当前前端产品讲解稿与语音入口交付。

## 里程碑

- [x] M1：创建前端任务文档并记录 BDD/TDD 基线。
- [x] M2：先补 RED 测试，锁定产品详情讲解稿文本框、生成讲解稿按钮与生成语音入口。
- [x] M3：实现产品详情讲解稿编辑区与 API 接入。
- [x] M4：调整产品列表生成按钮语义并完成联动刷新。
- [x] M5：完成验证、证据记录与收尾预览。

## 预期验证

- `node --test scripts/showroom-admin-frontend.test.mjs`
- 需要时补充的产品详情定向测试
- 若运行库允许，真实浏览器从 `http://localhost:8081/showroom/product` 进入产品详情并验证讲解稿/语音按钮

## Current Status

Completed on 2026-05-20. 产品详情讲解稿编辑区、`生成讲解稿`、`生成语音` 入口均已接入，真实 Playwright 路径完成登录、打开产品详情、生成中文讲解稿并触发双语语音生成。

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-product-narration-editor.test.mjs scripts/showroom-admin-product-detail-entry.test.mjs scripts/showroom-admin-product-company-field-layout.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs`
- PASS: `npx.cmd eslint src/api/showroom-admin/index.ts src/views/showroom-admin/product/ProductDetailDialog.vue scripts/showroom-admin-product-narration-editor.test.mjs`
- PASS: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-codex-bilingual-narration\frontend-feature-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-showroom-product-codex-bilingual-narration --mode preview`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-codex-narration run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-codex-bilingual-narration\scripts\verify-product-narration-dialog.mjs`，真实页面完成产品详情打开、讲解稿生成与双语语音生成，返回 `generatedNarrationVersionId=10`、`savedNarrationVersionId=11`、`zhNarrationVersionId=11`、`enNarrationVersionId=12`、`scriptLength=166`。

## Blockers

- 无当前 blocker。
