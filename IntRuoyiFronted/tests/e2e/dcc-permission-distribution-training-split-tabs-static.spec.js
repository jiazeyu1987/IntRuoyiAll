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

const readWorkspaceSource = (relativePath) => {
  const absolutePath = path.join(workspaceRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required workspace file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

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
const retireSql = readWorkspaceSource(
  'ruoyi-vue-pro/sql/mysql/20260714_dcc_distribution_training_menu_retire.sql'
)

assert.equal(
  packageJson.scripts['e2e:dcc:permission-distribution-training-split-tabs:static'],
  'node tests/e2e/dcc-permission-distribution-training-split-tabs-static.spec.js',
  'package.json must expose the split distribution/training tab static gate'
)

for (const token of [
  'label="分发规则"',
  'label="培训规则"',
  '<CategoryDistributionRulesTab',
  '<CategoryTrainingRulesTab',
  "'distribution-rules'",
  "'training-rules'"
]) {
  assert.ok(categoriesPage.includes(token), `文控权限页必须包含拆分页签能力：${token}`)
}

assert.ok(!categoriesPage.includes('label="分发培训"'), '文控权限页不应继续显示合并的分发培训页签')
assert.ok(!categoriesPage.includes('CategoryDistributionTrainingTab'), '文控权限页不应继续加载合并组件')

for (const [source, tableKey, apiToken, title] of [
  [
    distributionTab,
    'dcc.controlledFile.permission.distributionRules',
    'getCategoryDistributionRules(category.id)',
    '分发规则'
  ],
  [
    trainingTab,
    'dcc.controlledFile.permission.trainingRules',
    'getCategoryTrainingRules(category.id)',
    '培训规则'
  ]
]) {
  assert.ok(source.includes('<UnifiedListTemplate'), `${title}页签必须使用标准列表模板`)
  assert.ok(source.includes(`table-key="${tableKey}"`), `${title}页签必须使用独立 table-key`)
  assert.ok(source.includes(apiToken), `${title}页签必须调用真实规则接口`)
  assert.ok(source.includes('data-user-table-column-explicit'), `${title}列表必须声明用户列配置`)
  assert.ok(source.includes('@column-change="saveColumnConfig"'), `${title}列表必须支持显示字段配置保存`)
  assert.ok(source.includes('@header-dragend="handleHeaderDragend"'), `${title}列表必须支持列宽拖拽持久化`)
  assert.ok(source.includes('v-model:page="queryParams.pageNo"'), `${title}列表必须支持分页`)
  assert.ok(source.includes('data-testid='), `${title}页签必须提供稳定测试标识`)
}

for (const [source, expectedTab] of [
  [distributionShell, "tab: 'distribution-rules'"],
  [trainingShell, "tab: 'training-rules'"]
]) {
  assert.ok(source.includes("path: '/dcc/controlled-file/categories'"), '旧路由兼容壳层必须跳转文控权限')
  assert.ok(source.includes(expectedTab), `旧路由兼容壳层必须跳转 ${expectedTab}`)
}

assert.ok(
  retireSql.includes("`visible` = b'0'") &&
    retireSql.includes("controlled-file/distribution") &&
    retireSql.includes("controlled-file/training"),
  '菜单退役迁移必须继续隐藏文件分发和培训规则独立入口'
)

assert.doesNotMatch(
  `${categoriesPage}\n${distributionTab}\n${trainingTab}\n${distributionShell}\n${trainingShell}`,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '拆分分发/培训页签不得引入 mock、placeholder、fallback、降级或吞异常'
)

console.log('PASS: DCC permission distribution/training split tabs static contract')
