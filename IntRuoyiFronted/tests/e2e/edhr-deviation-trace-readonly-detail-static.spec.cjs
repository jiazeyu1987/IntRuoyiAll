const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const trace = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr/components/DeviationTracePane.vue'),
  'utf8'
)
const runner = fs.readFileSync(
  path.join(root, 'tests/e2e/active-order-deviation-real-readonly.e2e.cjs'),
  'utf8'
)
const detail = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-deviation/DeviationDetail.vue'),
  'utf8'
)
const backendRoot = path.resolve(root, '../IntRuoyiBackend')
const responseVo = fs.readFileSync(
  path.join(backendRoot, 'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProEdhrDeviationRespVO.java'),
  'utf8'
)
const responseService = fs.readFileSync(
  path.join(backendRoot, 'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeviationServiceImpl.java'),
  'utf8'
)
const api = fs.readFileSync(path.join(root, 'src/api/mes/pro/edhr/deviation.ts'), 'utf8')
const ncrApi = fs.readFileSync(path.join(root, 'src/api/mes/pro/edhr/nonconformanceReview.ts'), 'utf8')
const inspectBatchSource = runner.match(/async function inspectBatch\(page, record\) \{[\s\S]*?\n\}/)?.[0] || ''

assert.match(trace, /import DeviationDetail from '@\/views\/mes\/pro\/edhr-deviation\/DeviationDetail\.vue'/)
assert.match(trace, /查看发起与处理/)
assert.match(trace, /<el-drawer[\s\S]*?DeviationDetail[\s\S]*?:readonly="true"/)
assert.match(trace, /selectedDeviationId/)
const detailsDeclaration = inspectBatchSource.indexOf('const details = []')
const linkedBatchBranch = inspectBatchSource.indexOf('if (record.deviations.length)')
assert.ok(
  detailsDeclaration >= 0 && detailsDeclaration < linkedBatchBranch,
  'deviation result details must be scoped for both linked and empty batches'
)
assert.match(runner, /expectedInitiationFields/)
assert.match(runner, /expectedHandlingFields/)
assert.match(runner, /CURRENT_VALID/)
assert.match(runner, /活跃订单偏差追溯详情必须只读/)
for (const label of ['关联不合格审批处置', '评审实际意见', 'QA处置签署人', '处置签名记录', '签名记录编号', '处置签署时间']) {
  assert.ok(runner.includes(label), `real UI runner must wait for NCR disposition evidence: ${label}`)
}
assert.match(runner, /QA处置签署人\\s\+用户 #\\d\+/)
assert.match(runner, /签名正文摘要\\s\+\[a-f0-9\]\{64\}/i)
for (const label of [
  '调查开始', '计划完成', '实际完成', '调查成员', '根因分析', '影响范围', '风险评估',
  '产品处置', '整改责任人', '整改期限', 'CAPA', '验证内容'
]) {
  assert.ok(detail.includes(`label="${label}"`), `read-only handling detail must show ${label}`)
}

assert.match(detail, /readonly\?: boolean/)
assert.match(detail, /readonly: false/)
assert.match(detail, /v-if="props\.readonly"[\s\S]*?label="根因分析"[\s\S]*?label="影响范围"[\s\S]*?label="风险评估"[\s\S]*?label="产品处置"[\s\S]*?label="验证内容"/)
assert.match(detail, /handling\.signatureEvidence/)
assert.match(detail, /当前版本电子签名证据/)
assert.match(detail, /verificationStatus/)
assert.match(detail, /handling\.signatureHistory/)
assert.match(detail, /detail\.initiatorSignatureId/)
assert.match(detail, /detail\.initiatorContentHash/)
assert.match(detail, /detail\.value\?\.closeReason === 'TRANSFERRED_TO_NCR'/)
assert.match(detail, /detail\.value\?\.closeReason === 'NORMAL_COMPLETED'/)
assert.match(detail, /getNonconformanceReview/)
assert.match(ncrApi, /export const getNonconformanceReview = async/)
assert.match(detail, /props\.readonly && detail\.closeReason === 'TRANSFERRED_TO_NCR'/)
assert.match(detail, /data-deviation-ncr-disposition-evidence/)
assert.match(detail, /ncrReviewDetail\?\.reviewStatus === 'pending_review'/)
assert.match(detail, /ncrReviewDetail\.reviewOpinion/)
assert.match(detail, /ncrReviewDetail\.disposition/)
assert.match(detail, /ncrReviewDetail\.qaUserId/)
assert.match(detail, /ncrSignatureEvidence\.signatureId/)
assert.match(detail, /ncrSignatureEvidence\.signedAt/)
assert.match(detail, /ncrSignatureEvidence\.aggregateHash/)
assert.match(detail, /traceSnapshotJson/)
assert.match(detail, /ncrReviewError/)
assert.match(detail, /ncrSignatureEvidenceError/)
assert.match(detail, /evidence\.actionType !== 'QA_DISPOSITION'/)
assert.match(detail, /evidence\.disposition !== review\.disposition/)
const ncrEvidenceStart = detail.indexOf('data-deviation-ncr-disposition-evidence')
const ncrEvidenceEnd = detail.indexOf('</section>', ncrEvidenceStart)
assert.ok(ncrEvidenceStart >= 0 && ncrEvidenceEnd > ncrEvidenceStart)
assert.doesNotMatch(
  detail.slice(ncrEvidenceStart, ncrEvidenceEnd),
  /<el-form|<el-input|@click="(?:handleDispose|submitNcrCreate|saveHandling|closeHandling)/,
  'embedded NCR disposition evidence must remain read-only'
)
assert.match(detail, /!props\.readonly/)
assert.match(detail, /detail\.status === 'OPEN' && detail\.level === 'CRITICAL' && !props\.readonly/)
assert.match(detail, /<el-button v-if="!props\.readonly"[^>]*>开始处理<\/el-button>/)
assert.match(detail, /v-if="detail\.status === 'OPEN' && !props\.readonly" class="handling-card__actions"/)
assert.match(detail, /<el-form v-if="detail\.status === 'OPEN' && !props\.readonly"/)
for (const handler of ['submitNcrCreate', 'saveHandling', 'openSignDialog', 'startHandling', 'signHandling', 'closeHandling']) {
  const start = detail.indexOf(`const ${handler} =`)
  const next = detail.indexOf('\nconst ', start + 1)
  assert.ok(start >= 0 && detail.slice(start, next < 0 ? undefined : next).includes('props.readonly'), `${handler} must not run in readonly mode`)
}

for (const field of [
  'discoveryDepartmentName', 'discovererName', 'discoveredAt', 'discoveryLocation',
  'productName', 'productSpecification', 'equipmentOrSystem', 'reportedAt',
  'receiverName', 'categoryCodesJson', 'emergencyAction', 'levelBasis'
]) {
  assert.match(responseVo, new RegExp(`private \\w+ ${field};`), `backend response must expose ${field}`)
  assert.match(api, new RegExp(`${field}\\?:`), `frontend response type must expose ${field}`)
  assert.match(responseService, new RegExp(`set${field[0].toUpperCase()}${field.slice(1)}\\(deviation\\.get${field[0].toUpperCase()}${field.slice(1)}\\(\\)\\)`), `detail response must map ${field}`)
}
for (const label of ['发现部门', '发现人', '发现时间', '发现地点', '产品名称', '产品规格', '设备或系统', '报告时间', '接收人', '偏差类别', '紧急措施', '等级依据']) {
  assert.ok(detail.includes(`label="${label}"`), `read-only initiation detail must show ${label}`)
}
assert.match(detail, /formatTime\(detail\.discoveredAt\)/)
assert.match(detail, /formatTime\(detail\.reportedAt\)/)
assert.match(detail, /formatTime\(detail\.initiatedAt\)/)
assert.match(detail, /dayjs\(value\).*format\('YYYY-MM-DD HH:mm:ss'\)/s)

console.log('PASS edhr-deviation-trace-readonly-detail-static')
