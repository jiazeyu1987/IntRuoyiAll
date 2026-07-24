const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const unifiedTemplate = readSource('src/components/UnifiedListTemplate/index.vue')
const userTableColumnsHook = readSource('src/hooks/web/useUserTableColumns.ts')
const projectCodePage = readSource('src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue')

assert.equal(
  packageJson.scripts['e2e:unified-list-template-sort:static'],
  'node tests/e2e/unified-list-template-sort-static.spec.js',
  'package.json must expose the standard list template sort static contract script'
)

assert.match(
  userTableColumnsHook,
  /sortable\?:\s*boolean\s*\|\s*'custom'/,
  'standard list column definitions must allow explicit sortable override metadata'
)
assert.match(
  userTableColumnsHook,
  /sortProp\?:\s*string/,
  'standard list column definitions must allow the displayed column key to map to a sort prop'
)

for (const [pattern, description] of [
  [/sortableColumns\?:\s*UnifiedListSortableColumnInput\[\]/, 'optional explicit sortable columns prop'],
  [/sortState\?:\s*UnifiedListSortState/, 'controlled sort state prop'],
  [/'update:sortState':\s*\[state:\s*UnifiedListSortState\]/, 'sort state update emit'],
  [/'sort-change':\s*\[state:\s*UnifiedListSortChange\]/, 'normalized sort change emit'],
  [/:sort-state="normalizedSortState"/, 'table slot sort state exposure'],
  [/:sortable-column-map="standardSortableColumnMap"/, 'table slot sortable column map exposure'],
  [/:sort-column-attrs="getStandardSortColumnAttrs"/, 'table slot sortable column attrs helper'],
  [/:handle-sort-change="handleStandardSortChange"/, 'table slot sort-change adapter'],
  [/const standardSortableColumns = computed/, 'computed sortable column registry'],
  [/const getStandardSortColumnAttrs = \(/, 'standard sort column attr helper'],
  [/sortable:\s*sortableColumn\.sortable\s*\|\|\s*DEFAULT_COLUMN_SORTABLE/, 'standard sortable columns default without per-column declarations'],
  [/const handleStandardSortChange = \(/, 'standard sort-change adapter'],
  [/emit\('update:sortState', nextState\)/, 'sort state update is emitted'],
  [/emit\('sort-change', \{ \.\.\.nextState, column: payload\?\.column \}\)/, 'normalized sort event is emitted']
]) {
  assert.match(unifiedTemplate, pattern, `UnifiedListTemplate must provide ${description}`)
}

assert.match(
  unifiedTemplate,
  /\.unified-list-template__table-shell\s+:deep\(\.el-table th\.is-sortable\)/,
  'standard list template must expose clickable header cursor affordance for sortable columns'
)

assert.match(
  unifiedTemplate,
  /const DEFAULT_COLUMN_SORTABLE = true/,
  'standard list template business columns must be sortable by default'
)
assert.match(
  unifiedTemplate,
  /column\.sortable !== false/,
  'standard list template must let pages explicitly opt out of default sorting'
)
assert.match(
  unifiedTemplate,
  /column\.business !== false/,
  'standard list template must not default non-business columns to sortable'
)
assert.match(
  unifiedTemplate,
  /column\.hideable !== false/,
  'standard list template must not default fixed structural columns to sortable'
)
assert.doesNotMatch(
  unifiedTemplate,
  /if \(!column\.sortable\) continue/,
  'standard list template must not require per-column sortable declarations'
)
assert.doesNotMatch(
  projectCodePage,
  /\{ key:\s*'associatedFileCount', label:\s*'关联文件数', width:\s*120, sortable:\s*'custom' \}/,
  'project code associated file count column must not need explicit sortable metadata'
)
assert.match(
  projectCodePage,
  /@sort-change="handleSortChange"/,
  'project code page must receive normalized standard-list sort events from UnifiedListTemplate'
)
assert.match(
  projectCodePage,
  /<template #table="\{ sortColumnAttrs, handleSortChange: handleTemplateSortChange \}">/,
  'project code table slot must consume standard-list sort helpers'
)
assert.match(
  projectCodePage,
  /@sort-change="handleTemplateSortChange"/,
  'project code el-table must delegate Element Plus sort-change to the template adapter'
)
assert.match(
  projectCodePage,
  /v-bind="sortColumnAttrs\('associatedFileCount'\)"/,
  'project code associated file count header must get sortable attrs from the standard list template'
)
assert.doesNotMatch(
  unifiedTemplate,
  /mock|placeholder data|fallback|降级|吞异常/i,
  'standard list sort implementation must not introduce mock, placeholder, fallback, downgrade, or swallowed errors'
)

console.log('PASS: standard list template sort static contract')
