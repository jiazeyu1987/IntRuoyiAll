const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')

assert(fs.existsSync(pagePath), 'Schedule order page must exist.')

const pageSource = fs.readFileSync(pagePath, 'utf8')

assert.ok(
  /const formatQuantity = \(value\?: number\) => \{[\s\S]*Math\.round\(quantity\)[\s\S]*: '0'/.test(pageSource),
  'Schedule order quantity display must round to integer without decimal places.'
)
assert.doesNotMatch(
  pageSource,
  /const formatQuantity = \(value\?: number\) => \{[\s\S]*toFixed\(2\)[\s\S]*'0\.00'/,
  'Schedule order quantity formatter must not keep two decimal places.'
)
for (const quantityToken of [
  'formatQuantity(row.totalQuantity ?? row.quantity)',
  'formatQuantity(feedback.feedbackQuantity)',
  'formatQuantity(row.plannedQuantity)',
  'formatQuantity(row.actualQuantity)',
  'formatQuantity(row.diffQuantity)',
  'formatQuantity(row.shortageQty)'
]) {
  assert.ok(pageSource.includes(quantityToken), `Schedule order quantity token must use integer formatter: ${quantityToken}`)
}
assert.doesNotMatch(pageSource, /catch\s*\{\s*\}/, 'Schedule order integer quantity fix must not introduce empty catch blocks.')

console.log('PASS: MES schedule order integer quantity static contract')
