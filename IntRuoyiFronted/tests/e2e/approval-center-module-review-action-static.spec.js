const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const approvalCenterPath = path.join(root, 'src/views/approval-center/index.vue')
const approvalCenter = fs.readFileSync(approvalCenterPath, 'utf8')

assert.match(
  approvalCenter,
  /const canReviewAction = \(row: ApprovalTaskSummaryVO\) =>[\s\S]*?canReviewInModule\(row\)/,
  '待办行审核入口必须使用后端动作能力合并直接审核和模块审核。'
)
assert.match(
  approvalCenter,
  /actions\.includes\('REVIEW_IN_MODULE'\)/,
  'eDHR REVIEW 工作任务必须保留模块审核能力。'
)
assert.match(
  approvalCenter,
  /actions\.includes\('APPROVE_IN_MODULE'\)/,
  'eDHR APPROVE 工作任务必须保留模块审核能力。'
)
assert.match(
  approvalCenter,
  /const openReviewAction = \(row: ApprovalTaskSummaryVO\) =>[\s\S]*?openReviewDialog\(row\)[\s\S]*?openDecisionDetail\(row\)/,
  '审核入口必须按任务能力选择统一审核弹窗或模块审核页。'
)

console.log('approval center module review action static contract passed')
