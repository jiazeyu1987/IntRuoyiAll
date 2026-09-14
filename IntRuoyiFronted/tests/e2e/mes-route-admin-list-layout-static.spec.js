const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/mes/pro/route/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

assert.match(
  source,
  /const ROUTE_LIST_TABLE_KEY = 'mes\.pro\.route\.main\.admin-layout-v1'/,
  '工艺路线列表必须升级用户列配置 key，使旧账号配置不再覆盖 admin 默认布局。'
)
assert.match(
  source,
  /:table-key="ROUTE_LIST_TABLE_KEY"/,
  '标准列表模板必须使用升级后的统一 table key。'
)
assert.match(
  source,
  /:data-user-table-key="ROUTE_LIST_TABLE_KEY"/,
  'Element Plus 表格标识必须与统一 table key 保持一致。'
)
assert.match(
  source,
  /useUserTableColumns\(ROUTE_LIST_TABLE_KEY,\s*routeDefaultColumns\)/,
  '显示字段保存必须使用升级后的统一 table key。'
)
assert.doesNotMatch(
  source,
  /table-key="mes\.pro\.route\.main"|data-user-table-key="mes\.pro\.route\.main"|useUserTableColumns\('mes\.pro\.route\.main'/,
  '工艺路线列表不得继续读取旧的个人列配置 key。'
)

const defaultColumnsMatch = source.match(
  /const routeDefaultColumns: UserTableColumnDefinition\[\] = \[([\s\S]*?)\n\]/
)
assert.ok(defaultColumnsMatch, '必须定义工艺路线默认列。')
const defaultColumns = defaultColumnsMatch[1]

for (const key of ['ownerName', 'keyProcessName', 'flowGraphConfigured']) {
  assert.match(
    defaultColumns,
    new RegExp(`\\{ key: '${key}',[^\\n]*visible: false[^\\n]*\\}`),
    `admin 默认布局必须隐藏 ${key} 列。`
  )
}

for (const key of [
  'code',
  'name',
  'status',
  'activeRouteVersionNo',
  'pendingRouteVersionNo',
  'productCodes',
  'createTime',
  'actions'
]) {
  const columnMatch = defaultColumns.match(new RegExp(`\\{ key: '${key}',[^\\n]*\\}`))
  assert.ok(columnMatch, `admin 默认布局必须保留 ${key} 列。`)
  assert.doesNotMatch(columnMatch[0], /visible:\s*false/, `${key} 列默认必须显示。`)
}

assert.match(source, /<UserTableColumnSettings/, '统一默认布局后必须继续保留“显示字段”入口。')
assert.match(
  source,
  /<UserTableColumnSettings[\s\S]*@change="saveRouteColumnConfig"/,
  '升级后的显示字段配置必须继续自动保存。'
)

for (const permission of [
  'mes:pro-route:create',
  'mes:pro-route:export',
  'mes:pro-route:update',
  'mes:pro-route:version-query',
  'mes:pro-route:delete'
]) {
  assert.match(
    source,
    new RegExp(`v-hasPermi="\\['${permission.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}'\\]"`),
    `布局统一不得移除权限控制：${permission}`
  )
}

console.log('PASS: mes route list uses unified admin default column layout')
