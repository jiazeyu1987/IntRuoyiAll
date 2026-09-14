import type {
  EdhrBatchExecutionRespVO,
  EdhrBatchExecutionTaskRespVO
} from '@/api/mes/pro/edhr/batchExecution'

const EDHR_BATCH_TASK_STATUS_APPROVED = 40
const EDHR_BATCH_TASK_STATUS_SKIPPED = 45

export const isRouteFormTask = (task: EdhrBatchExecutionTaskRespVO) =>
  Boolean(task.formTemplateId || task.batchRecordReportId)

export const isOptionalRouteFormTask = (task: EdhrBatchExecutionTaskRespVO) =>
  task.requiredPolicy === 'OPTIONAL' && isRouteFormTask(task)

export const isRequiredBatchRecordTask = (task: EdhrBatchExecutionTaskRespVO) =>
  task.requiredFlag !== false && task.requiredPolicy !== 'OPTIONAL' && isRouteFormTask(task)

export const resolveRequiredBatchRecordTaskTotal = (batch?: EdhrBatchExecutionRespVO) =>
  (batch?.tasks || []).filter(isRequiredBatchRecordTask).length

export const resolveBatchRequiredCompletedCount = (batch?: EdhrBatchExecutionRespVO) =>
  (batch?.tasks || [])
    .filter(isRequiredBatchRecordTask)
    .filter(
      (task) =>
        task.status === EDHR_BATCH_TASK_STATUS_APPROVED || task.status === EDHR_BATCH_TASK_STATUS_SKIPPED
    ).length

export const resolveBatchRequiredProgress = (batch: EdhrBatchExecutionRespVO) => {
  const total = resolveRequiredBatchRecordTaskTotal(batch)
  if (total === 0) return 0
  return Math.min(100, Math.round((resolveBatchRequiredCompletedCount(batch) / total) * 100))
}

export const resolveBatchRequiredProgressText = (batch?: EdhrBatchExecutionRespVO) =>
  `${resolveBatchRequiredCompletedCount(batch) || 0} / ${resolveRequiredBatchRecordTaskTotal(batch)}`
