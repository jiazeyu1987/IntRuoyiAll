const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const trainingRulesTab = readSource(
  'src/views/dcc/controlled-file/training/components/TrainingRulesReadonlyTab.vue'
)

const contentWraps = trainingRulesTab.match(/<ContentWrap>[\s\S]*?<\/ContentWrap>/g) || []
assert.ok(contentWraps.length >= 2, '培训规则页必须保留工具栏与映射区两个工作面')

const toolbarWrap = contentWraps[0]
const mappingWrap = contentWraps[1]

assert.match(
  toolbarWrap,
  /data-testid="dcc-training-rule-toolbar"/,
  '培训规则工具栏必须提供稳定的工具栏测试标识'
)

assert.match(
  toolbarWrap,
  /data-testid="dcc-training-rule-context"/,
  '培训规则工具栏必须提供稳定的上下文测试标识'
)

assert.match(
  toolbarWrap,
  /trainingRuleToolbarContextText/,
  '培训规则工具栏必须展示由当前类别和映射数量派生的上下文文案'
)

assert.match(
  trainingRulesTab,
  /const trainingRuleToolbarContextText = computed/,
  '培训规则上下文必须由 computed 派生，避免模板堆叠条件'
)

assert.match(
  trainingRulesTab,
  /mappingRows\.value\.length/,
  '已选择类别时必须展示当前培训对象映射数量'
)

assert.match(
  trainingRulesTab,
  /请选择文件类别后查看培训对象映射/,
  '未选择类别时必须保留明确操作指引'
)

assert.doesNotMatch(
  mappingWrap,
  /<el-alert[\s\S]*培训对象完全继承分发页签解析出的接收用户/,
  '映射区不应继续用独立 info alert 显示固定继承说明'
)

assert.match(
  mappingWrap,
  /trainingRuleWarningText/,
  '类别配置风险必须由合并后的 warning 文案展示'
)

const warningAlertCount = (mappingWrap.match(/type="warning"/g) || []).length
assert.equal(warningAlertCount, 1, '培训规则类别配置风险最多只能显示一条 warning alert')

assert.match(
  trainingRulesTab,
  /const trainingRuleWarningText = computed/,
  '类别配置风险必须由 computed 统一合并'
)

for (const riskToken of [
  '!currentCategory.value.trainingRequired',
  '!currentCategory.value.distributionRequired',
  '未开启“要求培训”',
  '未开启“要求分发”'
]) {
  assert.ok(trainingRulesTab.includes(riskToken), `培训规则风险提示必须保留真实类别条件：${riskToken}`)
}

for (const behaviorToken of [
  'getFileCategoryList()',
  'getSimpleDeptList()',
  'getSimpleUserList()',
  'getCategoryDistributionRules(queryParams.categoryId)',
  'buildResolvedTrainingUsers',
  'handleQuery',
  'resetQuery',
  'resolveTrainingPageErrorMessage'
]) {
  assert.ok(trainingRulesTab.includes(behaviorToken), `培训规则原有只读行为必须保留：${behaviorToken}`)
}

assert.doesNotMatch(
  `${toolbarWrap}\n${mappingWrap}`,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '培训规则上下文收敛不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC training rules context static contract')
