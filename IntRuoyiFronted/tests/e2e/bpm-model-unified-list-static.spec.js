const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const source = readSource('src/views/bpm/model/index.vue')

assert.match(
  source,
  /import\s+UnifiedListTemplate\s+from\s+['"]@\/components\/UnifiedListTemplate\/index\.vue['"]/,
  'BPM model page must use the standard UnifiedListTemplate'
)
assert.match(
  source,
  /<UnifiedListTemplate[\s\S]*table-key="bpm\.model\.main"/,
  'BPM model page must render a single standard list with a stable table key'
)
assert.doesNotMatch(
  source,
  /<draggable\b|from\s+['"]vuedraggable['"]|CategoryDraggableModel/,
  'BPM model page must not render category grouped draggable cards'
)
assert.doesNotMatch(
  source,
  /categoryGroup\s*=\s*categoryList\.map|modelList\.filter\(\(model[\s\S]*?categoryName\s*==\s*category\.name/,
  'BPM model page must not filter models into category groups'
)
assert.match(
  source,
  /const\s+modelList\s*=\s*ref<[\s\S]*?>\(\[\]\)/,
  'BPM model page must keep the full model list as the primary data source'
)
assert.match(
  source,
  /const\s+filteredModelList\s*=\s*computed\(/,
  'BPM model page must filter the unified model list without dropping uncategorized models'
)
assert.match(
  source,
  /const\s+pagedModelList\s*=\s*computed\(/,
  'BPM model page must paginate the unified client-side model list'
)
assert.match(
  source,
  /<el-table[\s\S]*:data="pagedModelList"/,
  'BPM model page table must render the paged unified model list'
)

for (const label of [
  '流程名',
  '流程分类',
  '可见范围',
  '流程类型',
  '表单信息',
  '最后发布',
  '操作'
]) {
  assert.match(
    source,
    new RegExp(`label="${label}"`),
    `BPM model list must include column: ${label}`
  )
}

for (const key of [
  'name',
  'categoryName',
  'visibleRange',
  'type',
  'formInfo',
  'deployment',
  'actions'
]) {
  assert.match(
    source,
    new RegExp(`isModelColumnVisible\\('${key}'\\)`),
    `BPM model column ${key} must be controlled by the standard column settings`
  )
}

for (const action of [
  'openModelView(row)',
  'openCreateApprovalParticipantConfig',
  'openApprovalParticipantConfig(row)',
  "openModelForm('copy'",
  'handleDeploy',
  'handleDefinitionList',
  'handleChangeState',
  'handleClean',
  'handleDelete'
]) {
  assert.match(
    source,
    new RegExp(action.replace(/[()']/g, '\\$&')),
    `missing preserved model action: ${action}`
  )
}

assert.match(source, />\s*查看\s*</, 'BPM model action column must include a view action')
assert.match(
  source,
  /const\s+viewDetailVisible\s*=\s*ref\(false\)/,
  'BPM model page must own the view dialog state'
)
assert.match(
  source,
  /const\s+selectedModel\s*=\s*ref<ModelInfo\s*\|\s*null>\(null\)/,
  'BPM model page must track the selected model for viewing'
)
assert.match(
  source,
  /const\s+openModelView\s*=\s*async\s*\(row:\s*ModelInfo\)\s*=>/,
  'BPM model page must expose an async openModelView handler'
)
assert.match(
  source,
  /<Dialog\s+:title="modelApprovalRouteDialogTitle"/,
  'BPM model page must render a read-only model view dialog'
)

for (const [rawName, displayName] of [
  ['DCC Controlled File Approval', 'DCC 受控文件审批'],
  ['Expense Dept Leader Approval', '费用部门负责人审批'],
  ['eDHR Approval V1', 'eDHR 审批 V1']
]) {
  assert.match(
    source,
    new RegExp(`${rawName}[\\s\\S]*${displayName}|${displayName}[\\s\\S]*${rawName}`),
    `BPM model page must map ${rawName} to ${displayName}`
  )
}
assert.match(
  source,
  /resolveModelDisplayName\(row\)/,
  'BPM model page must render the translated model display name'
)

assert.match(
  source,
  /useUserTableColumns\('bpm\.model\.main',\s*modelDefaultColumns\)/,
  'BPM model page must use persisted standard list column settings'
)
assert.match(
  source,
  /useTableQuickFilter\(\s*'bpm\.model\.main'/,
  'BPM model page must use the standard quick filter'
)
assert.match(
  source,
  /ModelApi\.getModelList\(queryParams\.name\)/,
  'BPM model page must keep the existing model list API contract'
)
assert.match(
  source,
  /CategoryApi\.getCategorySimpleList\(\)/,
  'BPM model page must keep loading category data for filter/category creation context'
)

console.log('PASS: BPM model unified standard list static contract')
