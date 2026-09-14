const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const source = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/approval-center/index.vue'),
  'utf8'
)

const dialogTemplate = source.slice(
  source.indexOf('<el-dialog\n      v-model="reviewDialogVisible"'),
  source.indexOf('</el-dialog>', source.indexOf('<el-dialog\n      v-model="reviewDialogVisible"'))
)
const submitReview = source.slice(
  source.indexOf('const submitReview = async () => {'),
  source.indexOf('const normalizeApprovalDisplayText')
)

assert.match(
  dialogTemplate,
  /:label="reviewForm\.result === 'REJECT' \? '不通过原因' : '审批意见'"/,
  'approve and reject must both expose an authentic reason field'
)
assert.doesNotMatch(
  dialogTemplate,
  /v-if="reviewForm\.result === 'REJECT'"[^>]*label="不通过原因"/,
  'approve must not hide the reason field'
)
assert.match(
  submitReview,
  /if \(!reviewForm\.reason\.trim\(\)\)/,
  'all review outcomes must reject a blank reason before submission'
)
assert.match(
  submitReview,
  /reason:\s*reviewForm\.reason\.trim\(\)/,
  'all review outcomes must submit the reviewer-entered reason'
)
assert.doesNotMatch(
  submitReview,
  /reason:\s*reviewForm\.result === 'REJECT'[^\n]*:\s*undefined/,
  'approve must not submit an undefined reason'
)

console.log('approval center authentic approve reason static contract passed')
