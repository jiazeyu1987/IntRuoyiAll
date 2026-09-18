const fs = require('fs')
const path = require('path')
const assert = require('assert')

const workspaceRoot = path.resolve(__dirname, '../../../../../../')
const read = (relativePath) =>
  fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const executionList = read('IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const historyList = read('IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue')
const router = read('IntRuoyiFronted/src/router/modules/remaining.ts')

assert.match(executionList, /data-edhr-batch-execution-source-detail/)
assert.match(executionList, />\s*详情\s*</)
assert.match(executionList, /edhr-batch-execution\/source-detail/)

assert.match(historyList, /data-edhr-history-source-detail/)
assert.match(historyList, />\s*详情\s*</)
assert.match(historyList, /edhr-batch-execution\/source-detail/)

const detailPagePath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionActiveOrderDetailPage.vue'
)
assert.ok(fs.existsSync(detailPagePath), '批次作用域活跃订单详情页面必须存在')
const detailPage = fs.readFileSync(detailPagePath, 'utf8')
assert.match(detailPage, /getEdhrBatchActiveOrderDetail/)
assert.doesNotMatch(detailPage, /getTeamLeaderActiveOrderDetail/)
assert.match(detailPage, /batchExecutionId/)

assert.match(router, /edhr-batch-execution\/source-detail/)
assert.match(router, /BatchExecutionActiveOrderDetailPage\.vue/)
assert.match(router, /mes:pro-edhr-batch-execution:query/)

console.log('batch-execution-source-detail-static: PASS')
