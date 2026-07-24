const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const routeFlowConfigPanel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowConfigPanel.vue'),
  'utf8'
)

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const reportRowTemplate = extractBetween(
  routeFlowConfigPanel,
  'class="route-flow-config-panel-report-list__row"',
  '</div>\n                </div>'
)
const personnelDialog = extractBetween(
  routeFlowConfigPanel,
  'title="工序表单填写设置"',
  '<Dialog v-model="formulaTimeDialogVisible"'
)

for (const token of [
  '生产填写',
  '设备填写',
  '质量填写'
]) {
  assert.ok(reportRowTemplate.includes(token), `report row must show clear personnel setting token: ${token}`)
}

for (const token of [
  '填写设置',
  '填写人来源',
  '填写人/填写范围',
  '请选择哪些人员可以填写',
  '保存填写设置',
  'submitProcessFormPermission'
]) {
  assert.ok(personnelDialog.includes(token), `personnel dialog must expose clear role token: ${token}`)
}

for (const forbiddenToken of [
  'label="工序"',
  'label="批记录表单"',
  'route-flow-config-panel-permission-flow-note',
  '处理时限（分钟）',
  'placeholder="处理时限"',
  '处理时限必须大于'
]) {
  assert.ok(!personnelDialog.includes(forbiddenToken), `personnel dialog must hide non-essential token: ${forbiddenToken}`)
}

for (const forbiddenToken of [
  '规则状态',
  '权限/派工',
  '审批位：',
  '批准/复核位：',
  '审核人：',
  '批准人：',
  '签名位权限',
  '保存权限/派工',
  '添加审核/批准人'
]) {
  assert.ok(!reportRowTemplate.includes(forbiddenToken), `report row must not expose confusing token: ${forbiddenToken}`)
  assert.ok(!personnelDialog.includes(forbiddenToken), `personnel dialog must not expose confusing token: ${forbiddenToken}`)
}

assert.ok(
  routeFlowConfigPanel.includes('signatureRules: []') &&
    !routeFlowConfigPanel.includes('cloneSignatureRules(permissionForm.signatureRules)'),
  'ordinary process save must not include process-level review or approval signature rules'
)
const personnelUiSource = `${reportRowTemplate}\n${personnelDialog}`
assert.ok(!/mock|placeholder data|fallback/i.test(personnelUiSource), 'role assignment UI must not introduce mock, placeholder, or fallback logic')
assert.ok(!routeFlowConfigPanel.includes('降级'), 'role assignment UI must not introduce downgrade logic')
assert.ok(!routeFlowConfigPanel.includes('吞异常'), 'role assignment UI must not introduce swallowed-exception logic')

console.log('PASS: eDHR role assignment UI static contract')
