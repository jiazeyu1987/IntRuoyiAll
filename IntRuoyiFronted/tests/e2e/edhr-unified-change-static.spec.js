const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/unifiedChange.ts')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-unified-change/UnifiedChangePage.vue')

assert(fs.existsSync(apiPath), 'eDHR 统一变更 API 文件必须存在。')
assert(fs.existsSync(pagePath), 'eDHR 统一变更页面必须存在。')

const api = fs.readFileSync(apiPath, 'utf8')
const page = fs.readFileSync(pagePath, 'utf8')

for (const endpoint of [
  '/mes/pro/edhr-change/unified/page',
  '/mes/pro/edhr-change/unified/impact/page',
  '/mes/pro/edhr-change/unified/event/page',
  '/mes/pro/edhr-change/unified/create',
  '/mes/pro/edhr-change/unified/submit',
  '/mes/pro/edhr-change/unified/recalculate-impact',
  '/mes/pro/edhr-change/unified/approve',
  '/mes/pro/edhr-change/unified/effect'
]) {
  assert.match(api, new RegExp(endpoint.replaceAll('/', '\\/')), `API 必须声明接口 ${endpoint}`)
}

for (const token of [
  "'FORM_TEMPLATE'",
  "'DHR_TEMPLATE'",
  "'RECORDBOOK_TEMPLATE'",
  "'DRAFT'",
  "'SUBMITTED'",
  "'APPROVED'",
  "'EFFECT_BLOCKED'",
  'EdhrUnifiedChangeCreateReqVO',
  'EdhrUnifiedChangeSubmitReqVO',
  'EdhrUnifiedChangeRecalculateImpactReqVO',
  'EdhrUnifiedChangeApproveReqVO',
  'EdhrUnifiedChangeEffectReqVO',
  'EdhrUnifiedChangeImpactRespVO',
  'EdhrUnifiedChangeEventRespVO',
  'diffSnapshotJson',
  'impactSummaryJson',
  'impactRecalculationHash',
  'approvalSignoffEvidenceHash',
  'effectSignoffEvidenceHash',
  'idempotencyKey',
  'getEdhrUnifiedChangePage',
  'getEdhrUnifiedChangeImpactPage',
  'getEdhrUnifiedChangeEventPage',
  'createEdhrUnifiedChange',
  'submitEdhrUnifiedChange',
  'recalculateEdhrUnifiedChangeImpact',
  'approveEdhrUnifiedChange',
  'requestEdhrUnifiedChangeEffect'
]) {
  assert.match(api, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `API 类型和方法缺少 ${token}`)
}

for (const label of [
  '统一变更',
  '影响范围',
  '差异快照',
  '对象类型',
  '原版本',
  '目标版本',
  '风险等级',
  '再培训',
  '再验证',
  '放行复检',
  '审批',
  '生效申请',
  '幂等键',
  '签核证据',
  '历史版本不可覆盖'
]) {
  assert.ok(page.includes(label), `页面必须呈现统一变更字段：${label}`)
}

for (const token of [
  'changeDialogVisible',
  'impactDrawerVisible',
  'eventDrawerVisible',
  'impactList',
  'eventList',
  'loadImpactList',
  'loadEventList',
  'openCreateDialog',
  'openSubmitDialog',
  'openApproveDialog',
  'openEffectDialog',
  'handleRecalculateImpact',
  'handleChangeConfirm',
  "v-hasPermi=\"['mes:pro-edhr-change:unified-create']\"",
  "v-hasPermi=\"['mes:pro-edhr-change:unified-submit']\"",
  "v-hasPermi=\"['mes:pro-edhr-change:unified-approve']\"",
  "v-hasPermi=\"['mes:pro-edhr-change:unified-effect']\"",
  "v-hasPermi=\"['mes:pro-edhr-change:impact-query']\"",
  "v-hasPermi=\"['mes:pro-edhr-change:event-query']\""
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `页面交互契约缺少 ${token}`)
}

for (const token of [
  'loadError',
  'actionError',
  'changeError',
  'message.error(resolveErrorMessage',
  '<el-alert v-if="loadError"',
  '<el-alert v-if="actionError"',
  '<el-alert v-if="changeError"'
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
  'OVERWRITE_CURRENT_VERSION_SUCCESS',
  'FORCE_EFFECT_SUCCESS',
  'silent',
  'catch {}',
  'catch{}'
]) {
  assert.doesNotMatch(api, new RegExp(forbidden, 'i'), `API 不得伪造统一变更：${forbidden}`)
  assert.doesNotMatch(page, new RegExp(forbidden, 'i'), `页面不得伪造统一变更：${forbidden}`)
}

console.log('PASS: eDHR unified change static contract')
