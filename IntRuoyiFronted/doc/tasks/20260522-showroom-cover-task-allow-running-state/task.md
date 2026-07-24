# 任务：展厅一键封面显示允许状态与当前运行产品

## 任务目标

- 为 showroom 前端固定任务区显示一键封面允许状态与当前运行产品。

## 里程碑

- [x] M1：补齐静态测试口径。
- [x] M2：实现封面任务区状态读取、轮询与展示。
- [x] M3：完成静态验证、真实页面验证与 closeout preview。

## 预期验证

- `node --test scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-batch-cover-auto-resume.test.mjs`
- `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/components/ProductListTable.vue src/views/showroom-admin/index.vue scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-batch-cover-auto-resume.test.mjs`

## 当前状态

- 状态：已完成
- 主任务文档：`D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260522-showroom-cover-task-allow-running-state\task.md`
