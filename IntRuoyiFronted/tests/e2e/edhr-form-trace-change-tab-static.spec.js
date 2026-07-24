const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const formTracePage = read('src/views/mes/pro/edhr/FormTracePage.vue')
const changeTabPath = 'src/views/mes/pro/edhr/form-trace/FormTraceChangeTab.vue'
const changeTab = read(changeTabPath)
const batchDetail = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')

assert.doesNotMatch(formTracePage, /<el-tab-pane\s+label="审计"\s+name="audit"/)
assert.doesNotMatch(formTracePage, /FormTraceAuditTab/)
assert.doesNotMatch(formTracePage, /<el-tab-pane\s+label="变更"\s+name="change"/)
assert.match(formTracePage, /<el-tab-pane\s+label="作废"\s+name="change"/)
assert.match(formTracePage, /<el-tab-pane\s+label="驳回"\s+name="reject"/)
assert.match(formTracePage, /<el-tab-pane\s+label="放行"\s+name="release"/)
assert.match(formTracePage, /FormTraceChangeTab/)
assert.match(formTracePage, /type FormTraceTabName = 'change' \| 'reject' \| 'release'/)

assert.match(changeTab, /UnifiedListTemplate/)
assert.match(changeTab, /table-key="mes\.pro\.edhr\.formTrace\.change"/)
assert.match(changeTab, /data-user-table-key="mes\.pro\.edhr\.formTrace\.change"/)
for (const label of ['变更编号', '类型', '状态', '对象', '状态变化', '原因', '申请时间', '生效时间', '操作']) {
  assert.match(changeTab, new RegExp(`label="${label}"`), `变更 Tab 必须保留 ${label} 列。`)
}
assert.match(changeTab, /getEdhrRecordChangePage/)
assert.match(changeTab, /getEdhrRecordChange/)
assert.doesNotMatch(changeTab, /getEdhrTrackingPage/)
assert.doesNotMatch(changeTab, /getEdhrReleasePage/)

assert.match(batchDetail, /path:\s*'\/mes\/pro\/feedback\/edhr-form-trace'/)
assert.match(batchDetail, /tab:\s*'change'/)
assert.doesNotMatch(batchDetail, /tab:\s*'audit'/)
assert.doesNotMatch(
  batchDetail,
  /path:\s*'\/mes\/pro\/feedback\/edhr-change'/,
  '批次详情内的变更入口不得继续指向旧 eDHR 变更记录页面。'
)

console.log('PASS: eDHR form trace change tab static contract')
