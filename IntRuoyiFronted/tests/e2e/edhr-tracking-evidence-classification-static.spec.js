const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')

const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const trackingPage = read('src/views/mes/pro/edhr/TrackingPage.vue')
const trackingApi = read('src/api/mes/pro/edhr/tracking.ts')

const expandEvidencePanel = extractBetween(
  trackingPage,
  '<div class="edhr-tracking__evidence">',
  '</el-table-column>'
)
const eventLabels = extractBetween(
  trackingPage,
  'const TRACKING_EVENT_LABELS',
  'const ARCHIVE_STATUS_LABELS'
)

for (const token of [
  '普通工序填写签名证据',
  '填写提交签名',
  '放行阶段审核/批准证据',
  '普通工序不要求审核/批准',
  '历史工序审核/批准证据（只读）'
]) {
  assert.ok(expandEvidencePanel.includes(token), `tracking evidence panel must expose token: ${token}`)
}

assert.ok(
  eventLabels.includes("SUBMIT: '填写提交签名'"),
  'tracking last event SUBMIT must be labeled as fill signature, not approval submit'
)
assert.ok(
  !eventLabels.includes("SUBMIT: '提交审批'"),
  'tracking last event SUBMIT must not imply process approval'
)

for (const token of ['evidenceCategory', 'evidenceCategoryName']) {
  assert.ok(trackingApi.includes(token), `tracking API type must expose event evidence field: ${token}`)
}

console.log('PASS: eDHR tracking evidence classification static contract')
