const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')
const api = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/feedback/index.ts'),
  'utf8'
).replace(/\r\n/g, '\n')
const backendVo = fs.readFileSync(
  path.resolve(
    root,
    '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlineActiveOrderRespVO.java'
  ),
  'utf8'
)

function extractBetween(source, startNeedle, endNeedle) {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, startNeedle + ' must exist.')
  const end = source.indexOf(endNeedle, start)
  assert.ok(end > start, endNeedle + ' must appear after ' + startNeedle + '.')
  return source.slice(start, end)
}

function extractBalancedBlock(source, startNeedle) {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, startNeedle + ' block must exist.')
  const open = source.indexOf('{', start)
  assert.ok(open > start, startNeedle + ' block must open.')
  let depth = 0
  for (let index = open; index < source.length; index += 1) {
    if (source[index] === '{') depth += 1
    if (source[index] === '}') depth -= 1
    if (depth === 0) return source.slice(start, index + 1)
  }
  throw new Error(startNeedle + ' block must close.')
}

const productionHeader = extractBetween(
  panel,
  'data-frontline-production-selection-grid',
  '<main class="frontline-operator-main'
)
const orderCard = extractBetween(
  productionHeader,
  'data-frontline-production-active-order-card',
  'data-frontline-production-process-nav-card'
)

assert.match(
  orderCard,
  /class="frontline-production-order-summary"/,
  'active-order card must render the dedicated production summary block.'
)
assert.match(
  orderCard,
  /data-frontline-production-active-order-summary/,
  'active-order card must expose a single formal summary marker.'
)
assert.match(
  orderCard,
  /{{\s*productionActiveOrderSummaryLabel\s*}}/,
  'active-order card must show the selected order as 编号-产品名(数量).'
)
assert.doesNotMatch(
  orderCard,
  /data-frontline-production-batch-code/,
  'active-order card must not split the batch code into a separate visible line.'
)
assert.doesNotMatch(
  orderCard,
  /data-frontline-production-product-name/,
  'active-order card must not split the product name into a separate visible line.'
)
assert.match(
  panel,
  /const productionActiveOrderSummaryLabel = computed\([\s\S]*selectedOrder\.workOrderCode\?\.trim\(\)[\s\S]*selectedOrder\.productName\?\.trim\(\)[\s\S]*formatProductionQuantity\(selectedOrder\.quantity\)[\s\S]*`\$\{workOrderCode\}-\$\{productName\}\(\$\{quantityText\}\)`/,
  'summary label must compose formal order code, product name, and quantity.'
)
assert.match(
  panel,
  /const productionActiveOrderSummaryLabel = computed\([\s\S]*一线活跃订单缺少正式订单号[\s\S]*一线活跃订单缺少正式产品名/,
  'summary label must fail fast when formal order code or product name is missing.'
)

assert.match(api, /export interface FrontlineActiveOrderVO[\s\S]*batchCode\?:\s*string/, 'frontend active-order contract must still expose batchCode for downstream payloads.')
assert.match(backendVo, /private String batchCode;/, 'backend frontline active-order response must still expose batchCode.')

const summaryBlock = extractBalancedBlock(panel, '.frontline-production-order-summary')
assert.match(summaryBlock, /display:\s*grid/, 'summary must use a stable grid.')
assert.match(summaryBlock, /white-space:\s*normal/, 'summary value must allow wrapping.')
assert.match(summaryBlock, /overflow-wrap:\s*anywhere/, 'long order and product text must wrap inside the card.')
assert.doesNotMatch(summaryBlock, /text-overflow:\s*ellipsis/, 'summary must not truncate values with ellipsis.')
assert.doesNotMatch(summaryBlock, /white-space:\s*nowrap/, 'summary must not force a single line.')

console.log('PASS: frontline production order summary shows 编号-产品名(数量) with full wrapping')
