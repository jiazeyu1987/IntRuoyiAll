import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '..')
const source = readFileSync(
  path.join(repoRoot, 'src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue'),
  'utf8'
)

test('dcc OnlyOffice viewer is configured as controlled read-only', () => {
  assert.match(source, /edit:\s*false/)
  assert.match(source, /comment:\s*false/)
  assert.match(source, /review:\s*false/)
  assert.match(source, /download:\s*false/)
  assert.match(source, /print:\s*false/)
  assert.match(source, /copy:\s*false/)
  assert.doesNotMatch(source, /download:\s*true/)
  assert.doesNotMatch(source, /print:\s*true/)
})

test('dcc OnlyOffice viewer does not rely on browser keyboard interception for copy protection', () => {
  assert.doesNotMatch(source, /clipboardData/)
  assert.doesNotMatch(source, /execCommand\s*\(\s*['"]copy['"]/)
  assert.doesNotMatch(source, /navigator\.clipboard/)
  assert.match(source, /copy:\s*false/)
  assert.match(source, /download:\s*false/)
})

test('dcc OnlyOffice viewer surfaces script and editor mount failures', () => {
  assert.match(source, /try\s*{[\s\S]*await loadOnlyOfficeScript\(baseUrl\)/)
  assert.match(source, /catch\s*\(\s*error\s*\)\s*{[\s\S]*errorMessage\.value\s*=/)
  assert.match(source, /window\.__dccOnlyOfficeScriptPromise\s*=\s*undefined/)
})

test('dcc OnlyOffice viewer surfaces document service errors from DocsAPI events', () => {
  assert.match(source, /events\s*:\s*{[\s\S]*onError\s*:/)
  assert.match(source, /errorCode/)
  assert.match(source, /errorDescription/)
  assert.match(source, /OnlyOffice 文档加载失败/)
})
