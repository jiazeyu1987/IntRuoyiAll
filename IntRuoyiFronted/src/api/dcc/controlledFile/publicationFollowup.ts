import request from '@/config/axios'

export type DccBusinessId = string

export interface DccPublicationVisibilityUserVO {
  userId: DccBusinessId
  userName?: string
  deptId?: DccBusinessId
  deptName?: string
  userStatus?: number
  resolutionReason?: string
  assignmentScopeResult: string
}

export interface DccPublicationVisibilityRuleVO {
  id: DccBusinessId
  sourceType: string
  sourceRuleId: DccBusinessId
  sourceScope?: string
  sourceSummary: string
  resolutionStatus: string
  resolutionMessage?: string
  users: DccPublicationVisibilityUserVO[]
}

export interface DccPublicationNotificationDeliveryVO {
  id: DccBusinessId
  batchId: DccBusinessId
  userId: DccBusinessId
  userName?: string
  deptName?: string
  candidateResolutionStatus?: string
  status: 'PENDING' | 'SENT' | 'FAILED'
  attemptCount: number
  sentAt?: string
  systemMessageId?: DccBusinessId
  lastErrorSummary?: string
  rowVersion: number
  reasonSummaries: string[]
}

export interface DccPublicationImpactTaskVO {
  id: DccBusinessId
  batchId: DccBusinessId
  publishedControlledFileId: DccBusinessId
  relatedMasterId: DccBusinessId
  relatedActiveControlledFileId?: DccBusinessId
  relatedFileNumber?: string
  relatedFileName?: string
  relatedVersionNo?: string
  assigneeUserId?: DccBusinessId
  assigneeUserName?: string
  taskStatus: 'PENDING' | 'UNASSIGNED' | 'IN_REVIEW' | 'COMPLETED'
  decision?: 'NO_REVISION_REQUIRED' | 'REVISION_REQUIRED'
  decisionReason?: string
  revisionTrackingStatus: string
  linkedRevisionControlledFileId?: DccBusinessId
  linkedRevisionVersion?: string
  rowVersion: number
  relationDirections: Array<'FORWARD' | 'REVERSE'>
}

export interface DccPublicationImpactRevisionOptionVO {
  controlledFileId: DccBusinessId
  versionNo: string
  status: string
  currentActive: boolean
}

export interface DccPublicationImpactRevisionOptionsVO {
  sourceIterations: DccPublicationImpactRevisionOptionVO[]
  openMajorRevision?: DccPublicationImpactRevisionOptionVO | null
}

export interface DccPublicationTimelineEventVO {
  eventId: DccBusinessId
  sequenceNo: number
  sourceType: 'BATCH' | 'NOTIFICATION' | 'IMPACT'
  sourceLabel: string
  actionLabel: string
  occurredAt: string
  actorId?: DccBusinessId
  objectId: DccBusinessId
  objectLabel: string
  statusBeforeLabel?: string
  statusAfterLabel?: string
  decisionLabel?: string
  directionLabels: string[]
  reason?: string
  errorSummary?: string
  systemMessageId?: DccBusinessId
  attemptCount?: number
  assigneeBefore?: DccBusinessId
  assigneeAfter?: DccBusinessId
  linkedRevisionControlledFileId?: DccBusinessId
  linkedRevisionVersion?: string
}

export interface DccPublicationFollowupVO {
  id: DccBusinessId
  publishedControlledFileId: DccBusinessId
  fileNumber: string
  fileName: string
  versionNo: string
  status: string
  publishedAt: string
  visibilityRules: DccPublicationVisibilityRuleVO[]
  notificationDeliveries: DccPublicationNotificationDeliveryVO[]
  impactTasks: DccPublicationImpactTaskVO[]
  timeline: DccPublicationTimelineEventVO[]
}

export interface DccPageResult<T> {
  list: T[]
  total: number
}

export const getPublicationFollowupByFile = (controlledFileId: DccBusinessId) =>
  request.get<DccPublicationFollowupVO | null>({
    url: `/dcc/publication-followups/files/${controlledFileId}`,
    ignoreErrorMessage: true
  })

export const getMyImpactTaskPage = (params: {
  pageNo: number
  pageSize: number
  taskStatus?: string
  revisionTrackingStatus?: string
}) => request.get<DccPageResult<DccPublicationImpactTaskVO>>({
  url: '/dcc/publication-followups/my-impact-tasks', params, ignoreErrorMessage: true
})

export const getPublicationFollowupManagementPage = (params: {
  pageNo: number
  pageSize: number
  fileNumber?: string
  versionNo?: string
  batchStatus?: string
  taskStatus?: string
  notificationStatus?: string
  assigneeUserId?: DccBusinessId
}) => request.get<DccPageResult<DccPublicationFollowupVO>>({
  url: '/dcc/publication-followups/management-page', params, ignoreErrorMessage: true
})

export const retryPublicationNotification = (
  deliveryId: DccBusinessId,
  data: { expectedVersion: number; reason: string }
) => request.post({
  url: `/dcc/publication-followups/notification-deliveries/${deliveryId}/retry`, data,
  ignoreErrorMessage: true
})

export const startImpactTask = (taskId: DccBusinessId, expectedVersion: number) =>
  request.post({ url: `/dcc/publication-impact-tasks/${taskId}/start`, data: { expectedVersion },
    ignoreErrorMessage: true })

export const decideImpactTask = (
  taskId: DccBusinessId,
  data: { expectedVersion: number; decision: string; reason: string }
) => request.post({ url: `/dcc/publication-impact-tasks/${taskId}/decision`, data,
  ignoreErrorMessage: true })

export const createImpactRevision = (
  taskId: DccBusinessId,
  data: { expectedVersion: number; sourceControlledFileId: DccBusinessId; reason: string }
) => request.post<DccBusinessId>({ url: `/dcc/publication-impact-tasks/${taskId}/create-revision`, data,
  ignoreErrorMessage: true })

export const getImpactRevisionOptions = (taskId: DccBusinessId) =>
  request.get<DccPublicationImpactRevisionOptionsVO>({
    url: `/dcc/publication-impact-tasks/${taskId}/revision-options`,
    ignoreErrorMessage: true
  })

export const linkImpactRevision = (
  taskId: DccBusinessId,
  data: { expectedVersion: number; revisionControlledFileId: DccBusinessId; reason: string }
) => request.post({ url: `/dcc/publication-impact-tasks/${taskId}/link-revision`, data,
  ignoreErrorMessage: true })
