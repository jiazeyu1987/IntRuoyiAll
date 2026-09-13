const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const apiSource = readSource('src/api/dcc/controlledFile/projectCodes.ts')
const panelSource = readSource(
  'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'
)

for (const apiToken of [
  'export interface DccProjectAccessRuleRespVO',
  'export interface DccProjectAccessRuleSaveReqVO',
  'export interface DccProjectAccessRuleBatchSaveReqVO',
  'getProjectCodeAccessRules',
  '/dcc/project-codes/${projectCodeId}/access-rules',
  'replaceProjectCodeAccessRules',
  "rules: DccProjectAccessRuleSaveReqVO[]"
]) {
  assert.ok(apiSource.includes(apiToken), `项目正式权限 API 契约必须包含 ${apiToken}`)
}

const accessDialog = extractBetween(
  panelSource,
  'data-testid="dcc-project-code-access-rules-dialog"',
  'const submitProjectAccessRules'
)
const normalizePayload = extractBetween(
  panelSource,
  'const normalizeProjectAccessRulePayload',
  'const productOnboardingFormData'
)

for (const panelToken of [
  '正式负责人/编制权限',
  'data-testid="dcc-project-code-access-rules-open"',
  'data-testid="dcc-project-code-access-rules-table"',
  'data-testid="dcc-project-code-access-rules-save"',
  'getProjectCodeAccessRules',
  'replaceProjectCodeAccessRules',
  'projectAccessRules',
  'projectAccessRuleFormRows',
  "subjectType: 'USER'",
  "accessLevel: 'OWNER'",
  "accessLevel: 'EDIT'",
  "accessLevel: 'VIEW'"
]) {
  assert.ok(panelSource.includes(panelToken), `项目代码页必须提供正式权限维护闭环：${panelToken}`)
}

assert.ok(
  !accessDialog.includes('projectLeader'),
  '正式 OWNER/EDIT 维护不得读取 projectLeader 描述字段作为授权来源'
)
assert.ok(
  !/fallback|mock|placeholder|默认负责人|自动负责人/.test(accessDialog),
  '正式权限维护不得引入 fallback、mock、placeholder 或默认负责人推断'
)
assert.match(
  normalizePayload,
  /const changeReason = row\.changeReason\?\.trim\(\)/,
  '正式权限保存必须规范化变更原因，不能把空白交给数据库约束或默认文案'
)
assert.ok(
  normalizePayload.includes('正式权限规则必须填写变更原因'),
  '正式权限保存必须在前端明确阻断空白变更原因'
)
assert.match(
  normalizePayload,
  /changeReason,?\s*\n\s*}\)/,
  '正式权限保存必须提交规范化后的变更原因'
)
assert.doesNotMatch(
  normalizePayload,
  /changeReason:\s*row\.changeReason\s*\|\|\s*null/,
  '正式权限保存不得把空白变更原因作为 null 交给后端或数据库兜底'
)

console.log('PASS: DCC project access rules static contract')
