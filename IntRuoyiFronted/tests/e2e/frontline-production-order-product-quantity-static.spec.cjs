const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')

const headerStart = panel.indexOf('data-frontline-production-selection-grid')
const mainStart = panel.indexOf('<main class="frontline-operator-main', headerStart)
assert.ok(headerStart >= 0 && mainStart > headerStart, 'production header must exist.')
const header = panel.slice(headerStart, mainStart)
const orderStart = header.indexOf('data-frontline-production-active-order-card')
const processStart = header.indexOf('data-frontline-production-process-nav-card', orderStart)
assert.ok(orderStart >= 0 && processStart > orderStart, 'production order card must exist.')
const orderCard = header.slice(orderStart, processStart)

assert.doesNotMatch(orderCard, /frontline-production-order-summary__label/, 'yellow-box content must not render or reserve a layout slot.')
assert.match(orderCard, /productionProductNameLabel/, 'the selected product name must remain visible in the expanded order-information area.')
assert.match(orderCard, /data-frontline-production-order-quantity/, 'red-box quantity area must have a stable marker.')
assert.match(orderCard, /selectedProductionOrderQuantityLabel\s*}}\s*件/, 'quantity area must display the formal order quantity and unit.')
assert.match(orderCard, /frontline-production-order-quantity/, 'quantity area must use a dedicated right-side layout class.')
const summaryStart = panel.indexOf('\n.frontline-production-order-summary {')
assert.ok(summaryStart >= 0, 'production order summary style must exist.')
const summaryEnd = panel.indexOf('\n}', summaryStart)
assert.ok(summaryEnd > summaryStart, 'production order summary style must close.')
const summaryStyle = panel.slice(summaryStart, summaryEnd)
assert.match(
  summaryStyle,
  /grid-template-columns:\s*minmax\(0,\s*1fr\)\s+minmax\(92px,\s*0\.35fr\)/,
  'order summary must let order information use the former yellow-box width and reserve an independent right-side quantity column.'
)
assert.match(
  panel,
  /\.frontline-production-order-quantity\s*\{[\s\S]*?font-size:\s*42px/,
  'quantity styling and marker must be present for the right-side area.'
)

console.log('PASS: frontline production order hides product content and shows quantity on the right')
