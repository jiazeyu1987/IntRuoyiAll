const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const sourcePath = path.resolve(
  __dirname,
  '../../src/components/UserTableColumnGlobalEnhancer/index.vue'
)
const source = fs.readFileSync(sourcePath, 'utf8')
const observerSource = source.slice(
  source.indexOf('const attachTableObserver'),
  source.indexOf('const isHeaderResizeGesture')
)

assert.match(
  observerSource,
  /observe\(table\.tableEl,\s*\{\s*childList:\s*true,\s*subtree:\s*true\s*\}\)/s,
  'managed table observer must continue to detect structural child changes'
)
assert.doesNotMatch(
  observerSource,
  /attributes:\s*true|attributeFilter/,
  'managed table observer must not observe attributes rewritten by applyTableState'
)

console.log('user table column observer loop static contract passed')
