const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const componentPath =
  'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue'
const source = fs.readFileSync(path.join(root, componentPath), 'utf8')

assert.match(
  source,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index\.vue'/,
  'DCC 产品目录必须导入标准列表模板。'
)
assert.match(
  source,
  /import \{ useUserTableColumns, type UserTableColumnDefinition \} from '@\/hooks\/web\/useUserTableColumns'/,
  'DCC 产品目录必须接入用户列配置。'
)
assert.match(
  source,
  /useTableQuickFilter,[\s\S]*type TableQuickFilterDefinition[\s\S]*from '@\/hooks\/web\/useTableQuickFilter'/,
  'DCC 产品目录必须接入快速过滤 hook。'
)

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="dcc\.productCatalog\.main"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, 'DCC 产品目录必须使用稳定 tableKey 接入标准列表模板。')
const template = templateMatch[0]

assert.match(template, /:query-model="queryParams"/, '标准模板必须绑定原查询参数。')
assert.match(
  template,
  /:filter-definitions="productCatalogQuickFilterDefinitions"/,
  '标准模板必须绑定产品目录快速过滤定义。'
)
assert.match(
  template,
  /:quick-filter-state="productCatalogQuickFilter\.state"/,
  '标准模板必须绑定产品目录快速过滤状态。'
)
assert.match(
  template,
  /@update:quick-filter-state="productCatalogQuickFilter\.updateState"/,
  '标准模板必须同步快速过滤状态。'
)
assert.match(
  template,
  /@quick-filter-query="productCatalogQuickFilter\.applyQuickFilter"/,
  '标准模板查询和重置必须复用快速过滤 hook。'
)
assert.match(template, /:columns="productCatalogColumns"/, '标准模板必须绑定显示字段配置。')
assert.match(
  template,
  /@column-change="saveProductCatalogColumnConfig"/,
  '显示字段变化必须自动保存。'
)
assert.match(template, /v-model:page="queryParams\.pageNo"/, '模板分页必须绑定 pageNo。')
assert.match(template, /v-model:limit="queryParams\.pageSize"/, '模板分页必须绑定 pageSize。')
assert.match(template, /@pagination="getList"/, '模板分页必须复用原列表查询。')
const actionsMatch = template.match(/<template #actions>([\s\S]*?)<\/template>/)
assert.ok(actionsMatch, '产品目录必须保留 actions 插槽承载新增入口。')
const actions = actionsMatch[0]
assert.match(actions, /openForm\('create'\)/, '产品目录 actions 插槽必须保留新增产品目录。')
assert.doesNotMatch(
  actions,
  /productCatalogQuickFilter\.resetQuickFilter|handleCompareRegistrationExpiry|>\s*重置\s*<|>\s*注册证有效期\s*</,
  '产品目录 actions 插槽不得继续渲染重置和注册证有效期按钮。'
)
assert.match(
  template,
  /<template\s+#table\b[^>]*>[\s\S]*<el-table[\s\S]*data-user-table-key="dcc\.productCatalog\.main"[\s\S]*@header-dragend="handleProductCatalogHeaderDragend"/,
  '产品目录表格必须接入列宽拖拽持久化。'
)
assert.match(
  template,
  /<el-table[\s\S]*class="dcc-product-catalog-resizable-table"[\s\S]*border[\s\S]*:allow-drag-last-column="true"/,
  '产品目录表格必须显式启用含末列在内的列宽拖拽。'
)
assert.match(
  source,
  /\.dcc-product-catalog-resizable-table[\s\S]*th\.el-table__cell::after[\s\S]*width:\s*8px[\s\S]*cursor:\s*col-resize/,
  '产品目录表头必须提供可见且可命中的列宽拖拽区域。'
)

const columnKeys = [
  'dataSource',
  'categoryLevel1',
  'categoryLevel2',
  'productSequence',
  'product',
  'productCode',
  'projectName',
  'projectCode',
  'registrationCertificateName',
  'registrationCertificateNumber',
  'certificateHolder',
  'registrationPlace',
  'effectiveDate',
  'expiryDate',
  'classification',
  'productStatus',
  'registrationInfoLink',
  'remark',
  'actions'
]

for (const field of columnKeys) {
  assert.match(source, new RegExp(`key:\\s*'${field}'`), `${field} 必须注册到产品目录列配置。`)
  assert.match(
    template,
    new RegExp(`isProductCatalogColumnVisible\\('${field}'\\)`),
    `${field} 列必须受显示字段配置控制。`
  )
}

for (const filter of [
  ['keyword', '关键词', 'text'],
  ['categoryLevel1', '产品类别 I', 'text'],
  ['categoryLevel2', '产品类别 II', 'text'],
  ['productStatus', '产品状态', 'select'],
  ['dataSource', '数据来源', 'select']
]) {
  const [key, label, type] = filter
  assert.match(
    source,
    new RegExp(
      `key:\\s*'${key}'[\\s\\S]*label:\\s*'${label}'[\\s\\S]*type:\\s*'${type}'[\\s\\S]*queryParamKey:\\s*'${key}'`
    ),
    `快速过滤必须支持 ${label} 并写入原 ${key} 查询参数。`
  )
}

assert.match(
  source,
  /useTableQuickFilter\([\s\S]*'dcc\.productCatalog\.main'[\s\S]*productCatalogQuickFilterDefinitions[\s\S]*queryParams[\s\S]*getList/,
  '快速过滤必须用稳定 tableKey 连接原查询参数和列表加载。'
)
assert.match(
  source,
  /useUserTableColumns\('dcc\.productCatalog\.main', productCatalogDefaultColumns\)/,
  '显示字段必须使用稳定 tableKey 持久化。'
)

for (const preservedToken of [
  "openForm('update'",
  'handleDelete',
  'row.registrationInfoLink',
  'target="_blank"',
  `v-hasPermi="['dcc:project-code:create']"`,
  `v-hasPermi="['dcc:project-code:update']"`,
  `v-hasPermi="['dcc:project-code:delete']"`,
  'getProductCatalogPage',
  'createProductCatalog',
  'updateProductCatalog',
  'deleteProductCatalog'
]) {
  assert.ok(source.includes(preservedToken), `产品目录原有能力必须保留：${preservedToken}`)
}

for (const removedToken of [
  'productCatalogQuickFilter.resetQuickFilter',
  'handleCompareRegistrationExpiry',
  'compareRegistrationExpiry',
  'expiryCompareLoading',
  'getExpiryCompareTooltip',
  'getExpiryCompareClass',
  'expiry-compare-'
]) {
  assert.ok(!source.includes(removedToken), `产品目录 toolbar 删除后不得保留无入口逻辑：${removedToken}`)
}

const mainListEnd = source.indexOf('<Dialog')
const mainListSource = source.slice(0, mainListEnd)
assert.doesNotMatch(
  mainListSource,
  /<Pagination[\s\S]*@pagination="getList"[\s\S]*\/>/,
  '产品目录分页必须由标准列表模板承载。'
)
assert.doesNotMatch(
  mainListSource,
  /<el-form[\s\S]*class="-mb-15px"/,
  '产品目录必须移除旧独立搜索表单。'
)

console.log('PASS: DCC product catalog unified list template static contract')
