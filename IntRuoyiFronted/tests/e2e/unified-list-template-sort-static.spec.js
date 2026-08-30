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
const productCatalogPage = readSource(
  'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue'
)
const registrationCertificatePage = readSource(
  'src/views/dcc/registration-certificate/index/index.vue'
)
const routeProductListPage = readSource('src/views/mes/pro/route/RouteProductList.vue')
const schedulerWorkbenchPage = readSource('src/views/mes/pro/scheduler-workbench/index.vue')
const scheduleOrderPage = readSource('src/views/mes/pro/scheduleorder/index.vue')

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
  [/sortable:\s*column\.sortable\s*\?\?\s*DEFAULT_COLUMN_SORTABLE/, 'explicit column sortable metadata preservation'],
  [/sortable:\s*sortableColumn\.sortable\s*\?\?\s*DEFAULT_COLUMN_SORTABLE/, 'standard sortable attrs preserve explicit false and custom values'],
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
  /const DEFAULT_COLUMN_SORTABLE = false/,
  'standard list template business columns must not be sortable unless a page declares a formal sort capability'
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
  /sortable:\s*(?:column|sortableColumn)\.sortable\s*\|\|\s*DEFAULT_COLUMN_SORTABLE/,
  'standard list template must not overwrite sortable: false with the default'
)
assert.match(
  projectCodePage,
  /\{ key:\s*'associatedFileCount', label:\s*'关联文件数', width:\s*120, sortable:\s*'custom' \}/,
  'project code associated file count must explicitly opt into backend custom sorting'
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
for (const field of ['projectName', 'projectCode']) {
  assert.match(
    productCatalogPage,
    new RegExp(`\\{ key:\\s*'${field}'[^}]*sortable:\\s*'custom'`),
    `product catalog ${field} must explicitly opt into backend custom sorting`
  )
}
for (const field of [
  'certificateNo',
  'ownerCompanyName',
  'productName',
  'classification',
  'projectCode',
  'versionNo',
  'status',
  'hasProjectCode',
  'hasRegistrationFile',
  'approvalDate',
  'effectiveDate',
  'expiryDate',
  'reminder',
  'remark'
]) {
  assert.match(
    registrationCertificatePage,
    new RegExp(`\\{ key:\\s*'${field}'[^}]*sortable:\\s*'custom'`),
    `registration certificate current list ${field} must explicitly opt into backend custom sorting`
  )
}
const oldColumnDefinitions = registrationCertificatePage.match(
  /const oldColumnDefinitions:\s*UserTableColumnDefinition\[\]\s*=\s*\[([\s\S]*?)\]\s*\r?\n\s*const \{/
)
assert.ok(oldColumnDefinitions, 'registration certificate old list column definition block must exist')
for (const field of [
  'certificateNo',
  'ownerCompanyName',
  'productName',
  'classification',
  'versionNo',
  'status',
  'expiryDate'
]) {
  assert.match(
    oldColumnDefinitions[1],
    new RegExp(`\\{ key:\\s*'${field}'[^}]*sortable:\\s*'custom'`),
    `registration certificate old list ${field} must explicitly opt into backend custom sorting`
  )
}
for (const field of [
  'itemCode',
  'itemName',
  'specification',
  'unitName',
  'quantity',
  'productionTime',
  'remark'
]) {
  assert.match(
    routeProductListPage,
    new RegExp(`\\{ key:\\s*'${field}'[^}]*sortable:\\s*true`),
    `route product local full-list column ${field} must explicitly opt into local full-data sorting`
  )
}
for (const field of [
  'routeCode',
  'routeName',
  'processCode',
  'processName',
  'wipOrderCount',
  'shiftCapacityTotal',
  'shiftStatus',
  'nightShiftEnabled',
  'plannedStartDate',
  'unfinishedDemandQuantity',
  'estimatedStartTime',
  'estimatedCompletionTime',
  'todayFeedbackQuantity'
]) {
  assert.match(
    schedulerWorkbenchPage,
    new RegExp(`\\{ key:\\s*'${field}'[^}]*sortable:\\s*true`),
    `scheduler workbench process WIP local full-list column ${field} must explicitly opt into local full-data sorting`
  )
}
assert.match(
  scheduleOrderPage,
  /sortColumnAttrs\(\{ key:\s*'priorityNo', sortable:\s*'custom' \}\)/,
  'schedule order priority header must remain an explicit backend custom sort field'
)
assert.doesNotMatch(
  unifiedTemplate,
  /mock|placeholder data|fallback|降级|吞异常/i,
  'standard list sort implementation must not introduce mock, placeholder, fallback, downgrade, or swallowed errors'
)

console.log('PASS: standard list template sort static contract')
