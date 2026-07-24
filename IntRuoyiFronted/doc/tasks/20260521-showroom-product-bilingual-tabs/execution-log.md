# 执行日志：展厅产品基础/详细信息双语 Tab 与英文语音编辑（前端）

BDD: 产品基础信息弹窗显示双语 tab -> Given 用户打开 `showroom/product` 的基础信息弹窗 / When 弹窗加载完成 / Then 页面必须展示 `中文` 与 `English` 两个 tab，中文 tab 保留当前中文编辑区，英文 tab 展示英文名称、英文描述字段、英文讲解稿、`AI翻译`、`生成语音` 与中英文音频播放器。

BDD: 产品详细信息弹窗显示双语 tab -> Given 用户打开 `showroom/product` 的详细信息弹窗 / When 弹窗加载完成 / Then 页面必须展示 `中文` 与 `English` 两个 tab，中文 tab 保留当前高级字段，英文 tab 展示 `registration_certificate_en / clinical_effect_en / fim_status_en` 和 `AI翻译`。

BDD: 英文 tab 的 AI翻译使用当前中文草稿 -> Given 用户已经在基础信息或详细信息中文 tab 中填写本次草稿 / When 用户点击英文 tab 的 `AI翻译` / Then 前端必须把当前中文字段和可选中文讲解稿提交给真实产品翻译接口，并把返回英文结果回填到当前英文草稿，不得本地伪造翻译结果。

BDD: 英文 tab 的生成语音使用当前中英文讲解稿 -> Given 用户已在基础信息弹窗中存在当前 revision 的中文讲解稿和英文讲解稿草稿 / When 用户点击英文 tab 的 `生成语音` / Then 前端必须先保存当前 ZH/EN 讲解稿 draft，再调用真实产品语音生成接口，并回填当前中英文音频播放器。

BDD: 产品列表不再保留单条语音按钮 -> Given 用户查看 `showroom/product` 列表操作列 / When 页面渲染完成 / Then 列表行不再渲染单条 `语音` 按钮，避免与英文 tab 内 `生成语音` 形成双入口。

RED: `node --test scripts/showroom-admin-product-bilingual-tabs.test.mjs` -> FAIL，旧前端仍缺少产品英文翻译接口类型、基础/详细信息双语 tab、英文讲解稿编辑区与列表单条 `语音` 按钮移除。

GREEN: `node --test scripts/showroom-admin-product-bilingual-tabs.test.mjs scripts/showroom-admin-product-narration-editor.test.mjs scripts/showroom-product-narration-action-disabled.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-company-field-layout.test.mjs scripts/showroom-admin-product-hall-operability.test.mjs` -> PASS，产品双语 tab、英文讲解稿编辑、翻译入口、列表语音入口迁移和受影响旧断言已全部通过源码回归。

GREEN: `node tests/e2e/showroom-product-publish-entry.spec.js` -> PASS，列表仍是唯一发布入口。

GREEN: `node tests/e2e/showroom-product-detail-basic-info.spec.js` -> PASS，详细信息弹窗继续不承接讲解稿编辑。

GREEN: `node tests/e2e/showroom-product-basic-info-narration-move.spec.js` -> PASS，基础信息弹窗继续承接讲解稿编辑，且绑定升级为中文讲解稿草稿。

GREEN: `node tests/e2e/showroom-product-whole-assignment.spec.js` -> PASS，整单指派链路保持 `指派 / 基础 / 详细`，旧单条 `语音` 按钮已去除。

GREEN: `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue src/views/showroom-admin/product/ProductDetailDialog.vue src/views/showroom-admin/product/contracts.ts src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-bilingual-tabs.test.mjs scripts/showroom-admin-product-narration-editor.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-product-narration-action-disabled.test.mjs --format stylish` -> PASS，定向前端文件无 lint 错误。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS，双语字段、英文讲解稿状态和双语详情弹窗改动均通过 `vue-tsc`。

GREEN: `node --test scripts/showroom-admin-product-bilingual-tabs.test.mjs` -> PASS，已额外锁定 `中文音频` 只渲染在中文 tab、`English` tab 仅保留 `英文音频`。
