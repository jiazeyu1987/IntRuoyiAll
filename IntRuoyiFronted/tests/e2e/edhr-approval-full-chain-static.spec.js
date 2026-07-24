const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const approvalDetailPath = path.join(root, 'src/views/mes/pro/edhr/ApprovalDetailPage.vue')
const approvalCenterPath = path.join(root, 'src/views/approval-center/index.vue')
const approvalDetail = fs.readFileSync(approvalDetailPath, 'utf8')
const approvalCenter = fs.readFileSync(approvalCenterPath, 'utf8')

assert.match(
  approvalDetail,
  /const resolveApproveSuccessMessage = \(result: Awaited<ReturnType<typeof approveEdhrExecution>>\)/,
  'EDHR 审批详情必须集中解析 REVIEW/APPROVE 成功文案'
)
assert.match(
  approvalDetail,
  /EDHR_APPROVAL_ACTION_RESULT_TYPE\.REVIEW_INTERMEDIATE[\s\S]*EDHR_APPROVAL_ACTION_RESULT_TYPE\.REVIEW_TO_APPROVE/,
  'REVIEW 成功文案必须同时校验动作结果类型和 SUBMITTED 中间态'
)
assert.match(
  approvalDetail,
  /EDHR_APPROVAL_ACTION_RESULT_TYPE\.FINAL_APPROVED/,
  'APPROVE 成功文案必须校验最终批准动作结果类型'
)
assert.match(
  approvalDetail,
  /审核签名已完成，等待其他审核人或最终批准。/,
  'REVIEW 通过后的 SUBMITTED 中间态必须提示审核签名完成而非最终批准'
)
assert.match(
  approvalDetail,
  /最终批准完成，eDHR 已审批关闭。/,
  'APPROVE 通过后的 APPROVED 终态必须提示最终批准完成'
)
assert.doesNotMatch(
  approvalDetail,
  /当前签字格已通过，等待其他审核人/,
  'REVIEW 成功文案不得继续使用旧的模糊签字格通过描述'
)
assert.doesNotMatch(
  approvalDetail,
  /\?\s*'eDHR 已审批关闭'\s*:/,
  'APPROVE 成功文案不得只说审批关闭，必须体现最终批准语义'
)
assert.match(
  approvalDetail,
  /if \(!workTaskId\.value\)[\s\S]*缺少 eDHR 工作任务上下文，不能审批。/,
  'EDHR 正式审批动作缺 workTaskId 必须 fail fast'
)

assert.match(
  approvalCenter,
  /const canReview = \(row: ApprovalTaskSummaryVO\) => \{[\s\S]*actions\.includes\('APPROVE'\)[\s\S]*actions\.includes\('REJECT'\)/,
  '统一审批中心直接审核按钮必须只由显式 APPROVE/REJECT 能力驱动'
)
assert.match(
  approvalCenter,
  /router\.push\(\{[\s\S]*path:\s*row\.detailRoute,[\s\S]*query:\s*row\.detailQuery \|\| \{\}/,
  '统一审批中心跳转模块详情必须传递后端 detailQuery，包括 EDHR workTaskId'
)
assert.doesNotMatch(
  approvalCenter,
  /row\.moduleCode\s*===\s*['"]EDHR['"][\s\S]*openReviewDialog/,
  '统一审批中心不得为 EDHR 增加模块特例直接通过/驳回入口'
)

console.log('edhr approval full-chain static contract passed')
