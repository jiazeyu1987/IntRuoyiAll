const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = readUtf8('src/api/mes/pro/processpool/teamLeader.ts')

const reviewDialogStart = page.indexOf('<el-dialog v-model="reviewVisible"')
assert(reviewDialogStart >= 0, '组长复核/分配弹窗必须存在。')
const correctionDialogStart = page.indexOf('data-production-report-correction-dialog', reviewDialogStart)
assert(correctionDialogStart > reviewDialogStart, '必须能定位到分配弹窗结束边界。')
const reviewDialog = page.slice(reviewDialogStart, correctionDialogStart)

assert.match(
  reviewDialog,
  /<el-form\s+v-if="reviewDialogMode === 'REVIEW'"[\s\S]*label="复核说明"[\s\S]*data-team-leader-review-signature/,
  '复核说明和签名内部字段只能在复核模式展示，分配报工模式不得显示。'
)
const allocationBlockStart = reviewDialog.indexOf('class="team-leader-workbench__allocation"')
assert(allocationBlockStart >= 0, '分配报工弹窗必须保留活跃订单分配区域。')
const allocationBlockEnd = reviewDialog.indexOf('<template #footer>', allocationBlockStart)
assert(allocationBlockEnd > allocationBlockStart, '必须能定位分配区域结束边界。')
const allocationBlock = reviewDialog.slice(allocationBlockStart, allocationBlockEnd)

assert.doesNotMatch(
  reviewDialog,
  /reviewDialogMode === 'ALLOCATION'\s*\?\s*'分配说明'/,
  '分配报工弹窗不得展示分配说明输入框。'
)
assert.doesNotMatch(
  allocationBlock,
  /复核签名ID|签名员工ID|签名快照/,
  '分配报工区域不得直出签名 ID、签名员工 ID 或签名快照字段。'
)
assert.doesNotMatch(
  allocationBlock,
  /可先按 FIFO 自动分配，再根据现场情况手动调整。/,
  '分配报工弹窗不得展示红框内 FIFO 辅助提示文案。'
)
assert.match(
  allocationBlock,
  /data-team-leader-fifo-allocation[\s\S]*FIFO 自动分配[\s\S]*新增分配行[\s\S]*data-team-leader-allocation-table/,
  '分配报工弹窗必须保留 FIFO 自动分配、新增分配行和正式分配表。'
)

const submitReviewStart = page.indexOf('const submitReview = async () => {')
assert(submitReviewStart >= 0, '必须保留提交复核/分配函数。')
const submitReviewEnd = page.indexOf('const openCorrection =', submitReviewStart)
assert(submitReviewEnd > submitReviewStart, '必须能定位 submitReview 函数边界。')
const submitReviewBlock = page.slice(submitReviewStart, submitReviewEnd)
const allocationOnlyBranch =
  submitReviewBlock.match(/if\s*\(reviewDialogMode\.value === 'ALLOCATION'\)\s*\{[\s\S]*?confirmTeamLeaderReportAllocation\(\{[\s\S]*?\}\)[\s\S]*?\n\s*\}/)?.[0] || ''
assert(allocationOnlyBranch, '确认分配必须存在分配模式专用提交分支。')
assert.doesNotMatch(
  allocationOnlyBranch,
  /reviewSignatureId|reviewSignatureEmployeeUserId|reviewSignatureSnapshotJson|reviewSignaturePayload/,
  '分配报工确认不得依赖隐藏的签名 ID、签名员工 ID 或签名快照字段。'
)
assert.match(
  submitReviewBlock,
  /if\s*\(reviewDialogMode\.value === 'ALLOCATION'\)[\s\S]*\}\s*else\s*\{[\s\S]*buildReviewSignaturePayload/,
  '复核模式仍必须保留正式签名载荷校验，不能因隐藏分配字段而放宽复核签名。'
)

assert.match(
  api,
  /export interface TeamLeaderReportAllocationConfirmReqVO\s*\{[\s\S]*reviewSignatureId\?:\s*number[\s\S]*reviewSignatureEmployeeUserId\?:\s*number[\s\S]*reviewSignatureSnapshotJson\?:\s*string/,
  '分配确认 API 请求类型必须把复核签名字段声明为可选，避免前端隐藏字段后仍要求用户手填内部字段。'
)
assert.doesNotMatch(
  api.match(/export const confirmTeamLeaderReportAllocation[\s\S]*?\n\}/)?.[0] || '',
  /requireReviewSignaturePayload/,
  '分配确认 API wrapper 不得在前端强制校验隐藏签名字段。'
)

console.log('PASS: team leader allocation dialog hides internal fields')
