# 任务：展厅产品管理增加全部发布按钮

## 任务目标

- 为 showroom 前端产品管理页新增 `全部发布` 按钮，并接入批量发布结果反馈。

## 里程碑

- [x] M1：补齐工具栏入口与静态测试。
- [x] M2：接入批量发布 API、确认提示与结果弹窗。
- [x] M3：完成前端静态验证与真实路径验证。

## 预期验证

- `node --test scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs`
- `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/components/ProductListTable.vue src/views/showroom-admin/index.vue scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs`

## 当前状态

- 状态：已完成
- 主任务文档：`D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260522-showroom-product-batch-publish-button\task.md`
