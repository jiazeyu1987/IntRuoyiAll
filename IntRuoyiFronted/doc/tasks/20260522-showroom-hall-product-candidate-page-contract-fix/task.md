# 任务：展柜维护产品候选分页契约修复

## Goal

修复后台展柜维护产品弹窗加载候选产品时报错 `展柜产品候选缺少真实产品数组：productPage` 的问题，使 `HallProductMappingDialog` 按当前真实 `/showroom/product/page` 分页契约 `PageResult { total, list }` 读取 `productPage.list`，并继续允许用户维护展柜产品映射。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\HallProductMappingDialog.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-hall-operability.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-product-candidate-page-contract-fix\**`

## Non-Scope

- 不修改 `/showroom/product/page` 后端接口契约
- 不引入 fallback、兼容分支、mock 数据或静默降级
- 不顺带调整展柜映射排序、表格结构或其他产品管理工具栏行为

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-batch-status-banner-align\task.md`
- Status before this task: `已完成`
- Impact: 最近同仓前端任务已完成，不阻塞本次展柜维护产品候选契约修复

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在与本任务无关的已修改文件和未跟踪任务目录
- Impact: 本任务只允许修改映射弹窗、直接相关静态测试与本任务文档；提交时必须单独暂存本任务文件

## Milestones

- [x] M1: 检查前置任务状态并创建当前任务文档
- [x] M2: 先补 RED 回归测试，锁定 `productPage.list` 候选产品契约
- [x] M3: 最小修复映射弹窗分页读取逻辑
- [x] M4: 运行定向验证并记录 GREEN 证据
- [x] M5: 更新缺陷证据、任务状态并执行 closeout preview

## Expected Verification

- `node --test scripts/showroom-admin-product-hall-operability.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/components/HallProductMappingDialog.vue scripts/showroom-admin-product-hall-operability.test.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-hall-product-candidate-page-contract open http://127.0.0.1:8081/showroom/hall --headed`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-hall-product-candidate-page-contract run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-product-candidate-page-contract-fix\scripts\verify-showroom-hall-product-candidate-page-contract.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-product-candidate-page-contract-fix\bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-hall-product-candidate-page-contract-fix --mode preview`

## Current Status

Completed on 2026-05-22.

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-product-hall-operability.test.mjs`
- PASS: `pnpm exec eslint src/views/showroom-admin/components/HallProductMappingDialog.vue scripts/showroom-admin-product-hall-operability.test.mjs`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-hall-product-candidate-page-contract open http://127.0.0.1:8081/showroom/hall --headed` + `run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-product-candidate-page-contract-fix\scripts\verify-showroom-hall-product-candidate-page-contract.mjs`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-product-candidate-page-contract-fix\bug-regression-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-hall-product-candidate-page-contract-fix --mode preview`，preview 结果为 `ready`
