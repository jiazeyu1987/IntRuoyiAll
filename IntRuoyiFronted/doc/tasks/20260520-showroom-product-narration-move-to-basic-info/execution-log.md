# Execution Log: 20260520-showroom-product-narration-move-to-basic-info

BDD: 展厅产品讲解稿移入基础信息 -> Given 用户在产品管理页同时拥有“基础信息”和“详细信息”两个入口 / When 用户维护产品基础信息 / Then 讲解稿编辑区与“生成讲解稿”按钮应出现在基础信息弹窗中，且详细信息弹窗不再保留这块内容。
RED: `node .\tests\e2e\showroom-product-basic-info-narration-move.spec.js` -> FAIL, `index.vue` 的产品基础信息弹窗仍缺少“讲解稿”字段，`ProductDetailDialog.vue` 仍保留旧讲解稿区域。
GREEN: `node .\tests\e2e\showroom-product-basic-info-narration-move.spec.js` -> PASS
GREEN: `node .\tests\e2e\showroom-product-detail-basic-info.spec.js` -> PASS
GREEN: `pnpm exec eslint src\views\showroom-admin\index.vue src\views\showroom-admin\product\ProductDetailDialog.vue tests\e2e\showroom-product-basic-info-narration-move.spec.js tests\e2e\showroom-product-detail-basic-info.spec.js` -> PASS
