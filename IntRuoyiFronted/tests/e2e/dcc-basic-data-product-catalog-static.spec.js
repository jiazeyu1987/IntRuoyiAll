const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const productCatalogPageSource = readSource(
  'src/views/dcc/controlled-file/basic-data/product-catalog/index.vue'
)
const productCatalogPanelSource = readSource(
  'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue'
)
const detailSource = readSource('src/views/dcc/controlled-file/detail/index.vue')
const productCatalogApiSource = readSource('src/api/dcc/controlledFile/productCatalog.ts')
const legacyShellPath = path.join(root, 'src/views/dcc/controlled-file/basic-data/index.vue')
const legacyShellSource = fs.existsSync(legacyShellPath) ? fs.readFileSync(legacyShellPath, 'utf8') : ''

assert.equal(
  packageJson.scripts['e2e:dcc:basic-data-product-catalog:static'],
  'node tests/e2e/dcc-basic-data-product-catalog-static.spec.js',
  'package.json 必须暴露 DCC 产品目录静态契约脚本'
)
assert.equal(
  packageJson.scripts['e2e:dcc:basic-data-global-submenu:static'],
  'node tests/e2e/dcc-basic-data-global-submenu-static.spec.js',
  'package.json 必须暴露 DCC 基础数据全局子入口静态契约脚本'
)

for (const requiredToken of [
  "defineOptions({ name: 'DccProductCatalogBasicDataPage' })",
  '<ProductCatalogTabPanel />',
  "import ProductCatalogTabPanel from '../components/ProductCatalogTabPanel.vue'"
]) {
  assert.ok(productCatalogPageSource.includes(requiredToken), `产品目录独立页面必须包含 ${requiredToken}`)
}

assert.ok(
  !legacyShellSource.includes('<el-tab-pane label="项目代码" name="project-code"'),
  'DCC 基础数据不得继续保留页内项目代码页签'
)
assert.ok(
  !legacyShellSource.includes('<el-tab-pane label="产品目录" name="product-catalog"'),
  'DCC 基础数据不得继续保留页内产品目录页签'
)
assert.ok(
  !legacyShellSource.includes('const BASIC_DATA_TAB_PRODUCT_CATALOG'),
  'DCC 基础数据不得继续保留产品目录 tab 状态逻辑'
)
assert.ok(
  !detailSource.includes("/dcc/controlled-file/basic-data"),
  '文件详情跳转不得继续指向旧的 DCC 基础数据页内 tab 路径'
)

for (const apiToken of [
  'export interface DccProductCatalogPageReqVO extends PageParam',
  'export interface DccProductCatalogSaveReqVO',
  'export interface DccProductCatalogUpdateReqVO extends DccProductCatalogSaveReqVO',
  'keyword?: string',
  'categoryLevel1?: string',
  'categoryLevel2?: string',
  'productStatus?: string',
  'dataSource?: string',
  'export interface DccProductCatalogRespVO',
  'dataSource: string',
  'projectName?: string | null',
  'projectCode?: string | null',
  'originalRowNo: number',
  'registrationInfoLink?: string | null',
  'getProductCatalogPage',
  '/dcc/product-catalog/page',
  'createProductCatalog',
  '/dcc/product-catalog/create',
  'updateProductCatalog',
  '/dcc/product-catalog/update',
  'deleteProductCatalog',
  '/dcc/product-catalog/delete'
]) {
  assert.ok(
    productCatalogApiSource.includes(apiToken),
    `产品目录 API 契约必须声明 ${apiToken}`
  )
}

for (const requiredToken of [
  '基础数据 / DCC产品目录',
  '关键词',
  '产品类别 I',
  '产品类别 II',
  '产品状态',
  '数据来源',
  '新增产品目录',
  'getProductCatalogPage',
  'createProductCatalog',
  'updateProductCatalog',
  'deleteProductCatalog',
  '数据来源',
  '产品类别 I',
  '产品类别 II',
  '产品序号',
  '产品',
  '产品编码',
  '项目名称',
  '项目代码',
  '注册证名称',
  '注册证号',
  '持证人',
  '注册地',
  '生效日期',
  '有效期至',
  '分类',
  '产品状态',
  '注册证信息链接',
  '备注'
]) {
  assert.ok(
    productCatalogPanelSource.includes(requiredToken),
    `产品目录独立页面必须包含 ${requiredToken}`
  )
}

for (const permissionToken of [
  `v-hasPermi="['dcc:project-code:create']"`,
  `v-hasPermi="['dcc:project-code:update']"`,
  `v-hasPermi="['dcc:project-code:delete']"`
]) {
  assert.ok(
    productCatalogPanelSource.includes(permissionToken),
    `产品目录维护入口必须绑定基础数据维护权限：${permissionToken}`
  )
}

for (const maintenanceToken of [
  "openForm('create'",
  "openForm('update'",
  'handleDelete',
  '产品目录维护',
  'submitForm',
  'formRules',
  '新增产品目录成功',
  '编辑产品目录成功',
  '删除产品目录成功'
]) {
  assert.ok(
    productCatalogPanelSource.includes(maintenanceToken),
    `产品目录独立页面必须提供维护能力：${maintenanceToken}`
  )
}

assert.match(
  productCatalogPanelSource,
  /<el-table-column[\s\S]*v-if="isProductCatalogColumnVisible\('actions'\)"[\s\S]*label="操作"[\s\S]*编辑[\s\S]*删除[\s\S]*<\/el-table-column>/,
  '产品目录表格必须提供编辑和删除行操作'
)

for (const statusToken of ['在研(N)', '在售(S)', '已取消(C)']) {
  assert.ok(
    productCatalogPanelSource.includes(statusToken),
    `产品状态映射必须包含 ${statusToken}`
  )
}

assert.match(
  productCatalogPanelSource,
  /row\.registrationInfoLink[\s\S]*target="_blank"/,
  '注册证信息链接列必须按外链按钮渲染'
)
const actionsMatch = productCatalogPanelSource.match(/<template #actions>([\s\S]*?)<\/template>/)
assert.ok(actionsMatch, '产品目录独立页面必须保留 actions 插槽')
const actionsSource = actionsMatch[0]
assert.match(
  actionsSource,
  /openForm\('create'\)/,
  '产品目录 actions 插槽必须保留新增产品目录入口'
)
assert.doesNotMatch(
  actionsSource,
  /productCatalogQuickFilter\.resetQuickFilter|handleCompareRegistrationExpiry|>\s*重置\s*<|>\s*注册证有效期\s*</,
  '产品目录 actions 插槽不得继续渲染重置和注册证有效期按钮'
)
assert.match(
  productCatalogPanelSource,
  /@quick-filter-query="productCatalogQuickFilter\.applyQuickFilter"[\s\S]*useTableQuickFilter\([\s\S]*queryParams[\s\S]*getList/,
  '产品目录查询必须通过标准列表快速过滤重置页码并刷新列表'
)

assert.match(
  productCatalogPanelSource,
  /<UnifiedListTemplate[\s\S]*class="dcc-product-catalog-list-template"[\s\S]*single-line-toolbar/,
  '产品目录顶部筛选条件区和右侧操作按钮必须启用统一列表单行工具栏'
)
assert.match(
  productCatalogPanelSource,
  /dcc-product-catalog-list-template\.unified-list-template--single-line-toolbar[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+auto[\s\S]*dcc-product-catalog-list-template\.unified-list-template--single-line-toolbar[\s\S]*\.unified-list-template__multi-filter[\s\S]*min-width:\s*0/,
  '产品目录单行工具栏必须把筛选主列设置为可收缩，避免右侧按钮换行或被裁切'
)

for (const removedToken of [
  'handleCompareRegistrationExpiry',
  'compareRegistrationExpiry',
  'expiryCompareLoading',
  'getExpiryCompareTooltip',
  'getExpiryCompareClass',
  'expiry-compare-'
]) {
  assert.ok(!productCatalogPanelSource.includes(removedToken), `产品目录按钮删除后不得保留无入口逻辑：${removedToken}`)
}

assert.ok(
  !productCatalogApiSource.includes('compareRegistrationExpiry'),
  '注册证有效期按钮删除后 API wrapper 不应继续暴露无前端入口的比对请求'
)

assert.doesNotMatch(
  `${productCatalogPageSource}\n${productCatalogPanelSource}`,
  /mock|placeholder data|fallback|降级|吞异常/i,
  'DCC 产品目录页面改造不得引入 mock、placeholder、fallback、降级或吞异常'
)

console.log('PASS: DCC product catalog static contract')
