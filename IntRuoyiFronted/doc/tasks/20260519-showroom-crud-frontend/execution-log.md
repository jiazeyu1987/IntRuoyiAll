# 执行记录：展厅产品与展厅 CRUD 前端

BDD: 产品管理完整 CRUD -> Given 用户进入产品管理 When 页面加载 Then 可查找、新增、编辑、删除产品，列表请求 `pageSize` 不超过 20。

BDD: 展厅管理完整 CRUD -> Given 用户进入展厅管理 When 页面加载 Then 可查找、新增、编辑、删除展厅，列表请求 `pageSize` 不超过 20。

BDD: 后台 banner 删除 -> Given 用户进入产品管理或展厅管理 When 页面渲染 Then 不出现“展厅后台 / 结构化内容、审批、指派和讲解资产统一管理”顶部 banner。

RED: `D:\Programs\node.exe --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-hall-list.test.mjs` -> FAIL, expected because `showroom-admin-toolbar` still existed, product/hall lists lacked create/search/delete actions, and delete API methods were missing.

GREEN: `D:\Programs\node.exe --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-hall-list.test.mjs` -> PASS, 14 tests.

GREEN: `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\.bin\eslint.cmd src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue src/views/showroom-admin/components/ProductListTable.vue src/views/showroom-admin/components/HallListTable.vue scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-hall-list.test.mjs` -> PASS.

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vite build --mode env.local` -> PASS.

REGRESSION: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> FAIL, repository baseline has many pre-existing auto-import type errors such as `ref`, `computed`, and `useMessage` missing across unrelated files; not introduced by this task.
