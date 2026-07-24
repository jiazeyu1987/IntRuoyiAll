const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const workOrderPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/workorder/index.vue')
const workOrderPageSource = fs.readFileSync(workOrderPagePath, 'utf8')

const codeColumnMatch = workOrderPageSource.match(
  /<el-table-column[\s\S]*?label="工单编号"[\s\S]*?<\/el-table-column\s*>/
)
assert(codeColumnMatch, 'Production work order page must render the work order code column.')

const codeColumnSource = codeColumnMatch[0]

assert(
  codeColumnSource.includes('@click="openForm(\'detail\', scope.row.id)"'),
  'Work order code column must preserve the existing detail entry click behavior.'
)

assert.doesNotMatch(
  codeColumnSource,
  /class="work-order-key-copy"/,
  'Work order code column must not render a copy button.'
)
assert.doesNotMatch(
  codeColumnSource,
  /aria-label="复制工单编号"/,
  'Work order code column must not keep copy aria label.'
)
assert.doesNotMatch(
  codeColumnSource,
  /title="复制工单编号"/,
  'Work order code column must not keep copy title.'
)
assert.doesNotMatch(codeColumnSource, /ep:copy-document/, 'Work order code column must not render copy icon.')
assert.doesNotMatch(
  workOrderPageSource,
  /handleCopyWorkOrderCode/,
  'Production work order page must not keep work order code copy handler.'
)
assert.doesNotMatch(
  workOrderPageSource,
  /navigator\.clipboard\.writeText/,
  'Production work order page must not write work order code to clipboard.'
)
assert(!/catch\s*\{\s*\}/.test(workOrderPageSource), 'Production work order page must not silently swallow failures.')

console.log('PASS: work order code copy button removal static contract')
