import {
  DCC_CONTROLLED_FILE_STATUS_OPTIONS,
  getDccControlledFileStageByStatus,
  getDccControlledFileStatusLabel,
  getDccControlledFileStatusTagType,
  isDccControlledFileWithdrawableStatus,
  type DccControlledFileStatus,
  type DccControlledFileTagType
} from '../shared/lifecycle'

export const CONTROLLED_FILE_STATUS_FILTER_OPTIONS = DCC_CONTROLLED_FILE_STATUS_OPTIONS

export interface ControlledFileHandlingSummary {
  nextStep: string
  responsibilityHint: string
}

export interface ControlledFileVersionSummary {
  versionText: string
  chainText: string
  effectiveText: string
  tagText: string
  tagType: DccControlledFileTagType
}

interface ControlledFileHandlingSummarySource {
  status?: string
  rejectReason?: string
  finalizationError?: string
  supersededByFileId?: number | null
  modifying?: boolean
  hasPendingTrainingAcknowledgement?: boolean
}

interface ControlledFileVersionSummarySource {
  versionNo?: string | null
  currentActiveVersionNo?: string | null
  effectiveDate?: string | null
  status?: string
  modifying?: boolean
  supersededByFileId?: number | null
  versionHistory?: Array<{
    id?: number
    versionNo?: string
    status?: string
  }>
}

const getStatusFromSource = (source: ControlledFileHandlingSummarySource) =>
  source.status as DccControlledFileStatus | undefined

const getStageResponsibilityHint = (status: DccControlledFileStatus | undefined) => {
  const stage = getDccControlledFileStageByStatus(status)
  if (!stage) {
    return '责任：申请人'
  }
  if (stage.requiredPermission === 'REVIEW') {
    return `责任：${stage.label}`
  }
  return `责任：${stage.label}`
}

const getPendingStageSummary = (
  status: DccControlledFileStatus | undefined,
  source: ControlledFileHandlingSummarySource
): ControlledFileHandlingSummary => {
  switch (status) {
    case 'PENDING_DOC_CONTROL_REVIEW':
    case 'PENDING_MATRIX_REVIEW':
    case 'PENDING_MATRIX_APPROVAL':
    case 'PENDING_DOC_CONTROL_APPROVAL':
      return {
        nextStep: `等待${getDccControlledFileStageByStatus(status)?.label || '审批'}`,
        responsibilityHint: getStageResponsibilityHint(status)
      }
    case 'PENDING_APPLICANT_REWORK':
      return {
        nextStep: '有流程回退，等待申请人处理',
        responsibilityHint: source.rejectReason ? `回退原因：${source.rejectReason}` : '责任：申请人'
      }
    case 'PENDING_APPLICANT_TRAINING_RECORD':
      return {
        nextStep: '等待上传培训记录',
        responsibilityHint: '责任：申请人'
      }
    case 'READY_TO_PUBLISH':
      return {
        nextStep: '等待提交发布申请',
        responsibilityHint: '责任：文控'
      }
    case 'FINALIZING':
      return {
        nextStep: '发布处理中，等待系统完成',
        responsibilityHint: '责任：系统发布任务'
      }
    case 'TRAINING_IN_PROGRESS':
      return {
        nextStep: '培训中，等待确认',
        responsibilityHint: source.hasPendingTrainingAcknowledgement
          ? '责任：培训对象'
          : '责任：使用部门'
      }
    case 'PENDING_MANUAL_DISTRIBUTION':
      return {
        nextStep: '等待文控下发',
        responsibilityHint: '责任：文控'
      }
    case 'ACTIVE':
      return {
        nextStep: '可查看和下载现行版',
        responsibilityHint: '责任：使用部门'
      }
    case 'REJECTED':
      return {
        nextStep: source.rejectReason ? '已驳回，等待申请人修订' : '已驳回，查看驳回原因',
        responsibilityHint: source.rejectReason ? `驳回原因：${source.rejectReason}` : '责任：申请人'
      }
    case 'WITHDRAWN':
      return source.supersededByFileId
        ? {
            nextStep: '已重新提交，查看新流程',
            responsibilityHint: '责任：申请人'
          }
        : {
            nextStep: '已撤回，可删除流程或重新提交',
            responsibilityHint: '责任：申请人'
          }
    case 'OBSOLETE':
      return {
        nextStep: '已作废，查看历史版本',
        responsibilityHint: '责任：系统记录'
      }
    case 'SUPERSEDED':
      return {
        nextStep: '已被替代，查看现行版',
        responsibilityHint: '责任：查看最新版本'
      }
    case 'DRAFT':
      return {
        nextStep: source.modifying ? '草稿修改中，完成后提交' : '完善后提交',
        responsibilityHint: '责任：申请人'
      }
    case 'FINALIZATION_FAILED':
      return {
        nextStep: source.finalizationError ? '发布失败，需处理阻塞' : '发布失败，需重试或排查',
        responsibilityHint: source.finalizationError
          ? `阻塞：${source.finalizationError}`
          : '责任：系统发布任务'
      }
    default:
      return {
        nextStep: '查看详情确认下一步',
        responsibilityHint: '责任：流程相关人'
      }
  }
}

export const getControlledFileHandlingSummary = (source: ControlledFileHandlingSummarySource): ControlledFileHandlingSummary => {
  const status = getStatusFromSource(source)
  if (source.finalizationError || status === 'FINALIZATION_FAILED') {
    return getPendingStageSummary('FINALIZATION_FAILED', source)
  }
  return getPendingStageSummary(status, source)
}

export const getControlledFileHandlingStatusLabel = (status: string | undefined) =>
  getDccControlledFileStatusLabel(status as DccControlledFileStatus | undefined)

export const getControlledFileHandlingStatusTagType = (status: string | undefined): DccControlledFileTagType =>
  getDccControlledFileStatusTagType(status as DccControlledFileStatus | undefined)

const getControlledFileVersionTag = (source: ControlledFileVersionSummarySource): Pick<ControlledFileVersionSummary, 'tagText' | 'tagType'> => {
  const status = source.status as DccControlledFileStatus | undefined
  if (source.modifying) {
    return { tagText: '修改中', tagType: 'warning' }
  }
  if (status === 'ACTIVE') {
    return { tagText: '现行', tagType: 'success' }
  }
  if (status === 'SUPERSEDED') {
    return { tagText: '已替代', tagType: 'info' }
  }
  if (status === 'OBSOLETE') {
    return { tagText: '已作废', tagType: 'info' }
  }
  if (source.currentActiveVersionNo && source.currentActiveVersionNo !== source.versionNo) {
    return { tagText: '历史版', tagType: 'info' }
  }
  if (status === 'REJECTED' || status === 'FINALIZATION_FAILED') {
    return { tagText: '阻塞', tagType: 'danger' }
  }
  if (status === 'DRAFT' || status === 'WITHDRAWN') {
    return { tagText: '草稿', tagType: 'info' }
  }
  return { tagText: '流转中', tagType: 'primary' }
}

const getControlledFileVersionChainText = (source: ControlledFileVersionSummarySource) => {
  const versionCount = source.versionHistory?.length || 0
  if (source.modifying) {
    return source.currentActiveVersionNo && source.currentActiveVersionNo !== source.versionNo
      ? `现行版：${source.currentActiveVersionNo}`
      : '当前版本正在修改'
  }
  if ((source.status as DccControlledFileStatus | undefined) === 'SUPERSEDED') {
    return source.supersededByFileId
      ? `后继文件：#${source.supersededByFileId}`
      : '已被后继版本替代'
  }
  if ((source.status as DccControlledFileStatus | undefined) === 'OBSOLETE') {
    return '已作废，保留历史记录'
  }
  if (source.currentActiveVersionNo && source.currentActiveVersionNo !== source.versionNo) {
    return `现行版：${source.currentActiveVersionNo}`
  }
  if (versionCount > 1) {
    return `版本链 ${versionCount} 个版本`
  }
  if ((source.status as DccControlledFileStatus | undefined) === 'ACTIVE') {
    return '现行版本'
  }
  return '单版本记录'
}

export const getControlledFileVersionSummary = (source: ControlledFileVersionSummarySource): ControlledFileVersionSummary => {
  const tag = getControlledFileVersionTag(source)
  return {
    versionText: `版本 ${source.versionNo || '-'}`,
    chainText: getControlledFileVersionChainText(source),
    effectiveText: source.effectiveDate ? `生效 ${source.effectiveDate}` : '生效未定',
    ...tag
  }
}

export const isControlledFileRowWithdrawable = (status: string | undefined) =>
  isDccControlledFileWithdrawableStatus(status as DccControlledFileStatus | undefined)

export const isControlledFileRowWithdrawnActionable = (
  status: string | undefined,
  supersededByFileId?: number | null
) => (status as DccControlledFileStatus | undefined) === 'WITHDRAWN' && !supersededByFileId

export const isControlledFileRowPreviewable = (status: string | undefined) => {
  const typedStatus = status as DccControlledFileStatus | undefined
  return typedStatus === 'ACTIVE' || typedStatus === 'SUPERSEDED'
}

export const isControlledFileRowDownloadable = (status: string | undefined) => {
  return (status as DccControlledFileStatus | undefined) === 'ACTIVE'
}

export const resolveControlledFileActionErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return fallback
}
