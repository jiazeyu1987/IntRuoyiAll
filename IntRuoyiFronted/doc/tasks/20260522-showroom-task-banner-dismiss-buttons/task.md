# 任务：展厅批量任务卡片增加关闭按钮

## 任务目标

- 为 showroom 前端任务卡片增加关闭与重显交互。

## 里程碑

- [x] M1：补齐静态测试口径。
- [x] M2：实现任务卡片关闭与重显交互。
- [x] M3：完成定向验证与 closeout preview。

## 预期验证

- `node --test scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-batch-cover-auto-resume.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-batch-cover-auto-resume.test.mjs`

## 当前状态

- 状态：已完成
- 主任务文档：`D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260522-showroom-task-banner-dismiss-buttons\task.md`
