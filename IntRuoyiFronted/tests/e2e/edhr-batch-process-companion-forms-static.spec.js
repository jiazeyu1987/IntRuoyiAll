const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const executionPath = path.join(repoRoot, 'src/views/mes/pro/edhr/ExecutionPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')
const execution = fs.readFileSync(executionPath, 'utf8')

assert(
  detail.includes('processTaskGroups') &&
    detail.includes('v-for="processGroup in processTaskGroups"'),
  '批次详情左侧必须按工序组渲染导航，而不是逐条重复渲染同工序表单。'
)

for (const requiredMarker of [
  'class="edhr-batch-detail__process-task-group"',
  'const selectedProcessTasks = computed',
  'class="edhr-batch-detail__rail-process-forms"',
  'v-for="task in selectedProcessTasks"',
  'selectProcessTask(task)',
  'handleSelectedPendingTaskAction(task)'
]) {
  assert(detail.includes(requiredMarker), `右侧工序详情必须保留可点击的表单任务项：${requiredMarker}`)
}

const processNavStart = detail.indexOf(
  '<nav class="edhr-batch-detail__process-panel edhr-batch-detail__process-list edhr-batch-detail__review-list"'
)
const formPanelStart = detail.indexOf(
  '<div\n            class="edhr-batch-detail__form-panel edhr-batch-detail__review-preview"',
  processNavStart
)
const processNav = detail.slice(processNavStart, formPanelStart)
assert(
  !processNav.includes('v-for="task in processGroup.tasks"'),
  '左侧工序导航不得继续展开同工序表单。'
)

for (const [slot, label] of [
  ['MAIN', '主生产表'],
  ['LOSS_REPORT', '损耗单'],
  ['PROCESS_INSPECTION', '过程检验单'],
  ['PARAMETER_RECORD', '参数记录表']
]) {
  assert(detail.includes(`${slot}: '${label}'`), `${slot} 必须显示业务可识别的表单槽位名称：${label}`)
}

assert(
  detail.includes('resolveProcessGroupStateClass') &&
    !processNav.includes('resolveProcessGroupStatusText(processGroup)') &&
    detail.includes('resolveTaskStatusLabel(task)') &&
    detail.includes('resolveTaskSlotBlocker(task) || task.disabledReason || task.gateMessage'),
  '左侧工序必须用背景表达整体状态且不显示完成计数，右侧继续显示表单自身状态和门禁原因。'
)

assert(
  execution.includes("route.query.returnPath === '/mes/pro/feedback/edhr-batch-execution/detail'") &&
    execution.includes("path: '/mes/pro/feedback/edhr-batch-execution/detail'") &&
    execution.includes('batchTaskId: typeof route.query.batchTaskId === \'string\' ? route.query.batchTaskId : undefined'),
  '执行页返回列表时必须识别批次详情来源，并保留 batchExecutionId / batchTaskId 上下文。'
)

console.log('PASS: eDHR batch detail shows companion forms in the selected process right panel and preserves return context.')
