# Frontend Feature Evidence

## Feature

批记录表单列表顶部“产品名称”快速筛选值输入框改为可输入 autocomplete：点击输入框展示当前批记录表单目录产品名称候选；选择候选立即过滤；手动输入或复制产品名称后仍点击“查询”过滤。

## Acceptance

- 候选来源只调用 `BatchRecordReportApi.getProductNameOptions(keyword, latestVersionOnly)`，不读取 DCC 项目代码或 MES 物料主数据。
- `TableQuickFilterDefinition` 支持 `triggerOnFocus?: boolean`，默认字段不聚焦触发，仅本产品名称筛选设置 `triggerOnFocus: true`。
- `TableQuickFilter` 渲染 `el-autocomplete`，选择候选后 `emit('query')`；查询按钮保留给手动输入路径。
- 批记录表单页保留 `queryParamKey: 'productName'`，候选和手输最终都走列表 `/page?productName=...`。

## BDD

- BDD: 点击产品名称输入框展示候选 -> Given 批记录表单目录存在多个产品名称 / When 用户点击产品名称筛选输入框 / Then 下拉展示当前批记录表单目录实际存在的产品名称候选。
- BDD: 点击候选立即过滤 -> Given 候选下拉中存在目标产品名称 / When 用户点击该候选 / Then 列表立即以该产品名称重新查询且无需点击查询按钮。
- BDD: 手动输入查询过滤 -> Given 用户手动输入或复制产品名称 / When 用户点击查询按钮 / Then 列表以输入文本作为 `productName` 过滤。

## RED

- RED: `node -e "<dfc71011^ snapshot assertions>"` -> FAIL, expected reason: task-start parent source lacked batch-record product options API wrapper, `triggerOnFocus` definition, and productName autocomplete quick-filter contract.

## GREEN

- GREEN: `node tests/e2e/edhr-batch-record-form-list-product-filter-autocomplete-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-batch-record-form-list-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.

## Verification

- Static contract verified API wrapper URL, quick-filter `triggerOnFocus`, `el-autocomplete` rendering, `@select` immediate query, query button retention, and no DCC/MES master data source in product suggestions.
- Type verification passed using `vue-tsc --noEmit -p tsconfig.relaxed.json`.
- Real E2E reached the page and observed `/page` returning business code `0`, total `320`, first page `20` rows, and `20` rows with non-empty product names; selecting candidates could not proceed because `/product-name-options` returned business code `404` from the running backend.

## Blockers

- 本机真实 E2E blocked：当前 `48081` runtime 未加载新增后端 endpoint；需要重启到包含本次代码的后端运行态后复跑候选下拉选择和手动查询路径。
