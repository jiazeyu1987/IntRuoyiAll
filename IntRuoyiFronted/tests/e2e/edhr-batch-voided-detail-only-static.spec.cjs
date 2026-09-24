const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (file) => fs.readFileSync(path.join(root, 'src', file), 'utf8')

const forbiddenActions = /(?:驳回|作废|上市放行|上传|撤回作废申请)/

const batchExecutionPage = read('views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const voidedStateStart = batchExecutionPage.indexOf(
  'v-else-if="resolveBatchVoidOperationState(row) === \'voided\'"'
)
assert.ok(voidedStateStart >= 0, '批次执行页必须存在已作废状态操作分支')
const voidedStateEnd = batchExecutionPage.indexOf('v-else-if=', voidedStateStart + 1)
assert.ok(voidedStateEnd > voidedStateStart, '无法定位已作废状态操作分支边界')
const voidedStateOperation = batchExecutionPage.slice(voidedStateStart, voidedStateEnd)
assert.equal(
  (voidedStateOperation.match(/<el-button\b/g) || []).length,
  1,
  '批次执行页已作废状态行只能渲染一个按钮'
)
assert.match(voidedStateOperation, />\s*详情\s*</, '批次执行页已作废状态行必须保留详情按钮')
assert.doesNotMatch(
  voidedStateOperation,
  forbiddenActions,
  '批次执行页已作废状态行不得渲染其它业务操作'
)

const voidedPage = read('views/mes/pro/edhr-batch/BatchVoidedPage.vue')
assert.match(voidedPage, /getPqcProductionReleasePage/)
assert.match(voidedPage, /PQC_RELEASE_VIEW_VOIDED/)
assert.match(voidedPage, /getPqcProductionReleaseOrderDetail/)
assert.match(voidedPage, /applicationId/)
assert.doesNotMatch(voidedPage, /getEdhrBatchExecutionPage/)
assert.doesNotMatch(voidedPage, /getTeamLeaderVoidedActiveOrderPage/)
assert.doesNotMatch(voidedPage, /readBlocked/)
assert.doesNotMatch(voidedPage, /数据异常/)
const operationStart = voidedPage.indexOf('<el-table-column label="操作"')
assert.ok(operationStart >= 0, '作废专用页必须存在操作列')
const operationEnd = voidedPage.indexOf('</el-table-column>', operationStart)
assert.ok(operationEnd > operationStart, '无法定位作废专用页操作列边界')
const operationColumn = voidedPage.slice(operationStart, operationEnd)
assert.equal(
  (operationColumn.match(/<el-button\b/g) || []).length,
  1,
  '作废专用页每行只能渲染一个按钮'
)
assert.match(operationColumn, />\s*详情\s*</, '作废专用页必须保留详情按钮')
assert.doesNotMatch(operationColumn, forbiddenActions, '作废专用页不得渲染其它业务操作')
assert.doesNotMatch(operationColumn, /:disabled=/, 'PQC放行申请详情不应被历史活跃订单快照阻断')

console.log('edhr-batch-voided-detail-only-static: PASS')
