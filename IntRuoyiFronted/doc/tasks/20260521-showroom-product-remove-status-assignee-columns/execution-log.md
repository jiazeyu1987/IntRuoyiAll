# 执行日志：删除展厅产品管理列表中的资料状态与指派对象列

BDD: 产品管理列表不再渲染资料状态与指派对象列 -> Given 用户进入 `http://localhost:8081/showroom/product` 的真实产品管理页并加载真实产品列表 / When 页面渲染产品表格 / Then 表头与列表行中不再出现 `资料状态` 与 `指派对象` 两列，其他列表行为与真实数据加载保持不变。

RED: `node --test scripts/showroom-admin-product-list.test.mjs` -> FAIL, `ProductListTable renders required product list columns` 仍匹配到 `<el-table-column label="资料状态"`，说明列表模板还在渲染待删除列。

RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-remove-status-assignee-columns run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-remove-status-assignee-columns\scripts\verify-showroom-product-columns-removed.mjs` -> FAIL, 真实页面表头仍返回 `["产品编码","中文名称","资料状态","审批状态","指派对象","英文名称","持证人","获证状态","音频","音色","操作"]`。

GREEN: `node --test scripts/showroom-admin-product-list.test.mjs` -> PASS，源码级回归确认产品列表仅保留 `产品编码 / 中文名称 / 审批状态 / 英文名称 / 持证人 / 获证状态 / 音频 / 音色 / 操作` 列，不再渲染 `资料状态` 与 `指派对象`。

GREEN: `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs --format stylish` -> PASS，删列后的组件与回归脚本通过静态检查。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-remove-status-assignee-columns run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-remove-status-assignee-columns\scripts\verify-showroom-product-columns-removed.mjs` -> PASS，真实 `http://127.0.0.1:8081/showroom/product` 表头只剩 `["产品编码","中文名称","审批状态","英文名称","持证人","获证状态","音频","音色","操作"]`，截图已写入 `output/playwright/showroom-product-columns-removed.png`。
