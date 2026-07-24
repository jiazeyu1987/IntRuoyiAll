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
const servicePath = path.join(
  repoRoot,
  '..',
  'ruoyi-vue-pro',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'service',
  'pro',
  'batchrecord',
  'MesProEdhrBatchExecutionServiceImpl.java'
)
const apiPath = path.join(repoRoot, 'src', 'api', 'mes', 'pro', 'edhr', 'batchExecution.ts')

const detail = fs.readFileSync(detailPath, 'utf8')
const service = fs.readFileSync(servicePath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')
const compactDetail = detail.replace(/\s+/g, ' ')

const extractConstBlock = (source, marker) => {
  const start = source.indexOf(marker)
  assert(start >= 0, `missing block: ${marker}`)
  const next = source.indexOf('\n\nconst ', start + marker.length)
  return next >= 0 ? source.slice(start, next) : source.slice(start)
}

const canOpenTaskBlock = extractConstBlock(
  detail,
  'const canOpenTask = (row: EdhrBatchExecutionTaskRespVO) =>'
)
const canOpenTaskExpression = detail.slice(
  detail.indexOf('const canOpenTask = (row: EdhrBatchExecutionTaskRespVO) =>'),
  detail.indexOf('const canSkipOptionalTask = (row: EdhrBatchExecutionTaskRespVO) =>')
)

assert(
  detail.includes('const selectedProcessTasks = computed') &&
    detail.includes('class="edhr-batch-detail__rail-process-forms"') &&
    detail.includes('resolvePendingTaskActionLabel(selectedTaskForEvidence)') &&
    detail.includes('handleSelectedPendingTaskAction(selectedTaskForEvidence)'),
  '批次详情页在尚无已填写记录时必须在右侧展示当前工序表单，并保留统一打开填写入口'
)

assert(
  detail.includes('const canHandlePendingTask = (row: EdhrBatchExecutionTaskRespVO) =>') &&
    detail.includes("return '跳过节点'") &&
    detail.includes("return '完成节点'") &&
    detail.includes('message.error(resolveTaskGateText(row) ||') &&
    detail.includes('await handleSkipSpecialNode(row)') &&
    detail.includes('await handleCompleteSpecialNode(row)'),
  '待处理入口必须覆盖来料检报告等特殊前置节点，避免只显示关闭阻塞项'
)

assert(
    detail.includes('v-for="task in selectedProcessTasks"') &&
    detail.includes('resolveFormSlotTypeLabel(task.formSlotType)') &&
    detail.includes('task.batchRecordReportName || task.batchRecordReportId') &&
    detail.includes('resolveTaskGateText(task)') &&
    detail.includes(':disabled="!canHandlePendingTask(task)"'),
  '当前工序表单列表必须展示全部表单名称、状态和门禁原因，并提供按权限禁用的打开入口'
)

assert(
  detail.includes('const resolvePendingTaskTitle = (row: EdhrBatchExecutionTaskRespVO) =>') &&
    detail.includes('specialNodeLabels[row.nodeType || \'\'] || row.processName || row.processCode') &&
    detail.includes('row.processName || row.processCode || row.batchRecordReportName') &&
    detail.includes('const resolvePendingTaskSortText = (row: EdhrBatchExecutionTaskRespVO) =>') &&
    detail.includes('const canHandlePendingTask = (row: EdhrBatchExecutionTaskRespVO) =>'),
  '待处理列表标题必须优先显示中文节点/工序名，并保留排序与可操作性判定'
)

assert(
  detail.includes('const hasActiveWorkTask = (row: EdhrBatchExecutionTaskRespVO) =>') &&
    canOpenTaskBlock.includes('hasActiveWorkTask(row)') &&
    detail.includes('activeWorkTaskId') &&
    detail.includes('allowedActions') &&
    detail.includes('disabledReason') &&
    detail.includes('const canHandlePendingTask = (row: EdhrBatchExecutionTaskRespVO) =>'),
  '待处理入口必须由后端工作任务与允许动作驱动，审核/批准/无关人员不能再靠 executionId 推断'
)

assert(
  detail.includes("sortedTasks.value.some((task) => hasAllowedTaskAction(task, 'CLOSE'))") &&
    compactDetail.includes('detail.value?.canClose === true || hasBatchCloseAction.value') &&
    detail.includes("hasAllowedTaskAction(row, 'CLOSE')") &&
    service.includes('isCurrentUserCloseOwner(closeRule, currentUserId)') &&
    detail.includes('const canOperateSpecialNode = (row: EdhrBatchExecutionTaskRespVO) =>'),
  '生产负责人关闭入口必须支持后端批次级 canClose 和特殊节点 CLOSE 动作，不能只按批次状态或前端权限展示'
)

assert(
  canOpenTaskExpression.includes("hasAllowedTaskAction(row, 'OPEN_FORM')") &&
    detail.includes('const canOpenTask = (row: EdhrBatchExecutionTaskRespVO) =>') &&
    canOpenTaskExpression.includes('row.available !== false') &&
    !canOpenTaskExpression.includes('!isOptionalTask(row)') &&
    !canOpenTaskExpression.includes("(!hasActiveWorkTask(row) || hasAllowedTaskAction(row, 'OPEN_FORM'))") &&
    detail.includes('normalizeTaskAccessReason(row.disabledReason)') &&
    detail.includes('normalizeTaskAccessReason(row.gateMessage)') &&
    detail.includes('handleSelectedPendingTaskAction'),
  '表单打开入口必须由后端 OPEN_FORM 动作驱动，可选表单有工作任务时也可打开填写，无权限或门禁未满足时显示明确禁用原因'
)

assert(
  detail.includes('const canSkipOptionalTask = (row: EdhrBatchExecutionTaskRespVO) =>') &&
    detail.includes("hasAllowedTaskAction(row, 'SKIP')") &&
    detail.includes("return '可选填写'") &&
    detail.includes("return '跳过表单'") &&
    detail.includes('handleSkipOptionalTask(task)') &&
    detail.includes(':title="currentSkipDialogTitle"') &&
    !detail.includes("if (isOptionalTask(row)) return '无需填写'"),
  '可选路线表单必须显示可选填写状态，并提供显式跳过表单动作，不能直接等同于无需填写'
)

assert(
  detail.includes('edhr-batch-detail__rail-process-form-optional-tag') &&
    /v-if="isOptionalTask\(task\)"[\s\S]*?class="edhr-batch-detail__rail-process-form-optional-tag"[\s\S]*?可选填写/.test(detail) &&
    /class="edhr-batch-detail__rail-process-form-state-tags"[\s\S]*?edhr-batch-detail__rail-process-form-optional-tag[\s\S]*?resolveTaskStatusLabel\(task\)/.test(detail),
  '右侧当前工序表单卡片必须把“可选填写”作为独立可见标签渲染，不能在草稿状态下丢失可选属性'
)

assert(
  detail.includes('edhr-batch-detail__pending-task-name') &&
    detail.includes('edhr-batch-detail__review-process-name') &&
    detail.includes('overflow-wrap: anywhere') &&
    !detail.includes('.edhr-batch-detail__process-report {\n  overflow: hidden;\n  text-overflow: ellipsis;\n  white-space: nowrap;'),
  '待处理工序和已填写工序必须显示完整工序名称，不得只用省略号截断'
)

assert(
  detail.includes('const selectedTaskForExecution = computed(() =>') &&
    detail.includes('if (!execution) return undefined') &&
    detail.includes('const selectedOpenableTask = computed(() =>') &&
    detail.includes('selectedTaskForExecution.value || selectedTaskForEvidence.value'),
  '待填写入口不能让 selectedTaskForExecution 与 selectedTaskForEvidence 互相递归'
)

assert(
  detail.includes('role="button"') &&
    detail.includes('tabindex="0"') &&
    detail.includes('@keydown.enter.prevent="selectProcessTask(task)"') &&
    detail.includes('@keydown.space.prevent="selectProcessTask(task)"'),
  '待填写工序卡片必须可键盘选择，并避免 button 内嵌按钮'
)

assert(
  !detail.includes('关闭阻塞项') &&
    !detail.includes('displayCloseBlockers') &&
    service.includes('未打开电子批记录'),
  '关闭阻塞项应仅保留在后端关闭校验，不应在批次详情页展示'
)

assert(
  detail.includes('edhr-batch-detail__pending-task-list') &&
    detail.includes('edhr-batch-detail__pending-task-item') &&
    detail.includes('edhr-batch-detail__rail-task-action'),
  '待填写工序入口必须有左侧可扫描列表样式和右侧操作按钮样式'
)

assert(
  detail.includes('edhr-batch-detail__pending-task-fillable') &&
    detail.includes('resolvePendingTaskFillableUsersText(selectedTaskForEvidence)') &&
    api.includes('fillableUsers?: EdhrBatchExecutionTaskFillableUserRespVO[]'),
  'pending task right rail must render backend fillableUsers'
)

console.log('edhr batch pending form entry static contract passed')
