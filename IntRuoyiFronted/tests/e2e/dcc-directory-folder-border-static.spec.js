const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const directoryPage = readSource('src/views/dcc/controlled-file/directories/index.vue')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const directoryTable = extractBetween(directoryPage, '<el-table', '</el-table>')
const styles = extractBetween(directoryPage, '<style scoped>', '</style>')

assert.equal(
  packageJson.scripts?.['e2e:dcc:directory-folder-border:static'],
  'node tests/e2e/dcc-directory-folder-border-static.spec.js',
  'package.json must expose the DCC directory folder border static contract'
)

assert.match(
  directoryTable,
  /class="directory-folder-toggle"[\s\S]*:class="resolveDirectoryFolderBorderClass\(row\)"/,
  'folder icon wrapper must bind the border-color state from the row'
)
assert.match(
  directoryPage,
  /const resolveDirectoryFolderBorderClass = \(row: ControlledFileDirectoryVO\) =>/,
  'directory page must expose an explicit folder border class resolver'
)
assert.match(
  directoryPage,
  /isDirectoryExpandable\(row\)\s*\?\s*'directory-folder-toggle--has-children'\s*:\s*'directory-folder-toggle--no-children'/,
  'folder border resolver must use the formal child-folder state'
)
assert.match(
  styles,
  /\.directory-folder-toggle--has-children\s*\{[\s\S]*color:\s*#16a34a;/,
  'folders with child folders must use a green border/icon outline'
)
assert.match(
  styles,
  /\.directory-folder-toggle--no-children\s*\{[\s\S]*color:\s*#111827;/,
  'folders without child folders must use a black border/icon outline'
)
assert.doesNotMatch(
  directoryTable + styles,
  /mock|placeholder data|fallback|降级|吞异常/i,
  'folder border color must not introduce mock data, fallback, degradation, or swallowed errors'
)

console.log('PASS: DCC directory folder border static contract')
