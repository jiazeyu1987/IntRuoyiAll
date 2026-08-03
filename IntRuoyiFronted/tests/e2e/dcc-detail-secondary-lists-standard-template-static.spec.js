const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src/views/dcc/controlled-file/detail/index.vue')
const source = fs.readFileSync(detailPath, 'utf8')

const expectedLists = [
  {
    title: '受控打印记录',
    tableKey: 'dcc.controlledFile.detail.controlledPrintRecords',
    data: 'pagedControlledPrintRecordRows',
    columns: 'controlledPrintRecordColumns',
    saving: 'controlledPrintRecordColumnSaving',
    change: 'saveControlledPrintRecordColumnConfig',
    reset: 'resetControlledPrintRecordColumnConfig',
    drag: 'handleControlledPrintRecordHeaderDragend'
  },
  {
    title: '培训状态',
    tableKey: 'dcc.controlledFile.detail.trainingStatus',
    data: 'pagedTrainingStatusRows',
    columns: 'trainingStatusColumns',
    saving: 'trainingStatusColumnSaving',
    change: 'saveTrainingStatusColumnConfig',
    reset: 'resetTrainingStatusColumnConfig',
    drag: 'handleTrainingStatusHeaderDragend'
  },
  {
    title: '签核追溯',
    tableKey: 'dcc.controlledFile.detail.signatureTrace',
    data: 'pagedSignatureTraceRows',
    columns: 'signatureTraceColumns',
    drag: 'handleSignatureTraceHeaderDragend',
    hideToolbar: true
  }
]

assert.match(
  source,
  /import\s+UnifiedListTemplate\s+from\s+['"]@\/components\/UnifiedListTemplate\/index\.vue['"]/,
  'DCC detail page must import the standard list template'
)

for (const list of expectedLists) {
  const titleIndex = source.indexOf(list.title)
  assert.notEqual(titleIndex, -1, `${list.title} title must exist`)
  const templateStart = source.lastIndexOf('<UnifiedListTemplate', titleIndex)
  const templateEnd = source.indexOf('</UnifiedListTemplate>', titleIndex)
  assert.notEqual(templateStart, -1, `${list.title} must be inside UnifiedListTemplate`)
  assert.notEqual(templateEnd, -1, `${list.title} UnifiedListTemplate must close`)
  assert.ok(templateStart < titleIndex && titleIndex < templateEnd, `${list.title} title must be inside standard list template`)
  const block = source.slice(templateStart, templateEnd)
  assert.ok(block.includes(`table-key="${list.tableKey}"`), `${list.title} must use stable table key ${list.tableKey}`)
  assert.ok(block.includes(`:columns="${list.columns}"`), `${list.title} must bind standard list columns`)
  if (list.hideToolbar) {
    assert.ok(block.includes(':show-query-form="false"'), `${list.title} must hide the yellow-box toolbar`)
    assert.ok(!block.includes('show-column-reset'), `${list.title} must not expose the reset-column yellow-box control`)
    assert.ok(!block.includes('@column-change='), `${list.title} must not expose column settings after toolbar removal`)
    assert.ok(!block.includes('@column-reset='), `${list.title} must not expose column reset after toolbar removal`)
  } else {
    assert.ok(block.includes(`:column-saving="${list.saving}"`), `${list.title} must expose column save state`)
    assert.ok(block.includes(`@column-change="${list.change}"`), `${list.title} must autosave column changes`)
    assert.ok(block.includes(`@column-reset="${list.reset}"`), `${list.title} must reset column config through standard template`)
  }
  assert.ok(block.includes(`:data="${list.data}"`), `${list.title} table must use paged rows from the standard template state`)
  assert.ok(block.includes('data-user-table-column-explicit'), `${list.title} table must opt into explicit column settings`)
  assert.ok(block.includes(`data-user-table-key="${list.tableKey}"`), `${list.title} table must expose matching data-user-table-key`)
  assert.ok(block.includes(`@header-dragend="${list.drag}"`), `${list.title} must persist dragged column widths`)
  assert.ok(block.includes('@sort-change="handleTemplateSortChange"'), `${list.title} must delegate sorting to the standard template`)
  assert.doesNotMatch(
    block,
    /fallback|mock|placeholder|降级|吞异常/,
    `${list.title} standard list block must not introduce fallback, mock, placeholder, downgrade, or swallowed errors`
  )
}

for (const tableKey of expectedLists.map((item) => item.tableKey)) {
  const escaped = tableKey.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  assert.match(
    source,
    new RegExp(`useUserTableColumns\\('${escaped}',`),
    `${tableKey} must use user column configuration hook`
  )
}

assert.doesNotMatch(
  source,
  /<el-table\s*[\s\S]*?:data="controlledPrintRecords"|<el-table\s+:data="flattenedTrainingAssignments"|<el-table\s+:data="signatureTraceRows"/,
  'target secondary lists must not remain as raw el-table blocks outside the standard list template'
)

console.log('PASS: DCC detail secondary lists use standard list template')
