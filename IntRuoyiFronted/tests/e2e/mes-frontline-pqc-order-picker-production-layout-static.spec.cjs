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
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) return source.slice(openIndex + 1, index)
    }
  }
  assert.fail(`unterminated selector body: ${selector}`)
}

const assertIncludesAll = (text, tokens, scope) => {
  for (const token of tokens) {
    assert.ok(text.includes(token), `${scope} must include: ${token}`)
  }
}

const extractConstFunction = (name) => {
  const declaration = `const ${name} = (`
  const start = source.indexOf(declaration)
  assert.ok(start >= 0, `missing function: ${name}`)
  const nextConst = source.indexOf('\nconst ', start + declaration.length)
  assert.ok(nextConst > start, `missing end for function: ${name}`)
  return source.slice(start, nextConst)
}

const pqcPickerStart = source.indexOf('v-if="activePicker && isPqcMode"')
assert.ok(pqcPickerStart >= 0, 'PQC picker overlay must exist.')
const pqcPickerEnd = source.indexOf('</section>', pqcPickerStart)
assert.ok(pqcPickerEnd > pqcPickerStart, 'PQC picker overlay must close before template end.')
const pqcPickerBlock = source.slice(pqcPickerStart, pqcPickerEnd)

assertIncludesAll(
  pqcPickerBlock,
  [
    ':class="{',
    "'frontline-picker--production-order': activePicker === 'order'"
  ],
  'PQC production-order picker must keep its dedicated order layout class.'
)

assertIncludesAll(
  extractBlock('.frontline-picker--production-order {'),
  [
    'z-index: 30;',
    'display: grid;',
    'place-items: center;',
    'border-radius: 0;',
    'background: rgba(17, 26, 21, 0.38);'
  ],
  'PQC production-order picker overlay style'
)

assertIncludesAll(
  extractBlock('.frontline-picker--production-order .frontline-picker__card {'),
  [
    'width: min(96%, 1770px);',
    'aspect-ratio: 1920 / 1080;',
    'grid-template-rows: auto minmax(0, 1fr) auto;',
    'padding: 32px;',
    'border: 3px solid var(--frontline-line);',
    'border-radius: 28px;',
    'background: var(--frontline-panel);'
  ],
  'PQC production-order picker card style'
)

assertIncludesAll(
  extractBlock('.frontline-picker--production-order .frontline-picker__options {'),
  [
    'grid-template-columns: repeat(6, minmax(0, 1fr));',
    'gap: 12px;',
    'align-content: start;',
    'min-height: 0;',
    'max-height: none;',
    'overflow: auto;'
  ],
  'PQC production-order picker option grid style'
)

assertIncludesAll(
  extractBlock('.frontline-picker--production-order .frontline-picker__option {'),
  [
    'display: flex;',
    'align-items: center;',
    'justify-content: center;',
    'height: auto;',
    'min-height: 132px;',
    'padding: 8px 10px;',
    'font-size: 15px;',
    'line-height: 1.1;',
    'text-align: center;',
    'word-break: break-word;',
    'overflow: visible;',
    'border-radius: 22px;'
  ],
  'PQC production-order picker option style'
)

assertIncludesAll(
  pqcPickerBlock,
  [
    'data-pqc-order-option',
    'option.activeOrder',
    'data-pqc-order-option-code',
    'data-pqc-order-option-product',
    'data-pqc-order-option-quantity',
    '{{ option.activeOrder.workOrderCode }}',
    '{{ option.activeOrder.productName }}',
    '{{ formatProductionQuantity(option.activeOrder.quantity) }}'
  ],
  'PQC production-order picker three-row summary'
)

assertIncludesAll(
  extractBlock('.frontline-picker--production-order .frontline-picker__close {'),
  ['height: 86px;', 'font-size: 36px;', 'border-radius: 22px;'],
  'PQC production-order picker close style'
)

const activeOrderLabelFunction = extractConstFunction('formatActiveOrderLabel')
assertIncludesAll(
  activeOrderLabelFunction,
  ['activeOrder.workOrderCode', 'activeOrder.workOrderName', '`订单 ${activeOrder.workOrderId}`'],
  'PQC production-order accessible option label'
)

console.log('PASS: PQC production-order picker matches production process picker layout')
