const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)

assert.match(
  source,
  /:type="getProcessWipShiftStatusTagType\(\)"/,
  'The zero-argument shift status tag helper must be called without a row argument.'
)
assert.doesNotMatch(
  source,
  /:type="getProcessWipShiftStatusTagType\(row\)"/,
  'The template must not pass a row to the zero-argument shift status tag helper.'
)
assert.match(
  source,
  /const getProcessWipShiftStatusTagType = \(\) => 'success'/,
  'The shift status tag helper must remain a zero-argument constant mapping.'
)

console.log('PASS: scheduler workbench shift status type contract')
