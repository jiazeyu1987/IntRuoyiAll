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

const processGroupStart = detail.indexOf('v-for="processGroup in processTaskGroups"')
const processGroupEnd = detail.indexOf(
  '<template v-for="task in postProcessSpecialTaskEntries"',
  processGroupStart
)
assert.notEqual(processGroupStart, -1, '普通工序组必须保留在左侧工序列表。')
assert.notEqual(processGroupEnd, -1, '普通工序组必须位于收尾特殊节点之前。')

const processGroupTemplate = detail.slice(processGroupStart, processGroupEnd)
assert(
  processGroupTemplate.includes('resolveProcessGroupStateClass(processGroup)'),
  '普通工序卡片必须绑定统一的工序组状态类。'
)
assert(
  !processGroupTemplate.includes('resolveProcessGroupStatusText(processGroup)') &&
    !processGroupTemplate.includes('<el-tag'),
  '普通工序卡片不得继续显示“0/1 已完成”等状态标签。'
)

assert(
  detail.includes("if (!requiredTasks.length || requiredTasks.every(isCompletedProcessTask)) return 'is-completed'"),
  '全部必填任务完成或无需填写时必须返回已完成状态类。'
)
assert(
  detail.includes("if (isCurrentExecutableProcessGroup(group) || isCurrentProcessGroup(group)) return 'is-in-progress'") &&
    detail.includes("task.status != null && task.status !== EDHR_BATCH_TASK_STATUS_WAITING") &&
    detail.includes("return hasStartedTask ? 'is-in-progress' : 'is-not-started'"),
  '当前工序或已开始但未完成的工序必须返回填写中状态类，非当前且全部待打开时保持未开始状态类。'
)

const readStyleBlock = (selector) => {
  const start = detail.indexOf(`${selector} {`)
  assert.notEqual(start, -1, `必须存在样式块：${selector}`)
  const end = detail.indexOf('}', start)
  assert.notEqual(end, -1, `样式块必须正确闭合：${selector}`)
  return detail.slice(start, end)
}

const completedStyle = readStyleBlock('.edhr-batch-detail__process-task-group.is-completed')
assert(
  completedStyle.includes('--edhr-process-state-background: #f0f9eb'),
  '已完成工序必须使用淡绿色背景。'
)

const inProgressStyle = readStyleBlock('.edhr-batch-detail__process-task-group.is-in-progress')
assert(
  inProgressStyle.includes('--edhr-process-state-background: #fff8e6'),
  '正在填写工序必须使用淡黄色背景。'
)

const groupStyle = readStyleBlock('.edhr-batch-detail__process-task-group')
assert(
  groupStyle.includes('--edhr-process-state-background: #f7f9fc'),
  '未开始工序必须保持当前浅灰白背景。'
)

const headStyle = readStyleBlock('.edhr-batch-detail__process-task-group-head')
assert(
  headStyle.includes('background: var(--edhr-process-state-background)'),
  '工序卡片按钮背景必须由工序组状态变量控制。'
)

console.log('PASS: eDHR batch process state background static contract')
