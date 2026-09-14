const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPagePath = path.join(repoRoot, 'src/views/dcc/controlled-file/detail/index.vue')

assert.equal(fs.existsSync(detailPagePath), true, 'missing DCC controlled file detail page')

const source = fs.readFileSync(detailPagePath, 'utf8').replace(/\r\n/g, '\n')
const scriptStart = source.indexOf('<script lang="ts" setup>')
assert.notEqual(scriptStart, -1, 'DCC controlled file detail page must keep a TypeScript setup script')

const scriptEnd = source.indexOf('</script>', scriptStart)
assert.notEqual(scriptEnd, -1, 'DCC controlled file detail page setup script must close')

const setupScript = source.slice(scriptStart, scriptEnd)

assert.doesNotMatch(
  setupScript,
  /const\s+getPagedDetailRows\s*=\s*<\s*T\s*>\s*\(/,
  'getPagedDetailRows must not use generic arrow syntax that is ambiguous for Vue SFC parsing'
)

assert.match(
  setupScript,
  /function\s+getPagedDetailRows\s*<\s*T\s*>\s*\(\s*rows:\s*T\[\],\s*pageNo:\s*number,\s*pageSize:\s*number\s*\)/,
  'getPagedDetailRows must use a named generic function declaration so Vite/ESLint can parse the SFC'
)

console.log('PASS: DCC controlled file detail generic helper avoids Vue SFC parse ambiguity')
