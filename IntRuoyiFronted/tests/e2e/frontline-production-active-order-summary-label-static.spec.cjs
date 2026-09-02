const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const viewSource = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)

const cardStart = viewSource.indexOf('data-frontline-production-active-order-card')
const cardEnd = viewSource.indexOf('data-frontline-production-process-nav-card', cardStart)
assert.ok(cardStart >= 0 && cardEnd > cardStart, 'production active order card template must exist.')
const cardTemplate = viewSource.slice(cardStart, cardEnd)

assert.match(
  cardTemplate,
  /data-frontline-production-active-order-summary/,
  'production active order card must render one formal summary label.'
)
assert.match(
  cardTemplate,
  /\{\{\s*productionActiveOrderSummaryLabel\s*\}\}/,
  'production active order card must display the formatted 编号-产品名(数量) label.'
)
assert.doesNotMatch(
  cardTemplate,
  /data-frontline-production-batch-code/,
  'production active order card must not render batch code as a separate line.'
)
assert.doesNotMatch(
  cardTemplate,
  /data-frontline-production-product-name/,
  'production active order card must not render product name as a separate line.'
)

const labelStart = viewSource.indexOf('const productionActiveOrderSummaryLabel = computed')
assert.ok(labelStart >= 0, 'productionActiveOrderSummaryLabel computed must exist.')
const labelEnd = viewSource.indexOf('const productionOrderLabel', labelStart + 1)
assert.ok(labelEnd > labelStart, 'production active order summary block must end before legacy order label.')
const labelBlock = viewSource.slice(labelStart, labelEnd)
assert.match(labelBlock, /selectedOrder\.workOrderCode\?\.trim\(\)/, 'summary must use formal work order code.')
assert.match(labelBlock, /selectedOrder\.productName\?\.trim\(\)/, 'summary must use formal product name.')
assert.match(labelBlock, /formatProductionQuantity\(selectedOrder\.quantity\)/, 'summary must use formatted order quantity.')
assert.match(
  labelBlock,
  /`\$\{workOrderCode\}-\$\{productName\}\(\$\{quantityText\}\)`/,
  'summary must be formatted as 编号-产品名(数量).'
)
assert.match(labelBlock, /一线活跃订单缺少正式产品名/, 'missing product name must fail fast.')

console.log('PASS: frontline production active order summary label static contract')
