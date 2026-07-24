# 任务：展柜管理页按需跳过产品分页加载

## Goal

优化后台 `展柜管理` 页首屏加载行为，使进入 `/showroom/hall` 时不再顺带请求 `/showroom/product/page`，仅按当前 `activeSection` 加载真实必需数据，避免展柜页初始化额外等待。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-frontend.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-page-skip-product-page\**`

## Non-Scope

- 不修改后端接口
- 不改变 `维护产品` 弹窗已完成的单次候选查询接口
- 不新增 fallback、mock 或绕过真实数据的分支

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-product-options-single-query\task.md`
- Status before this task: `Completed on 2026-05-22`
- Impact: `维护产品` 单次候选查询优化已完成，不阻塞本次展柜页首屏按需加载优化

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在与本任务无关的进行中改动和未跟踪任务目录
- Impact: 本任务只允许修改后台壳页按需加载逻辑、相关静态测试和本任务文档；提交时必须单独暂存本任务文件

## Milestones

- [x] M1: 检查前置任务并创建任务文档
- [x] M2: 先补 RED 静态测试，锁定 hall 页不应首屏拉 product page
- [x] M3: 最小修改后台壳页按 section 加载数据
- [x] M4: 跑定向验证并记录 GREEN 结果

## Expected Verification

- `node --test scripts/showroom-admin-frontend.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/index.vue scripts/showroom-admin-frontend.test.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-hall-page-skip-product-page open http://127.0.0.1:8081/showroom/hall --headed`
- `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-hall-page-skip-product-page run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-page-skip-product-page\scripts\verify-showroom-hall-page-skip-product-page.mjs`

## Current Status

Completed on 2026-05-22.

## Verification Summary

- PASS: `node --test scripts/showroom-admin-frontend.test.mjs`
- PASS: `pnpm exec eslint src/views/showroom-admin/index.vue scripts/showroom-admin-frontend.test.mjs`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-hall-page-skip-product-page open http://127.0.0.1:8081/showroom/hall --headed` + `run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-page-skip-product-page\scripts\verify-showroom-hall-page-skip-product-page.mjs`
