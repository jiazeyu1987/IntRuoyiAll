const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const distributionTab = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryDistributionRulesTab.vue'
)
const rulesSection = readSource(
  'src/views/dcc/controlled-file/shared/governance/CategoryDepartmentRulesSection.vue'
)

assert.equal(
  packageJson.scripts['e2e:dcc:distribution-category-autoload:static'],
  'node tests/e2e/dcc-distribution-category-autoload-static.spec.js',
  'package.json must expose the DCC distribution category autoload static contract'
)

assert.match(
  distributionTab,
  /data-testid="dcc-distribution-rules-tab"/,
  'distribution rules tab must expose a stable test id'
)
assert.match(
  distributionTab,
  /<UnifiedListTemplate[\s\S]*table-key="dcc\.controlledFile\.permission\.distributionRules"/,
  'distribution rules must use the standard list template'
)
assert.match(distributionTab, />\s*刷新\s*</, 'manual reload button must be labeled 刷新')
assert.doesNotMatch(
  distributionTab,
  />\s*查询规则\s*</,
  'distribution rules tab must not require a separate 查询规则 step'
)

assert.match(
  distributionTab,
  /const loading = ref\(false\)/,
  'distribution rules tab must track loading state'
)
assert.match(
  distributionTab,
  /getCategoryDistributionRules\(category\.id\)/,
  'distribution rules tab must load rules for each category row'
)
assert.match(
  distributionTab,
  /@column-change="saveColumnConfig"[\s\S]*@pagination="handlePagination"/,
  'distribution rules tab must keep standard list field display and pagination hooks'
)
assert.match(
  distributionTab,
  /catch \(error\) \{[\s\S]*errorMessage\.value = resolveErrorMessage\(error, '分发规则加载失败，请查看错误提示后重试。'\)/,
  'distribution rule loading must expose failures and clear loading state'
)

assert.match(rulesSection, /loading\?: boolean/, 'shared rules section must accept optional loading prop')
assert.match(rulesSection, /<el-table v-loading="loading"/, 'shared rules table must render loading state')

const contractSource = `${distributionTab}\n${rulesSection}`
assert.doesNotMatch(
  contractSource,
  /mock|降级|吞异常/i,
  'distribution category autoload must not introduce mock, downgrade, or swallowed-error behavior'
)

console.log('PASS: DCC distribution category autoload static contract')
