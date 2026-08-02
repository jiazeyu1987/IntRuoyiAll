import dayjs from 'dayjs'
import type {
  ControlledFileDistributionStatusVO,
  ControlledFileSignatureSummaryVO,
  ControlledFileTrainingAssignmentVO,
  ControlledFileTrainingStatusVO,
  ControlledFileVO
} from '@/api/dcc/controlledFile/workflow'
import {
  getDccControlledFileStatusLabel,
  getDccControlledFileStatusTagType,
  type DccControlledFileStatus,
  type DccControlledFileTagType
} from '../shared/lifecycle'
import {
  formatDccHashShort,
  getDccControlledCopyHashStatusLabel,
  getDccControlledCopyHashStatusTagType,
  getDccSignatureEvidenceStatusLabel,
  getDccSignatureEvidenceStatusTagType,
  getDccSignatureMeaningLabel,
  getDccSignatureTaskActionLabel
} from '../shared/signature-evidence'
import {
  resolveControlledActionProjection,
  type ControlledActionProjectionState
} from '@/api/form-center/actionProjection'

type DetailDateValue = string | number | Date | readonly number[] | null | undefined

export interface ControlledFileDetailActionState {
  canPreview: boolean
  canDownload: boolean
  canPrint: boolean
  canObsolete: boolean
  canPublish: boolean
  canManualRelease: boolean
  canAcknowledgeTraining: boolean
  canRetryFinalization: boolean
  projectionStates: Record<DccDetailProjectionAction, ControlledActionProjectionState>
  blockerMessages: string[]
}

export interface TrainingAssignmentRow extends ControlledFileTrainingAssignmentVO {
  departmentId: number
  trainingStatus: string
}

export interface DetailLifecycleTimelineItem {
  key: string
  categoryLabel: string
  title: string
  timeText: string
  description: string
  actorText?: string
  tagType: DccControlledFileTagType
}

export interface DetailTrainingAssignmentSummary {
  statusLabel: string
  statusTagType: DccControlledFileTagType
  progressText: string
  eligibilityLabel: string
  eligibilityTagType: DccControlledFileTagType
  departmentStatusLabel: string
  departmentStatusTagType: DccControlledFileTagType
  acknowledgedAtText: string
}

export interface DetailTrainingCompletionSummary {
  totalCount: number
  completedCount: number
  pendingCount: number
  completionText: string
  pendingNamesText: string
  latestAcknowledgedAtText: string
}

interface DetailLifecycleTimelineContext {
  userNameMap: Map<number, string>
  deptNameMap: Map<number, string>
}

type DetailActionReadableState = Pick<
  ControlledFileVO,
  | 'status'
  | 'canPreview'
  | 'canDownload'
  | 'canPrint'
  | 'canObsolete'
  | 'canPublish'
  | 'canManualRelease'
  | 'hasPendingTrainingAcknowledgement'
>

export type DccDetailProjectionAction =
  | 'PREVIEW'
  | 'DOWNLOAD'
  | 'PRINT'
  | 'OBSOLETE'
  | 'PUBLISH'
  | 'MANUAL_RELEASE'
  | 'ACKNOWLEDGE_TRAINING'

const DCC_DETAIL_PROJECTION_LABELS: Record<DccDetailProjectionAction, string> = {
  PREVIEW: '预览受控文件',
  DOWNLOAD: '下载受控文件',
  PRINT: '受控打印',
  OBSOLETE: '作废当前版本',
  PUBLISH: '发布申请',
  MANUAL_RELEASE: '正式下发',
  ACKNOWLEDGE_TRAINING: '确认培训'
}

const DCC_DETAIL_PROJECTION_FIELDS: Record<
  DccDetailProjectionAction,
  keyof DetailActionReadableState
> = {
  PREVIEW: 'canPreview',
  DOWNLOAD: 'canDownload',
  PRINT: 'canPrint',
  OBSOLETE: 'canObsolete',
  PUBLISH: 'canPublish',
  MANUAL_RELEASE: 'canManualRelease',
  ACKNOWLEDGE_TRAINING: 'hasPendingTrainingAcknowledgement'
}

const hasDetailProjectionField = (
  detail: DetailActionReadableState,
  field: keyof DetailActionReadableState
) => Object.prototype.hasOwnProperty.call(detail, field)

export const resolveDccDetailActionProjection = (
  detail: DetailActionReadableState | undefined,
  action: DccDetailProjectionAction
) => {
  const label = DCC_DETAIL_PROJECTION_LABELS[action]
  const field = DCC_DETAIL_PROJECTION_FIELDS[action]
  if (!detail || !hasDetailProjectionField(detail, field)) {
    return resolveControlledActionProjection(undefined, label)
  }
  const allowed = Boolean(detail[field])
  return resolveControlledActionProjection(
    {
      actionCode: action,
      actionLabel: label,
      allowed,
      permissionGranted: allowed
    },
    label
  )
}

const DISTRIBUTION_STATUS_LABEL_MAP = new Map<string, string>([
  ['PENDING', '待分发'],
  ['SENT', '已发送'],
  ['READ', '已阅读'],
  ['ACKNOWLEDGED', '已确认'],
  ['RECOVERED', '已回收']
])

const TRAINING_STATUS_LABEL_MAP = new Map<string, string>([
  ['PENDING', '待确认'],
  ['ACKNOWLEDGED', '已确认']
])

const toText = (value: unknown) => String(value ?? '').trim()

const normalizeDetailDateValue = (value: DetailDateValue): string | number | Date | undefined => {
  if (value === null || value === undefined || value === '') {
    return undefined
  }
  if (Array.isArray(value)) {
    const parts = value.map((item) => Number(item))
    if (parts.length < 3 || parts.some((item) => !Number.isFinite(item))) {
      return undefined
    }
    const [year, month, day, hour = 0, minute = 0, second = 0, millisecond = 0] = parts
    return new Date(year, Math.max(month - 1, 0), day, hour, minute, second, millisecond)
  }
  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (!trimmed) {
      return undefined
    }
    if (/^\d+$/.test(trimmed)) {
      return Number(trimmed)
    }
    return trimmed
  }
  if (typeof value === 'number' || value instanceof Date) {
    return value
  }
  return undefined
}

const formatDetailDateValue = (value: DetailDateValue, format: string) => {
  const normalized = normalizeDetailDateValue(value)
  if (!normalized) {
    return '-'
  }
  const formatted = dayjs(normalized)
  return formatted.isValid() ? formatted.format(format) : toText(value)
}

export const formatControlledFileDate = (value: DetailDateValue) =>
  formatDetailDateValue(value, 'YYYY-MM-DD')

export const formatControlledFileDateTime = (value: DetailDateValue) =>
  formatDetailDateValue(value, 'YYYY-MM-DD HH:mm:ss')

const getDetailDateSortValue = (value: DetailDateValue) => {
  const normalized = normalizeDetailDateValue(value)
  if (!normalized) {
    return undefined
  }
  const parsed = dayjs(normalized)
  return parsed.isValid() ? parsed.valueOf() : undefined
}

export const buildDetailUserDisplayName = (user: {
  nickname?: string | null
  username?: string | null
} | null | undefined) => {
  const nickname = toText(user?.nickname)
  const username = toText(user?.username)
  if (nickname && username) {
    return `${nickname} (${username})`
  }
  return nickname || username || '-'
}

export const formatTrainingProgressText = (
  accumulatedViewSeconds: number | undefined,
  requiredViewSeconds: number | undefined
) => {
  const accumulated = Math.max(0, Number(accumulatedViewSeconds || 0))
  const required = Math.max(0, Number(requiredViewSeconds || 600))
  const accumulatedMinutes = Math.floor(accumulated / 60)
  const accumulatedRemainSeconds = accumulated % 60
  const requiredMinutes = Math.floor(required / 60)
  const requiredRemainSeconds = required % 60
  return `${accumulatedMinutes}分${accumulatedRemainSeconds}秒 / ${requiredMinutes}分${requiredRemainSeconds}秒`
}

export const getDetailStatusLabel = (status: string | undefined) =>
  getDccControlledFileStatusLabel(status as DccControlledFileStatus | undefined)

export const getDetailStatusTagType = (status: string | undefined): DccControlledFileTagType =>
  getDccControlledFileStatusTagType(status as DccControlledFileStatus | undefined)

export const getDetailActionState = (
  detail: DetailActionReadableState | undefined
): ControlledFileDetailActionState => {
  const status = detail?.status as DccControlledFileStatus | undefined
  const projectionStates = {
    PREVIEW: resolveDccDetailActionProjection(detail, 'PREVIEW'),
    DOWNLOAD: resolveDccDetailActionProjection(detail, 'DOWNLOAD'),
    PRINT: resolveDccDetailActionProjection(detail, 'PRINT'),
    OBSOLETE: resolveDccDetailActionProjection(detail, 'OBSOLETE'),
    PUBLISH: resolveDccDetailActionProjection(detail, 'PUBLISH'),
    MANUAL_RELEASE: resolveDccDetailActionProjection(detail, 'MANUAL_RELEASE'),
    ACKNOWLEDGE_TRAINING: resolveDccDetailActionProjection(detail, 'ACKNOWLEDGE_TRAINING')
  }
  const blockerMessages = Object.values(projectionStates)
    .filter((state) => state.projectionMissing || state.effectFailedPending)
    .map((state) => state.blockerMessage)
    .filter((message, index, messages) => Boolean(message) && messages.indexOf(message) === index)
  return {
    canPreview: projectionStates.PREVIEW.allowed,
    canDownload: projectionStates.DOWNLOAD.allowed,
    canPrint: projectionStates.PRINT.allowed,
    canObsolete: projectionStates.OBSOLETE.allowed,
    canPublish: projectionStates.PUBLISH.allowed,
    canManualRelease: projectionStates.MANUAL_RELEASE.allowed,
    canAcknowledgeTraining: projectionStates.ACKNOWLEDGE_TRAINING.allowed,
    canRetryFinalization: status === 'FINALIZATION_FAILED',
    projectionStates,
    blockerMessages
  }
}

export const getPendingTrainingAssignments = (
  trainingStatuses: ControlledFileTrainingStatusVO[] | undefined,
  currentUserId: number | undefined
) => {
  if (!currentUserId) {
    return []
  }
  return (trainingStatuses ?? [])
    .flatMap((status) => status.assignments ?? [])
    .filter((assignment) => assignment.userId === currentUserId && assignment.status !== 'ACKNOWLEDGED')
}

export const flattenTrainingAssignments = (
  trainingStatuses: ControlledFileTrainingStatusVO[] | undefined
): TrainingAssignmentRow[] => {
  return (trainingStatuses ?? []).flatMap((trainingStatus) =>
    (trainingStatus.assignments ?? []).map((assignment) => ({
      ...assignment,
      departmentId: trainingStatus.departmentId,
      trainingStatus: trainingStatus.status
    }))
  )
}

export const getDetailTrainingAssignmentSummary = (
  row: TrainingAssignmentRow
): DetailTrainingAssignmentSummary => {
  const acknowledged = row.status === 'ACKNOWLEDGED'
  const eligible = Boolean(row.eligibleToAcknowledge)
  return {
    statusLabel: getTrainingStatusLabel(row.status),
    statusTagType: acknowledged ? 'success' : 'warning',
    progressText: formatTrainingProgressText(row.accumulatedViewSeconds, row.requiredViewSeconds),
    eligibilityLabel: acknowledged ? '已确认' : eligible ? '可确认' : '未达标',
    eligibilityTagType: acknowledged || eligible ? 'success' : 'info',
    departmentStatusLabel: getTrainingStatusLabel(row.trainingStatus),
    departmentStatusTagType: row.trainingStatus === 'ACKNOWLEDGED' ? 'success' : 'info',
    acknowledgedAtText: formatControlledFileDateTime(row.acknowledgedAt)
  }
}

export const isVersionHistoryVisibleToReader = (_status: string | undefined) => true

export const getDistributionStatusLabel = (status: string | undefined) => {
  return status ? DISTRIBUTION_STATUS_LABEL_MAP.get(status) ?? status : '-'
}

const DISTRIBUTION_MEDIUM_LABEL_MAP = new Map<string, string>([
  ['PUBLIC_FOLDER', '公盘目录'],
  ['PAPER', '纸质发放']
])

export const getDistributionMediumLabel = (distributionMedium: string | undefined) => {
  return distributionMedium
    ? DISTRIBUTION_MEDIUM_LABEL_MAP.get(distributionMedium) ?? distributionMedium
    : '-'
}

export const getDistributionAckUserSummary = (
  distribution: ControlledFileDistributionStatusVO,
  userNameMap: Map<number, string>
) => {
  if (!distribution.acknowledgedBy) {
    return '-'
  }
  return userNameMap.get(distribution.acknowledgedBy) || `用户#${distribution.acknowledgedBy}`
}

export const getTrainingStatusLabel = (status: string | undefined) => {
  return status ? TRAINING_STATUS_LABEL_MAP.get(status) ?? status : '-'
}

export const getSignatureActionLabel = getDccSignatureTaskActionLabel

export const getSignatureMeaningLabel = getDccSignatureMeaningLabel

export const getSignatureEvidenceStatusLabel = getDccSignatureEvidenceStatusLabel

export const getSignatureEvidenceStatusTagType = getDccSignatureEvidenceStatusTagType

export const getControlledCopyHashStatusLabel = getDccControlledCopyHashStatusLabel

export const getControlledCopyHashStatusTagType = getDccControlledCopyHashStatusTagType

export const formatSignatureHashShort = formatDccHashShort

export const resolveReadSideErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return fallback
}

export const getDistributionRecipientSummary = (
  distribution: ControlledFileDistributionStatusVO,
  userNameMap: Map<number, string>
) => {
  const recipients = distribution.recipients ?? []
  if (recipients.length) {
    return recipients
      .map((recipient) => {
        const userName = userNameMap.get(recipient.userId) || `用户#${recipient.userId}`
        const acknowledgedText = recipient.acknowledgedAt
          ? `已签收 ${formatControlledFileDateTime(recipient.acknowledgedAt)}`
          : '待签收'
        const ackComment = toText(recipient.ackComment)
        return ackComment ? `${userName}（${acknowledgedText}，${ackComment}）` : `${userName}（${acknowledgedText}）`
      })
      .join('、')
  }
  if (!distribution.recipientUserIds?.length) {
    return '-'
  }
  return distribution.recipientUserIds
    .map((userId) => userNameMap.get(userId) || `用户#${userId}`)
    .join('、')
}

export const getTrainingAssignmentUserSummary = (
  assignment: ControlledFileTrainingAssignmentVO,
  userNameMap: Map<number, string>
) => {
  return userNameMap.get(assignment.userId) || `用户#${assignment.userId}`
}

export const getDetailTrainingCompletionSummary = (
  rows: TrainingAssignmentRow[],
  userNameMap: Map<number, string>
): DetailTrainingCompletionSummary => {
  const totalCount = rows.length
  const completedRows = rows.filter((row) => row.status === 'ACKNOWLEDGED' || Boolean(row.acknowledgedAt))
  const pendingRows = rows.filter((row) => row.status !== 'ACKNOWLEDGED' && !row.acknowledgedAt)
  const latestAcknowledgedAt = completedRows
    .map((row) => getDetailDateSortValue(row.acknowledgedAt))
    .filter((value): value is number => value !== undefined)
    .sort((left, right) => right - left)[0]
  return {
    totalCount,
    completedCount: completedRows.length,
    pendingCount: pendingRows.length,
    completionText: totalCount ? `${completedRows.length}/${totalCount} 已完成` : '暂无培训对象',
    pendingNamesText: pendingRows.length
      ? pendingRows.map((row) => getTrainingAssignmentUserSummary(row, userNameMap)).join('、')
      : '无未完成人员',
    latestAcknowledgedAtText: latestAcknowledgedAt
      ? formatControlledFileDateTime(latestAcknowledgedAt)
      : '-'
  }
}

export const getSignatureActorSummary = (
  signature: ControlledFileSignatureSummaryVO,
  userNameMap: Map<number, string>
) => {
  return signature.actorNicknameSnapshot || userNameMap.get(signature.actorId) || `用户#${signature.actorId}`
}

export const formatSignatureSnapshotValue = (value?: string | number | null) => {
  if (value === undefined || value === null) return '旧版证据未记录'
  const text = String(value).trim()
  return text || '旧版证据未记录'
}

type DetailLifecycleTimelineDraft = DetailLifecycleTimelineItem & {
  sortValue: number
}

const appendLifecycleTimelineItem = (
  items: DetailLifecycleTimelineDraft[],
  item: Omit<DetailLifecycleTimelineItem, 'timeText'> & {
    occurredAt: DetailDateValue
    dateOnly?: boolean
  }
) => {
  const sortValue = getDetailDateSortValue(item.occurredAt)
  if (sortValue === undefined) {
    return
  }
  const timeText = item.dateOnly
    ? formatControlledFileDate(item.occurredAt)
    : formatControlledFileDateTime(item.occurredAt)
  if (timeText === '-') {
    return
  }
  items.push({
    key: item.key,
    categoryLabel: item.categoryLabel,
    title: item.title,
    timeText,
    description: item.description,
    actorText: item.actorText,
    tagType: item.tagType,
    sortValue
  })
}

export const buildDetailLifecycleTimelineItems = (
  file: ControlledFileVO | undefined,
  context: DetailLifecycleTimelineContext
): DetailLifecycleTimelineItem[] => {
  if (!file) {
    return []
  }
  const items: DetailLifecycleTimelineDraft[] = []
  const requesterName = userNameMapValue(context.userNameMap, file.requesterId)
  const fileVersionText = `版本 ${file.versionNo || '-'}`

  appendLifecycleTimelineItem(items, {
    key: 'file-submitted',
    categoryLabel: '提交',
    title: '提交申请',
    occurredAt: file.submittedTime,
    description: `${file.fileNumber || file.title || '受控文件'} ${fileVersionText} 已提交。`,
    actorText: requesterName,
    tagType: 'primary'
  })
  appendLifecycleTimelineItem(items, {
    key: 'file-rejected',
    categoryLabel: '审批',
    title: '流程驳回',
    occurredAt: file.rejectedTime,
    description: file.rejectReason || '审批流程已驳回。',
    actorText: requesterName,
    tagType: 'danger'
  })
  appendLifecycleTimelineItem(items, {
    key: 'file-approved',
    categoryLabel: '审批',
    title: '审批通过',
    occurredAt: file.approvedTime,
    description: '审批流程已完成。',
    tagType: 'success'
  })
  appendLifecycleTimelineItem(items, {
    key: 'external-review-closed',
    categoryLabel: '外审',
    title: '外来评审闭环',
    occurredAt: file.externalReview?.closedTime,
    description:
      file.externalReview?.conclusionComment ||
      file.externalReview?.reviewConclusion ||
      '外来文件评审已闭环。',
    tagType: 'info'
  })
  appendLifecycleTimelineItem(items, {
    key: 'file-stamped',
    categoryLabel: '发布',
    title: '受控副本生成',
    occurredAt: file.stampedTime,
    description: `${fileVersionText} 受控副本已生成。`,
    tagType: 'primary'
  })
  appendLifecycleTimelineItem(items, {
    key: 'file-published',
    categoryLabel: '发布',
    title: '正式发布',
    occurredAt: file.publishedTime,
    description: `${fileVersionText} 已发布。`,
    tagType: 'success'
  })
  appendLifecycleTimelineItem(items, {
    key: 'file-effective',
    categoryLabel: '生效',
    title: '版本生效',
    occurredAt: file.effectiveDate,
    dateOnly: true,
    description: `${fileVersionText} 生效。`,
    tagType: 'success'
  })

  for (const version of file.versionHistory ?? []) {
    if (version.id !== file.id) {
      appendLifecycleTimelineItem(items, {
        key: `version-${version.id}-published`,
        categoryLabel: '版本',
        title: `版本 ${version.versionNo || '-'} 发布`,
        occurredAt: version.publishedTime,
        description: `${version.fileNumber || version.title || '历史版本'}：${getDetailStatusLabel(version.status)}`,
        tagType: 'info'
      })
    }
    appendLifecycleTimelineItem(items, {
      key: `version-${version.id}-obsolete`,
      categoryLabel: '版本',
      title: `版本 ${version.versionNo || '-'} 作废`,
      occurredAt: version.obsoletedTime,
      description: version.remark || `${version.fileNumber || version.title || '历史版本'} 已作废。`,
      tagType: 'warning'
    })
  }

  for (const distribution of file.distributionStatuses ?? []) {
    const departmentName = context.deptNameMap.get(distribution.departmentId) || `部门#${distribution.departmentId}`
    appendLifecycleTimelineItem(items, {
      key: `distribution-${distribution.id}-issued`,
      categoryLabel: '分发',
      title: '文控发放',
      occurredAt: distribution.acknowledgedAt,
      description: `${departmentName} · ${getDistributionMediumLabel(distribution.distributionMedium)} · ${getDistributionStatusLabel(distribution.status)}`,
      actorText: getDistributionAckUserSummary(distribution, context.userNameMap),
      tagType: 'primary'
    })
    appendLifecycleTimelineItem(items, {
      key: `distribution-${distribution.id}-recovered`,
      categoryLabel: '分发',
      title: '纸质回收',
      occurredAt: distribution.recoveredAt,
      description: `${departmentName} · ${getDistributionMediumLabel(distribution.distributionMedium)} 已回收。`,
      actorText: distribution.recoveredBy
        ? userNameMapValue(context.userNameMap, distribution.recoveredBy)
        : undefined,
      tagType: 'warning'
    })
    for (const recipient of distribution.recipients ?? []) {
      const recipientName = userNameMapValue(context.userNameMap, recipient.userId)
      appendLifecycleTimelineItem(items, {
        key: `distribution-${distribution.id}-recipient-${recipient.id}-read`,
        categoryLabel: '分发',
        title: '接收人阅读',
        occurredAt: recipient.readAt,
        description: `${departmentName} · ${getDistributionMediumLabel(distribution.distributionMedium)}。`,
        actorText: recipientName,
        tagType: 'info'
      })
      appendLifecycleTimelineItem(items, {
        key: `distribution-${distribution.id}-recipient-${recipient.id}-acknowledged`,
        categoryLabel: '分发',
        title: '接收人签收',
        occurredAt: recipient.acknowledgedAt,
        description: recipient.ackComment || `${departmentName} 接收人已签收。`,
        actorText: recipientName,
        tagType: 'success'
      })
    }
  }

  for (const trainingStatus of file.trainingStatuses ?? []) {
    const departmentName =
      context.deptNameMap.get(trainingStatus.departmentId) || `部门#${trainingStatus.departmentId}`
    for (const assignment of trainingStatus.assignments ?? []) {
      appendLifecycleTimelineItem(items, {
        key: `training-${trainingStatus.id}-assignment-${assignment.id}`,
        categoryLabel: '培训',
        title: '培训确认',
        occurredAt: assignment.acknowledgedAt,
        description: `${departmentName} · ${formatTrainingProgressText(assignment.accumulatedViewSeconds, assignment.requiredViewSeconds)}`,
        actorText: userNameMapValue(context.userNameMap, assignment.userId),
        tagType: 'success'
      })
    }
  }

  for (const signature of file.signatureSummaries ?? []) {
    const actionLabel = getSignatureActionLabel(signature.taskActionResult)
    const meaningLabel = getSignatureMeaningLabel(signature.meaningCode)
    appendLifecycleTimelineItem(items, {
      key: `signature-${signature.id}`,
      categoryLabel: '签名',
      title: '电子签名',
      occurredAt: signature.signedAt,
      description: [meaningLabel, actionLabel, signature.comment].filter((item) => toText(item)).join(' · '),
      actorText: getSignatureActorSummary(signature, context.userNameMap),
      tagType: signature.taskActionResult === 'REJECTED' ? 'danger' : 'success'
    })
  }

  return items
    .sort((left, right) => right.sortValue - left.sortValue)
    .map(({ sortValue: _sortValue, ...item }) => item)
}

const userNameMapValue = (userNameMap: Map<number, string>, userId: number | null | undefined) => {
  if (!userId) {
    return undefined
  }
  return userNameMap.get(userId) || `用户#${userId}`
}
