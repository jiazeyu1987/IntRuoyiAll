const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const source = fs
  .readFileSync(path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

const extractBlock = (selector) => {
  const selectorIndex = source.indexOf(selector)
  assert.ok(selectorIndex >= 0, `missing selector: ${selector}`)
  const openIndex = source.indexOf('{', selectorIndex)
  assert.ok(openIndex > selectorIndex, `missing selector body: ${selector}`)
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    if (source[index] === '{') depth += 1
    if (source[index] === '}') {
      depth -= 1
      if (depth === 0) return source.slice(openIndex + 1, index)
    }
  }
  assert.fail(`unterminated selector: ${selector}`)
}

const pickerStart = source.indexOf('v-if="activePicker && isPqcMode"')
const pickerEnd = source.indexOf('</section>', pickerStart)
assert.ok(pickerStart >= 0 && pickerEnd > pickerStart, 'PQC picker must exist.')
const picker = source.slice(pickerStart, pickerEnd)

for (const token of [
  'data-pqc-order-option',
  'option.activeOrder',
  'data-pqc-order-option-code',
  'data-pqc-order-option-product',
  'data-pqc-order-option-quantity',
  '<span>编码</span>',
  '<span>产品</span>',
  '<span>数量</span>',
  '{{ option.activeOrder.workOrderCode }}',
  '{{ option.activeOrder.productName }}',
  '{{ formatProductionQuantity(option.activeOrder.quantity) }}'
]) {
  assert.ok(picker.includes(token), `PQC order option summary must include: ${token}`)
}

assert.match(
  source,
  /if \(activePicker\.value === 'order'\) \{[\s\S]*filteredActiveOrderOptions\.value\.map\(\(order\) => \(\{[\s\S]*activeOrder: order/,
  'Order picker options must preserve the formal active-order object for all three values.'
)

const summary = extractBlock('.frontline-order-picker-option {')
for (const token of [
  'display: grid;',
  'grid-template-rows: repeat(3, auto);',
  'width: 100%;',
  'min-width: 0;'
]) {
  assert.ok(summary.includes(token), `Order option summary style must include: ${token}`)
}

const row = extractBlock('.frontline-order-picker-option__row {')
for (const token of [
  'display: grid;',
  'grid-template-columns: 42px minmax(0, 1fr);',
  'min-width: 0;'
]) {
  assert.ok(row.includes(token), `Order option row style must include: ${token}`)
}

const value = extractBlock('.frontline-order-picker-option__value {')
for (const token of [
  'font-size: 15px;',
  'line-height: 1.15;',
  'overflow: visible;',
  'text-overflow: clip;',
  'white-space: normal;',
  'overflow-wrap: anywhere;'
]) {
  assert.ok(value.includes(token), `Order option value style must include: ${token}`)
}

const activeValue = extractBlock(
  '.frontline-picker--production-order .frontline-picker__option.active .frontline-order-picker-option__value {'
)
assert.ok(
  activeValue.includes('color: #ffffff;'),
  'Selected order summary values must remain readable on the dark active card.'
)

console.log('PASS: PQC order picker cards display complete code, product and quantity rows')
