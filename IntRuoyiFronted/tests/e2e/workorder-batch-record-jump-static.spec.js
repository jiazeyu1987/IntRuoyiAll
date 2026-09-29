const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const workOrderPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/workorder/index.vue')
const batchExecutionPagePath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'
)

const workOrderSource = fs.readFileSync(workOrderPagePath, 'utf8')
const batchExecutionSource = fs.readFileSync(batchExecutionPagePath, 'utf8')

assert(
  workOrderSource.includes('测试添加') &&
    !workOrderSource.includes('创建 ERP 测试单</el-button') &&
    workOrderSource.includes('handleCreateKingdeeProductionOrder(scope.row)'),
  'Work order row ERP test action must be renamed to 测试添加 while preserving the existing create handler.'
)

assert(
  workOrderSource.includes('批记录') &&
    workOrderSource.includes('handleOpenBatchRecord(scope.row)') &&
    workOrderSource.includes("path: '/mes/pro/feedback/edhr-batch-execution'") &&
    workOrderSource.includes('workOrderCode: row.code'),
  'Work order rows must expose a 批记录 action that navigates to eDHR batch execution with the current work order code.'
)

assert(batchExecutionSource.includes('route.query.workOrderCode'), 'Batch list must consume read-only work order filter.')
assert(!batchExecutionSource.includes('openCreateDialog'), 'No manual create dialog may be opened.')

console.log('PASS: work order batch record jump static contract')
