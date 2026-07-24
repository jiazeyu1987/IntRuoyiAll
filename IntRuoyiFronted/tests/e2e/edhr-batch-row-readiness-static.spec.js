const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const detailPath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')
const detailSource = fs.readFileSync(detailPath, 'utf8')

assert(
  source.includes('openDetail(row)') &&
    !source.includes('@click="openReadinessDialog(row)"') &&
    !source.includes('openReadinessDialog(selectedTraceBatch)'),
  'Batch execution row readiness preflight must move out of list actions and into the fill/detail flow.'
)

assert(
  detailSource.includes('预检结果') &&
    detailSource.includes('handleReleasePrecheck') &&
    detailSource.includes('openReleaseCheckGroup'),
  'Detail fill flow must provide release precheck entry.'
)

assert(
  detailSource.includes('UX_CHECKLIST_ITEMS') && detailSource.includes('体验检查'),
  'Detail fill flow must provide experience checklist entry.'
)

console.log('PASS: eDHR batch row readiness static contract')
