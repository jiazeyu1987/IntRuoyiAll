const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const backendRoot = path.resolve(root, '..', 'ruoyi-vue-pro')

const readFrontend = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readBackend = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const approvalApi = readFrontend('src/api/mes/pro/edhr/approval.ts')
const signaturesApi = readFrontend('src/api/mes/pro/edhr/signatures.ts')
const trackingApi = readFrontend('src/api/mes/pro/edhr/tracking.ts')
const approvalDetail = readFrontend('src/views/mes/pro/edhr/ApprovalDetailPage.vue')
const signaturePage = readFrontend('src/views/mes/pro/edhr/SignaturePage.vue')
const approvalCenter = readFrontend('src/views/approval-center/index.vue')
const executionService = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionServiceImpl.java'
)
const signatureService = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureService.java'
)
const workTaskService = readBackend(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java'
)

assert.match(
  signatureService,
  /ACTION_REVIEW_APPROVE\s*=\s*"REVIEW_APPROVE"/,
  '后端必须新增独立 REVIEW 通过签名动作，不能继续把审核签名记录为 APPROVE'
)
assert.match(
  executionService,
  /TASK_TYPE_REVIEW[\s\S]*ACTION_REVIEW_APPROVE/,
  'REVIEW 通过必须写入 REVIEW_APPROVE 签名动作'
)
assert.match(
  executionService,
  /TASK_TYPE_APPROVE[\s\S]*\{\s*[\s\S]*return MesProBatchRecordExecutionSignatureService\.ACTION_APPROVE/,
  'APPROVE 通过必须保留最终批准 APPROVE 签名动作'
)
assert.doesNotMatch(
  executionService,
  /return MesProBatchRecordExecutionSignatureService\.ACTION_APPROVE;\s*\}\s*private String resolveApprovalActionName/,
  'APPROVE 签名动作不得作为未知任务类型的默认兜底'
)

assert.match(approvalApi, /taskType\?:\s*'REVIEW'\s*\|\s*'APPROVE'/, '审批详情类型必须接收后端 taskType')
assert.match(approvalApi, /workTaskId:\s*number[\s\S]*\)\s*=>/, '审批详情 API 必须强制 workTaskId')
assert.match(
  approvalDetail,
  /if \(!workTaskId\.value\)[\s\S]*缺少 eDHR 工作任务上下文，无法加载审批详情。[\s\S]*return/,
  '审批详情加载阶段缺 workTaskId 必须 fail fast，不能先请求能力再提交失败'
)
assert.match(
  approvalDetail,
  /const resolveApprovalTaskKind[\s\S]*taskType[\s\S]*REVIEW[\s\S]*APPROVE/,
  '详情页必须基于后端 taskType 区分 REVIEW 与 APPROVE'
)
assert.match(approvalDetail, /审核签名|复核完成/, 'REVIEW 任务按钮、弹窗或提示必须体现审核签名/复核完成语义')
assert.match(approvalDetail, /最终批准/, 'APPROVE 任务按钮、弹窗或提示必须体现最终批准语义')
assert.doesNotMatch(
  approvalDetail,
  />\s*通过\s*<\/el-button>[\s\S]*>\s*驳回\s*<\/el-button>/,
  '详情页动作按钮不得继续使用不区分 REVIEW/APPROVE 的“通过/驳回”'
)

for (const source of [signaturesApi, approvalDetail, signaturePage]) {
  assert.match(source, /REVIEW_APPROVE/, '前端签名合同和展示必须支持 REVIEW_APPROVE')
}
assert.match(trackingApi, /EdhrTrackingEventType[\s\S]*REVIEW_APPROVE/, '前端追踪事件类型必须支持 REVIEW_APPROVE')
assert.match(trackingApi, /actionType\?:[\s\S]*REVIEW_APPROVE/, '前端追踪 actionType 合同必须支持 REVIEW_APPROVE')
assert.match(approvalDetail, /REVIEW_APPROVE:\s*['"]审核签名|REVIEW_APPROVE:\s*['"]复核完成/, '详情页签名动作标签必须区分 REVIEW 通过')
assert.match(signaturePage, /REVIEW_APPROVE:\s*['"]审核签名|REVIEW_APPROVE:\s*['"]复核完成/, '签名页动作标签必须区分 REVIEW 通过')

assert.match(
  approvalCenter,
  /const canReview = \(row: ApprovalTaskSummaryVO\) => \{[\s\S]*actions\.includes\('APPROVE'\)[\s\S]*actions\.includes\('REJECT'\)/,
  '统一审批中心直接审核必须只由显式 APPROVE/REJECT 能力驱动'
)
assert.doesNotMatch(
  approvalCenter,
  /row\.moduleCode\s*===\s*['"]EDHR['"][\s\S]*reviewApprovalTask/,
  '统一审批中心不得为 EDHR 增加直接审核提交分支'
)
assert.match(
  approvalCenter,
  /const openReviewAction = \(row: ApprovalTaskSummaryVO\) =>[\s\S]*?openReviewDialog\(row\)[\s\S]*?openDecisionDetail\(row\)/,
  '审批中心必须集中分发直接审核弹窗和 eDHR 模块审核入口'
)

assert.match(workTaskService, /MesProEdhrCandidateResolver/, '工作任务候选解析必须委托共享候选解析入口')
assert.match(executionService, /MesProEdhrCandidateResolver/, '提交审核候选解析必须委托共享候选解析入口')
assert.doesNotMatch(
  executionService,
  /private List<ReviewCandidateUser> resolveReviewCandidates/,
  '提交审核不得保留独立候选解析方法'
)
assert.match(workTaskService, /MesProEdhrCandidateContract/, '候选解析必须收敛到同一候选合同对象')
assert.doesNotMatch(
  workTaskService,
  /resolveProcessFormCandidateUserSnapshot/,
  '提交预检、任务生成和详情展示不得保留独立漂移的候选解析入口'
)

console.log('PASS: EDHR approval consistency static contract')
