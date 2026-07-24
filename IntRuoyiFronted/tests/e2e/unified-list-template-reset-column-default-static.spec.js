const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const componentSource = fs.readFileSync(
  path.join(root, 'src/components/UnifiedListTemplate/index.vue'),
  'utf8'
)

assert.match(
  componentSource,
  /withDefaults\(defineProps[\s\S]*showColumnReset:\s*false/,
  'UnifiedListTemplate must hide the reset-column button by default; pages that need it must explicitly pass show-column-reset=true.'
)
assert.match(
  componentSource,
  /showColumnSettings:\s*true/,
  'UnifiedListTemplate must still show the display-field control by default.'
)
assert.match(
  componentSource,
  /showQuickFilter:\s*true/,
  'UnifiedListTemplate must still show quick filtering by default.'
)

console.log('PASS: unified list template reset-column default contract')
