# 执行日志：展柜后台产品分页切换异常排查

BDD: 展柜后台产品列表分页应按页切换 -> Given 用户登录 `芋道源码 / admin` 并进入 `展柜 -> 产品管理` / When 依次点击分页 `1`、`2`、`3`、`4` / Then 每次都应基于对应 `pageNo` 重新请求并展示该页数据，而不是停留在前一页数据。

BDD: 前端分页事件应透传到接口参数 -> Given 底部分页组件触发页码切换 / When 用户点击新页码 / Then 前端必须把新的 `pageNo` 传给 `/showroom/product/page`，并用返回的 `productPage.list` 覆盖当前表格数据。

BDD: 后端分页切片应随 pageNo 变化 -> Given `/showroom/product/page` 收到不同的 `pageNo` / When 总条数大于单页大小 / Then 接口返回的 `list` 应按排序后的数据窗口切片，不同页码不应稳定返回同一批记录。

INFO: 前端代码确认 `ProductListTable.vue` 使用 `el-pagination`，`@current-change` 触发 `handlePageChange(pageNo)`，随后 `emit('page-change', { pageNo, pageSize: props.pageSize })`。

INFO: 父组件 `src/views/showroom-admin/index.vue` 中 `handleProductPageChange` 会更新 `productPageNo` / `productPageSize` 并执行 `loadProductRows()`；`loadProductRows()` 会将 `productPage.list` 直接覆盖到 `productRows`。

INFO: 后端 `ShowroomAdminController#getProductPage` 直接委托 `ShowroomApiRuntime#listProducts(reqVO)`；运行时使用 `pageResult(rows, req.pageNo(), req.pageSize())` 做切片。

INFO: 后端已存在集成测试 `productPageShouldReturnTotalAndRespectRequestedPageSlice`，至少覆盖了 `pageNo=1` 与 `pageNo=2` 的切片行为。

GREEN: `node --test scripts/showroom-admin-frontend.test.mjs` -> PASS，静态断言确认前端仍通过 `page-change -> handleProductPageChange -> loadProductRows()` 走服务端分页。

GREEN: 直连本地运行后端 `127.0.0.1:48082/admin-api/showroom/product/page` 验证分页切片 -> PASS：
- `pageNo=1` 返回 `product_001..product_020`
- `pageNo=2` 返回 `product_021..product_039`
- `pageNo=3` 返回 `product_040..product_059`
- `pageNo=4` 返回 `product_060..product_079`

RED: Playwright 真实前端复现 `inspect-showroom-product-pagination-live.mjs` -> FAIL，现象为：
- 第 `3` 页接口响应已切到 `product_040..product_059`
- 第 `4` 页接口响应已切到 `product_060..product_079`
- 但表格 DOM 仍停留在第 `2` 页 `product_021..product_039`

RED: 同次 Playwright 复现捕获前端首个未处理异常 -> FAIL，`pageerror` 为 `产品列表第 10 行字段为空：owner_company_id`，随后出现多次 `Cannot set properties of null (setting '__vnode')`，说明列表更新在渲染阶段被打断。

INFO: 问题触发的首个坏数据为第 `3` 页第 `10` 条 `product_049`：
- `revision.fields.owner_company_id` 缺失
- `displayRevision.fields.owner_company_id` 缺失
- `revision.status=DRAFT`
- 接口却返回 `incomplete=false`

INFO: 根因链路确认：
- 前端 `ProductListTable.vue` 的 `resolveOwnerCompanyId()` 在非 incomplete 行上把 `owner_company_id` 视为必填，缺失时直接 `throw`。
- 后端 `ShowroomPublishContract.requiredProductPublishFields()` 当前只包含 `name_cn`、`name_en`。
- 后端 `ShowroomPersistentContentService.isProductIncomplete()` 因而不会把缺少 `owner_company_id` 的产品标为 incomplete。
- `product_049` 被后端当成完整产品返回到第 `3` 页，前端渲染时抛错并中断更新，所以用户看到“切到第 3、4 页时列表不变化”。
