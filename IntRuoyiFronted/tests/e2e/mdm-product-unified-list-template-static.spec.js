const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/mdm/product/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')
const apiSource = fs.readFileSync(path.join(root, 'src/api/mdm/product/index.ts'), 'utf8')

assert.match(
  source,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index.vue'/,
  '产品主数据主列表必须导入标准列表模板。'
)
assert.match(
  source,
  /import \{ useUserTableColumns, type UserTableColumnDefinition \} from '@\/hooks\/web\/useUserTableColumns'/,
  '产品主数据主列表必须接入显示字段和列宽持久化 hook。'
)
assert.match(
  source,
  /useTableQuickFilter,[\s\S]*type TableQuickFilterDefinition[\s\S]*from '@\/hooks\/web\/useTableQuickFilter'/,
  '产品主数据主列表必须接入快速过滤 hook。'
)

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="mdm\.product\.main"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, '产品主数据主列表必须使用稳定 tableKey 接入标准列表模板。')
const template = templateMatch[0]

assert.match(template, /:query-model="queryParams"/, '标准模板必须绑定原查询参数。')
assert.match(
  template,
  /:quick-filter-state="productQuickFilter\.state"/,
  '标准模板必须绑定产品快速过滤状态。'
)
assert.match(
  template,
  /@update:quick-filter-state="productQuickFilter\.updateState"/,
  '标准模板必须同步快速过滤状态。'
)
assert.match(template, /@quick-filter-query="handleQuery"/, '快速过滤查询必须保留空输入重置逻辑。')
assert.match(template, /:columns="productColumns"/, '标准模板必须绑定显示字段配置。')
assert.match(template, /:show-column-reset="false"/, '产品主数据列表必须关闭重置列按钮。')
assert.match(
  template,
  /@column-change="saveProductColumnConfig"/,
  '显示字段变化必须自动保存。'
)
assert.match(template, /v-model:page="queryParams\.pageNo"/, '模板分页必须绑定 pageNo。')
assert.match(template, /v-model:limit="queryParams\.pageSize"/, '模板分页必须绑定 pageSize。')
assert.match(template, /@pagination="getList"/, '模板分页必须复用原列表查询。')
const actionsMatch = template.match(/<template #actions>([\s\S]*?)<\/template>/)
assert.ok(actionsMatch, '产品主数据列表必须保留业务操作插槽。')
const actions = actionsMatch[1]
assert.doesNotMatch(actions, /handleQuery/, '操作区不得重复显示搜索按钮。')
assert.doesNotMatch(actions, /resetQuery/, '操作区不得重复显示重置按钮。')
assert.doesNotMatch(actions, /openShowroomMappingDialog|展厅映射/, '操作区不得显示展厅映射按钮。')
assert.match(actions, /openForm\('create'\)/, '新增操作必须保留。')
assert.match(actions, /openImportDialog/, '导入操作必须保留。')
assert.match(actions, /handleExport/, '导出操作必须保留。')
assert.match(
  template,
  /<template\s+#table\b[^>]*>[\s\S]*<el-table[\s\S]*data-user-table-key="mdm\.product\.main"[\s\S]*@header-dragend="handleProductHeaderDragend"/,
  '产品表格必须接入列宽拖拽持久化。'
)
assert.match(
  template,
  /<el-table[\s\S]*class="mdm-product-resizable-table"[\s\S]*border[\s\S]*:allow-drag-last-column="true"/,
  '产品主数据表格必须显式启用含末列在内的列宽拖拽。'
)
assert.match(
  source,
  /\.mdm-product-resizable-table[\s\S]*th\.el-table__cell::after[\s\S]*width:\s*8px[\s\S]*cursor:\s*col-resize/,
  '产品主数据表头必须提供可见且可命中的列宽拖拽区域。'
)

for (const field of [
  'productCode',
  'nameCn',
  'nameEn',
  'modelSpecification',
  'category',
  'status',
  'updateTime',
  'actions'
]) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`), `${field} 必须注册到产品列配置。`)
  assert.match(
    template,
    new RegExp(`isProductColumnVisible\\('${field}'\\)`),
    `${field} 列必须受显示字段配置控制。`
  )
}

assert.match(
  source,
  /const productQuickFilterDefinitions: TableQuickFilterDefinition\[\] = \[/,
  '必须定义产品主数据快速过滤字段。'
)
assert.match(
  source,
  /key: 'keyword'[\s\S]*label: '关键词'[\s\S]*queryParamKey: 'keyword'/,
  '快速过滤必须支持关键词并写入原 keyword 查询参数。'
)
assert.match(
  source,
  /key: 'productCode'[\s\S]*label: '产品编码'[\s\S]*queryParamKey: 'productCode'/,
  '快速过滤必须支持产品编码并写入原 productCode 查询参数。'
)
assert.match(
  source,
  /key: 'status'[\s\S]*label: '状态'[\s\S]*type: 'select'[\s\S]*queryParamKey: 'status'/,
  '快速过滤必须支持状态并写入原 status 查询参数。'
)
assert.match(
  source,
  /useTableQuickFilter\([\s\S]*'mdm\.product\.main'[\s\S]*productQuickFilterDefinitions[\s\S]*queryParams[\s\S]*getList/,
  '快速过滤必须用稳定 tableKey 连接原查询参数和列表加载。'
)
assert.match(
  source,
  /useUserTableColumns\('mdm\.product\.main', productDefaultColumns\)/,
  '显示字段必须使用稳定 tableKey 持久化。'
)
const mainListEnd = source.indexOf('<el-dialog')
const mainListSource = source.slice(0, mainListEnd)
assert.doesNotMatch(
  mainListSource,
  /<Pagination[\s\S]*@pagination="getList"[\s\S]*\/>/,
  '产品主数据主列表分页必须由标准列表模板承载。'
)
assert.doesNotMatch(
  mainListSource,
  /<el-form[\s\S]*class="-mb-15px"/,
  '产品主数据主列表必须移除旧独立搜索表单。'
)
assert.doesNotMatch(source, /showroomMapping|ShowroomProductMapping|展厅映射/, '展厅映射页面逻辑必须删除。')
assert.doesNotMatch(
  apiSource,
  /ShowroomProductMapping|previewShowroomProductMapping|confirmShowroomProductMapping|mdm-mapping-(preview|confirm)/,
  '展厅映射前端 API 必须删除。'
)

console.log('PASS: mdm product unified list template static contract')
