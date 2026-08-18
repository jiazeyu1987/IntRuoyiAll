import type {
  FrontlinePqcProcessVO,
  FrontlinePqcTaskOptionVO
} from '@/api/mes/pro/feedback'

export type FrontlinePqcTaskAvailabilityIssueCode =
  | 'TASK_NOT_CREATED'
  | 'TASK_DETAILS_MISSING'
  | 'NO_PENDING_TASK'
  | 'PENDING_TASK_INVALID'

export interface FrontlinePqcTaskAvailabilityIssue {
  code: FrontlinePqcTaskAvailabilityIssueCode
  message: string
}

type FrontlinePqcTaskAvailabilityProcess = Pick<
  FrontlinePqcProcessVO,
  'pqcTaskOptions' | 'taskSummary'
>

type DiagnosticTaskOption = Partial<FrontlinePqcTaskOptionVO>

const PQC_INSPECTION_TYPES = new Set(['FIRST', 'PATROL', 'FINAL'])

const isPositiveInteger = (value: unknown) =>
  typeof value === 'number' && Number.isInteger(value) && value > 0

const hasText = (value: unknown) => typeof value === 'string' && value.trim().length > 0

export const resolveFrontlinePqcTaskInvalidFields = (
  option?: DiagnosticTaskOption
): string[] => {
  if (!option) {
    return ['任务明细']
  }
  const invalidFields: string[] = []
  if (!isPositiveInteger(option.pqcTaskId)) {
    invalidFields.push('任务编号')
  }
  if (!isPositiveInteger(option.regulationVersionId)) {
    invalidFields.push('QA规程版本')
  }
  if (!isPositiveInteger(option.qaProcessId)) {
    invalidFields.push('QA工序')
  }
  if (!PQC_INSPECTION_TYPES.has(String(option.inspectionType || ''))) {
    invalidFields.push('检验类型')
  }
  if (!hasText(option.businessDate)) {
    invalidFields.push('业务日期')
  }
  if (!hasText(option.shiftCode)) {
    invalidFields.push('班次')
  }
  if (!isPositiveInteger(option.roundNo)) {
    invalidFields.push('轮次')
  }
  if (!isPositiveInteger(option.plannedInspectionQuantity)) {
    invalidFields.push('计划检验数量')
  }
  if (!Array.isArray(option.inspectionItems) || option.inspectionItems.length === 0) {
    invalidFields.push('检验项目')
  }
  return invalidFields
}

export const isExecutableFrontlinePqcTaskOption = (
  option?: DiagnosticTaskOption
) => Boolean(
  option?.taskStatus === 'PENDING' &&
  resolveFrontlinePqcTaskInvalidFields(option).length === 0
)

const formatInvalidPendingTask = (option: DiagnosticTaskOption) => {
  const taskLabel = isPositiveInteger(option.pqcTaskId)
    ? `任务 ${option.pqcTaskId}`
    : '任务（编号缺失）'
  return `${taskLabel} 缺少或无效字段：${resolveFrontlinePqcTaskInvalidFields(option).join('、')}`
}

const countTaskStatuses = (taskOptions: DiagnosticTaskOption[]) => {
  const counts = new Map<string, number>()
  for (const option of taskOptions) {
    const status = hasText(option.taskStatus) ? String(option.taskStatus) : 'UNKNOWN'
    counts.set(status, (counts.get(status) || 0) + 1)
  }
  return counts
}

const formatTaskStatusSummary = (taskOptions: DiagnosticTaskOption[]) => {
  const counts = countTaskStatuses(taskOptions)
  const labels: Record<string, string> = {
    SUBMITTED: '已提交',
    CONFIRMED: '已确认',
    CANCELLED: '已取消'
  }
  const knownStatuses = ['SUBMITTED', 'CONFIRMED', 'CANCELLED']
  const segments = knownStatuses
    .filter((status) => counts.has(status))
    .map((status) => `${labels[status]} ${counts.get(status)} 条`)
  for (const [status, count] of counts) {
    if (!knownStatuses.includes(status)) {
      segments.push(`未知状态 ${status} ${count} 条`)
    }
  }
  return segments.join('、')
}

export const resolveFrontlinePqcTaskAvailabilityIssue = (
  process: FrontlinePqcTaskAvailabilityProcess
): FrontlinePqcTaskAvailabilityIssue | undefined => {
  if (!process.taskSummary) {
    return {
      code: 'TASK_DETAILS_MISSING',
      message: 'PQC任务汇总缺失：接口未返回当前工序任务汇总。'
    }
  }
  if (!Array.isArray(process.pqcTaskOptions)) {
    return {
      code: 'TASK_DETAILS_MISSING',
      message: 'PQC任务明细缺失：接口未返回当前工序任务明细列表。'
    }
  }

  const taskOptions: DiagnosticTaskOption[] = process.pqcTaskOptions
  if (taskOptions.length === 0) {
    if (process.taskSummary.totalCount > 0) {
      return {
        code: 'TASK_DETAILS_MISSING',
        message:
          `PQC任务明细缺失：汇总显示 ${process.taskSummary.totalCount} 条任务，` +
          '但接口未返回任务明细。'
      }
    }
    return {
      code: 'TASK_NOT_CREATED',
      message: 'PQC任务未生成：当前工序尚未生成任务，请检查任务生成。'
    }
  }

  if (taskOptions.some(isExecutableFrontlinePqcTaskOption)) {
    return undefined
  }

  const pendingTasks = taskOptions.filter((option) => option.taskStatus === 'PENDING')
  if (pendingTasks.length > 0) {
    return {
      code: 'PENDING_TASK_INVALID',
      message: `PQC任务数据不完整：${pendingTasks.map(formatInvalidPendingTask).join('；')}。`
    }
  }

  if (taskOptions.every((option) => option.taskStatus === 'SUBMITTED')) {
    return {
      code: 'NO_PENDING_TASK',
      message: 'PQC任务已提交：当前工序任务正在等待PQC组长复核，不能重复提交。'
    }
  }
  if (taskOptions.every((option) => option.taskStatus === 'CONFIRMED')) {
    return {
      code: 'NO_PENDING_TASK',
      message: 'PQC任务已确认：当前工序任务已完成复核，没有待执行任务。'
    }
  }
  if (taskOptions.every((option) => option.taskStatus === 'CANCELLED')) {
    return {
      code: 'NO_PENDING_TASK',
      message: 'PQC任务已取消：当前工序没有可执行任务，请联系PQC组长重新生成。'
    }
  }
  return {
    code: 'NO_PENDING_TASK',
    message:
      `PQC任务状态不可执行：当前工序没有待执行任务（` +
      `${formatTaskStatusSummary(taskOptions)}）。`
  }
}
