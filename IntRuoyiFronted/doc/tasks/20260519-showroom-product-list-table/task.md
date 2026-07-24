# 任务：展厅后台产品列表表格化

## 目标

新增独立的 `ProductListTable` 组件，用真实产品列表数据渲染展厅后台产品列表表格，覆盖公司归属、生命周期、资料状态和审批状态等可见信息，替代当前“产品详情表 / N 个产品”的统计行表达。

## 里程碑

- [x] 记录 BDD 场景与 TDD 证据
- [x] 补充 RED 测试，锁定组件列与文案要求
- [x] 实现 `ProductListTable.vue` 的最小可用表格渲染
- [x] 运行 `node --test` 与 ESLint 验证
- [x] 提交本任务直接相关改动

## 预期验证

- `node --test scripts/showroom-admin-product-list.test.mjs`
- `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs`

## 当前状态

已完成。

## 验证结果

- RED: `D:\Programs\node.exe --test scripts/showroom-admin-product-list.test.mjs` 失败，原因是 `ProductListTable.vue must exist`。
- GREEN: `D:\Programs\node.exe --test scripts/showroom-admin-product-list.test.mjs` 通过，4 个测试全部通过。
- ESLint: `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs` 通过。
- Review Round 1 RED: `D:\Programs\node.exe --test scripts/showroom-admin-product-list.test.mjs` 失败，原因是组件仍依赖无真实来源的 `ownerCompanyName` / `updatedAt`，且缺少 `productCode` / `currentRevisionId` / `owner_company_id` / `revisionNo` 等真实契约断言。
- Review Round 1 GREEN: `D:\Programs\node.exe --test scripts/showroom-admin-product-list.test.mjs` 通过，5 个测试全部通过。
- Review Round 1 ESLint: `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs` 通过。

## 集成说明

- 本任务未修改 `src/views/showroom-admin/index.vue`。
- 主 agent 需要在产品页集成 `ProductListTable`，用 `ShowroomAdminApi.getProductPage` 的 snapshot 结合 `ShowroomAdminApi.getProduct(id)` 的 revision 组装 enriched row 后传入 `products`。
- 支持的 row 契约为：`productId`、`productCode`、`currentRevisionId`、`incomplete`、`live`，以及 `revision.nameCn`、`revision.nameEn`、`revision.status`、`revision.revisionNo`、`revision.fields.owner_company_id`、`revision.fields.product_owner_type`、`revision.fields.lifecycle_stage`。

## 收尾结果

- `task-closeout-cleanup` 预览已运行，未删除文件。
- 预览阻塞原因：脚本检测主分支为 `master`，但当前没有 `master` 的 checked-out worktree；本任务按用户要求保留独立 worktree 供主 agent 集成。
