import type { TrainingExecutionRowVO, TrainingTaskProgressVO } from '@/api/dcc/controlledFile/training'
import type { UserVO } from '@/api/system/user'
import { formatDccSimpleUserLabel } from '../shared/utils'
import { formatDateTimeValue } from '@/utils/formatTime'

export const TRAINING_PROGRESS_STATUS_OPTIONS = [
  { label: '待查看', value: 'PENDING_VIEW' },
  { label: '已达标待确认', value: 'READY_TO_ACKNOWLEDGE' },
  { label: '已完成', value: 'ACKNOWLEDGED' }
]

const STATUS_LABEL_MAP = new Map<string, string>(
  TRAINING_PROGRESS_STATUS_OPTIONS.map((item) => [item.value, item.label])
)

export const getTrainingProgressStatusLabel = (status: string | undefined) =>
  status ? STATUS_LABEL_MAP.get(status) || status : '-'

export const getTrainingProgressStatusTagType = (status: string | undefined) => {
  if (status === 'ACKNOWLEDGED') return 'success'
  if (status === 'READY_TO_ACKNOWLEDGE') return 'warning'
  return 'info'
}

export const formatTrainingSeconds = (seconds: number | undefined) => {
  const total = Math.max(0, Number(seconds || 0))
  const minutes = Math.floor(total / 60)
  const remainSeconds = total % 60
  return `${minutes}分${remainSeconds}秒`
}

export const formatTrainingProgressText = (
  accumulatedViewSeconds: number | undefined,
  requiredViewSeconds: number | undefined
) => {
  return `${formatTrainingSeconds(accumulatedViewSeconds)} / ${formatTrainingSeconds(requiredViewSeconds)}`
}

export const buildTrainingProgressPercent = (
  accumulatedViewSeconds: number | undefined,
  requiredViewSeconds: number | undefined
) => {
  const required = Math.max(1, Number(requiredViewSeconds || 600))
  const accumulated = Math.max(0, Number(accumulatedViewSeconds || 0))
  return Math.min(100, Math.round((accumulated / required) * 100))
}

interface TrainingTaskSummarySource {
  accumulatedViewSeconds?: number
  requiredViewSeconds?: number
  eligibleToAcknowledge?: boolean
  acknowledgedAt?: number
  publishedTime?: number
  status?: TrainingTaskProgressVO['status']
}

export const getTrainingTaskSummary = (source: TrainingTaskSummarySource) => {
  const requiredViewSeconds = Math.max(1, Number(source.requiredViewSeconds || 600))
  const accumulatedViewSeconds = Math.max(0, Number(source.accumulatedViewSeconds || 0))
  const remainingSeconds = Math.max(0, requiredViewSeconds - accumulatedViewSeconds)
  const acknowledged = Boolean(source.acknowledgedAt) || source.status === 'ACKNOWLEDGED'
  const readyToAcknowledge =
    Boolean(source.eligibleToAcknowledge) || source.status === 'READY_TO_ACKNOWLEDGE'
  const hintText = acknowledged
    ? '已确认完成'
    : readyToAcknowledge
      ? '已达标，进入培训后可确认'
      : `还需 ${formatTrainingSeconds(remainingSeconds)}`
  const timeText = source.acknowledgedAt
    ? `确认：${formatDateTimeValue(source.acknowledgedAt, '-')}`
    : `发布：${formatDateTimeValue(source.publishedTime, '-')}`

  return {
    statusLabel: getTrainingProgressStatusLabel(source.status),
    tagType: getTrainingProgressStatusTagType(source.status),
    progressPercent: buildTrainingProgressPercent(accumulatedViewSeconds, requiredViewSeconds),
    progressText: `${formatTrainingSeconds(accumulatedViewSeconds)} / ${formatTrainingSeconds(
      requiredViewSeconds
    )}`,
    hintText,
    timeText
  }
}

export const buildResolvedTrainingUsers = (
  departmentId: number,
  userList: UserVO[],
  userNameMap: Map<number, string>
) => {
  return userList
    .filter((item) => Number(item.deptId) === Number(departmentId))
    .map((item) => userNameMap.get(item.id) || formatDccSimpleUserLabel(item))
}

export const buildDepartmentNames = (
  departmentIds: number[] | undefined,
  deptNameMap: Map<number, string>
) => {
  const names = (departmentIds || []).map((item) => deptNameMap.get(item) || `部门#${item}`)
  return names.length ? names.join('、') : '-'
}

export const resolveTrainingPageErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return fallback
}

export const sortTrainingRowsByPublishedTime = <T extends Pick<TrainingTaskProgressVO | TrainingExecutionRowVO, 'publishedTime'>>(
  rows: T[]
) =>
  [...rows].sort((left, right) => {
    const leftTime = left.publishedTime || 0
    const rightTime = right.publishedTime || 0
    return rightTime - leftTime
  })
