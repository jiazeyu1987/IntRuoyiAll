const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start)
  assert.notEqual(end, -1, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end)
}

const packageJson = JSON.parse(readSource('package.json'))
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:list-detail-entry:static'],
  'node tests/e2e/dcc-list-detail-entry-static.spec.js',
  'package.json must expose the DCC list detail entry static contract'
)

const browserActionColumn = extractBetween(
  browserPage,
  '<el-table-column label="操作"',
  '</el-table-column>',
  'browser action column'
)

assert.match(browserActionColumn, /browser-row-actions/, 'browser actions must use compact row action layout')
assert.match(browserActionColumn, />\s*预览\s*</, 'browser action column must keep preview action')
assert.match(browserActionColumn, />\s*下载\s*</, 'browser action column must keep download action')
assert.match(browserActionColumn, />\s*更多\s*</, 'browser action column must keep more action')
assert.doesNotMatch(
  browserActionColumn,
  /mock|placeholder|fallback|降级|吞异常/i,
  'list detail entry must not introduce mock, fallback, downgrade, or swallowed errors'
)

console.log('PASS: DCC controlled-browser list detail entry static contract')
