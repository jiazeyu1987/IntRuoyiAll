const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/release.ts')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-release/ReleasePage.vue')

assert(fs.existsSync(apiPath), 'eDHR 放行 API 文件必须存在。')
assert(fs.existsSync(pagePath), 'eDHR 放行页面必须存在。')

const api = fs.readFileSync(apiPath, 'utf8')
const page = fs.readFileSync(pagePath, 'utf8')

for (const endpoint of [
  '/mes/pro/edhr-release/submit',
  '/mes/pro/edhr-release/approve',
  '/mes/pro/edhr-release/reject',
  '/mes/pro/edhr-release/withdraw',
  '/mes/pro/edhr-release/event/page'
]) {
  assert.match(api, new RegExp(endpoint.replaceAll('/', '\\/')), `API 必须声明接口 ${endpoint}`)
}

for (const token of [
  "'PENDING_APPROVAL'",
  "'RELEASED'",
  "'REJECTED'",
  "'WITHDRAWN'",
  'EdhrReleaseSubmitReqVO',
  'EdhrReleaseApproveReqVO',
  'EdhrReleaseRejectReqVO',
  'EdhrReleaseWithdrawReqVO',
  'EdhrReleaseEventRespVO',
  'idempotencyKey',
  'signoffEvidenceHash',
  'submitEdhrRelease',
  'approveEdhrRelease',
  'rejectEdhrRelease',
  'withdrawEdhrRelease',
  'getEdhrReleaseEventPage'
]) {
  assert.match(api, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `API 类型和方法缺少 ${token}`)
}

for (const label of [
  '电子批记录放行追溯',
  '事务事件',
  '签核证据',
  '检查项',
  '待审批',
  '已放行',
  '已驳回',
  '已撤回'
]) {
  assert.ok(page.includes(label), `放行追溯页面必须呈现历史追溯字段：${label}`)
}

for (const token of [
  'eventDrawerVisible',
  'eventList',
  'loadEventList',
  "v-hasPermi=\"['mes:pro-edhr-release:event-query']\""
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `放行追溯页面缺少历史追溯交互：${token}`)
}

for (const forbidden of [
  '执行预检',
  '提交放行',
  '批准放行',
  '驳回放行',
  '撤回放行',
  'transactionDialogVisible',
  'transactionDialogTitle',
  'transactionSubmitting',
  'openSubmitDialog',
  'openApproveDialog',
  'openRejectDialog',
  'openWithdrawDialog',
  'submitReleaseTransaction',
  'approveReleaseTransaction',
  'rejectReleaseTransaction',
  'withdrawReleaseTransaction',
  'handlePrecheck',
  'precheckEdhrRelease',
  'submitEdhrRelease',
  'approveEdhrRelease',
  'rejectEdhrRelease',
  'withdrawEdhrRelease',
  "v-hasPermi=\"['mes:pro-edhr-release:precheck']\"",
  "v-hasPermi=\"['mes:pro-edhr-release:submit']\"",
  "v-hasPermi=\"['mes:pro-edhr-release:approve']\"",
  "v-hasPermi=\"['mes:pro-edhr-release:reject']\"",
  "v-hasPermi=\"['mes:pro-edhr-release:withdraw']\""
]) {
  assert.doesNotMatch(page, new RegExp(forbidden.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `放行追溯页面不得作为放行操作入口：${forbidden}`)
}

for (const token of [
  'loadError',
  'actionError',
  'message.error(resolveErrorMessage',
  '<el-alert v-if="loadError"',
  '<el-alert v-if="actionError"'
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `页面必须暴露后端失败原因：${token}`)
}

for (const forbidden of [
  'mock',
  'fixture',
  'demo',
  'DEFAULT_SUCCESS',
  'MOCK_SIGNOFF',
  'silent',
  'catch {}',
  'catch{}'
]) {
  assert.doesNotMatch(api, new RegExp(forbidden, 'i'), `API 不得伪造放行事务生命周期：${forbidden}`)
  assert.doesNotMatch(page, new RegExp(forbidden, 'i'), `页面不得伪造放行事务生命周期：${forbidden}`)
}

console.log('PASS: eDHR release transaction lifecycle static contract')
