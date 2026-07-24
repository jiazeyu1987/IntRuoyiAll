# 生产工单产品候选过滤执行日志

BDD: 产品名称候选过滤生产工单 -> Given 用户打开生产工单列表 / When 在“产品名称”输入框搜索并选择候选产品 / Then 查询请求携带该候选产品 ID，列表只返回该产品相关工单。

BDD: 产品编码候选过滤生产工单 -> Given 用户打开生产工单列表 / When 在“产品编码”输入框搜索并选择候选产品 / Then 查询请求携带该候选产品 ID，列表只返回该产品相关工单。

BDD: 产品名称与编码候选冲突 -> Given 用户分别选择了不同产品的名称候选和编码候选 / When 点击查询 / Then 页面提示“产品名称与产品编码不是同一产品”，不发送工单分页查询。

RED: `node tests/e2e/workorder-product-candidate-filters-static.spec.js` -> FAIL，现有生产工单页缺少“产品名称”和“产品编码”远程候选过滤框。

GREEN: `node tests/e2e/workorder-product-candidate-filters-static.spec.js` -> PASS，页面已渲染两个候选过滤框，并通过真实 MES 物料产品分页接口按名称/编码搜索候选。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS，前端 relaxed TypeScript 检查通过。

BDD: 生产工单重点列文本充分利用列宽 -> Given 重点列已经加宽 / When 单元格内仍有空白但文本提前省略 / Then 单元格内部布局应占满整列，文本优先使用可用宽度，复制按钮固定在右侧。

RED: `node tests/e2e/workorder-key-columns-static.spec.js` -> FAIL，旧样式使用 `inline-flex` 和 `max-width: calc(100% - 28px)`，导致重点列有空白仍提前省略。

BDD: 生产工单重点列进一步加宽 -> Given 用户查看生产工单列表截图 / When 重点列内容仍出现较多省略 / Then 工单名称、产品编码、产品名称、规格型号、计划数量等重点列进一步加宽，减少可见内容截断。

RED: `node tests/e2e/workorder-key-columns-static.spec.js` -> FAIL，静态契约要求重点列新宽度后，页面仍使用上一版较窄列宽。

GREEN: `node tests/e2e/workorder-key-columns-static.spec.js` -> PASS，重点列宽度已提升到工单编号 340、工单名称 340、产品编码 260、产品名称 340、规格型号 360、计划数量 180。

GREEN: `node tests/e2e/workorder-product-candidate-filters-static.spec.js` -> PASS，产品候选过滤既有契约未回归。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS，前端 relaxed TypeScript 检查通过。


BDD: 生产工单重点列加宽置前并可复制 -> Given 用户打开生产工单列表 / When 查看红框重点信息列 / Then 工单编号、工单名称、产品编码、产品名称、规格型号、计划数量置于其它列之前，列宽更适合完整展示，并且每列都有复制按钮。

RED: `node tests/e2e/workorder-key-columns-static.spec.js` -> FAIL，工单名称等重点列缺少复制按钮，计划数量未置于生产车间之前，重点列宽度不足。

GREEN: `node tests/e2e/workorder-key-columns-static.spec.js` -> PASS，重点列已置前加宽，且工单编号、工单名称、产品编码、产品名称、规格型号、计划数量均提供复制按钮。

GREEN: `node tests/e2e/workorder-product-candidate-filters-static.spec.js` -> PASS，产品名称/产品编码候选过滤既有契约未回归。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS，前端 relaxed TypeScript 检查通过。
BDD: 产品名称候选仅展示名称并自动查询 -> Given 用户在“产品名称”输入框输入关键字 / When 下拉候选出现并选择某个产品名称 / Then 候选项只显示产品名称，选择后自动触发生产工单查询。

RED: `node tests/e2e/workorder-product-candidate-filters-static.spec.js` -> FAIL，产品编码候选下拉仍展示组合标签，不符合“只显示编码、选择即搜索”。

BDD: 产品编码候选仅展示编码并自动查询 -> Given 用户在“产品编码”输入框输入关键字 / When 下拉候选出现并选择某个产品编码 / Then 候选项只显示产品编码，选择后自动触发生产工单查询。

GREEN: `node tests/e2e/workorder-product-candidate-filters-static.spec.js` -> PASS，产品编码候选下拉只显示编码，选择编码后自动触发查询。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS，前端 relaxed TypeScript 检查通过。
