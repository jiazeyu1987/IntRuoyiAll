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
    workOrderSource.includes('prefillWorkOrderCode: row.code'),
  'Work order rows must expose a 批记录 action that navigates to eDHR batch execution with the current work order code.'
)

assert(
  batchExecutionSource.includes('const route = useRoute()') &&
    batchExecutionSource.includes('getPrefillWorkOrderCodeFromRoute') &&
    batchExecutionSource.includes('prefillWorkOrderForCreateDialog') &&
    batchExecutionSource.includes('route.query.prefillWorkOrderCode') &&
    batchExecutionSource.includes('createForm.workOrderId = matchedWorkOrder.id') &&
    batchExecutionSource.includes('createForm.batchCode = matchedWorkOrder.batchCode || createForm.batchCode'),
  'eDHR batch execution page must consume route query when opening the create dialog and prefill the matching work order.'
)

assert(
  /onMounted\(\s*\(\)\s*=>\s*\{[\s\S]*getList\(\)[\s\S]*if\s*\(\s*getPrefillWorkOrderCodeFromRoute\(\)\s*\)\s*\{[\s\S]*openCreateDialog\(\)/.test(
    batchExecutionSource
  ),
  'eDHR batch execution page must automatically open the create dialog when routed with prefillWorkOrderCode.'
)

console.log('PASS: work order batch record jump static contract')
