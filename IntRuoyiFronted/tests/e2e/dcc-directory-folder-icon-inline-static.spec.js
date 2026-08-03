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
const directoryNameLabelIndex = directoryTable.indexOf('label="目录名称"')
assert.ok(directoryNameLabelIndex >= 0, 'Missing directory name column label')
const directoryNameColumnStart = directoryTable.lastIndexOf(
  '<el-table-column',
  directoryNameLabelIndex
)
assert.ok(directoryNameColumnStart >= 0, 'Missing directory name column start')
const directoryNameColumnEnd = directoryTable.indexOf('</el-table-column>', directoryNameLabelIndex)
assert.ok(directoryNameColumnEnd > directoryNameColumnStart, 'Missing directory name column end')
const directoryNameColumn = directoryTable.slice(directoryNameColumnStart, directoryNameColumnEnd)
const styles = extractBetween(directoryPage, '<style scoped>', '</style>')

assert.equal(
  packageJson.scripts?.['e2e:dcc:directory-folder-icon-inline:static'],
  'node tests/e2e/dcc-directory-folder-icon-inline-static.spec.js',
  'package.json must expose the DCC directory folder icon inline static contract'
)

assert.match(directoryTable, /ref="directoryTableRef"/, 'directory table must expose a table ref')
assert.match(
  directoryNameColumn,
  /class-name="dcc-directory-name-column"/,
  'directory name column must have a stable class for scoped tree-icon styling'
)
assert.match(
  directoryNameColumn,
  /'directory-name-cell'/,
  'directory name content must use a dedicated inline cell wrapper'
)
assert.match(
  directoryNameColumn,
  /class="\['directory-name-cell', \{ 'is-expandable': isDirectoryExpandable\(row\) \}\]"/,
  'directory name cell wrapper must expose the expandable state for full-cell click affordance'
)
assert.match(
  directoryNameColumn,
  /role="button"/,
  'directory name cell wrapper must act as the clickable expand control'
)
assert.match(
  directoryNameColumn,
  /@click="toggleDirectoryRow\(row\)"/,
  'clicking anywhere in the directory name cell must expand or collapse the row'
)
assert.match(
  directoryNameColumn,
  /@keydown\.enter\.prevent="toggleDirectoryRow\(row\)"/,
  'keyboard Enter on the directory name cell must expand or collapse the row'
)
assert.match(
  directoryNameColumn,
  /@keydown\.space\.prevent="toggleDirectoryRow\(row\)"/,
  'keyboard Space on the directory name cell must expand or collapse the row'
)
assert.match(
  directoryNameColumn,
  /class="directory-folder-toggle"/,
  'directory rows must render a folder icon control before the name'
)
assert.match(
  directoryNameColumn,
  /<Icon icon="ep:folder"/,
  'directory rows must use the Element Plus folder icon shown in the target sidebar'
)
assert.doesNotMatch(
  directoryNameColumn,
  /@click\.stop="toggleDirectoryRow\(row\)"/,
  'folder icon alone must not own the expand click because the whole red-box name range should expand'
)
assert.match(
  directoryNameColumn,
  /class="directory-name-cell__text"/,
  'directory text must have its own ellipsis-safe inline element'
)
assert.match(
  directoryNameColumn,
  /resolveDirectoryChildLoadError\(row\)/,
  'child load error tag must remain visible beside the directory name'
)

assert.match(
  directoryPage,
  /const directoryTableRef = ref\(\)/,
  'directory table ref must be declared for Element Plus row expansion'
)
assert.match(
  directoryPage,
  /const isDirectoryExpandable = \(row: ControlledFileDirectoryVO\) =>/,
  'directory page must explicitly decide whether a folder can expand'
)
assert.match(
  directoryPage,
  /const toggleDirectoryRow = \(row: ControlledFileDirectoryVO\) =>/,
  'directory page must expose a folder-icon expand handler'
)
assert.match(
  directoryPage,
  /typeof toggleRowExpansion !== 'function'/,
  'folder expand handler must fail fast when Element Plus expansion is unavailable'
)
assert.match(
  directoryPage,
  /throw new Error\('文档目录表格展开方法不可用'\)/,
  'missing Element Plus expansion support must surface an explicit error'
)

assert.match(
  styles,
  /:deep\(\.dcc-directory-name-column\s+\.el-table__expand-icon\)\s*\{[\s\S]*display:\s*none;/,
  'default Element Plus triangle expand icon must be hidden in the directory name column'
)
assert.match(
  styles,
  /\.directory-name-cell\s*\{[\s\S]*display:\s*flex;[\s\S]*width:\s*100%;[\s\S]*align-items:\s*center;[\s\S]*white-space:\s*nowrap;/,
  'directory icon, text, and error tag must stay on one row across the full name cell width'
)
assert.match(
  styles,
  /\.directory-name-cell\.is-expandable\s*\{[\s\S]*cursor:\s*pointer;/,
  'expandable directory name cells must show that the full cell range is clickable'
)
assert.match(
  styles,
  /\.directory-folder-toggle\s*\{[\s\S]*display:\s*inline-flex;[\s\S]*align-items:\s*center;[\s\S]*color:\s*#1677ff;/,
  'folder icon control must match the blue operational icon style'
)
assert.match(
  styles,
  /\.directory-name-cell__text\s*\{[\s\S]*overflow:\s*hidden;[\s\S]*text-overflow:\s*ellipsis;[\s\S]*white-space:\s*nowrap;/,
  'directory text must remain ellipsis-safe after the icon is added'
)

assert.doesNotMatch(
  directoryNameColumn + styles,
  /mock|placeholder data|fallback|降级|吞异常/i,
  'folder icon display must not introduce mock data, fallback, degradation, or swallowed errors'
)

console.log('PASS: DCC directory folder icon inline static contract')
