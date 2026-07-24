const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

const readSource = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const pages = [
  {
    label: '点检保养项目',
    path: 'src/views/mes/dv/subject/index.vue',
    tableKey: 'mes.dv.subject.main',
    hookPrefix: 'subject'
  },
  {
    label: '点检保养方案',
    path: 'src/views/mes/dv/checkplan/index.vue',
    tableKey: 'mes.dv.checkPlan.main',
    hookPrefix: 'checkPlan'
  },
  {
    label: '点检记录',
    path: 'src/views/mes/dv/checkrecord/index.vue',
    tableKey: 'mes.dv.checkRecord.main',
    hookPrefix: 'checkRecord'
  },
  {
    label: '保养记录',
    path: 'src/views/mes/dv/maintenrecord/index.vue',
    tableKey: 'mes.dv.maintenRecord.main',
    hookPrefix: 'maintenRecord'
  },
  {
    label: '维修单',
    path: 'src/views/mes/dv/repair/index.vue',
    tableKey: 'mes.dv.repair.main',
    hookPrefix: 'repair'
  }
]

for (const page of pages) {
  const source = readSource(page.path)

  assert.match(
    source,
    /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index.vue'/,
    `${page.label} must import UnifiedListTemplate`
  )
  assert.match(
    source,
    /useUserTableColumns/,
    `${page.label} must use persisted user table columns`
  )
  assert.match(
    source,
    /useTableQuickFilter/,
    `${page.label} must use standard table quick filter`
  )
  assert.ok(
    source.includes(`table-key="${page.tableKey}"`),
    `${page.label} must use stable table key ${page.tableKey}`
  )
  assert.ok(
    source.includes(`data-user-table-key="${page.tableKey}"`),
    `${page.label} table must expose the same user table key`
  )
  assert.match(
    source,
    new RegExp(`const ${page.hookPrefix}DefaultColumns[\\s\\S]*UserTableColumnDefinition\\[\\]`),
    `${page.label} must declare default display columns`
  )
  assert.match(
    source,
    new RegExp(`const ${page.hookPrefix}QuickFilterDefinitions[\\s\\S]*TableQuickFilterDefinition`),
    `${page.label} must declare quick filter definitions`
  )
  assert.match(
    source,
    new RegExp(`is${page.hookPrefix[0].toUpperCase()}${page.hookPrefix.slice(1)}ColumnVisible\\('operation'\\)`),
    `${page.label} operation column must stay visible through column settings`
  )
  assert.ok(
    !source.includes('<template #extra-filters>'),
    `${page.label} must remove the duplicate red-box extra filter area`
  )
  assert.ok(
    !source.includes('<el-button @click="handleQuery">'),
    `${page.label} must remove the duplicate red-box search button`
  )
  assert.ok(
    !source.includes('<el-button @click="resetQuery">'),
    `${page.label} must remove the duplicate red-box reset button`
  )
}

console.log('PASS: MES DV equipment management standard list pages static contract')
