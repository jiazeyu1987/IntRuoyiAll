const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted')

const read = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')

const activeOrderPanel = read(
  'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const batchDetailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const historyPage = read('src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue')
const traceDrawer = read('src/views/mes/pro/edhr/form-trace/BatchExecutionTraceDrawer.vue')

assert(
  activeOrderPanel.includes("recordScope?: 'DETAIL_RECORD' | 'FORMAL_BATCH_SOURCE_DETAIL'"),
  '详情批记录必须显式声明独立的记录范围'
)
assert(
  activeOrderPanel.includes('data-active-order-detail-record-scope'),
  '详情批记录必须有稳定的记录范围标识'
)
assert(
  activeOrderPanel.includes('批记录状态'),
  '共享详情面板必须明确标识批记录状态'
)

const formSurfaceStart = batchDetailPage.indexOf('class="edhr-batch-detail__form-surface"')
const sourceDetailStart = batchDetailPage.indexOf('data-edhr-source-detail-record')
assert(formSurfaceStart >= 0 && sourceDetailStart > formSurfaceStart, '正式批记录和详情批记录必须分别渲染')
const formalSurfaceRegion = batchDetailPage.slice(formSurfaceStart, sourceDetailStart)
assert(
  formalSurfaceRegion.includes('data-edhr-formal-batch-record'),
  '批次执行详情的正式批记录区域必须有稳定标识'
)
assert(
  formalSurfaceRegion.includes('<EdhrExecutionReadonlyForm'),
  '正式批记录区域必须渲染正式 eDHR 只读表单'
)
assert(
  !formalSurfaceRegion.includes('ActiveOrderSubmissionDetailPanel'),
  '详情批记录不得嵌入正式批记录区域'
)
assert(
  batchDetailPage.includes(':record-scope="\'FORMAL_BATCH_SOURCE_DETAIL\'"'),
  '批次执行详情必须以来源详情范围加载详情批记录'
)

assert(
  historyPage.includes("path: '/mes/pro/feedback/edhr-batch-execution/active-order-detail'"),
  '历史批记录必须跳转到独立的活跃订单详情页'
)
assert(
  historyPage.includes("from: '/mes/pro/feedback/edhr-batch-history'"),
  '历史批记录跳转必须保留历史列表返回来源'
)

for (const [name, source] of [['批次执行追溯', traceDrawer]]) {
  assert(
    source.includes("import EdhrExecutionReadonlyForm from '@/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue'"),
    `${name}必须保留正式 eDHR 批记录组件`
  )
  assert(source.includes('<EdhrExecutionReadonlyForm'), `${name}必须渲染正式批记录`)
  assert(source.includes('data-edhr-source-detail-record'), `${name}必须单独标识详情批记录`)
  assert(
    source.includes(':record-scope="\'FORMAL_BATCH_SOURCE_DETAIL\'"'),
    `${name}必须把活跃订单数据标识为正式批记录来源详情`
  )
  assert(!source.includes('详情里的正式单据'), `${name}不得把详情批记录称为正式单据`)
}

console.log('formal-batch-record-detail-record-separation static contract passed')
