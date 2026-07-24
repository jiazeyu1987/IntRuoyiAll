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

const pendingListStart = detail.indexOf(
  '<div v-if="pendingTaskEntries.length" class="edhr-batch-detail__pending-task-list"'
)
const executionListStart = detail.indexOf('v-for="execution in executionReviews"', pendingListStart)
assert.notEqual(pendingListStart, -1, '批次详情页必须保留左侧待处理工序列表。')
assert.notEqual(executionListStart, -1, '批次详情页必须保留已填写工序列表，作为左侧待处理列表结束锚点。')

const pendingListBlock = detail.slice(pendingListStart, executionListStart)
for (const forbiddenToken of [
  'resolvePendingTaskDescription(task)',
  'resolvePendingTaskFillableUsersText(task)',
  'edhr-batch-detail__fill-carrier-control-wrap',
  'resolveTaskStatusLabel(task)',
  'resolvePendingTaskRoleLabel(task)',
  'resolvePendingTaskActionLabel(task)',
  'handlePendingTaskAction(task)'
]) {
  assert.ok(
    !pendingListBlock.includes(forbiddenToken),
    `左侧待处理工序卡片不得继续承载详情或动作：${forbiddenToken}`
  )
}

for (const requiredToken of [
  'resolvePendingTaskSortText(task)',
  'resolvePendingTaskTitle(task)',
  '@click="selectProcessTask(task)"',
  '@keydown.enter.prevent="selectProcessTask(task)"',
  '@keydown.space.prevent="selectProcessTask(task)"'
]) {
  assert.ok(
    pendingListBlock.includes(requiredToken),
    `左侧待处理工序列表必须保留选择能力和可扫描主信息：${requiredToken}`
  )
}

const railStart = detail.indexOf('<aside class="edhr-batch-detail__review-rail" aria-label="当前工序摘要">')
const railEnd = detail.indexOf('</aside>', railStart)
assert.notEqual(railStart, -1, '批次详情页必须保留右侧当前工序摘要栏。')
assert.notEqual(railEnd, -1, '批次详情页右侧当前工序摘要栏必须正确闭合。')

const railBlock = detail.slice(railStart, railEnd)
for (const requiredToken of [
  'edhr-batch-detail__rail-task-detail',
  'selectedTaskForEvidence',
  'resolvePendingTaskDescription(selectedTaskForEvidence)',
  'resolvePendingTaskFillableUsersText(selectedTaskForEvidence)',
  'openPendingTaskByFillCarrier(selectedTaskForEvidence',
  'resolveTaskStatusLabel(selectedTaskForEvidence)',
  'resolvePendingTaskRoleLabel(selectedTaskForEvidence)',
  'resolvePendingTaskActionLabel(selectedTaskForEvidence)',
  'handlePendingTaskAction(selectedTaskForEvidence)'
]) {
  assert.ok(
    railBlock.includes(requiredToken),
    `右侧当前工序摘要栏必须承载待处理工序详情和操作：${requiredToken}`
  )
}

console.log('PASS: EDHR pending task detail is relocated to the right rail.')
