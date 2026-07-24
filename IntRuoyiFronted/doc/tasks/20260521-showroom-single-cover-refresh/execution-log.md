# Execution Log: 单个封面生成后刷新列表显示

BDD: 生成封面后当前列表应刷新 -> Given 用户在产品基础信息弹窗点击 `AI生成` 成功 / When 前端收到真实 `coverImage` / Then 当前产品列表必须刷新，不应继续显示“未上传”。

BDD: 仅同步封面字段基线 -> Given 用户可能在弹窗里还有其他未保存修改 / When AI 生成成功 / Then 前端只应把 baseline 中的 `cover_image` 更新为新值，不得把其他未保存字段误判为已保存。

RED: `node --test scripts/showroom-admin-product-cover-field.test.mjs` -> FAIL，回归断言要求成功路径调用 `syncGeneratedProductCoverBaseline(...)` 与 `loadProductRows()`，旧实现仅更新 `productForm.coverImage`。

GREEN: `node --test scripts/showroom-admin-product-cover-field.test.mjs` -> PASS。

GREEN: 真实 Playwright 复验 -> PASS：
- 搜索 `product_001` 后，生成前 `coverBefore=0`
- 点击 `AI生成` 成功返回 `/admin-api/infra/file/28/get/showroom/product/cover/20260521/product-product_001-cover.png`
- 当前会话稍后直接查 DOM，`product_001` 行 `imgCount=1`
- 列表行文本已切到 `V15 / 已发布`
