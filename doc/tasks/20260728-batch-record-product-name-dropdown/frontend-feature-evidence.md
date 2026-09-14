# Frontend Feature Evidence

## Feature

批记录表单列表顶部“产品名称”快速筛选值输入框改为可输入 autocomplete：点击输入框展示当前批记录表单目录产品名称候选；选择候选立即过滤；手动输入或复制产品名称后仍点击“查询”过滤。

## Acceptance

- 候选来源只调用 `BatchRecordReportApi.getProductNameOptions(keyword, latestVersionOnly)`，不读取 DCC 项目代码或 MES 物料主数据。
- `TableQuickFilterDefinition` 支持 `triggerOnFocus?: boolean`，默认字段不聚焦触发，仅本产品名称筛选设置 `triggerOnFocus: true`。
- `TableQuickFilterDefinition` 支持 autocomplete `popperClass`，默认使用专用候选下拉样式完整展示较长产品名称。
- `TableQuickFilter` 渲染 `el-autocomplete`，选择候选后 `emit('query')`；查询按钮保留给手动输入路径。
- 快速过滤字段选择框、条件选择框、产品名称输入框均锁定不收缩宽度，避免“产品名称”“包含”和输入值被挤成省略号。
- 批记录表单页保留 `queryParamKey: 'productName'`，候选和手输最终都走列表 `/page?productName=...`。
- 批记录表单列表工具栏移除红框中的“批量删除”按钮；仅服务于批量删除的多选列、选择状态和批量删除处理函数同步移除，右侧单条“删除”动作保留。
- 点击“填写人”列必须打开 `批记录表单填写人设置` 小弹窗用于更换填写人；全屏 `填写配置` 只通过右侧动作进入。

## BDD

- BDD: 点击产品名称输入框展示候选 -> Given 批记录表单目录存在多个产品名称 / When 用户点击产品名称筛选输入框 / Then 下拉展示当前批记录表单目录实际存在的产品名称候选。
- BDD: 点击候选立即过滤 -> Given 候选下拉中存在目标产品名称 / When 用户点击该候选 / Then 列表立即以该产品名称重新查询且无需点击查询按钮。
- BDD: 手动输入查询过滤 -> Given 用户手动输入或复制产品名称 / When 用户点击查询按钮 / Then 列表以输入文本作为 `productName` 过滤。
- BDD: 产品名称完整显示 -> Given 产品名称筛选位于较窄工具栏 / When 页面展示“产品名称”“包含”和较长产品名称候选 / Then 字段、条件和候选名称不被 flex 收缩成省略号，候选可换行完整阅读。
- BDD: 删除批量删除按钮 -> Given 批记录表单列表顶部工具栏显示导入与最新版本开关 / When 用户查看截图红框位置 / Then 不再显示“批量删除”按钮，且不保留仅服务于该按钮的多选列、选中状态和批量删除处理函数。
- BDD: 点击填写人列更换填写人 -> Given 某条批记录表单存在已配置填写人和辅助填写映射 / When 用户点击列表“填写人”列 / Then 打开 `批记录表单填写人设置` 小弹窗，而不是全屏 `填写配置`。

## RED

- RED: `node -e "<dfc71011^ snapshot assertions>"` -> FAIL, expected reason: task-start parent source lacked batch-record product options API wrapper, `triggerOnFocus` definition, and productName autocomplete quick-filter contract.
- RED: `node tests/e2e/edhr-batch-record-form-list-product-filter-autocomplete-static.spec.js` -> FAIL, expected reason: autocomplete 尚未使用专用 popper 样式，字段/条件/输入宽度未锁定不收缩。
- RED: `node tests/e2e/batch-record-form-latest-version-switch-static.spec.js` -> FAIL, expected reason: 页面仍绑定 `@click="handleBatchDelete"`，截图位置仍有“批量删除”按钮。
- RED: `node tests/e2e/edhr-batch-record-form-list-static.spec.js` -> FAIL, expected reason: 填写人列处理函数仍因 `fillAssignments` 调用全屏 `openCellRulesDialog(row)`。

## GREEN

- GREEN: `node tests/e2e/edhr-batch-record-form-list-product-filter-autocomplete-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-batch-record-form-list-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/batch-record-form-latest-version-switch-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/batch-record-force-unbind-delete-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.

## Verification

- Static contract verified API wrapper URL, quick-filter `triggerOnFocus`, `el-autocomplete` rendering, `@select` immediate query, query button retention, and no DCC/MES master data source in product suggestions.
- Static contract also verified full-display layout: field width `120px`, operator width `92px`, product value width `clamp(280px, 32vw, 420px)`, and autocomplete popper wraps long candidate names.
- Static contract verified toolbar batch delete removal: no “批量删除” button text, no `handleBatchDelete`, no selection column, no `selectedRows`, and no batch unbind confirmation copy.
- Static contract verified filler-column behavior: `openBatchRecordFormPermissionDialog` opens `permissionDialogVisible` and no longer branches to `openCellRulesDialog(row)` for `fillAssignments`; right-side `填写配置` still opens full cell-rule configuration.
- Type verification passed using `vue-tsc --noEmit -p tsconfig.relaxed.json`.
- Real E2E reached the page and observed `/page` returning business code `0`, total `320`, first page `20` rows, and `20` rows with non-empty product names; selecting candidates could not proceed because `/product-name-options` returned business code `404` from the running backend.

## Blockers

- 本机真实 E2E blocked：当前 `48081` runtime 未加载新增后端 endpoint；需要重启到包含本次代码的后端运行态后复跑候选下拉选择和手动查询路径。
