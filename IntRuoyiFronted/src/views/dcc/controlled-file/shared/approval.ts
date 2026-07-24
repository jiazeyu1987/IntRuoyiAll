import {
  getDccControlledFileStageByStatus,
  getDccControlledFileStageByKey,
  type DccControlledFileStageKey,
  type DccControlledFileStatus
} from './lifecycle'
import type { ControlledFileRouteSnapshotVO } from '@/api/dcc/controlledFile/workflow'
import { resolveDccStageDisplayName } from './stage-name'

export const DCC_BPM_TASK_STATUS = {
  SKIP: -2,
  NOT_START: -1,
  WAIT: 0,
  RUNNING: 1,
  APPROVE: 2,
  REJECT: 3,
  CANCEL: 4,
  RETURN: 5,
  APPROVING: 7
} as const

export interface DccTaskLike {
  id?: string
  taskDefinitionKey?: string
  status?: number
  assigneeUserId?: number | string
  assignee?: number | string
  assigneeUser?: {
    id?: number
    nickname?: string
  }
  ownerUser?: {
    id?: number
    nickname?: string
  }
}

export interface DccTaskStageProgress {
  stageCode: DccControlledFileStageKey
  stageName: string
  stageOrder: number
  totalCount: number
  approvedCount: number
  rejectedCount: number
  runningCount: number
  waitingCount: number
  completionText: string
  sameLayerHint: string
  isCurrent: boolean
  isCompleted: boolean
  isPending: boolean
}

interface BuildDccTaskStageProgressInput {
  routeSnapshots: ControlledFileRouteSnapshotVO[] | undefined
  taskList: DccTaskLike[] | undefined
  fileStatus: DccControlledFileStatus | string | undefined
}

interface BuildDccTaskCenterRowViewInput extends BuildDccTaskStageProgressInput {
  taskName?: string
  processInstanceId?: string
}

export interface DccTaskHandlingHint {
  handlingHint: string
  responsibilityHint: string
}

const STAGE_CODE_SET = new Set<DccControlledFileStageKey>([
  'DOC_CONTROL_REVIEW',
  'MATRIX_REVIEW',
  'MATRIX_APPROVAL',
  'DOC_CONTROL_APPROVAL'
])

const toStageCode = (
  stageCode: string | undefined,
  stageNo: number | undefined
): DccControlledFileStageKey | undefined => {
  if (stageCode && STAGE_CODE_SET.has(stageCode as DccControlledFileStageKey)) {
    return stageCode as DccControlledFileStageKey
  }
  switch (stageNo) {
    case 1:
      return 'DOC_CONTROL_REVIEW'
    case 2:
      return 'MATRIX_REVIEW'
    case 3:
      return 'MATRIX_APPROVAL'
    case 4:
      return 'DOC_CONTROL_APPROVAL'
    default:
      return undefined
  }
}

const buildStageSameLayerHint = (
  approvedCount: number,
  totalCount: number,
  isCurrent: boolean,
  isCompleted: boolean
) => {
  if (totalCount <= 0) {
    return '未解析到审批人'
  }
  if (isCompleted || approvedCount >= totalCount) {
    return '本层已完成'
  }
  if (isCurrent) {
    return '同层会签未完成'
  }
  return '待进入本层'
}

export const buildDccTaskStageProgress = ({
  routeSnapshots,
  taskList,
  fileStatus
}: BuildDccTaskStageProgressInput): DccTaskStageProgress[] => {
  const currentStage = getDccControlledFileStageByStatus(fileStatus as DccControlledFileStatus)
  const currentStageOrder = currentStage?.order ?? Number.MAX_SAFE_INTEGER

  return (routeSnapshots ?? [])
    .map((snapshot) => {
      const stageCode = toStageCode(snapshot.stageCode, snapshot.stageNo)
      if (!stageCode) {
        return undefined
      }
      const stageDefinition = getDccControlledFileStageByKey(stageCode)
      const matchedTasks = (taskList ?? []).filter((item) => item.taskDefinitionKey === stageCode)
      const resolvedCount = snapshot.resolvedUserIds?.length ?? 0
      const totalCount = Math.max(resolvedCount, matchedTasks.length)
      const approvedCount = matchedTasks.filter(
        (item) => item.status === DCC_BPM_TASK_STATUS.APPROVE
      ).length
      const rejectedCount = matchedTasks.filter(
        (item) => item.status === DCC_BPM_TASK_STATUS.REJECT
      ).length
      const runningCount = matchedTasks.filter(
        (item) =>
          item.status === DCC_BPM_TASK_STATUS.RUNNING ||
          item.status === DCC_BPM_TASK_STATUS.APPROVING
      ).length
      const waitingCount = Math.max(totalCount - approvedCount - rejectedCount - runningCount, 0)
      const stageOrder = snapshot.stageOrder ?? snapshot.stageNo ?? stageDefinition?.order ?? 0
      const isCurrent = currentStage?.key === stageCode
      const isCompleted =
        approvedCount >= totalCount && totalCount > 0
          ? true
          : Boolean(currentStage && stageOrder < currentStageOrder)
      const isPending = !isCurrent && !isCompleted

      return {
        stageCode,
        stageName: resolveDccStageDisplayName(
          stageCode,
          snapshot.stageName || stageDefinition?.label || stageCode
        ),
        stageOrder,
        totalCount,
        approvedCount,
        rejectedCount,
        runningCount,
        waitingCount,
        completionText: `${approvedCount}/${totalCount}`,
        sameLayerHint: buildStageSameLayerHint(approvedCount, totalCount, isCurrent, isCompleted),
        isCurrent,
        isCompleted,
        isPending
      } satisfies DccTaskStageProgress
    })
    .filter((item): item is DccTaskStageProgress => Boolean(item))
    .sort((left, right) => left.stageOrder - right.stageOrder)
}

export const buildDccTaskHandlingHint = (
  currentStage: DccTaskStageProgress | undefined,
  fileStatus: DccControlledFileStatus | string | undefined
): DccTaskHandlingHint => {
  switch (fileStatus as DccControlledFileStatus | undefined) {
    case 'PENDING_APPLICANT_REWORK':
      return {
        handlingHint: '有流程回退，等待申请人处理',
        responsibilityHint: '责任：申请人'
      }
    case 'PENDING_APPLICANT_TRAINING_RECORD':
      return {
        handlingHint: '等待申请人上传培训记录',
        responsibilityHint: '责任：申请人'
      }
    case 'PENDING_MANUAL_DISTRIBUTION':
      return {
        handlingHint: '等待文控正式下发',
        responsibilityHint: '责任：文控'
      }
    case 'TRAINING_IN_PROGRESS':
      return {
        handlingHint: '等待培训对象确认',
        responsibilityHint: '责任：培训对象'
      }
    case 'FINALIZING':
      return {
        handlingHint: '发布处理中',
        responsibilityHint: '责任：系统发布任务'
      }
    case 'FINALIZATION_FAILED':
      return {
        handlingHint: '发布失败，需文控处理',
        responsibilityHint: '责任：文控'
      }
    case 'REJECTED':
      return {
        handlingHint: '流程已驳回',
        responsibilityHint: '责任：申请人'
      }
    default:
      break
  }
  if (currentStage?.isCurrent) {
    return {
      handlingHint: `等待${currentStage.stageName}处理`,
      responsibilityHint: `${currentStage.completionText} 已完成，${currentStage.sameLayerHint}`
    }
  }
  if (currentStage?.isPending) {
    return {
      handlingHint: `下一步：${currentStage.stageName}`,
      responsibilityHint: currentStage.sameLayerHint
    }
  }
  return {
    handlingHint: '查看详情确认下一步',
    responsibilityHint: '责任：流程相关人'
  }
}

export const buildDccTaskCenterRowView = ({
  fileStatus,
  routeSnapshots,
  taskList
}: BuildDccTaskCenterRowViewInput) => {
  const stageProgress = buildDccTaskStageProgress({
    routeSnapshots,
    taskList,
    fileStatus
  })
  const currentStage =
    stageProgress.find((item) => item.isCurrent) ??
    stageProgress.find((item) => item.isPending) ??
    stageProgress.at(-1)
  const handling = buildDccTaskHandlingHint(currentStage, fileStatus)

  return {
    stageProgress,
    currentStageLabel: currentStage?.stageName ?? '-',
    sameLayerProgressText: currentStage?.completionText ?? '-',
    sameLayerHint: currentStage?.sameLayerHint ?? '无审批进度',
    handlingHint: handling.handlingHint,
    responsibilityHint: handling.responsibilityHint,
    primaryActionText: '处理审批',
    secondaryActionText: '查看审批'
  }
}
