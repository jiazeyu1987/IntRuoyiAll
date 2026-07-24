const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)

const detail = fs.readFileSync(detailPath, 'utf8')

const reviewHeaderStart = detail.indexOf('<div class="edhr-batch-detail__process-header">')
const pendingListStart = detail.indexOf(
  '<div v-if="pendingTaskEntries.length" class="edhr-batch-detail__pending-task-list"',
  reviewHeaderStart
)
assert.notEqual(reviewHeaderStart, -1, '工序列表必须保留顶部上下文容器。')
assert.notEqual(pendingListStart, -1, '工序列表必须保留待处理工序列表。')

const processHeaderBlock = detail.slice(reviewHeaderStart, pendingListStart)
for (const requiredToken of [
  'edhr-batch-detail__process-context',
  'detail?.workOrderCode',
  'resolveCurrentBatchRecordNo()',
  '<span :title="detail?.workOrderCode || \'\'">{{ detail?.workOrderCode || \'--\' }}</span>',
  '<span :title="resolveCurrentBatchRecordNo()">{{ resolveCurrentBatchRecordNo() }}</span>'
]) {
  assert.ok(
    processHeaderBlock.includes(requiredToken),
    `工序列表顶部绿色位置必须展示当前批记录上下文：${requiredToken}`
  )
}

for (const forbiddenToken of ['生产工单：', '批记录号：']) {
  assert.ok(!processHeaderBlock.includes(forbiddenToken), `工序列表顶部上下文不得显示标签文案：${forbiddenToken}`)
}

assert.ok(
  !processHeaderBlock.includes('<div class="edhr-batch-detail__review-subtitle">工序</div>'),
  '工序列表顶部不得继续显示“工序”标题。'
)

const pendingTitleLine = '<div class="edhr-batch-detail__pending-task-title">待处理工序</div>'
assert.ok(
  !detail.includes(pendingTitleLine),
  '左侧红框位置不得继续显示“待处理工序”标题。'
)

for (const requiredToken of [
  'const resolveCurrentBatchRecordNo = () =>',
  'selectedExecution.value?.batchRecordReportCode',
  'selectedTaskForEvidence.value?.batchRecordReportCode',
  'selectedExecution.value?.batchRecordReportName',
  'selectedTaskForEvidence.value?.batchRecordReportName',
  'selectedExecution.value?.batchRecordReportId',
  'selectedTaskForEvidence.value?.batchRecordReportId'
]) {
  assert.ok(
    detail.includes(requiredToken),
    `批记录号必须优先使用当前已填写或待处理工序对应字段：${requiredToken}`
  )
}

console.log('PASS: EDHR process header shows work order and batch record context.')
