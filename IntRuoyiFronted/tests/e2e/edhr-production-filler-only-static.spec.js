const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const routeFlowConfigPanel = fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteFlowConfigPanel.vue'), 'utf8')

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
const fillerDialog = extractBetween(
  routeFlowConfigPanel,
  'title="批记录表单填写人设置"',
  '<Dialog v-model="formulaTimeDialogVisible"'
)
const submitBlock = extractBetween(
  routeFlowConfigPanel,
  'const submitProcessFormPermission = async () => {',
  'const numericValue ='
)

for (const token of ['填写人：', '批记录表单填写人设置']) {
  assert.ok(reportRowTemplate.includes(token) || fillerDialog.includes(token), `ordinary process UI must show filler token: ${token}`)
}

for (const token of [
  '填写人设置',
  '填写人来源',
  '填写人',
  '请选择个人或角色',
  '保存填写设置'
]) {
  assert.ok(fillerDialog.includes(token), `filler dialog must expose token: ${token}`)
}

for (const forbiddenToken of [
  'route-flow-config-panel-permission-flow-note',
  '处理时限（分钟）',
  'placeholder="处理时限"',
  '处理时限必须大于'
]) {
  assert.ok(!fillerDialog.includes(forbiddenToken), `filler dialog must hide non-essential token: ${forbiddenToken}`)
}

for (const forbiddenToken of [
  '审核人：',
  '批准人：',
  '审核人设置',
  '批准人设置',
  '添加审核/批准人',
  '签名角色',
  '签名位Key',
  'buildReviewRuleStatus',
  'buildApproveRuleStatus',
  'signatureRoleOptions',
  'addSignatureRule',
  'removeSignatureRule',
  'cloneSignatureRules(permissionForm.signatureRules)'
]) {
  assert.ok(!reportRowTemplate.includes(forbiddenToken), `report row must not expose ordinary-process approval token: ${forbiddenToken}`)
  assert.ok(!fillerDialog.includes(forbiddenToken), `dialog must not expose ordinary-process approval token: ${forbiddenToken}`)
}

assert.ok(
  submitBlock.includes('signatureRules: []'),
  'ordinary process permission save payload must clear process-level review/approval signature rules'
)
assert.ok(
  !submitBlock.includes('cloneSignatureRules(permissionForm.signatureRules)'),
  'ordinary process permission save must not resubmit process-level signature rules'
)
assert.ok(!/mock|placeholder data|fallback/i.test(`${reportRowTemplate}\n${fillerDialog}`), 'production filler UI must not introduce mock, placeholder, or fallback logic')
assert.ok(!routeFlowConfigPanel.includes('降级'), 'production filler UI must not introduce downgrade logic')
assert.ok(!routeFlowConfigPanel.includes('吞异常'), 'production filler UI must not introduce swallowed-exception logic')

console.log('PASS: eDHR ordinary process production filler static contract')
