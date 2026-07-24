const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const accessRulePage = readSource(
  'src/views/dcc/controlled-file/components/DirectoryAuthorizationTabPanel.vue'
)

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const rulePanelHeader = extractBetween(
  accessRulePage,
  '<el-col :span="17">',
  '<el-table v-loading="loading" :data="rules"'
)

for (const redundantCopy of ['当前目录已选中', '未选择目录']) {
  assert.ok(
    !rulePanelHeader.includes(redundantCopy),
    `访问规则标题区不应继续显示低信息量副标题：${redundantCopy}`
  )
}

assert.ok(
  rulePanelHeader.includes('data-testid="dcc-access-rule-header-context"'),
  '访问规则标题上下文必须提供稳定测试标识'
)

assert.ok(
  rulePanelHeader.includes('accessRuleHeaderContextText'),
  '访问规则标题区必须展示由当前选择和规则数量派生的上下文文案'
)

assert.ok(
  accessRulePage.includes('const accessRuleHeaderContextText = computed'),
  '访问规则标题上下文必须由 computed 派生，避免在模板中堆叠条件表达式'
)

assert.ok(
  accessRulePage.includes('rules.value.length'),
  '已选择目录时必须展示当前规则数量'
)

assert.ok(
  accessRulePage.includes('从左侧选择目录后维护访问规则'),
  '未选择目录时必须给出明确操作指引'
)

for (const behaviorToken of [
  'reloadCurrentRules',
  'addRule',
  'saveRules',
  'handleBoundDirectoryClick',
  'getDirectoryAccessRules',
  ':disabled="!selectedDirectoryId"',
  'row.canQuery',
  'row.canDownload',
  'row.active',
  'handleQueryPermissionChange(row)',
  'mergeRuleReadPermission'
]) {
  assert.ok(accessRulePage.includes(behaviorToken), `访问规则原有行为必须保留：${behaviorToken}`)
}

assert.ok(
  !/mock|placeholder data|fallback|降级|吞异常/.test(rulePanelHeader),
  '访问规则标题上下文不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC access rule header context static contract')
