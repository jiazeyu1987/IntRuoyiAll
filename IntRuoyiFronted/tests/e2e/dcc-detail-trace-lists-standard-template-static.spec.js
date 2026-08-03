const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src/views/dcc/controlled-file/detail/index.vue')
const source = fs.readFileSync(detailPath, 'utf8')

const expectMatch = (pattern, message) => {
  assert.match(source, pattern, message)
}

expectMatch(
  /import\s+UnifiedListTemplate\s+from\s+['"]@\/components\/UnifiedListTemplate\/index\.vue['"]/,
  'DCC detail page must import the standard list template'
)

const expectedLists = [
  {
    title: '审批路线快照',
    tableKey: 'dcc.controlledFile.detail.routeSnapshot',
    data: 'pagedRouteSnapshotRows',
    columns: 'routeSnapshotColumns',
    saving: 'routeSnapshotColumnSaving',
    change: 'saveRouteSnapshotColumnConfig',
    reset: 'resetRouteSnapshotColumnConfig',
    drag: 'handleRouteSnapshotHeaderDragend'
  },
  {
    title: '版本历史',
    tableKey: 'dcc.controlledFile.detail.versionHistory',
    data: 'pagedVersionHistoryRows',
    columns: 'versionHistoryColumns',
    saving: 'versionHistoryColumnSaving',
    change: 'saveVersionHistoryColumnConfig',
    reset: 'resetVersionHistoryColumnConfig',
    drag: 'handleVersionHistoryHeaderDragend'
  },
  {
    title: '分发状态',
    tableKey: 'dcc.controlledFile.detail.distributionStatus',
    data: 'pagedDistributionStatusRows',
    columns: 'distributionStatusColumns',
    saving: 'distributionStatusColumnSaving',
    change: 'saveDistributionStatusColumnConfig',
    reset: 'resetDistributionStatusColumnConfig',
    drag: 'handleDistributionStatusHeaderDragend'
  }
]

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
  assert.ok(block.includes(`:column-saving="${list.saving}"`), `${list.title} must expose column save state`)
  assert.ok(block.includes(`@column-change="${list.change}"`), `${list.title} must autosave column changes`)
  assert.ok(block.includes(`@column-reset="${list.reset}"`), `${list.title} must reset column config through standard template`)
  assert.ok(block.includes(`:data="${list.data}"`), `${list.title} table must use paged rows from the standard template state`)
  assert.ok(block.includes('data-user-table-column-explicit'), `${list.title} table must opt into explicit column settings`)
  assert.ok(block.includes(`data-user-table-key="${list.tableKey}"`), `${list.title} table must expose matching data-user-table-key`)
  assert.ok(block.includes(`@header-dragend="${list.drag}"`), `${list.title} must persist dragged column widths`)
  assert.doesNotMatch(
    block,
    /fallback|mock|placeholder|降级|吞异常/,
    `${list.title} standard list block must not introduce fallback, mock, placeholder, downgrade, or swallowed errors`
  )
}

for (const tableKey of expectedLists.map((item) => item.tableKey)) {
  const escaped = tableKey.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  expectMatch(
    new RegExp(`useUserTableColumns\\('${escaped}',`),
    `${tableKey} must use user column configuration hook`
  )
}

assert.doesNotMatch(
  source,
  /<el-table\s+:data="routeSnapshotRows"|<el-table\s+:data="fileDetail\?\.versionHistory \|\| \[\]"|<el-table\s*\n\s*:data="fileDetail\?\.distributionStatuses \|\| \[\]"/,
  'target lists must not remain as raw el-table blocks outside the standard list template'
)

console.log('PASS: DCC detail trace lists use standard list template')
