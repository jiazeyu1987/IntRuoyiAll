import type { ControlledFileVO } from '@/api/dcc/controlledFile/workflow'
import type { TrainingTaskProgressVO } from '@/api/dcc/controlledFile/training'
import {
  DCC_ACTION_PROJECTION_MISSING_REASON,
  getDccControlledFileStatusLabel,
  getDccControlledFileStatusTagType,
  hasDccControlledFileActionProjection,
  isDccControlledFileActionAllowed,
  resolveDccActionProjectionReadonlyReason,
  type DccControlledFileTagType
} from '../shared/lifecycle'
import { getControlledFileHandlingSummary } from '../shared/handlingSummary'
import { formatDateTimeValue } from '@/utils/formatTime'

export type DccWorkbenchTone = 'primary' | 'warning' | 'danger' | 'success' | 'info'

export interface DccWorkbenchStatusSection {
  key: string
  label: string
  tone: DccWorkbenchTone
  routePath: string
}

export interface DccWorkbenchMetricSource {
  approvalTodoTotal: number
  pendingDistributionTotal: number
  trainingTodoTotal: number
  finalizationFailedTotal: number
}

export interface DccWorkbenchMetricItem extends DccWorkbenchStatusSection {
  count: number
}

export interface DccWorkbenchFileRow {
  id: number
  title: string
  fileNumber: string
  versionNo: string
  status: string
  statusLabel: string
  statusTagType: DccControlledFileTagType
  nextStep: string
  responsibilityHint: string
  timeText: string
  primaryActionText: string
  actionBlockReason: string
}

export interface DccWorkbenchTrainingRow {
  progressId: number
  controlledFileId: number
  title: string
  fileNumber: string
  versionNo: string
  status: string
  statusLabel: string
  progressText: string
}

export const DCC_WORKBENCH_STATUS_SECTIONS: DccWorkbenchStatusSection[] = [
  {
    key: 'approvalTodoTotal',
    label: '我的审批待办',
    tone: 'primary',
    routePath: '/approval-center?moduleCode=DCC&viewType=TODO'
  },
  {
    key: 'pendingDistributionTotal',
    label: '待文控下发',
    tone: 'warning',
    routePath: '/dcc/controlled-file/browser'
  },
  {
    key: 'trainingTodoTotal',
    label: '待培训确认',
    tone: 'success',
    routePath: '/dcc/controlled-file/training-mine'
  },
  {
    key: 'finalizationFailedTotal',
    label: '发布失败',
    tone: 'danger',
    routePath: '/dcc/controlled-file/browser'
  }
]

export const buildDccWorkbenchMetricItems = (
  source: DccWorkbenchMetricSource
): DccWorkbenchMetricItem[] =>
  DCC_WORKBENCH_STATUS_SECTIONS.map((section) => ({
    ...section,
    count: source[section.key as keyof DccWorkbenchMetricSource] ?? 0
  }))

export const resolveWorkbenchErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return fallback
}

const resolveWorkbenchFilePrimaryAction = (file: ControlledFileVO) => {
  if (isDccControlledFileActionAllowed(file, 'MANUAL_RELEASE')) {
    return '下发'
  }
  if (isDccControlledFileActionAllowed(file, 'RETRY_FINALIZATION')) {
    return '重试发布'
  }
  return '查看'
}

const resolveWorkbenchFileActionBlockReason = (file: ControlledFileVO) => {
  if (!hasDccControlledFileActionProjection(file)) {
    return DCC_ACTION_PROJECTION_MISSING_REASON
  }
  if (resolveWorkbenchFilePrimaryAction(file) !== '查看') {
    return ''
  }
  return resolveDccActionProjectionReadonlyReason(file, '后端动作投影未放行工作台快捷操作。')
}

const resolveFileTime = (file: ControlledFileVO) =>
  formatDateTimeValue(
    file.rejectedTime ||
      file.publishedTime ||
      file.approvedTime ||
      file.submittedTime ||
      file.obsoletedTime,
    '-'
  )

export const toWorkbenchFileRow = (file: ControlledFileVO): DccWorkbenchFileRow => {
  const nextStep = getControlledFileHandlingSummary(file)
  return {
    id: file.id,
    title: file.title || file.fileName || '-',
    fileNumber: file.fileNumber || '-',
    versionNo: file.versionNo || '-',
    status: file.status,
    statusLabel: getDccControlledFileStatusLabel(file.status as any),
    statusTagType: getDccControlledFileStatusTagType(file.status as any),
    nextStep: nextStep.nextStep,
    responsibilityHint: nextStep.responsibilityHint,
    timeText: resolveFileTime(file),
    primaryActionText: resolveWorkbenchFilePrimaryAction(file),
    actionBlockReason: resolveWorkbenchFileActionBlockReason(file)
  }
}

const trainingStatusLabelMap = new Map([
  ['PENDING_VIEW', '待学习'],
  ['READY_TO_ACKNOWLEDGE', '待确认'],
  ['ACKNOWLEDGED', '已确认']
])

export const toWorkbenchTrainingRow = (
  item: TrainingTaskProgressVO
): DccWorkbenchTrainingRow => ({
  progressId: item.progressId,
  controlledFileId: item.controlledFileId,
  title: item.title || item.fileName || '-',
  fileNumber: item.fileNumber || '-',
  versionNo: item.versionNo || '-',
  status: item.status,
  statusLabel: trainingStatusLabelMap.get(item.status) || item.status,
  progressText: `${item.accumulatedViewSeconds || 0}/${item.requiredViewSeconds || 0} 秒`
})
