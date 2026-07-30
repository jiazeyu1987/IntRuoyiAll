const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')

const readSource = (absolutePath) => {
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${absolutePath}`)
  return fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')
}

const productCatalogPanelSource = readSource(
  path.join(
    frontendRoot,
    'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue'
  )
)
const productCatalogApiSource = readSource(
  path.join(frontendRoot, 'src/api/dcc/controlledFile/productCatalog.ts')
)
const pageReqSource = readSource(
  path.join(
    workspaceRoot,
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/productcatalog/vo/DccProductCatalogPageReqVO.java'
  )
)
const mapperSource = readSource(
  path.join(
    workspaceRoot,
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/productcatalog/DccProductCatalogMapper.java'
  )
)

const templateMatch = productCatalogPanelSource.match(
  /<UnifiedListTemplate[\s\S]*?table-key="dcc\.productCatalog\.main"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, 'DCC 产品目录必须使用统一列表模板。')
const template = templateMatch[0]

for (const field of ['projectName', 'projectCode']) {
  assert.match(
    template,
    new RegExp(`v-bind="sortColumnAttrs\\('${field}'\\)"`),
    `${field} 表头必须保留统一列表排序按钮。`
  )
}

assert.match(
  template,
  /v-model:sort-state="productCatalogSortState"/,
  'DCC 产品目录必须把排序状态绑定给统一列表模板。'
)
assert.match(
  template,
  /@sort-change="handleProductCatalogSortChange"/,
  'DCC 产品目录必须在统一列表排序变化时刷新正式分页查询。'
)
assert.match(
  template,
  /<template\s+#table="\{\s*sortColumnAttrs,\s*handleSortChange:\s*handleTemplateSortChange\s*\}"[\s\S]*<el-table[\s\S]*@sort-change="handleTemplateSortChange"/,
  'el-table 排序事件必须先交给统一列表模板归一化。'
)

assert.match(
  productCatalogPanelSource,
  /const productCatalogSortState = ref<[\s\S]*>\(\{\}\)/,
  'DCC 产品目录必须持有当前项目字段排序状态。'
)
assert.match(
  productCatalogPanelSource,
  /const PRODUCT_CATALOG_SERVER_SORT_FIELDS = new Set\(\[\s*'projectName',\s*'projectCode'\s*\]\)/,
  'DCC 产品目录只能向后端发送白名单排序字段。'
)
assert.match(
  productCatalogPanelSource,
  /const handleProductCatalogSortChange = \(\{ prop, order \}: [\s\S]*\) => \{[\s\S]*queryParams\.pageNo = 1[\s\S]*queryParams\.sortField = sortField[\s\S]*queryParams\.sortOrder = order === 'ascending' \? 'asc' : 'desc'[\s\S]*getList\(\)[\s\S]*\}/,
  '点击项目名称/项目代码排序必须重置页码、写入 sortField/sortOrder 并重新请求列表。'
)

for (const apiToken of ["sortField?: string", "sortOrder?: 'asc' | 'desc'"]) {
  assert.ok(productCatalogApiSource.includes(apiToken), `前端分页请求类型必须声明 ${apiToken}`)
}

for (const voToken of [
  'private String sortField;',
  'private String sortOrder;'
]) {
  assert.ok(pageReqSource.includes(voToken), `后端分页请求 VO 必须声明 ${voToken}`)
}

for (const mapperToken of [
  'applyPageSort(wrapper, reqVO);',
  'PROJECT_SORT_FIELD_NAME',
  'PROJECT_SORT_FIELD_CODE',
  'DccProductCatalogDO::getProjectName',
  'DccProductCatalogDO::getProjectCode',
  'orderByAsc(DccProductCatalogDO::getDataSource)',
  'orderByAsc(DccProductCatalogDO::getOriginalRowNo)'
]) {
  assert.ok(mapperSource.includes(mapperToken), `后端 Mapper 必须包含项目字段分页排序契约：${mapperToken}`)
}
assert.doesNotMatch(mapperSource, /\.last\(/, '产品目录项目字段排序不得用 last 拼接未受控 SQL。')

console.log('PASS: DCC product catalog project field sort static contract')
