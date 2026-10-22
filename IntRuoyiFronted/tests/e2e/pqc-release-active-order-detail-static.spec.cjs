const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/production-release/PqcProductionReleasePage.vue'), 'utf8')
const productionLeaderDetail = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue'),
  'utf8'
)
const batchExecutionDetail = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/edhr-batch/BatchExecutionActiveOrderDetailPage.vue'),
  'utf8'
)
const batchVoidedDetail = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/edhr-batch/BatchVoidedPage.vue'),
  'utf8'
)
const nonconformanceDetail = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'),
  'utf8'
)
const marker = source.indexOf('data-pqc-production-release-detail')
assert.ok(marker >= 0, 'PQC release needs a detail action')
assert.ok(marker < source.indexOf('data-pqc-production-release-approve'), 'Detail precedes release')
assert.match(source, /@click="openActiveOrderDetail\(row\)"/)
assert.match(source, /:disabled="!hasActiveOrderDetail\(row\)"/)
const start = source.indexOf('const openActiveOrderDetail =')
const end = source.indexOf('\nconst ', start + 1)
assert.ok(start >= 0 && end > start)
const handler = source.slice(start, end)
assert.match(handler, /getPqcProductionReleaseOrderDetail\(row.applicationId\)/)
assert.doesNotMatch(handler, /router\.push|router\.replace|getTeamLeaderActiveOrderDetail/)
assert.match(source, /data-pqc-production-release-detail-back/)
assert.match(source, /ActiveOrderSubmissionDetailPanel/)

const detailPages = [
  ['生产组长', productionLeaderDetail, 'data-team-leader-active-order-detail-back'],
  ['批次执行/历史追溯', batchExecutionDetail, 'data-edhr-batch-source-detail-back'],
  ['PQC生产放行', source, 'data-pqc-production-release-detail-back'],
  ['批次执行作废', batchVoidedDetail, 'data-edhr-batch-voided-detail-back'],
  ['不合格评审', nonconformanceDetail, 'data-edhr-ncr-active-order-detail-back']
]

for (const [name, page, backMarker] of detailPages) {
  assert.match(page, /ActiveOrderDetailLayout/, `${name} 详情必须使用统一详情布局组件`)
  assert.match(page, new RegExp(`<ActiveOrderDetailLayout\\b[^>]*>\\s*<el-button\\b(?=[^>]*${backMarker})`), `${name} 返回按钮必须直接参与统一Grid布局，避免宽度退化为窄按钮`)
  assert.match(page, new RegExp(`<el-button\\b(?=[^>]*${backMarker})[^>]*>`), `${name} 详情必须保留返回按钮`)
  const backButton = page.match(new RegExp(`<el-button\\b(?=[^>]*${backMarker})[^>]*>`))?.[0]
  assert.ok(backButton, `${name} 详情需要返回按钮`)
  assert.doesNotMatch(backButton, /:icon=|icon=/, `${name} 详情返回按钮必须是无图标默认按钮`)
  assert.doesNotMatch(backButton, /\s(?:type|size|class|style|link|text|plain|round|circle)(?:\s|=|>)/, `${name} 返回按钮不得单独覆盖默认呈现`)
}

assert.match(productionLeaderDetail, /const goBack = \(\) => \{\s*router\.back\(\)/)
assert.match(batchExecutionDetail, /router\.push\(\{ path: resolveReturnPath\(\) \}\)/)
assert.match(source, /data-pqc-production-release-detail-back @click="detailVisible = false"/)
assert.match(batchVoidedDetail, /data-edhr-batch-voided-detail-back @click="detailVisible = false"/)
assert.match(nonconformanceDetail, /data-edhr-ncr-active-order-detail-back\s+@click="activeOrderDetailVisible = false"/)

const sharedLayout = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/processpool/components/ActiveOrderDetailLayout.vue'),
  'utf8'
)
assert.match(sharedLayout, /class="active-order-detail-layout"/)
assert.match(sharedLayout, /display:\s*grid/)
assert.match(sharedLayout, /gap:\s*16px/)
assert.match(sharedLayout, /<slot\s*(?:\/>|><\/slot>)/)

console.log('PASS: active-order detail return buttons share the same default layout and style')
