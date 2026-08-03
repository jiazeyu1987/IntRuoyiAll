const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(root, '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const readWorkspaceSource = (relativePath) =>
  fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const projectCodePageSource = readSource(
  'src/views/dcc/controlled-file/basic-data/project-code/index.vue'
)
const productCatalogPageSource = readSource(
  'src/views/dcc/controlled-file/basic-data/product-catalog/index.vue'
)
const projectCodePanelSource = readSource(
  'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'
)
const detailSource = readSource('src/views/dcc/controlled-file/detail/index.vue')
const dccSchema = readWorkspaceSource('IntRuoyiBackend/sql/mysql/20260513_dcc_base_schema.sql')
const legacyShellPath = path.join(root, 'src/views/dcc/controlled-file/basic-data/index.vue')
const legacyShellSource = fs.existsSync(legacyShellPath) ? fs.readFileSync(legacyShellPath, 'utf8') : ''

assert.equal(
  packageJson.scripts['e2e:dcc:basic-data-global-submenu:static'],
  'node tests/e2e/dcc-basic-data-global-submenu-static.spec.js',
  'package.json 必须暴露 DCC 基础数据全局子入口静态契约脚本'
)

for (const requiredToken of [
  "defineOptions({ name: 'DccProjectCodeBasicDataPage' })",
  '<ProjectCodeTabPanel />',
  "import ProjectCodeTabPanel from '../components/ProjectCodeTabPanel.vue'"
]) {
  assert.ok(projectCodePageSource.includes(requiredToken), `项目代码独立页面必须包含 ${requiredToken}`)
}

for (const requiredToken of [
  "defineOptions({ name: 'DccProductCatalogBasicDataPage' })",
  '<ProductCatalogTabPanel />'
]) {
  assert.ok(productCatalogPageSource.includes(requiredToken), `产品目录独立页面必须包含 ${requiredToken}`)
}

assert.ok(
  !legacyShellSource.includes('<el-tabs'),
  'DCC 基础数据不得继续使用页内 tabs 作为项目代码/产品目录切换壳'
)
assert.ok(
  !legacyShellSource.includes('const BASIC_DATA_TAB_PROJECT_CODE'),
  'DCC 基础数据不得继续保留项目代码 tab 状态逻辑'
)
assert.ok(
  detailSource.includes("path: '/mdm/project-code'"),
  '文件详情跳转必须改为新的全局基础数据项目代码子页面'
)
assert.ok(
  projectCodePanelSource.includes("path: '/mdm/project-code'"),
  '项目代码详情抽屉路由必须改为新的全局基础数据项目代码子页面'
)

for (const schemaToken of [
  '基础数据',
  '/mdm',
  'DCC项目代码',
  'DCC产品目录',
  'dcc/controlled-file/basic-data/project-code/index',
  'dcc/controlled-file/basic-data/product-catalog/index',
  '`parent_id` = 990200',
  "source_menu.`path` = 'controlled-file/categories'",
  'system_role_menu'
]) {
  assert.ok(dccSchema.includes(schemaToken), `DCC 菜单迁移 SQL 必须包含 ${schemaToken}`)
}

assert.doesNotMatch(
  `${projectCodePageSource}\n${productCatalogPageSource}\n${projectCodePanelSource}\n${detailSource}`,
  /mock|placeholder data|fallback|降级|吞异常/i,
  'DCC 基础数据全局子入口改造不得引入 mock、placeholder、fallback、降级或吞异常'
)

console.log('PASS: DCC basic-data global submenu static contract')
