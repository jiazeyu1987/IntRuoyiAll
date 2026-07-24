const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const distributionTab = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryDistributionRulesTab.vue'
)

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const listTemplate = extractBetween(
  distributionTab,
  '<UnifiedListTemplate',
  '</UnifiedListTemplate>'
)
const drawerShell = extractBetween(
  distributionTab,
  '<el-drawer',
  '</el-drawer>'
)

assert.match(
  listTemplate,
  /table-key="dcc\.controlledFile\.permission\.distributionRules"/,
  '分发规则页签必须使用标准列表 table-key'
)

assert.match(
  listTemplate,
  /:filter-definitions="quickFilterDefinitions"/,
  '分发规则页签必须提供快速筛选配置'
)

assert.match(
  distributionTab,
  /const rows = computed<RuleListRow\[\]>/,
  '分发规则列表必须由真实类别和规则数据派生'
)

assert.match(
  distributionTab,
  /activeRuleCount/,
  '已选择类别时必须展示当前分发部门规则数量'
)

assert.match(
  distributionTab,
  /empty-text="当前暂无分发规则"/,
  '无分发规则时必须保留明确空状态'
)

assert.doesNotMatch(
  drawerShell,
  /<el-alert[\s\S]*请选择文件类别后维护分发规则/,
  '规则抽屉不应继续用独立 info alert 显示选择类别提示'
)

for (const behaviorToken of [
  'loadData',
  'openRuleDrawer',
  'addRule',
  'removeRule',
  'saveRules',
  'getCategoryDistributionRules(category.id)',
  'replaceCategoryDistributionRules',
  'data-user-table-column-explicit',
  ':error-message="drawerErrorMessage"'
]) {
  assert.ok(distributionTab.includes(behaviorToken), `分发规则原有行为必须保留：${behaviorToken}`)
}

assert.doesNotMatch(
  `${listTemplate}\n${drawerShell}`,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '分发规则工具栏上下文不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC distribution toolbar context static contract')
