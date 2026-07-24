const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const componentPath = path.resolve(frontendRoot, 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const source = fs.readFileSync(componentPath, 'utf8')

const selectBlockMatch = source.match(/<el-select[\s\S]*?class="route-flow-config-panel-product-select"[\s\S]*?<\/el-select>/)
assert.ok(selectBlockMatch, '工艺流程排产配置弹窗必须保留产品编号选择框。')

const selectBlock = selectBlockMatch[0]
assert.match(
  selectBlock,
  /:label="buildRouteProductCode\(product\)"/,
  '产品下拉选项 label 必须只来自完整产品编号。'
)
assert.doesNotMatch(
  selectBlock,
  /:label="buildRouteProductLabel\(product\)"/,
  '产品下拉选项不得再使用编号、名称、参数拼接 label。'
)
assert.doesNotMatch(
  selectBlock,
  /product\.itemName|product\.specification/,
  '产品下拉选项模板不得展示名称或参数，避免误导为按名称/参数选择。'
)

assert.match(
  source,
  /const selectedRouteProduct = computed\(\(\) =>/,
  '页面必须用 selectedRouteProduct 计算当前选中产品详情。'
)
assert.match(
  source,
  /const buildRouteProductCode = \(product: ProRouteProductVO\) =>/,
  '页面必须提供只返回产品编号的 buildRouteProductCode。'
)
assert.match(source, /编号：/, '选中产品详情文本必须显示“编号：”。')
assert.match(source, /名称：/, '选中产品详情文本必须显示“名称：”。')
assert.match(source, /参数：/, '选中产品详情文本必须显示“参数：”。')
assert.match(
  source,
  /route-flow-config-panel-product-detail/,
  '选中产品编号、名称、参数必须使用独立只读详情文本区域展示。'
)
assert.match(
  source,
  /white-space:\s*normal/,
  '产品详情文本必须允许换行，确保长编号完整显示。'
)
assert.match(
  source,
  /word-break:\s*break-all/,
  '产品详情文本必须允许长编号断行，避免编号被截断。'
)
