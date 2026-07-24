const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const source = fs.readFileSync(path.join(repoRoot, 'src/views/approval-center/index.vue'), 'utf8')

const applyRouteQueryMatch = source.match(/const applyRouteQuery = \(\) => \{([\s\S]*?)\n\}/)
assert(applyRouteQueryMatch, 'Approval center must keep an applyRouteQuery function')

const applyRouteQuery = applyRouteQueryMatch[1]

assert.match(
  applyRouteQuery,
  /const\s+shouldResetPage\s*=/,
  'Route sync must decide whether pageNo should reset from semantic route changes'
)
assert.match(
  applyRouteQuery,
  /queryParams\.viewType\s*!==\s*nextViewType/,
  'Route sync must compare viewType before resetting pageNo'
)
assert.match(
  applyRouteQuery,
  /\(queryParams\.moduleCode\s*\|\|\s*''\)\s*!==\s*\(nextModuleCode\s*\|\|\s*''\)/,
  'Route sync must compare moduleCode before resetting pageNo'
)
assert.match(
  applyRouteQuery,
  /queryParams\.keyword\s*!==\s*nextKeyword/,
  'Route sync must compare keyword before resetting pageNo'
)
assert.match(
  applyRouteQuery,
  /if\s*\(shouldResetPage\)\s*\{\s*queryParams\.pageNo\s*=\s*1\s*\}/,
  'Route sync may reset to first page only when tab/module/keyword changes'
)
assert.doesNotMatch(
  applyRouteQuery.replace(/if\s*\(shouldResetPage\)\s*\{\s*queryParams\.pageNo\s*=\s*1\s*\}/, ''),
  /queryParams\.pageNo\s*=\s*1/,
  'Route sync must not unconditionally reset pageNo on every route watcher run'
)

console.log('PASS: approval center pagination preserves page on route watcher refresh')
