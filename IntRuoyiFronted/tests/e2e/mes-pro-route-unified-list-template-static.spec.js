const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/mes/pro/route/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

assert.match(
  source,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index.vue'/,
  '工艺流程列表必须导入标准列表模板。'
)
assert.match(
  source,
  /import \{ useUserTableColumns, type UserTableColumnDefinition \} from '@\/hooks\/web\/useUserTableColumns'/,
  '工艺流程列表必须接入显示字段和列宽持久化 hook。'
)
assert.match(
  source,
  /useTableQuickFilter,[\s\S]*type TableQuickFilterDefinition[\s\S]*from '@\/hooks\/web\/useTableQuickFilter'/,
  '工艺流程列表必须接入标准快速过滤 hook。'
)

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="mes\.pro\.route\.main"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, '工艺流程列表必须用 tableKey mes.pro.route.main 接入 UnifiedListTemplate。')
const template = templateMatch[0]

assert.match(template, /:query-model="queryParams"/, '标准模板必须绑定原查询参数。')
assert.match(template, /:quick-filter-state="routeQuickFilter\.state"/, '标准模板必须绑定快速过滤状态。')
assert.match(template, /@quick-filter-query="routeQuickFilter\.applyQuickFilter"/, '标准模板必须触发快速过滤查询。')
assert.match(template, /:columns="routeColumns"/, '标准模板必须绑定显示字段配置。')
assert.match(template, /@column-change="saveRouteColumnConfig"/, '显示字段变化必须自动保存。')
assert.match(template, /:show-column-settings="false"/, '标准模板内置右侧显示字段入口必须关闭。')
assert.doesNotMatch(template, /@column-reset="resetRouteColumnConfig"/, '标准模板不应保留列配置重置入口。')
assert.match(template, /<template #extra-filters>[\s\S]*<UserTableColumnSettings[\s\S]*:show-reset="false"/, '显示字段必须通过 extra-filters 移动到快速过滤右侧并隐藏重置列。')
assert.match(template, /v-model:page="queryParams\.pageNo"/, '标准模板分页必须绑定 pageNo。')
assert.match(template, /v-model:limit="queryParams\.pageSize"/, '标准模板分页必须绑定 pageSize。')
assert.match(template, /@pagination="getList"/, '标准模板分页必须复用原列表查询。')
assert.match(template, /<template #actions>[\s\S]*handleRouteWorkbookExcelImport[\s\S]*handleExport/, '标准模板工具栏必须保留单一路线导入和导出操作。')
assert.doesNotMatch(template, /handleMarkdownImport/, '标准模板工具栏不应再保留 Markdown 导入操作。')
assert.doesNotMatch(template, /handleSheet1ExcelImport/, '标准模板工具栏不应再保留 Sheet1 Excel 导入操作。')
assert.match(template, /<template\s+#table\b[^>]*>[\s\S]*<el-table[\s\S]*data-user-table-key="mes\.pro\.route\.main"[\s\S]*@header-dragend="handleRouteHeaderDragend"/, '表格必须接入列宽拖拽持久化。')

for (const field of [
  'code',
  'name',
  'ownerName',
  'keyProcessName',
  'status',
  'flowGraphConfigured',
  'activeRouteVersionNo',
  'pendingRouteVersionNo',
  'productCodes',
  'createTime'
]) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`), `${field} 必须注册到工艺流程列表列配置。`)
  assert.match(
    template,
    new RegExp(`isRouteColumnVisible\\('${field}'\\)`),
    `${field} 列必须受显示字段配置控制。`
  )
}

assert.match(source, /key:\s*'actions'[\s\S]*label:\s*'操作'[\s\S]*hideable:\s*false/, '操作列必须注册且不可隐藏。')
assert.match(template, /label="操作"[\s\S]*fixed="right"/, '操作列必须固定在右侧。')
assert.doesNotMatch(template, /v-if="isRouteColumnVisible\('actions'\)"/, '操作列不应受显示字段配置隐藏。')

assert.match(source, /const routeQuickFilterDefinitions: TableQuickFilterDefinition\[\] = \[/, '必须定义工艺流程快速过滤字段。')
assert.match(source, /key: 'code'[\s\S]*label: '路线编码'[\s\S]*queryParamKey: 'code'/, '快速过滤必须支持路线编码。')
assert.match(source, /key: 'name'[\s\S]*label: '路线名称'[\s\S]*queryParamKey: 'name'/, '快速过滤必须支持路线名称。')
assert.match(source, /key: 'status'[\s\S]*label: '状态'[\s\S]*type: 'select'[\s\S]*queryParamKey: 'status'/, '快速过滤必须支持状态。')
assert.match(source, /const routeQuickFilter = useTableQuickFilter\([\s\S]*'mes\.pro\.route\.main'[\s\S]*routeQuickFilterDefinitions[\s\S]*queryParams[\s\S]*getList/, '必须用标准 hook 连接快速过滤和查询。')
assert.match(source, /useUserTableColumns\('mes\.pro\.route\.main', routeDefaultColumns\)/, '必须用稳定 tableKey 保存显示字段。')

assert.doesNotMatch(source, /class="-mb-15px"/, '旧搜索栏样式必须移除。')
assert.doesNotMatch(source, /<Pagination[\s\S]*@pagination="getList"[\s\S]*\/>/, '分页必须由标准列表模板承载。')

console.log('PASS: mes pro route unified list template static contract')
