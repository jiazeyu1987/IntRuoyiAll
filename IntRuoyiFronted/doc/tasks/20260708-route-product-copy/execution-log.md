# Execution Log: 工艺路线关联产品行复制

BDD: route_product_copy_clones_product_and_bom -> Given 用户在工艺路线编辑页打开关联产品 Tab / When 点击某行复制并选择新产品 / Then 新产品关联被创建，生产参数继承源行，源产品 BOM 配置同步复制到目标产品。

BDD: route_product_copy_rejects_invalid_target -> Given 用户复制关联产品 / When 未选择目标产品或目标产品已被其它路线关联 / Then 后端直接返回校验错误，前端不关闭弹窗且不伪造成功。

RED: `node tests/e2e/mes-pro-route-product-copy-static.spec.js` -> FAIL, `RouteProductList.vue` 尚未提供单行复制按钮、复制弹窗和 `copyRouteProduct` API 调用。

GREEN: `node tests/e2e/mes-pro-route-product-copy-static.spec.js` -> PASS, 前端静态契约确认复制按钮、复制弹窗、目标产品选择、继承生产参数、调用后端复制接口、成功刷新列表。

GREEN: `node tests/e2e/mes-pro-route-product-copy-static.spec.js` -> PASS, 静态契约补充确认新增入口、编辑入口、`createRouteProduct`、`updateRouteProduct` 与 `copyRouteProduct` 三条路径均保留。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check` -> PASS, relaxed TypeScript 检查通过。直接 `npm run ts:check` 曾因 Node 默认堆内存不足失败，未发现类型错误。

GREEN: experience-preflight -> PASS, `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/route --target-text 路线编码 --timeout 90000` 已通过，真实登录进入工艺路线页面。

GREEN: `node tests/e2e/mes-pro-route-product-copy-real.e2e.js` -> PASS, 真实页面路径使用测试租户 `tenant_id=122`，在工艺路线 `ROUTE-XLSX-00002` 的“关联产品”Tab 点击行级“复制”，源产品 `YXN.069.001.1011` 复制到目标产品 `A002.09.002.230396`，新关联 `id=922103` 创建成功，生产参数继承源行，目标产品 BOM 数量为 26 条且与源产品 BOM 完全一致；验证后通过页面删除复制产生的关联产品行。

GREEN: cleanup-restore -> PASS, 通过真实页面确认并恢复验证候选路线 `ROUTE-XLSX-00002`，状态从 `1` 恢复为 `0`；本次 E2E 未遗留复制关联行或临时截图。

CHANGE: `RouteProductList.vue` 在关联产品操作列新增“复制”；复制弹窗使用 `MdItemSelect` 选择目标产品，默认继承源行生产数量、生产用时、时间单位和备注；提交时调用正式后端复制接口，成功后刷新列表。

GREEN: task-closeout-cleanup preview -> PASS, 仅保留任务核心文档、正式静态测试和生产代码；无本任务临时产物需要清理。
