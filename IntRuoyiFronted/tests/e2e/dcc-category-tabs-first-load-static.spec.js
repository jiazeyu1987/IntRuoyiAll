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
const categoriesPage = readSource('src/views/dcc/controlled-file/categories/index.vue')
const distributionTab = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryDistributionRulesTab.vue'
)
const trainingTab = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryTrainingRulesTab.vue'
)

assert.equal(
  packageJson.scripts?.['e2e:dcc:category-tabs-first-load:static'],
  'node tests/e2e/dcc-category-tabs-first-load-static.spec.js',
  'package.json must expose the DCC category tabs first-load performance contract'
)

for (const [label, name] of [
  ['类别列表', 'list'],
  ['审阅矩阵', 'review-matrix'],
  ['查看矩阵', 'view-matrix'],
  ['目录授权', 'directory-auth'],
  ['分发规则', 'distribution-rules'],
  ['培训规则', 'training-rules']
]) {
  assert.match(
    categoriesPage,
    new RegExp(`<el-tab-pane[\\s\\S]*label="${label}"[\\s\\S]*name="${name}"[\\s\\S]*\\slazy[\\s\\S]*>`),
    `文控权限 ${label} 页签必须使用 Element Plus lazy，避免非激活页签首屏挂载`
  )
}

assert.match(
  categoriesPage,
  /const loadedTabNames = ref<Set<PermissionTabName>>\(new Set\(\[activeTab\.value\]\)\)/,
  '文控权限页必须维护已访问页签集合，未访问页签不得挂载业务组件'
)
assert.match(
  categoriesPage,
  /const isTabPaneMounted = \(tab: PermissionTabName\) => loadedTabNames\.value\.has\(tab\)/,
  '文控权限页必须通过 isTabPaneMounted 控制页签内容挂载边界'
)
for (const [component, tabName] of [
  ['UnifiedListTemplate', 'list'],
  ['CategoryReviewMatrixTable', 'review-matrix'],
  ['CategoryViewMatrixTable', 'view-matrix'],
  ['DirectoryAuthorizationTabPanel', 'directory-auth'],
  ['CategoryDistributionRulesTab', 'distribution-rules'],
  ['CategoryTrainingRulesTab', 'training-rules']
]) {
  assert.match(
    categoriesPage,
    new RegExp(`<${component}[\\s\\S]*v-if="isTabPaneMounted\\('${tabName}'\\)"`),
    `${component} must not mount before its tab is first activated`
  )
}

assert.match(
  categoriesPage,
  /const ensureActiveTabLoaded = async \(tab: PermissionTabName\) => \{[\s\S]*if \(tab !== 'list'\) \{[\s\S]*return[\s\S]*\}[\s\S]*await loadData\(\)/,
  '父页面首次进入非类别列表页签时不得加载类别列表数据'
)
assert.doesNotMatch(
  categoriesPage,
  /onMounted\(async \(\) => \{[\s\S]*await loadData\(\)[\s\S]*\}\)/,
  '父页面 onMounted 不得无条件加载类别列表数据'
)

for (const [source, label] of [
  [distributionTab, '分发规则'],
  [trainingTab, '培训规则']
]) {
  assert.match(
    source,
    /const loadedRuleCategoryIds = ref\(new Set<number>\(\)\)/,
    `${label} 页签必须记录已加载规则的类别 ID，避免首屏全量重复请求`
  )
  assert.match(
    source,
    /const ensureVisibleRuleRowsLoaded = async \(\) => \{[\s\S]*visibleCategoryIds[\s\S]*Promise\.all\(/,
    `${label} 页签必须只为当前可见页类别按需加载规则`
  )
  assert.doesNotMatch(
    source,
    /activeCategories\.map\(async \(category\) => \{[\s\S]*getCategory(?:Distribution|Training)Rules\(category\.id\)/,
    `${label} 页签首次加载不得对全部类别执行 N+1 规则请求`
  )
}

assert.doesNotMatch(
  `${categoriesPage}\n${distributionTab}\n${trainingTab}`,
  /mock|placeholder data|fallback|降级|吞异常/i,
  'category tab first-load optimization must not add mock data, degradation, fallback, or swallowed errors'
)

console.log('PASS: DCC category tabs first-load static contract')
