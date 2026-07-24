const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const feedbackApiPath = path.join(repoRoot, 'src/api/mes/pro/feedback/index.ts')
const approvalApiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/approval.ts')
const fieldAuditApiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/fieldAudit.ts')
const executionPagePath = path.join(repoRoot, 'src/views/mes/pro/edhr/ExecutionPage.vue')
const approvalDetailPagePath = path.join(repoRoot, 'src/views/mes/pro/edhr/ApprovalDetailPage.vue')
const batchDetailPagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')

for (const filePath of [
  feedbackApiPath,
  approvalApiPath,
  fieldAuditApiPath,
  executionPagePath,
  approvalDetailPagePath,
  batchDetailPagePath
]) {
  assert(fs.existsSync(filePath), `${filePath} 必须存在。`)
}

const feedbackApi = fs.readFileSync(feedbackApiPath, 'utf8')
const approvalApi = fs.readFileSync(approvalApiPath, 'utf8')
const fieldAuditApi = fs.readFileSync(fieldAuditApiPath, 'utf8')
const executionPage = fs.readFileSync(executionPagePath, 'utf8')
const approvalDetailPage = fs.readFileSync(approvalDetailPagePath, 'utf8')
const batchDetailPage = fs.readFileSync(batchDetailPagePath, 'utf8')

assert.match(feedbackApi, /workTaskId:\s*number/, 'eDHR 提交 API 类型必须要求 workTaskId。')
assert.match(feedbackApi, /interface\s+ProFeedbackEdhrReviewAssigneeOptionVO\s*\{[\s\S]*signatureCellKey:\s*string[\s\S]*candidates:\s*ProFeedbackEdhrReviewCandidateUserVO\[\]/, 'eDHR 执行详情类型必须暴露审核/批准候选选项。')
assert.doesNotMatch(feedbackApi, /interface\s+ProFeedbackEdhrSubmitReqVO\s*\{[\s\S]*reviewAssigneeSelections:/, '普通工序提交 API 不应要求审核/批准选择快照。')
assert.match(feedbackApi, /interface\s+ProFeedbackEdhrFormReviewSignReqVO\s*\{[\s\S]*workTaskId:\s*number/, 'eDHR 表单复核签名 API 类型必须要求 workTaskId。')
assert.match(feedbackApi, /getEdhrExecution:\s*async\s*\(id:\s*number,\s*workTaskId\??:\s*number\)/, 'eDHR 执行详情 API 必须接收可选 workTaskId。')
assert.match(approvalApi, /workTaskId:\s*number/, 'eDHR 审批 API 类型必须要求 workTaskId。')
assert.match(fieldAuditApi, /workTaskId:\s*number/, '字段审计保存 API 类型必须要求 workTaskId。')
assert.match(executionPage, /route\.query\.workTaskId/, '填写页必须从路由读取 workTaskId。')
assert.match(executionPage, /getEdhrExecution\(currentExecutionId,\s*workTaskId\.value\)/, '填写页加载详情必须携带 workTaskId 供后端校验进入权限。')
assert.match(executionPage, /saveEdhrFieldChanges\(\{[\s\S]*workTaskId:\s*workTaskId\.value/, '填写页保存字段审计必须携带 workTaskId。')
assert.match(executionPage, /workTaskId:\s*workTaskId\.value/, '填写页提交必须携带 workTaskId。')
assert.doesNotMatch(executionPage, /v-for="option in reviewAssigneeOptions"/, '普通工序提交弹窗不应渲染审核/批准候选人选择。')
assert.doesNotMatch(executionPage, /v-model="submitForm\.reviewAssigneeSelections\[option\.signatureCellKey\]"/, '普通工序提交弹窗不应绑定审核/批准候选人。')
assert.doesNotMatch(executionPage, /reviewAssigneeSelections:\s*reviewAssigneeSelections\.map/, '普通工序提交 payload 不应携带审核/批准候选人。')
assert.match(executionPage, /cosignEdhrExecution\(\{[\s\S]*workTaskId:\s*workTaskId\.value/, '填写页表单复核签名必须携带 workTaskId。')
assert.match(approvalDetailPage, /route\.query\.workTaskId/, '审批详情页必须从路由读取 workTaskId。')
assert.match(approvalDetailPage, /workTaskId:\s*workTaskId\.value/, '审批详情页审批或驳回必须携带 workTaskId。')
assert.match(batchDetailPage, /route\.query\.workTaskId/, '从任务看板进入批执行明细后，打开填写页必须继续透传 workTaskId。')

console.log('PASS: eDHR work task context static contract')
