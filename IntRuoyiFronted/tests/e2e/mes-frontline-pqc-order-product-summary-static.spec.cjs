const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const workspaceRoot = path.resolve(frontendRoot, '..')
const panel = fs
  .readFileSync(path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
  .replace(/\r\n/g, '\n')
const api = fs
  .readFileSync(path.join(frontendRoot, 'src/api/mes/pro/feedback/index.ts'), 'utf8')
  .replace(/\r\n/g, '\n')
const candidate = fs
  .readFileSync(
    path.join(
      workspaceRoot,
      'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineActiveOrderCandidate.java'
    ),
    'utf8'
  )
  .replace(/\r\n/g, '\n')
const responseVo = fs
  .readFileSync(
    path.join(
      workspaceRoot,
      'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlineActiveOrderRespVO.java'
    ),
    'utf8'
  )
  .replace(/\r\n/g, '\n')

const extractBlock = (source, selector) => {
  const start = source.indexOf(selector)
  assert.ok(start >= 0, `missing selector: ${selector}`)
  const open = source.indexOf('{', start)
  assert.ok(open > start, `missing selector body: ${selector}`)
  let depth = 0
  for (let index = open; index < source.length; index += 1) {
    if (source[index] === '{') depth += 1
    if (source[index] === '}') {
      depth -= 1
      if (depth === 0) return source.slice(open + 1, index)
    }
  }
  assert.fail(`unterminated selector: ${selector}`)
}

const activeOrderType = extractBlock(api, 'export interface FrontlineActiveOrderVO')
assert.match(activeOrderType, /productName:\s*string/, 'Product name must be a required active-order field.')
assert.match(activeOrderType, /quantity:\s*number/, 'Production quantity must be a required active-order field.')

assert.match(candidate, /BigDecimal quantity/, 'Backend active-order candidate must carry production quantity.')
assert.match(responseVo, /private BigDecimal quantity;/, 'Active-order response must expose production quantity.')

const headerStart = panel.indexOf('<header class="frontline-operator-top is-pqc">')
const headerEnd = panel.indexOf('</header>', headerStart)
assert.ok(headerStart >= 0 && headerEnd > headerStart, 'PQC top summary header must exist.')
const header = panel.slice(headerStart, headerEnd)

for (const token of [
  'data-pqc-order-summary-card',
  'data-pqc-order-code',
  'data-pqc-product-name',
  'data-pqc-product-quantity',
  'class="frontline-production-order-summary"',
  'class="frontline-production-order-summary__value"',
  'class="frontline-production-order-quantity"',
  '{{ productionProductNameLabel }}',
  '{{ selectedProductionOrderQuantityLabel }} 件'
]) {
  assert.ok(header.includes(token), `PQC order summary must include: ${token}`)
}

assert.match(
  panel,
  /const selectedActiveOrder = computed\(\(\) => deviceState\.selectedActiveOrder\)/,
  'All summary values must use the same selected active order.'
)
assert.match(
  panel,
  /const formatProductionQuantity = \(quantity: number\)[\s\S]*Number\.isFinite\(quantity\)[\s\S]*quantity <= 0[\s\S]*return String\(quantity\)/,
  'Production quantity formatting must reject invalid values and remove insignificant trailing zeros.'
)

assert.doesNotMatch(
  header,
  /frontline-order-summary__field|<span>生产订单<\/span>|<span>产品名称<\/span>|<span>产品数量<\/span>/,
  'PQC must use the production layout without the old three-column field labels.'
)

const summaryCard = extractBlock(panel, '.frontline-production-order-summary {')
for (const token of [
  'display: grid;',
  'grid-template-columns: minmax(0, 1fr) minmax(92px, 0.35fr);',
  'gap: 12px;',
  'align-items: center;'
]) {
  assert.ok(summaryCard.includes(token), `Order summary card style must include: ${token}`)
}

const sharedTypography = extractBlock(panel, '.frontline-production-stage,\n.frontline-operator-top.is-pqc {')
for (const token of [
  'font-size: 16px;',
  'font-size: 20px;',
  'font-size: 24px;',
  'font-size: 32px;',
  'font-size: 36px;',
  'font-weight: 400;',
  'font-weight: 600;'
]) {
  assert.ok(sharedTypography.includes(token), `Both headers must share typography: ${token}`)
}
const pqcTop = extractBlock(panel.slice(panel.lastIndexOf('\n.frontline-operator-top.is-pqc {')), '\n.frontline-operator-top.is-pqc {')
assert.ok(pqcTop.includes('text-wrap: balance;'), 'Long PQC process names must wrap in balanced lines.')
assert.doesNotMatch(pqcTop, /font-size: clamp\(/, 'PQC must not override shared typography with larger viewport-based fonts.')

for (const token of [
  'grid-template-rows: clamp(130px, 7.8125vw, 150px) minmax(0, 1fr);',
  'grid-template-columns: minmax(0, 1.45fr) minmax(0, 1.55fr) minmax(0, 0.6fr) minmax(160px, 0.55fr);'
]) {
  assert.ok(panel.includes(token), `PQC responsive layout must include: ${token}`)
}

console.log('PASS: frontline PQC uses the production order, product, and quantity layout')
