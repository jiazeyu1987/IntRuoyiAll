# 任务：集成展厅产品与展厅列表页

## 目标

将“产品管理”和“展厅管理”从统计概览行改为符合设计文档的真实列表页，分别渲染产品列表表格和展厅列表表格。

## 里程碑

- [x] 启动产品列表和展厅列表两个 worker 并完成第一轮审查
- [x] 打回不符合真实后端契约的 worker 结果
- [x] 补充集成失败测试
- [x] 集成 `ProductListTable` 与 `HallListTable`
- [x] 修复产品管理页首屏逐条 `getProduct` 的 N+1 请求
- [x] 运行结构测试、worker 测试和 ESLint
- [x] 提交本轮 N+1 修复变更

## 预期验证

- `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-hall-list.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/index.vue src/views/showroom-admin/components/ProductListTable.vue src/views/showroom-admin/components/HallListTable.vue scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-hall-list.test.mjs`
- `node --test scripts/showroom-admin-frontend.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/index.vue scripts/showroom-admin-frontend.test.mjs`

## 当前状态

已完成。

## 验证结果

- `D:\Programs\node.exe --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-hall-list.test.mjs` 通过，13 个测试全部通过。
- `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/index.vue src/views/showroom-admin/components/ProductListTable.vue src/views/showroom-admin/components/HallListTable.vue scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-hall-list.test.mjs` 通过。
- RED: `D:\Programs\node.exe --test scripts/showroom-admin-frontend.test.mjs` 失败，原因是 `src/views/showroom-admin/index.vue` 仍保留 `enrichProductRows`，产品首屏仍逐条调用 `getProduct(id)` 做详情 enrich。
- GREEN: `D:\Programs\node.exe --test scripts/showroom-admin-frontend.test.mjs` 通过，8 个测试全部通过。
- GREEN: `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/index.vue scripts/showroom-admin-frontend.test.mjs` 通过。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-showroom-admin-list-integration --mode preview` 通过，未发现需要清理的附属产物。
- Git 提交：`89158a26` `任务: 修复展厅产品页首屏N+1请求`

## 说明

- 产品管理页首屏改为直接消费真实 `getProductPage` 列表；`getProduct(id)` 仅在点击编辑时按需获取单条详情。
- 展厅管理页使用真实 `getHallPage` 列表渲染列表，并从 `productMappings` 派生产品数量与排序明细。
- 本任务未触碰前台 `showroom-frontstage` 文件。
