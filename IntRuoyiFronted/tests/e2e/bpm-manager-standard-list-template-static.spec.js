const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const pages = [
  {
    name: '流程表单',
    path: 'src/views/bpm/form/index.vue',
    tableKey: 'bpm.form.main',
    columnPrefix: 'Form',
    columns: ['id', 'name', 'status', 'remark', 'createTime', 'actions'],
    preservedActions: [
      "openForm('create')",
      "openForm('copy'",
      "openForm('update'",
      'openDetail(scope.row.id)',
      'handleDelete(scope.row.id)'
    ],
    requiredPermissions: [
      'bpm:form:create',
      'bpm:form:update',
      'bpm:form:query',
      'bpm:form:delete'
    ]
  },
  {
    name: '流程分类',
    path: 'src/views/bpm/category/index.vue',
    tableKey: 'bpm.category.main',
    columnPrefix: 'Category',
    columns: ['id', 'name', 'code', 'description', 'status', 'sort', 'createTime', 'actions'],
    preservedActions: ["openForm('create')", "openForm('update'", 'handleDelete(scope.row.id)'],
    requiredPermissions: ['bpm:category:create', 'bpm:category:update', 'bpm:category:delete']
  },
  {
    name: '用户分组',
    path: 'src/views/bpm/group/index.vue',
    tableKey: 'bpm.user-group.main',
    columnPrefix: 'UserGroup',
    columns: ['id', 'name', 'description', 'members', 'status', 'createTime', 'actions'],
    preservedActions: ["openForm('create')", "openForm('update'", 'handleDelete(scope.row.id)'],
    requiredPermissions: [
      'bpm:user-group:create',
      'bpm:user-group:update',
      'bpm:user-group:delete'
    ]
  },
  {
    name: '流程表达式',
    path: 'src/views/bpm/processExpression/index.vue',
    tableKey: 'bpm.process-expression.main',
    columnPrefix: 'ProcessExpression',
    columns: ['id', 'name', 'status', 'expression', 'createTime', 'actions'],
    preservedActions: ["openForm('create')", "openForm('update'", 'handleDelete(scope.row.id)'],
    requiredPermissions: [
      'bpm:process-expression:create',
      'bpm:process-expression:update',
      'bpm:process-expression:delete'
    ]
  }
]

for (const page of pages) {
  const source = readSource(page.path)
  const visibleFn = `is${page.columnPrefix}ColumnVisible`

  assert.match(
    source,
    /import\s+UnifiedListTemplate\s+from\s+['"]@\/components\/UnifiedListTemplate\/index\.vue['"]/,
    `${page.name} must import UnifiedListTemplate`
  )
  assert.match(
    source,
    new RegExp(`<UnifiedListTemplate[\\s\\S]*table-key="${page.tableKey.replace(/\./g, '\\.')}"`),
    `${page.name} must render the standard list template with a stable table key`
  )
  assert.match(
    source,
    new RegExp(`useUserTableColumns\\('${page.tableKey.replace(/\./g, '\\.')}',`),
    `${page.name} must use persisted standard column settings`
  )
  assert.match(
    source,
    new RegExp(`useTableQuickFilter\\(\\s*'${page.tableKey.replace(/\./g, '\\.')}'`),
    `${page.name} must use the standard quick filter`
  )
  assert.match(
    source,
    /data-user-table-column-explicit/,
    `${page.name} table must opt into explicit user column controls`
  )
  assert.match(
    source,
    /@header-dragend=/,
    `${page.name} must persist dragged column widths`
  )
  assert.doesNotMatch(
    source,
    /<ContentWrap>\s*<el-form[\s\S]*?<\/ContentWrap>\s*[\s\S]*?<ContentWrap>\s*<el-table/,
    `${page.name} must not keep the split legacy search/table ContentWrap layout`
  )
  assert.doesNotMatch(
    source,
    /<Pagination[\s\S]*@pagination="getList"/,
    `${page.name} pagination must be owned by UnifiedListTemplate`
  )

  for (const key of page.columns) {
    assert.match(
      source,
      new RegExp(`${visibleFn}\\('${key}'\\)`),
      `${page.name} column ${key} must be controlled by standard display-field settings`
    )
  }
  for (const action of page.preservedActions) {
    assert.match(
      source,
      new RegExp(action.replace(/[()']/g, '\\$&')),
      `${page.name} must preserve action: ${action}`
    )
  }
  for (const permission of page.requiredPermissions) {
    assert.match(source, new RegExp(permission), `${page.name} must preserve permission ${permission}`)
  }
}

console.log('PASS: BPM manager standard list template static contract')
