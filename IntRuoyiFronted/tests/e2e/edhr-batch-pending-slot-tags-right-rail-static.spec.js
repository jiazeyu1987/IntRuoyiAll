const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

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

const railTaskStart = detail.indexOf('<div v-if="selectedTaskForEvidence" class="edhr-batch-detail__rail-task-detail">')
const railEnd = detail.indexOf('</aside>', railTaskStart)
assert(railTaskStart >= 0 && railEnd > railTaskStart, '必须能定位右侧待处理详情模板')
const railTaskTemplate = detail.slice(railTaskStart, railEnd)

assert(
  !detail.includes('edhr-batch-detail__slot-status-list') &&
    !detail.includes('slotStatusEntries(task)') &&
    !detail.includes('edhr-batch-detail__slot-blocker'),
  '待处理工序卡片不得展示槽位状态标签或缺失配置提示'
)

assert(
  !railTaskTemplate.includes('edhr-batch-detail__rail-slot-status-list') &&
    !railTaskTemplate.includes('slotStatusEntries(selectedTaskForEvidence)') &&
    !railTaskTemplate.includes('edhr-batch-detail__rail-slot-blocker') &&
    !railTaskTemplate.includes('resolveTaskSlotBlocker(selectedTaskForEvidence)'),
  '右侧当前工序摘要栏不得展示槽位状态标签和缺失配置提示'
)

assert(
  !detail.includes('.edhr-batch-detail__rail-slot-status-list') &&
    !detail.includes('.edhr-batch-detail__rail-slot-blocker') &&
    detail.includes('.edhr-batch-detail__pending-task-item {') &&
    detail.includes('resolveTaskSlotBlocker'),
  '必须移除右侧槽位标签样式，并保留待处理卡片与槽位校验'
)

console.log('edhr batch hidden slot tags right rail static contract passed')
