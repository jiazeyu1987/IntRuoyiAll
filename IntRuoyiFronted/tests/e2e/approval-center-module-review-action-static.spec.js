const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const approvalCenterPath = path.join(root, 'src/views/approval-center/index.vue')
const approvalCenter = fs.readFileSync(approvalCenterPath, 'utf8')

assert.match(
  approvalCenter,
  /\{\{\s*resolveDecisionActionLabel\(row\)\s*\}\}/,
  '待办行的模块处理入口必须使用后端动作能力解析按钮文案，不能固定显示“详情”。'
)
assert.match(
  approvalCenter,
  /actions\.includes\('REVIEW_IN_MODULE'\)[\s\S]*?return '审核'/,
  'eDHR REVIEW 工作任务必须在审批中心显示“审核”入口。'
)
assert.match(
  approvalCenter,
  /actions\.includes\('APPROVE_IN_MODULE'\)[\s\S]*?return '批准'/,
  'eDHR APPROVE 工作任务必须在审批中心显示“批准”入口。'
)
assert.match(
  approvalCenter,
  /canReview\(row\)[\s\S]*?return '详情'/,
  '已支持统一弹窗直接审核的任务，第二入口必须保持为详情，避免重复显示审核按钮。'
)

console.log('approval center module review action static contract passed')
