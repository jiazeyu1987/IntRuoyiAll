const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const trackingPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/TrackingPage.vue')
const source = fs.readFileSync(trackingPagePath, 'utf8')

const contentStart = source.indexOf('<ContentWrap>')
const alertStart = source.indexOf('<el-alert v-if="loadError"', contentStart)
assert.ok(contentStart >= 0, '追踪页必须保留 ContentWrap 页面容器。')
assert.ok(alertStart > contentStart, '追踪页必须保留错误提示，并位于表格前。')

const preAlertSource = source.slice(contentStart, alertStart)

assert.ok(!preAlertSource.includes('<el-form'), '截图黄框内顶部筛选表单必须从追踪页删除。')

for (const removedLabel of [
  '执行编号',
  '工单号',
  '批次号',
  '当前状态',
  '处理人',
  '处理时间',
  '查询',
  '重置',
  '高级筛选',
  '工序编号',
  '工作站编号',
  '提交人编号',
  '审批人编号',
  '流程实例编号'
]) {
  assert.ok(!preAlertSource.includes(removedLabel), `截图黄框内容必须从追踪页顶部区域删除：${removedLabel}`)
}

assert(
  !source.includes('<el-form-item label="工序ID"') &&
    !source.includes('<el-form-item label="工作站ID"') &&
    !source.includes('<el-form-item label="提交人ID"') &&
    !source.includes('<el-form-item label="审批人ID"') &&
    !source.includes('<el-form-item label="流程实例">'),
  'Tracking page must not keep raw ID/process-instance filters in the primary toolbar.'
)

assert(
  source.includes('label="生产上下文"') &&
    source.includes('label="当前阶段"') &&
    source.includes('label="最后处理"') &&
    !source.includes('<el-table-column label="工单号"') &&
    !source.includes('<el-table-column label="批次号"') &&
    !source.includes('<el-table-column label="当前节点"') &&
    !source.includes('<el-table-column label="当前处理人"') &&
    !source.includes('<el-table-column label="最后事件"') &&
    !source.includes('<el-table-column label="意见/原因"') &&
    !source.includes('<el-table-column label="最后处理时间"'),
  'Tracking page must group production context, current stage, and latest handling into compact business columns.'
)

assert(
  source.includes('type="expand"') &&
    source.includes('追踪证据') &&
    source.includes('row.processInstanceId') &&
    source.includes('row.workOrderId') &&
    source.includes('row.batchId'),
  'Tracking page must keep technical identifiers available in an expandable evidence section.'
)

assert(
  source.includes('formatArchiveStatusLabel(row.archiveStatus)') &&
    source.includes('empty-text="暂无追踪记录"') &&
    source.includes('edhr-query__table') &&
    !source.includes('edhr-query__advanced') &&
    !source.includes('advancedFilterNames') &&
    !source.includes('<el-collapse-item title="高级筛选" name="advanced">'),
  'Tracking page must format archive status, show an explicit empty state, and remove yellow-box advanced filters.'
)

for (const queryKey of [
  "executionCode: typeof route.query.executionCode === 'string' ? route.query.executionCode : ''",
  "workOrderCode: typeof route.query.workOrderCode === 'string' ? route.query.workOrderCode : ''",
  "batchCode: typeof route.query.batchCode === 'string' ? route.query.batchCode : ''",
  'actorName:'
]) {
  assert.ok(source.includes(queryKey), `移除可见筛选控件不得删除追踪页后端查询参数支持：${queryKey}`)
}

assert(
  source.includes('getEdhrTrackingPage({') &&
    source.includes('Pagination') &&
    source.includes('@click="openTrackingDetail(row)"'),
  'Tracking page must keep list loading, pagination, and execution detail entry.'
)

console.log('PASS: EDHR tracking page UI static contract')
