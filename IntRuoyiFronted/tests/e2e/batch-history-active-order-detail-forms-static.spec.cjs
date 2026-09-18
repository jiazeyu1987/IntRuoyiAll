const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted')

const read = (file) => fs.readFileSync(file, 'utf8')

const historyPage = read(
  path.join(frontendRoot, 'src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue')
)
const traceDrawer = read(
  path.join(frontendRoot, 'src/views/mes/pro/edhr/form-trace/BatchExecutionTraceDrawer.vue')
)

const assertUsesSeparatedRecordViews = (source, name) => {
  assert(
    source.includes('ActiveOrderSubmissionDetailPanel'),
    `${name} 必须展示活跃订单详情批记录`
  )
  assert(
    source.includes('getEdhrBatchActiveOrderDetail'),
    `${name} 必须按批次执行编号加载详情批记录来源`
  )
  assert(
    source.includes('缺少正式活跃订单来源'),
    `${name} 缺少 activeOrderId 时必须明确失败，不能按工单号或批次号推断`
  )
  assert(
    source.includes(
      "import EdhrExecutionReadonlyForm from '@/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue'"
    ),
    `${name} 必须保留正式 eDHR 批记录表单组件`
  )
  assert(
    /<EdhrExecutionReadonlyForm\b/.test(source),
    `${name} 必须渲染正式 eDHR 批记录表单组件`
  )
  assert(
    source.includes('data-edhr-formal-batch-record') &&
      source.includes('data-edhr-source-detail-record'),
    `${name} 必须把正式批记录和详情批记录分别标识`
  )
  assert(
    source.includes(':record-scope="\'FORMAL_BATCH_SOURCE_DETAIL\'"'),
    `${name} 必须把活跃订单详情标识为正式批记录来源详情`
  )
  assert(
    !source.includes("return 'full'"),
    `${name} 不得在正式表单槽位缺失时降级为全量详情`
  )
}

assertUsesSeparatedRecordViews(historyPage, '历史追溯列表')
assertUsesSeparatedRecordViews(traceDrawer, '批记录追溯抽屉')

assert(
  historyPage.includes(':display-mode="selectedSubmissionFormMode"') &&
    historyPage.includes(':production-route-process-id="selectedExecution.routeProcessId"'),
  '历史追溯列表选择工序后必须按生产/PQC模式展示详情单据'
)
assert(
  traceDrawer.includes(':display-mode="selectedRecordSubmissionFormMode"') &&
    traceDrawer.includes(':production-route-process-id="selectedRecordExecution.routeProcessId"'),
  '批记录追溯抽屉选择工序后必须按生产/PQC模式展示详情单据'
)

console.log('batch-history-active-order-detail-forms static contract passed')
