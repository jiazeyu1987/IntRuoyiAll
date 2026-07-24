const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const routeFlowConfigPanel = readSource('src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const processIndexPage = readSource('src/views/mes/pro/process/index.vue')
const routeFlowGraphDesigner = readSource('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const flowConfigApi = readSource('src/api/mes/pro/route/flowconfig.ts')
const permissionRuleApi = readSource('src/api/mes/pro/edhr/processFormPermissionRule.ts')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const reportRowTemplate = extractBetween(
  routeFlowConfigPanel,
  '<el-table-column v-if="configType === \'BATCH\'" label="批记录表单"',
  '<el-table-column label="备注"'
)

const permissionDialog = extractBetween(
  routeFlowConfigPanel,
  'title="批记录表单填写人设置"',
  '<Dialog v-model="formulaTimeDialogVisible"'
)

assert.strictEqual(
  packageJson.scripts['e2e:edhr:process-form-permission:static'],
  'node tests/e2e/edhr-process-form-permission-static.spec.js',
  'package.json must expose the eDHR process-form permission static script'
)

for (const token of [
  'route-flow-config-panel-filler-cell',
  'route-flow-config-panel-filler-users',
  '填写人：',
  'openProcessFormPermissionDialog(row, report)',
  'buildFillRuleCandidateUserText(report)'
]) {
  assert.ok(reportRowTemplate.includes(token), `report row must render permission token: ${token}`)
}

for (const token of [
  '批记录表单填写人设置',
  '填写人来源',
  '填写人',
  '请选择个人或角色',
  '完成策略',
  '保存填写设置',
  'submitProcessFormPermission'
]) {
  assert.ok(permissionDialog.includes(token), `permission dialog must keep token: ${token}`)
}

for (const token of [
  'buildFillRuleStatus',
  'buildFillRuleCandidateUserText',
  'candidateUsers',
  'loadProcessFormPermissionRules',
  'openProcessFormPermissionDialog',
  'validateCandidateRuleForSubmit',
  'getSimpleUserList',
  'getSimpleRoleList'
]) {
  assert.ok(routeFlowConfigPanel.includes(token), `route-flow-config-panel page must keep permission behavior: ${token}`)
}

for (const token of [
  'USER',
  'USERS',
  'ROLE',
  'ANY_ONE',
  'ALL',
  'APPROVAL',
  'APPROVE',
  'REVIEW'
]) {
  assert.ok(permissionRuleApi.includes(token), `permission API model must support token: ${token}`)
}

for (const endpoint of [
  '/mes/pro/edhr-process-form-permission-rule/get',
  '/mes/pro/edhr-process-form-permission-rule/save'
]) {
  assert.ok(permissionRuleApi.includes(endpoint), `permission API must call endpoint: ${endpoint}`)
}

assert.ok(
  flowConfigApi.includes('permissionRule?: EdhrProcessFormPermissionRuleRespVO | null'),
  'route-flow-config-panel batch record model must carry process-form permission rule state'
)
assert.ok(
  permissionRuleApi.includes('permissionScopeId?: number | null'),
  'permission API response must expose the backend-managed permission scope id'
)
assert.ok(
  routeFlowConfigPanel.includes('.route-flow-config-panel-filler-cell'),
  'report row must keep the single filler setting cell'
)
assert.ok(
  routeFlowConfigPanel.includes('.route-flow-config-panel-filler-users'),
  'configured filler rule must expose parsed candidate users under the configured status'
)
assert.ok(
  routeFlowConfigPanel.includes("buildFillRuleStatus(report).label === '已配置'"),
  'filler user names must render only under configured fill status'
)
assert.ok(
  routeFlowConfigPanel.includes("join('、')"),
  'multiple parsed filler users must be joined with Chinese separators'
)
assert.ok(
  routeFlowConfigPanel.includes('.route-flow-config-panel-permission-rule') &&
    routeFlowConfigPanel.includes('.route-flow-config-panel-permission-field'),
  'permission dialog must have stable readable layout class hooks'
)
assert.ok(
  routeFlowConfigPanel.includes('填写人：') && routeFlowConfigPanel.includes("label=\"填写人\""),
  'report row permission status must expose the single batch-record form filler setting'
)
assert.ok(
  routeFlowConfigPanel.includes("{ label: '个人', value: 'USERS' }") &&
    routeFlowConfigPanel.includes("{ label: '角色', value: 'ROLE' }"),
  'filler source options must be limited to personal users and roles'
)
assert.ok(
  routeFlowConfigPanel.includes('grid-template-columns: repeat(2, minmax(0, 1fr))'),
  'permission dialog fields must wrap into a two-column readable layout instead of one crowded row'
)

for (const forbiddenToken of ['组装Ⅰ', '组装I', '精洗', '粗洗', '清洗', '清洁', '光固Ⅰ', '光固I']) {
  assert.ok(
    !permissionRuleApi.includes(forbiddenToken),
    `permission API must not introduce process-specific special handling: ${forbiddenToken}`
  )
}

assert.ok(
  routeFlowConfigPanel.includes('placeholder="权限范围"'),
  'route flow page must carry inherited permission scope id from batch-record route configuration'
)
assert.ok(
  !routeFlowConfigPanel.includes('permissionScopeId: report.permissionScopeId || null'),
  'route flow save payload must not drop backend-managed permission scope id by truthy-only coercion'
)

for (const forbiddenToken of [
  'route-flow-config-panel-permission-flow-note',
  '处理时限（分钟）',
  'placeholder="处理时限"',
  '处理时限必须大于'
]) {
  assert.ok(!permissionDialog.includes(forbiddenToken), `permission dialog must hide non-essential token: ${forbiddenToken}`)
}

assert.ok(
  !routeFlowConfigPanel.includes('dueMinutes: 120') && !routeFlowConfigPanel.includes('rule?.dueMinutes || 120'),
  'permission dialog must not keep a finite default due-minutes value'
)
assert.ok(
  routeFlowConfigPanel.includes('signatureRules: []'),
  'ordinary process permission save payload must keep review and approval out of process-level configuration'
)
for (const forbiddenToken of [
  'equipmentFillRule',
  'qualityFillRule',
  'equipmentFillRuleStatus',
  'qualityFillRuleStatus',
  'fillRuleKindOptions',
  "value: 'EQUIPMENT'",
  "value: 'QUALITY'",
  '生产填写',
  '设备填写',
  '质量填写',
  'getSimpleDeptList',
  'DEPT_LEADER',
  "value: 'DEPT'"
]) {
  assert.ok(!routeFlowConfigPanel.includes(forbiddenToken), `route-flow-config-panel must remove legacy token: ${forbiddenToken}`)
  assert.ok(!permissionRuleApi.includes(forbiddenToken), `permission API must remove legacy token: ${forbiddenToken}`)
}

for (const forbiddenUserVisibleToken of [
  '生产填写人',
  '质量填写人',
  '设备填写人',
  'productionFillerNames',
  'qualityFillerNames',
  'equipmentFillerNames'
]) {
  assert.ok(!processIndexPage.includes(forbiddenUserVisibleToken), `工序设置列表必须移除旧填写人列：${forbiddenUserVisibleToken}`)
  assert.ok(!routeFlowGraphDesigner.includes(forbiddenUserVisibleToken), `工艺流程工序详情必须移除旧填写人字段：${forbiddenUserVisibleToken}`)
}

assert.ok(!/mock|placeholder data|fallback/i.test(permissionRuleApi), 'permission API must not introduce mock, placeholder, or fallback logic')
assert.ok(!/mock|placeholder data|fallback/i.test(permissionDialog), 'permission dialog must not introduce mock, placeholder, or fallback logic')
assert.ok(!routeFlowConfigPanel.includes('降级'), 'route-flow-config-panel page must not introduce downgrade logic')
assert.ok(!routeFlowConfigPanel.includes('吞异常'), 'route-flow-config-panel page must not introduce swallowed-exception logic')

console.log('PASS: eDHR process-form permission static contract')
