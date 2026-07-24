const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')

const processNavStart = detail.indexOf(
  '<nav class="edhr-batch-detail__process-panel edhr-batch-detail__process-list edhr-batch-detail__review-list"'
)
const formPanelStart = detail.indexOf(
  '<div\n            class="edhr-batch-detail__form-panel edhr-batch-detail__review-preview"',
  processNavStart
)

assert(processNavStart >= 0 && formPanelStart > processNavStart, '必须能识别左侧工序导航区域。')

const processNav = detail.slice(processNavStart, formPanelStart)

assert(
  processNav.includes('v-for="processGroup in processTaskGroups"') &&
    processNav.includes('@click="selectProcessTask(processGroup.primaryTask)"'),
  '左侧必须继续按工序分组导航。'
)

assert(
  !processNav.includes('edhr-batch-detail__process-task-form-list') &&
    !processNav.includes('v-for="task in processGroup.tasks"'),
  '左侧工序导航不得展开主生产表或辅助表单任务。'
)

for (const requiredMarker of [
  'const selectedProcessTasks = computed',
  'class="edhr-batch-detail__rail-process-forms"',
  'aria-label="当前工序表单列表"',
  'v-if="selectedProcessTasks.length"',
  'v-for="task in selectedProcessTasks"',
  '@click="selectProcessTask(task)"',
  '@click.stop="handlePendingTaskAction(task)"',
  'description="当前工序未配置表单"'
]) {
  assert(detail.includes(requiredMarker), `右侧当前工序表单列表缺少：${requiredMarker}`)
}

for (const [slot, label] of [
  ['MAIN', '主生产表'],
  ['LOSS_REPORT', '损耗单'],
  ['PROCESS_INSPECTION', '过程检验单'],
  ['PARAMETER_RECORD', '参数记录表']
]) {
  assert(detail.includes(`${slot}: '${label}'`), `${slot} 必须显示为 ${label}。`)
}

assert(
  detail.includes('resolveTaskStatusLabel(task)') &&
    detail.includes('resolveTaskSlotBlocker(task) || task.disabledReason || task.gateMessage'),
  '右侧每张表单必须显示自己的状态和门禁原因。'
)

console.log('PASS: eDHR companion forms are shown in the right panel for the selected process.')
