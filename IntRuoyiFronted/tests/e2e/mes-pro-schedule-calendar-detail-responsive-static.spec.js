const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/task/calendar/index.vue')

assert(fs.existsSync(pagePath), 'Schedule calendar page must exist.')

const source = fs.readFileSync(pagePath, 'utf8')
const styleStart = source.indexOf('<style scoped>')
const styleEnd = source.indexOf('</style>', styleStart)

assert.notEqual(styleStart, -1, 'Schedule calendar page must keep scoped styles.')
assert.notEqual(styleEnd, -1, 'Schedule calendar page scoped style must close.')

const styles = source.slice(styleStart, styleEnd)

function cssBlock(selector) {
  const start = styles.indexOf(`${selector} {`)
  assert.notEqual(start, -1, `Style block ${selector} must exist.`)
  const bodyStart = styles.indexOf('{', start) + 1
  const bodyEnd = styles.indexOf('\n}', bodyStart)
  assert.notEqual(bodyEnd, -1, `Style block ${selector} must close.`)
  return styles.slice(bodyStart, bodyEnd)
}

function assertContains(block, property, message) {
  assert.ok(block.includes(property), message)
}

assertContains(
  cssBlock('.action-row'),
  'flex-wrap: wrap',
  'Day detail action row must wrap buttons instead of overflowing the sidebar.'
)

assertContains(
  cssBlock('.action-row :deep(.el-button)'),
  'margin: 0',
  'Day detail action row must reset Element Plus adjacent button margins inside a flex gap.'
)

const detailGridBlock = cssBlock('.setting-grid,\n.preview-summary-grid,\n.detail-summary-grid')
assertContains(
  detailGridBlock,
  'min-width: 0',
  'Day detail summary grid must be allowed to shrink inside the sidebar.'
)
assertContains(
  detailGridBlock,
  'max-width: 100%',
  'Day detail summary grid must stay within the sidebar width.'
)

const totalCardBlock = cssBlock('.detail-total-quantity-card')
assertContains(
  totalCardBlock,
  'flex-wrap: wrap',
  'Total quantity card must wrap the label, value, and unit when space is tight.'
)
assertContains(
  totalCardBlock,
  'box-sizing: border-box',
  'Total quantity card padding and border must be included in its sidebar width.'
)

const totalValueBlock = cssBlock('.detail-total-quantity-card strong')
assertContains(
  totalValueBlock,
  'overflow-wrap: anywhere',
  'Total quantity value must stay visible instead of forcing horizontal overflow.'
)
assertContains(
  totalValueBlock,
  'max-width: 100%',
  'Total quantity value must not exceed the card width.'
)

console.log('PASS: MES schedule calendar day detail responsive overflow static contract')
