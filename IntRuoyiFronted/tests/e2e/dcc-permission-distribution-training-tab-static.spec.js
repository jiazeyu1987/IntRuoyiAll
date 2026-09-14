const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const readWorkspaceSource = (relativePath) =>
  fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const categoriesPage = readSource('src/views/dcc/controlled-file/categories/index.vue')
const distributionTab = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryDistributionRulesTab.vue'
)
const trainingTab = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryTrainingRulesTab.vue'
)
const distributionShell = readSource('src/views/dcc/controlled-file/distribution/index.vue')
const trainingShell = readSource('src/views/dcc/controlled-file/training/index.vue')
const remainingRoute = readSource('src/router/modules/remaining.ts')
const retireSql = readWorkspaceSource(
  'IntRuoyiBackend/sql/mysql/20260714_dcc_distribution_training_menu_retire.sql'
)

assert.equal(
  packageJson.scripts['e2e:dcc:permission-distribution-training-tab:static'],
  'node tests/e2e/dcc-permission-distribution-training-tab-static.spec.js',
  'package.json must expose the DCC permission distribution/training tab static gate'
)

assert.ok(categoriesPage.includes('label="分发规则"'), '文控权限页必须提供分发规则页签')
assert.ok(categoriesPage.includes('label="培训规则"'), '文控权限页必须提供培训规则页签')
assert.ok(
  categoriesPage.includes('CategoryDistributionRulesTab') &&
    categoriesPage.includes('CategoryTrainingRulesTab'),
  '文控权限页必须接入分发规则和培训规则组件'
)
assert.ok(
  categoriesPage.includes("'distribution-rules'") && categoriesPage.includes("'training-rules'"),
  '文控权限页必须支持拆分后的分发规则和培训规则查询页签'
)
assert.ok(!categoriesPage.includes('label="分发培训"'), '文控权限页不应继续提供合并分发培训页签')

for (const [source, title, apiToken] of [
  [distributionTab, '分发规则', 'getCategoryDistributionRules(categoryId)'],
  [trainingTab, '培训规则', 'getCategoryTrainingRules(categoryId)']
]) {
  assert.ok(source.includes('<UnifiedListTemplate'), `${title}页签必须使用标准列表模板`)
  assert.ok(source.includes(apiToken), `${title}页签必须保留真实规则接口：${apiToken}`)
  assert.ok(source.includes('data-user-table-column-explicit'), `${title}页签必须保留用户列配置能力`)
}

for (const shell of [
  [distributionShell, 'DccControlledFileDistribution', '分发规则已并入文控权限', "tab: 'distribution-rules'"],
  [trainingShell, 'DccControlledFileTraining', '培训规则已并入文控权限', "tab: 'training-rules'"]
]) {
  const [source, componentName, title, tabQuery] = shell
  assert.ok(source.includes(componentName), `兼容壳层必须保留组件名：${componentName}`)
  assert.ok(source.includes(title), `兼容壳层必须说明入口已整合：${title}`)
  assert.ok(source.includes("path: '/dcc/controlled-file/categories'"), '兼容壳层必须跳转文控权限')
  assert.ok(source.includes(tabQuery), '兼容壳层必须跳转拆分后的规则页签')
}

assert.ok(
  remainingRoute.includes("activeMenu: '/dcc/controlled-file/categories'"),
  '隐藏兼容路由必须激活文控权限菜单'
)

assert.ok(
  retireSql.includes("`visible` = b'0'") &&
    retireSql.includes('`status` = 1') &&
    retireSql.includes("controlled-file/distribution") &&
    retireSql.includes("controlled-file/training"),
  '菜单退役迁移必须隐藏文件分发和培训规则独立入口'
)

assert.doesNotMatch(
  `${categoriesPage}\n${distributionTab}\n${trainingTab}\n${distributionShell}\n${trainingShell}`,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '分发培训拆分不得引入 mock、placeholder、fallback、降级或吞异常'
)

console.log('PASS: DCC permission distribution/training tab static contract')
