const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const typePath = path.join(repoRoot, 'src/api/mes/pro/edhr/batchExecution.ts')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const typeSource = fs.readFileSync(typePath, 'utf8')
const railStart = pageSource.indexOf('<aside class="edhr-batch-detail__review-rail"')
const railEnd = pageSource.indexOf('</aside>', railStart)
assert(railStart >= 0 && railEnd > railStart, '页面必须保留右侧一级操作栏')
const railSource = pageSource.slice(railStart, railEnd)
const topPreviewSource = pageSource.slice(0, railStart)

assert(!pageSource.includes('primaryFormFillMetaItems'), 'Page must not aggregate right red-box fill metadata')
assert(!railSource.includes('primaryFormFillMetaItems'), 'Right rail must not keep red-box fill metadata')
assert(
  !topPreviewSource.includes('class="edhr-batch-detail__primary-fill-meta"'),
  'Top preview must not render fill metadata'
)
assert(pageSource.includes("['FIELD_CHANGE', 'SUBMIT']"), '填写必须聚合 FIELD_CHANGE + SUBMIT')
assert(
  pageSource.includes('signatureDisplayAt || record.selectedSignedAt || record.signedAt'),
  '签核时间必须优先使用 signatureDisplayAt、selectedSignedAt，再使用 signedAt'
)
assert(
  !pageSource.includes('resolvePrimaryFormFillersText') && !pageSource.includes('resolvePrimaryFormSubmitTimesText'),
  'Independent filler and submitted-at metadata calculators must not remain'
)
assert(
  !pageSource.includes('<el-popover'),
  '填写人和提交时间不得放在弹出层里'
)
assert(
  !pageSource.includes('<div class="edhr-batch-detail__rail-label">签核摘要</div>'),
  '右侧轨道不得继续展示签核摘要'
)

for (const field of [
  'selectedSignedAt',
  'signatureDisplayAt',
  'signatureTimeMode',
  'selectedTimeZone',
  'selectedTimeReason',
  'selectedTimePolicyVersion',
  'selectedTimeAuditHash'
]) {
  assert(typeSource.includes(field), `签名记录类型必须补齐 ${field}`)
}

console.log('PASS mes-edhr-batch-review-signoff-summary-static')
