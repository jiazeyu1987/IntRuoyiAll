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
const categoriesPage = readSource('src/views/dcc/controlled-file/categories/index.vue')
const accessRulePage = readSource('src/views/dcc/controlled-file/access-rules/index.vue')
const directoryPage = readSource('src/views/dcc/controlled-file/directories/index.vue')
const dccSchema = readWorkspaceSource('ruoyi-vue-pro/sql/mysql/20260513_dcc_base_schema.sql')

assert.equal(
  packageJson.scripts?.['e2e:dcc:permission-tabs-merge:static'],
  'node tests/e2e/dcc-permission-tabs-merge-static.spec.js',
  'package.json must expose the DCC permission tabs merge static contract'
)

for (const token of [
  'label="类别列表"',
  'label="审阅矩阵"',
  'label="查看矩阵"',
  'label="目录授权"',
  'label="分发规则"',
  'label="培训规则"',
  '<CategoryDistributionRulesTab',
  '<CategoryTrainingRulesTab',
  '<DirectoryAuthorizationTabPanel'
]) {
  assert.ok(categoriesPage.includes(token), `文控权限页必须包含 ${token}`)
}
assert.match(
  categoriesPage,
  /<CategoryReviewMatrixTable[\s\S]*?:active="activeTab === 'review-matrix'"[\s\S]*?\/>/,
  '文控权限页必须包含审阅矩阵页签组件并绑定激活状态'
)
assert.match(
  categoriesPage,
  /<CategoryViewMatrixTable[\s\S]*?:active="activeTab === 'view-matrix'"[\s\S]*?\/>/,
  '文控权限页必须包含查看矩阵页签组件并绑定激活状态'
)

assert.ok(
  categoriesPage.includes("defineOptions({ name: 'DccControlledFileCategories' })"),
  '文控权限页必须保留原组件名以兼容既有菜单组件绑定'
)
assert.ok(
  categoriesPage.includes("'distribution-rules'") &&
    categoriesPage.includes("'training-rules'") &&
    categoriesPage.includes("type PermissionTabName = (typeof TAB_NAMES)[number]") &&
    categoriesPage.includes("const activeTab = ref<PermissionTabName>(resolveActiveTab(route.query.tab))"),
  '文控权限页必须显式声明并初始化目录授权、分发规则和培训规则页签状态'
)
assert.ok(
  categoriesPage.includes("tab === 'directory-auth'") || categoriesPage.includes("'directory-auth'"),
  '文控权限页必须使用 directory-auth 作为目录授权页签标识'
)
assert.ok(
  categoriesPage.includes("tab === 'distribution-rules'") || categoriesPage.includes("'distribution-rules'"),
  '文控权限页必须使用 distribution-rules 作为分发规则页签标识'
)
assert.ok(
  categoriesPage.includes("tab === 'training-rules'") || categoriesPage.includes("'training-rules'"),
  '文控权限页必须使用 training-rules 作为培训规则页签标识'
)

for (const token of [
  "path: '/dcc/controlled-file/categories'",
  "query: {",
  "tab: 'directory-auth'",
  'directoryId: row.id'
]) {
  assert.ok(directoryPage.includes(token), `目录管理页访问规则入口必须改为文控权限页签入口：${token}`)
}

assert.ok(
  accessRulePage.includes("router.replace({") &&
    accessRulePage.includes("path: '/dcc/controlled-file/categories'") &&
    accessRulePage.includes("tab: 'directory-auth'"),
  '旧访问规则页必须作为兼容壳层重定向到文控权限目录授权页签'
)
assert.ok(
  !accessRulePage.includes('dcc-access-rule-bound-directory-list'),
  '旧访问规则页不应继续承载完整目录授权主体'
)

for (const schemaToken of [
  "SELECT 6802, 'DCC访问规则'",
  "SELECT 6803, '文控权限'",
  "'controlled-file/access-rules'",
  "'controlled-file/categories'"
]) {
  assert.ok(dccSchema.includes(schemaToken), `DCC 菜单种子必须体现兼容入口与文控权限重命名：${schemaToken}`)
}

assert.doesNotMatch(
  `${categoriesPage}\n${accessRulePage}\n${directoryPage}`,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '文控权限页签整合不得引入 mock、placeholder、fallback、降级或吞异常'
)

console.log('PASS: DCC permission tabs merge static contract')
