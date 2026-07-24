const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')

const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const batchRecordFormListPage = read('src/views/mes/pro/batchrecordformlist/index.vue')
const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
const batchDetailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')

const routeUseDialog = extractBetween(
  batchRecordFormListPage,
  'title="批记录表单填写人设置"',
  'const submitBatchRecordFormPermission = async () => {'
)
const routeUseSubmit = extractBetween(
  batchRecordFormListPage,
  'const submitBatchRecordFormPermission = async () => {',
  'const getList = async () => {'
)
const submitDialog = extractBetween(
  executionPage,
  '<Dialog title="提交 eDHR 执行"',
  '<Dialog title="表单复核签名"'
)
const submitHandler = extractBetween(
  executionPage,
  'const handleSubmitExecution = async () => {',
  'const openFormReviewSignDialog = () => {'
)
const pendingMainPreviewStart = batchDetailPage.indexOf(
  'class="edhr-batch-detail__form-panel edhr-batch-detail__review-preview"'
)
const pendingRailStart = batchDetailPage.indexOf('<aside class="edhr-batch-detail__review-rail"')
assert.ok(pendingMainPreviewStart >= 0, 'Missing pending main preview start')
assert.ok(pendingRailStart > pendingMainPreviewStart, 'Missing right rail after pending main preview')
const pendingMainPreview = batchDetailPage.slice(pendingMainPreviewStart, pendingRailStart)
const pendingDetailPanel = extractBetween(
  batchDetailPage,
  '<aside class="edhr-batch-detail__review-rail"',
  '</aside>'
)
const pendingTaskRules = extractBetween(
  batchDetailPage,
  'const canOpenTask =',
  'const canOperateSpecialNode ='
)

for (const token of ['批记录表单填写人设置', '填写人来源', '填写人', '保存填写设置']) {
  assert.ok(routeUseDialog.includes(token), `ordinary process config must keep filler token: ${token}`)
}

for (const forbiddenToken of ['fillRuleKindOptions', "'EQUIPMENT'", "'QUALITY'", '设备填写', '质量填写']) {
  assert.ok(!batchRecordFormListPage.includes(forbiddenToken), `ordinary process config must remove legacy filler token: ${forbiddenToken}`)
}

for (const forbiddenToken of [
  '审核人设置',
  '批准人设置',
  '添加审核/批准人',
  '签名角色',
  '签名位Key',
  'cloneSignatureRules(permissionForm.signatureRules)'
]) {
  assert.ok(!routeUseDialog.includes(forbiddenToken), `ordinary process config must not expose approval token: ${forbiddenToken}`)
}

assert.ok(
  routeUseSubmit.includes('fillRule: cloneCandidateRule(permissionForm.fillRule)'),
  'ordinary process config save must persist only the fill rule payload'
)

for (const forbiddenToken of [
  'reviewAssigneeOptions.length > 0',
  'reviewAssigneeOptionLabel',
  'reviewCandidateOptionLabel',
  'reviewAssigneeOptionError',
  '请选择审核/批准人',
  '请选择全部审核/批准人后再提交',
  'reviewAssigneeSelections',
  'signatureRules'
]) {
  assert.ok(!submitDialog.includes(forbiddenToken), `ordinary submit dialog must not expose approval selector: ${forbiddenToken}`)
  assert.ok(!submitHandler.includes(forbiddenToken), `ordinary submit handler must not require approval selector: ${forbiddenToken}`)
}

assert.ok(
  !/reviewAssigneeSelections\s*:/.test(submitHandler),
  'ordinary submit payload must not send process-level review/approval assignee selections'
)
assert.ok(
  submitDialog.includes('提交密码') && submitDialog.includes('签名时间'),
  'ordinary submit dialog must keep electronic signature password and signature time fields'
)

for (const forbiddenToken of [
  '审批</div>',
  'approvalStatus',
  "REVIEWER: '审核人'",
  "APPROVER: '批准人'",
  "'REVIEW_APPROVE'",
  "'REVIEW_REJECT'",
  "hasAllowedTaskAction(row, 'APPROVE')",
  "hasAllowedTaskAction(row, 'REJECT')",
  '去审核',
  '去批准'
]) {
  assert.ok(!pendingDetailPanel.includes(forbiddenToken), `ordinary pending detail must not expose approval state: ${forbiddenToken}`)
  assert.ok(!pendingTaskRules.includes(forbiddenToken), `ordinary pending task rules must not route to approval action: ${forbiddenToken}`)
}

assert.ok(
  pendingDetailPanel.includes('primaryFormFillMetaItems') &&
    pendingDetailPanel.includes('{{ item.label }}'),
  'ordinary pending detail must keep filler information in the right-side first-level area'
)
assert.ok(
  !pendingMainPreview.includes('class="edhr-batch-detail__primary-fill-meta"'),
  'ordinary pending detail must not keep filler information in the top red-box preview area'
)

console.log('PASS: eDHR ordinary process fill-only T4 static contract')
