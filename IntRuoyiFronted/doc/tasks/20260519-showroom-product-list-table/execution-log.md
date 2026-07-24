# 执行记录：展厅后台产品列表表格化

## BDD 场景

- BDD: 产品列表表格按真实数据渲染 -> Given 提供真实产品列表数据 When 渲染 `ProductListTable` Then 页面显示中文名称、英文名称、所属公司、产品归属/类型、生命周期、资料状态、审批状态、更新时间和操作列。
- BDD: 资料未完善状态显式可见 -> Given 某产品资料不完整 When 渲染表格 Then 表格中必须出现“资料未完善”的明确标签或文本。
- BDD: 产品页主体不是统计行 -> Given 产品列表页组件 When 读取源码与渲染结构 Then 不应把“产品详情表 / N 个产品”作为产品页主体表达。

## RED

- RED: `D:\Programs\node.exe --test scripts/showroom-admin-product-list.test.mjs` -> FAIL, `ProductListTable.vue must exist`，目标组件尚未创建。

## GREEN

- GREEN: `D:\Programs\node.exe --test scripts/showroom-admin-product-list.test.mjs` -> PASS, 4 个测试全部通过。
- GREEN: `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs` -> PASS。

## Review Round 1

- BDD: 产品列表兼容真实后端契约 -> Given `/showroom/product/page` 返回 snapshot 且 `/showroom/product/get?id=` 返回 revision When 主 agent 组装 enriched row 并传给 `ProductListTable` Then 表格只要求真实可得字段，不要求 `ownerCompanyName` 或 `updatedAt`。
- RED: `D:\Programs\node.exe --test scripts/showroom-admin-product-list.test.mjs` -> FAIL, 测试新增真实契约断言后，组件缺少 `产品编码` / `currentRevisionId` / `owner_company_id` / `revisionNo`，且仍包含 `ownerCompanyName` / `updatedAt`。
- GREEN: `D:\Programs\node.exe --test scripts/showroom-admin-product-list.test.mjs` -> PASS, 5 个测试全部通过。
- GREEN: `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs` -> PASS。

## 收尾预览

- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-showroom-product-list-table --mode preview` -> BLOCKED, 未删除文件；阻塞原因为未找到脚本检测出的 `master` 主分支 worktree。

## 备注

- 产品列表组件只接收真实列表数据，不引入 mock 数据。
- 字段缺失时采用显式 normalize/resolve 规则，不做静默成功兜底。
