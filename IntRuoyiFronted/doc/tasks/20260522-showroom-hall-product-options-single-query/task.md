# 任务：展柜维护产品改为单次候选查询

## Goal

将后台 `展柜管理 -> 维护产品` 弹窗从“前端逐页串行拉取 `/showroom/product/page` 全量分页候选”改为“调用后端专用轻量候选接口一次获取产品候选”，显著降低打开弹窗的等待时间，同时保持真实产品集合与展柜映射契约不变。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\showroom-admin\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\HallProductMappingDialog.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\hall\contracts.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-hall-operability.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-product-options-single-query\**`

## Non-Scope

- 不修改展柜产品保存接口 `/showroom/hall/update-product-mapping`
- 不调整展柜列表、产品详情页或其他批量工具栏行为
- 不引入 fallback、mock 数据或静默降级

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-product-mapping-slow-diagnosis\task.md`
- Status before this task: `Completed on 2026-05-22`
- Impact: 已确认当前慢点来自逐页串行拉取完整产品池，不阻塞本次单次候选查询改造

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在与本任务无关的进行中改动和未跟踪任务目录
- Impact: 本任务只允许修改展柜维护产品弹窗、直接相关 API/测试和本任务文档；提交时必须单独暂存本任务文件

## Milestones

- [x] M1: 检查前置任务并创建前端任务文档
- [x] M2: 先补 RED 静态回归测试，锁定弹窗必须使用专用候选接口
- [x] M3: 最小修改前端 API 与弹窗加载逻辑
- [x] M4: 运行定向验证并记录 GREEN 证据

## Expected Verification

- `node --test scripts/showroom-admin-product-hall-operability.test.mjs`
- `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/components/HallProductMappingDialog.vue src/views/showroom-admin/hall/contracts.ts scripts/showroom-admin-product-hall-operability.test.mjs`

## Current Status

Completed on 2026-05-22.

## Verification Summary

- PASS: `node --test scripts/showroom-admin-product-hall-operability.test.mjs`
- PASS: `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/components/HallProductMappingDialog.vue src/views/showroom-admin/hall/contracts.ts scripts/showroom-admin-product-hall-operability.test.mjs`
- PASS: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
- PASS: live Playwright verification on `http://127.0.0.1:8081/showroom/hall`
