# 任务：展厅产品批量任务状态展示对齐

## 任务目标

- 将产品管理中的批量任务状态统一到固定任务区显示，并移除工具栏状态标签。

## 里程碑

- [x] M1：补齐静态测试口径。
- [x] M2：实现固定任务区与工具栏状态清理。
- [x] M3：完成静态验证、真实页面验证与 closeout preview。

## 预期验证

- `node --test scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-batch-cover-auto-resume.test.mjs`
- `node tests/e2e/showroom-product-toolbar-layout.spec.js`
- `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue src/views/showroom-admin/index.vue scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-batch-cover-auto-resume.test.mjs tests/e2e/showroom-product-toolbar-layout.spec.js`

## 当前状态

- 状态：已完成
- 主任务文档：`D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260522-showroom-product-batch-status-banner-align\task.md`
