# 执行记录：展厅管理改为只维护产品集合不手工维护顺序

BDD: 展厅管理映射入口只维护产品集合 -> Given 用户进入展厅管理并打开某一展厅的维护映射弹窗 / When 用户调整该展厅包含的产品 / Then 页面应只要求用户维护产品集合，不再暴露手工 `displayOrder` 输入。

BDD: 保存展厅产品集合时仍走真实映射契约 -> Given 用户已经选择当前展厅包含的产品 / When 用户点击保存 / Then 前端仍应调用真实 `/showroom/hall/update-product-mapping` 契约保存映射，不得 mock 成功或跳过保存。

REPRO: 初始实现中，展厅列表仍显示“排序明细”，映射弹窗仍要求用户逐行维护 `displayOrder`；真实 `showroom/hall` 页面打开“维护产品”后还暴露出候选产品只来自产品首屏 20 条，部分 hall 的既有产品不在当前候选中时会直接报错。
ROOT CAUSE: 前端将 hall 产品集合维护和手工顺序维护耦合在同一个表单里，并且错误地复用了产品管理首屏分页数据作为完整产品候选源。
REGRESSION TEST: 更新 `scripts/showroom-admin-hall-list.test.mjs` 与 `scripts/showroom-admin-product-hall-operability.test.mjs`，覆盖“去掉排序明细/手工顺序输入”“入口改为维护产品”“弹窗会主动读取完整产品分页候选”。
RED: `D:\Programs\node.exe --test scripts/showroom-admin-hall-list.test.mjs` -> FAIL，列表仍显示“排序明细”，操作按钮仍为“维护映射”。
RED: `D:\Programs\node.exe --test scripts/showroom-admin-product-hall-operability.test.mjs` -> FAIL，弹窗仍暴露 `displayOrder` 输入，也未主动调用 `ShowroomAdminApi.getProductPage` 拉齐完整产品候选。
GREEN: `D:\Programs\node.exe --test scripts/showroom-admin-hall-list.test.mjs scripts/showroom-admin-product-hall-operability.test.mjs` -> PASS。
GREEN: `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/components/HallListTable.vue src/views/showroom-admin/components/HallProductMappingDialog.vue src/views/showroom-admin/hall/contracts.ts src/views/showroom-admin/hall/HallWorkbench.vue scripts/showroom-admin-hall-list.test.mjs scripts/showroom-admin-product-hall-operability.test.mjs` -> PASS。
GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-hall-products-no-order run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-hall-products-no-manual-order\verify-showroom-hall-products-no-order.mjs` -> PASS，真实页面可打开“维护产品”弹窗，且不再出现 `displayOrder` 文本或数字输入框。
GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-hall-products-save run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-hall-products-no-manual-order\verify-showroom-hall-products-save.mjs` -> PASS，真实页面打开展厅“维护产品”后，直接点击“保存产品”可收到成功响应与成功提示。
RISK: 产品管理区仍存在一个独立日志 `未找到瑛泰医疗所属公司映射`，它不影响展厅产品维护与保存，但属于另一个现存问题。
BLOCKER: 无剩余 blocker。
