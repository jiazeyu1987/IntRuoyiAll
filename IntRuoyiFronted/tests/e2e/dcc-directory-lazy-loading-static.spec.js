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
const directoryApi = readSource('src/api/dcc/controlledFile/directories.ts')

assert.equal(
  packageJson.scripts?.['e2e:dcc:directory-lazy-loading:static'],
  'node tests/e2e/dcc-directory-lazy-loading-static.spec.js',
  'package.json must expose the DCC directory lazy loading static contract'
)

assert.match(
  directoryApi,
  /export const getDirectoryChildren = async/,
  'directory API must expose direct-child loading'
)
assert.match(
  directoryApi,
  /url: '\/dcc\/directories\/children'/,
  'direct-child API must call /dcc/directories/children'
)
assert.match(
  directoryApi,
  /export const searchDirectories = async/,
  'directory API must expose remote directory search'
)

assert.match(
  directoryPage,
  /getDirectoryChildren,[\s\S]*searchDirectories,[\s\S]*type ControlledFileDirectoryVO/,
  'directory page must import direct-child loading and remote search APIs'
)
assert.doesNotMatch(
  directoryPage,
  /getDirectoryTree/,
  'directory page must not load the full directory tree for initial rendering'
)
assert.match(
  directoryPage,
  /<el-table[\s\S]*\slazy[\s\S]*:load="loadDirectoryChildren"[\s\S]*:tree-props="\{ children: 'children', hasChildren: 'hasChildren' \}"/,
  'directory table must use Element Plus lazy tree loading with hasChildren'
)
assert.match(
  directoryPage,
  /useTreeTableExpand\(false\)/,
  'directory table must start collapsed so opening the page does not expand or render every branch'
)
assert.match(
  directoryPage,
  /const getList = async \(\) => \{[\s\S]*?directories\.value = await getDirectoryChildren\(\)/,
  'initial directory load must request only root directories'
)
assert.match(
  directoryPage,
  /const loadDirectoryChildren = async \([\s\S]*?getDirectoryChildren\(row\.id\)/,
  'expanding a directory row must request only that row direct children'
)
assert.match(
  directoryPage,
  /const loadDirectoryChildren = async \([\s\S]*?resolve\(children\)/,
  'lazy loader must resolve loaded children back to the table'
)
assert.match(
  directoryPage,
  /const handleQuery = async \([\s\S]*?searchDirectories\(appliedQueryParams\.name/,
  'keyword search must use remote directory search instead of filtering an already-loaded full tree'
)

assert.doesNotMatch(
  directoryPage,
  /mock|placeholder data|fallback|降级|吞异常/i,
  'directory lazy loading must not introduce mock data, fallback, degradation, or swallowed errors'
)

console.log('PASS: DCC directory lazy loading static contract')
