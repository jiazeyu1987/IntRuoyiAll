const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const scheduleOrderPath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const source = fs.readFileSync(scheduleOrderPath, 'utf8')

const dialogStart = source.indexOf('v-model="operationLogDialogVisible"')
assert(dialogStart >= 0, '排产工单追溯弹框必须存在。')
const dialogOpenStart = source.lastIndexOf('<Dialog', dialogStart)
const dialogEnd = source.indexOf('</Dialog>', dialogStart)
assert(dialogOpenStart >= 0 && dialogEnd > dialogOpenStart, '排产工单追溯弹框必须存在完整 Dialog。')

const dialogSource = source.slice(dialogOpenStart, dialogEnd + '</Dialog>'.length)

assert(
  dialogSource.includes('width="min(1120px, calc(100vw - 24px))"'),
  '排产工单追溯弹框必须使用更宽的响应式宽度，避免日志和字段差异拥挤。'
)

assert(
  dialogSource.includes('class="schedule-order-pool__trace-summary"'),
  '排产工单追溯弹框顶部必须展示摘要区，先呈现排产编码、日志数量和最近操作。'
)

assert(
  dialogSource.includes('operationLogSummary.scheduleOrderCode') &&
    dialogSource.includes('operationLogSummary.totalCount') &&
    dialogSource.includes('operationLogSummary.latestOperationType') &&
    dialogSource.includes('operationLogSummary.latestTime'),
  '排产工单追溯摘要必须包含排产编码、日志数量、最近操作和最近时间。'
)

assert(
  dialogSource.includes('class="schedule-order-pool__trace-timeline"') &&
    dialogSource.includes('class="schedule-order-pool__trace-card"'),
  '排产工单追溯日志必须以时间线卡片展示，不能只依赖主表格阅读。'
)

assert(
  dialogSource.includes('class="schedule-order-pool__trace-card-reason"'),
  '每条追溯记录必须在卡片内直接展示操作原因。'
)

assert(
  dialogSource.includes('class="schedule-order-pool__trace-empty"') &&
    dialogSource.includes('<el-empty'),
  '追溯弹框必须为空日志提供明确空状态。'
)

assert(
  dialogSource.includes('class="schedule-order-pool__trace-value"'),
  '字段差异的新旧值必须使用可换行值块展示，避免长文本只靠 tooltip。'
)

assert(
  source.includes('const operationLogSummary = computed') &&
    source.includes('latestOperationType') &&
    source.includes('latestTime'),
  '追溯弹框必须提供摘要 computed，集中维护顶部上下文。'
)

assert(
  source.includes('.schedule-order-pool__trace-summary') &&
    source.includes('.schedule-order-pool__trace-timeline') &&
    source.includes('.schedule-order-pool__trace-value') &&
    source.includes('overflow-wrap: anywhere'),
  '追溯弹框必须补充摘要、时间线和长值换行样式。'
)

console.log('PASS: MES schedule order trace dialog readability static contract')
