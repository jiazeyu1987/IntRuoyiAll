const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/flowIntervention.ts')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-flow-intervention/FlowInterventionPage.vue')

assert(fs.existsSync(apiPath), 'eDHR 流程干预 API 文件必须存在。')
assert(fs.existsSync(pagePath), 'eDHR 流程干预页面必须存在。')

const api = fs.readFileSync(apiPath, 'utf8')
const page = fs.readFileSync(pagePath, 'utf8')

for (const endpoint of [
  '/mes/pro/edhr-flow-intervention/page',
  '/mes/pro/edhr-flow-intervention/event/page',
  '/mes/pro/edhr-flow-intervention/return',
  '/mes/pro/edhr-flow-intervention/withdraw',
  '/mes/pro/edhr-flow-intervention/transfer',
  '/mes/pro/edhr-flow-intervention/add-sign',
  '/mes/pro/edhr-flow-intervention/admin-intervene'
]) {
  assert.match(api, new RegExp(endpoint.replaceAll('/', '\\/')), `API 必须声明接口 ${endpoint}`)
}

for (const token of [
  "'RETURN'",
  "'WITHDRAW'",
  "'TRANSFER'",
  "'ADD_SIGN'",
  "'ADMIN_INTERVENE'",
  'EdhrFlowInterventionReturnReqVO',
  'EdhrFlowInterventionWithdrawReqVO',
  'EdhrFlowInterventionTransferReqVO',
  'EdhrFlowInterventionAddSignReqVO',
  'EdhrFlowInterventionAdminReqVO',
  'EdhrFlowEventRespVO',
  'idempotencyKey',
  'signoffEvidenceHash',
  'authorizationBasis',
  'submitReturnIntervention',
  'submitWithdrawIntervention',
  'submitTransferIntervention',
  'submitAddSignIntervention',
  'submitAdminIntervention',
  'getEdhrFlowInterventionPage',
  'getEdhrFlowEventPage'
]) {
  assert.match(api, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `API 类型和方法缺少 ${token}`)
}

for (const label of [
  '流程干预',
  '流程日志',
  '退回',
  '撤回',
  '转办',
  '加签',
  '管理员干预',
  '原因',
  '签核证据',
  '授权依据',
  '完整性复检',
  '幂等键',
  '目标处理人'
]) {
  assert.ok(page.includes(label), `页面必须呈现流程干预字段：${label}`)
}

for (const token of [
  'interventionDialogVisible',
  'eventDrawerVisible',
  'flowEventList',
  'targetUserOptions',
  'loadTargetUserOptions',
  'getSimpleUserList',
  'resolveTargetUserLabel',
  'loadFlowEventList',
  'openReturnDialog',
  'openWithdrawDialog',
  'openTransferDialog',
  'openAddSignDialog',
  'openAdminInterveneDialog',
  'submitReturnInterventionAction',
  'submitWithdrawInterventionAction',
  'submitTransferInterventionAction',
  'submitAddSignInterventionAction',
  'submitAdminInterventionAction',
  "v-hasPermi=\"['mes:pro-edhr-flow-intervention:return']\"",
  "v-hasPermi=\"['mes:pro-edhr-flow-intervention:withdraw']\"",
  "v-hasPermi=\"['mes:pro-edhr-flow-intervention:transfer']\"",
  "v-hasPermi=\"['mes:pro-edhr-flow-intervention:add-sign']\"",
  "v-hasPermi=\"['mes:pro-edhr-flow-intervention:admin-intervene']\"",
  "v-hasPermi=\"['mes:pro-edhr-flow-intervention:event-query']\""
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `页面交互契约缺少 ${token}`)
}

assert.match(
  page,
  /<el-select[\s\S]*v-model="interventionForm\.targetUserId"[\s\S]*placeholder="选择目标处理人"/,
  '流程干预目标处理人必须从正式用户列表选择，不能要求业务用户手填数字 ID。'
)
assert.doesNotMatch(
  page,
  /<el-input-number[\s\S]*v-model="interventionForm\.targetUserId"/,
  '流程干预目标处理人不得使用数字输入框。'
)

for (const token of [
  'loadError',
  'actionError',
  'interventionError',
  'message.error(resolveErrorMessage',
  '<el-alert v-if="loadError"',
  '<el-alert v-if="actionError"',
  '<el-alert v-if="interventionError"'
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `页面必须暴露后端失败原因：${token}`)
}

for (const forbidden of [
  'mock',
  'fixture',
  'demo',
  'DEFAULT_SUCCESS',
  'MOCK_SIGNOFF',
  'DIRECT_STATUS_UPDATE',
  'silent',
  'catch {}',
  'catch{}'
]) {
  assert.doesNotMatch(api, new RegExp(forbidden, 'i'), `API 不得伪造流程干预：${forbidden}`)
  assert.doesNotMatch(page, new RegExp(forbidden, 'i'), `页面不得伪造流程干预：${forbidden}`)
}

console.log('PASS: eDHR flow intervention static contract')
