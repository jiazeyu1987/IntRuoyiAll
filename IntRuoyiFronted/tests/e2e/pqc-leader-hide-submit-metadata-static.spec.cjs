const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const page = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

const detailMarker = 'data-pqc-leader-detail-tab'
const detailMarkerStart = page.indexOf(detailMarker)
assert.ok(detailMarkerStart >= 0, 'PQC detail tab must keep a stable marker.')

const detailTabStart = page.lastIndexOf('<ContentWrap', detailMarkerStart)
const detailTabEnd = page.indexOf('</ContentWrap>', detailMarkerStart)
assert.ok(
  detailTabStart >= 0 && detailTabEnd > detailTabStart,
  'PQC detail tab content block must be locatable.'
)

const detailTabBlock = page.slice(detailTabStart, detailTabEnd)

assert.doesNotMatch(
  detailTabBlock,
  /<el-descriptions-item\s+label="提交摘要">|detail\.submittedSummary/,
  'PQC detail tab must not render the submission summary row.'
)
assert.doesNotMatch(
  detailTabBlock,
  /data-pqc-submission-log|PQC提交日志|data-pqc-submission-signature-id/,
  'PQC detail tab must not render the PQC submission log section.'
)
assert.match(
  detailTabBlock,
  /PQC项目明细[\s\S]*data-pqc-leader-item-snapshot-table/,
  'PQC detail tab must keep the formal PQC item detail table.'
)

console.log('PASS: PQC detail tab hides submission metadata and keeps item details')
