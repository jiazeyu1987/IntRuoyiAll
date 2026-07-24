const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const dualTrack = read('tests/e2e/edhr-dual-track-real-flow.e2e.js')
const fullChain = read('tests/e2e/edhr-full-chain-multi-user-real-flow.e2e.js')
const batch881 = read('tests/e2e/edhr-881MO090863-full-flow.e2e.js')

assert.ok(
  !dualTrack.includes("['FIELD_CHANGE', 'FORM_REVIEW', 'SUBMIT', 'APPROVE']"),
  'dual-track ordinary/internal records must not require FORM_REVIEW and APPROVE signatures'
)
assert.ok(
  dualTrack.includes("['FIELD_CHANGE', 'SUBMIT']"),
  'dual-track ordinary/internal records must require field-change evidence and SUBMIT signature'
)
assert.ok(
  !batch881.includes('const reviewSign = await formReviewSign(page, config, index)'),
  '881 ordinary process flow must not perform form review signing before submit'
)
assert.ok(
  !batch881.includes('const approval = await approveExecution(page, config, executionCode, index)'),
  '881 ordinary process flow must not approve each ordinary process before close'
)
assert.ok(
  !batch881.includes('formReviewSignatureId: reviewSign?.signatureId') &&
    !batch881.includes('approvalSignatureId: approval?.signatureId'),
  '881 processed task evidence must not require form review or approval signature ids'
)
assert.ok(
  !batch881.includes('缺少 FORM_REVIEW 签名') && !batch881.includes('缺少 APPROVE 签名'),
  '881 review timeline must not fail ordinary process when FORM_REVIEW or APPROVE signatures are absent'
)
assert.ok(
  batch881.includes('缺少 SUBMIT 签名'),
  '881 review timeline must still require SUBMIT signature evidence'
)
assert.ok(
  !fullChain.includes('提交弹窗必须展示审核/批准人选择器'),
  'full-chain ordinary submit helper must not require review/approval assignee selector'
)
assert.ok(
  fullChain.includes('复盘时间线必须包含放行阶段审核/批准记录'),
  'full-chain batch evidence must move review/approval requirement to release-stage wording'
)

console.log('PASS: eDHR ordinary E2E contract cleanup static contract')
