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
  'active-order card must render the dedicated three-line summary block.'
)
const orderCodeIndex = orderCard.indexOf('data-frontline-production-order-code')
const batchCodeIndex = orderCard.indexOf('data-frontline-production-batch-code')
const productNameIndex = orderCard.indexOf('data-frontline-production-product-name')
assert.ok(orderCodeIndex >= 0, 'first summary line must expose the order code marker.')
assert.ok(batchCodeIndex >= 0, 'second summary line must expose the production batch code marker.')
assert.ok(productNameIndex >= 0, 'third summary line must expose the product name marker.')
assert.ok(
  orderCodeIndex < batchCodeIndex && batchCodeIndex < productNameIndex,
  'summary lines must be ordered as order code, batch code, product name.'
)
assert.match(orderCard, /{{\s*productionOrderLabel\s*}}/, 'first line must use the selected order code label.')
assert.match(orderCard, /v-if="productionBatchCodeLabel"/, 'batch line must render only when the order has a batch code.')
assert.match(orderCard, /{{\s*productionBatchCodeLabel\s*}}/, 'second line must use the selected order batch code.')
assert.match(orderCard, /{{\s*productionProductNameLabel\s*}}/, 'third line must use the selected order product name.')
assert.match(
  panel,
  /const productionOrderLabel = computed\([\s\S]*selectedOrder\.workOrderCode\?\.trim\(\)[\s\S]*一线活跃订单缺少正式订单号/,
  'first line must fail fast when the formal order code is missing instead of displaying an order name.'
)

assert.match(api, /export interface FrontlineActiveOrderVO[\s\S]*batchCode\?:\s*string/, 'frontend active-order contract must expose batchCode.')
assert.match(backendVo, /private String batchCode;/, 'backend frontline active-order response must expose batchCode.')

const summaryBlock = extractBalancedBlock(panel, '.frontline-production-order-summary')
assert.match(summaryBlock, /display:\s*grid/, 'three-line summary must use a stable grid.')
assert.match(summaryBlock, /white-space:\s*normal/, 'three-line summary values must allow wrapping.')
assert.match(summaryBlock, /overflow-wrap:\s*anywhere/, 'long order, batch, and product text must wrap inside the card.')
assert.doesNotMatch(summaryBlock, /text-overflow:\s*ellipsis/, 'three-line summary must not truncate values with ellipsis.')
assert.doesNotMatch(summaryBlock, /white-space:\s*nowrap/, 'three-line summary must not force a single line.')

console.log('PASS: frontline production order summary shows order, batch, and product as full wrapping lines')
