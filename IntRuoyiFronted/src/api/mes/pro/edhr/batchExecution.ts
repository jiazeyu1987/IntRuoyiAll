import request from '@/config/axios'
import { downloadByData } from '@/utils/filt'
import type { TableQuickFilterValue } from '@/hooks/web/useTableQuickFilter'

export const EDHR_BATCH_ARCHIVE_ARTIFACT_FINAL_PDF = 'BATCH_FINAL_PDF'
export const EDHR_BATCH_STATUS_CREATED = 0
export const EDHR_BATCH_STATUS_IN_PROGRESS = 10
export const EDHR_BATCH_STATUS_READY_TO_CLOSE = 20
export const EDHR_BATCH_STATUS_REWORK_REQUIRED = 25
export const EDHR_BATCH_STATUS_CLOSED = 30
export const EDHR_BATCH_STATUS_ARCHIVED = 40
export const EDHR_BATCH_STATUS_REJECTED = 50
export const EDHR_BATCH_STATUS_VOIDED = 60
export const EDHR_BATCH_TASK_STATUS_WAITING = 0
export const EDHR_BATCH_TASK_STATUS_DRAFT = 10
export const EDHR_BATCH_TASK_STATUS_SUBMITTED = 20
export const EDHR_BATCH_TASK_STATUS_REJECTED = 30
export const EDHR_BATCH_TASK_STATUS_REWORK_REQUIRED = 35
export const EDHR_BATCH_TASK_STATUS_APPROVED = 40
export const EDHR_BATCH_TASK_STATUS_SKIPPED = 45
export const EDHR_BATCH_TASK_STATUS_BLOCKED = 50

export const EDHR_BATCH_NODE_ROUTE_FORM = 'ROUTE_FORM'
export const EDHR_BATCH_NODE_INCOMING_INSPECTION_REPORT = 'INCOMING_INSPECTION_REPORT'
export const EDHR_BATCH_NODE_STERILIZATION_REPORT = 'STERILIZATION_REPORT'
export const EDHR_BATCH_NODE_FINISHED_PRODUCT_INSPECTION_REPORT =
  'FINISHED_PRODUCT_INSPECTION_REPORT'
export const EDHR_BATCH_NODE_FINISHED_PRODUCT_INSPECTION_RECORD =
  'FINISHED_PRODUCT_INSPECTION_RECORD'

export type EdhrRouteId = string | number
export type EdhrRecordCategory = 'BATCH_RECORD' | 'INTERNAL_RECORD'
export type EdhrValidationProfile = 'CONTROLLED_BATCH' | 'INTERNAL_TRACE'
export type EdhrBatchFormSlotType =
  | 'MAIN'
  | 'PROCESS_INSPECTION'
  | 'LOSS_REPORT'
  | 'PARAMETER_RECORD'
  | 'OTHER_INTERNAL'
export type EdhrBatchRequiredPolicy =
  | 'REQUIRED'
  | 'CONDITIONAL_REQUIRED'
  | 'OPTIONAL'
  | 'SKIPPABLE_CONTROLLED'
export type EdhrBatchOwnerRoleKey = 'PRODUCTION' | 'QUALITY' | 'EQUIPMENT' | 'QA' | 'ARCHIVE'
export type EdhrBatchArchiveVisibility =
  | 'FINAL_DHR'
  | 'INTERNAL_REVIEW'
  | 'AUDIT_ONLY'
  | 'ATTACHMENT_REFERENCE'

export interface EdhrSignatureTimeReqVO {
  selectedSignedAt?: string
  selectedTimeZone?: string
  selectedTimeReason?: string
}

export interface EdhrBatchExecutionPageReqVO extends PageParam {
  batchExecutionCode?: string
  workOrderCode?: string
  batchCode?: string
  productCode?: string
  routeCode?: string
  status?: number
  excludeStatuses?: number[]
  excludeReleased?: boolean
  completedTraceOnly?: boolean
  createTime?: string[]
  quickFilter?: TableQuickFilterValue
}

export interface EdhrBatchExecutionOpenOrCreateReqVO {
  workOrderId: number
  batchCode: string
  routeId?: number
  remark?: string
}

export interface EdhrBatchExecutionRouteOptionRespVO {
  routeId: number
  routeCode?: string
  routeName?: string
  batchRouteEnabled?: boolean
}

export type EdhrLocalStateSampleState =
  | 'CLOSE'
  | 'PRECHECK'
  | 'RELEASE_APPROVAL'
  | 'ARCHIVE'
  | 'ARCHIVED'
  | 'QUALITY_TERMINAL'

export interface EdhrLocalStateSampleReqVO {
  state: EdhrLocalStateSampleState
}

export interface EdhrLocalStateSampleRespVO {
  batchExecutionId: number
  batchExecutionCode?: string
  sampleState: EdhrLocalStateSampleState
  detailPath?: string
  routeQuery?: Record<string, string>
}

export interface EdhrRehearsalReadinessReqVO {
  routeId: number
  executorUserId: number
  approverUserId: number
  archiverUserId: number
}

export interface EdhrRehearsalReadinessItem {
  code?: string
  status?: 'PASS' | 'BLOCKER' | string
  severity?: 'INFO' | 'BLOCKER' | string
  roleKey?: string
  subjectId?: number
  message?: string
  suggestion?: string
}

export interface EdhrRehearsalReadinessResult {
  overallStatus?: 'PASS' | 'BLOCKED' | string
  items?: EdhrRehearsalReadinessItem[]
}

export interface EdhrBatchExecutionTaskOpenReqVO {
  batchExecutionId: EdhrRouteId
  taskId: EdhrRouteId
  workTaskId?: EdhrRouteId
}

export interface EdhrBatchExecutionSpecialNodeSkipReqVO {
  taskId: number
  reason: string
  password: string
  attachments?: EdhrBatchSpecialNodeAttachment[]
}

export interface EdhrBatchExecutionSpecialNodeCompleteReqVO {
  taskId: number
  sterilizationBatchNo?: string
  attachments?: EdhrBatchSpecialNodeAttachment[]
}

export interface EdhrBatchExecutionSpecialNodeAttachmentDeletePendingReqVO {
  taskId: number
  attachment: EdhrBatchSpecialNodeAttachment
}

export interface EdhrBatchSpecialNodeAttachment {
  uploadToken: string
  fileId: number
  fileUrl: string
  storageConfigId: number
  storagePath: string
  fileName: string
  contentType: string
  fileSize: number
  sha256: string
  storageRetentionJson: string
  storageRetentionHash: string
}

export interface EdhrBatchSpecialNodeAttachmentPrepareUploadReqVO {
  taskId: number
  file: File | Blob
}

export interface EdhrBatchExecutionTaskOpenRespVO {
  taskId: number
  executionId: number
  workTaskId?: number
  routeProcessId?: number
  batchRecordReportId?: string
  formBindingKey?: string
  formTemplateId?: number
  formTemplateName?: string
  formTemplateVersionId?: number
  formTemplateVersionNo?: string
  formCenterInstanceId?: number
  recordCategory?: EdhrRecordCategory
  validationProfile?: EdhrValidationProfile
  permissionScopeId?: number | null
  routeBindingId?: number
  routeBindingSnapshotHash?: string
  batchRecordSort?: number
  instanceScope?: 'PROCESS' | 'BATCH_SHARED' | string
  sharedFormKey?: string
  fillableScopeJson?: string
  executionMode?: 'SEQUENTIAL' | 'PARALLEL'
  status?: number
  executionPageQuery?: Record<string, string | number | null | undefined>
}

export interface EdhrBatchExecutionTaskFillableUserRespVO {
  userId: number
  displayName?: string
}

export interface EdhrBatchExecutionCurrentProcessFillerRespVO {
  userId: number
  displayName?: string
}

export interface EdhrBatchExecutionCloseReqVO {
  id: EdhrRouteId
  comment: string
  password: string
  signatureTime?: EdhrSignatureTimeReqVO
}

export interface EdhrBatchExecutionQualityRejectReqVO {
  id: EdhrRouteId
  reason: string
  password: string
  signatureTime?: EdhrSignatureTimeReqVO
}

export interface EdhrBatchExecutionReexecuteReqVO {
  sourceRejectedBatchExecutionId: EdhrRouteId
  reason: string
  remark?: string
}

export interface EdhrBatchExecutionArchiveGenerateReqVO {
  batchExecutionId: EdhrRouteId
  artifactType: string
  workTaskId: EdhrRouteId
  signatureTime?: EdhrSignatureTimeReqVO
}

export interface EdhrBatchExecutionArchiveRespVO {
  id: number
  batchExecutionId: number
  artifactType?: string
  archiveVersion?: number
  archiveStatus?: string
  fileName?: string
  fileSize?: number
  contentHash?: string
  sourceManifestJson?: string
  generatedAt?: string
  canDownloadArchive?: boolean
  failureReason?: string
}

export interface EdhrBatchExecutionTaskRespVO {
  id: number
  batchExecutionId?: number
  nodeType?: string
  routeProcessId?: number
  routeProcessSort?: number
  processCode?: string
  processName?: string
  batchRecordReportId?: string
  batchRecordReportCode?: string
  batchRecordReportName?: string
  batchRecordVersionNo?: string
  formSlotType?: EdhrBatchFormSlotType
  formBindingKey?: string
  formTemplateId?: number
  formTemplateName?: string
  formTemplateVersionId?: number
  formTemplateVersionNo?: string
  formCenterInstanceId?: number
  recordCategory?: EdhrRecordCategory
  validationProfile?: EdhrValidationProfile
  requiredPolicy?: EdhrBatchRequiredPolicy
  ownerRoleKey?: EdhrBatchOwnerRoleKey
  archiveVisibility?: EdhrBatchArchiveVisibility
  permissionScopeId?: number | null
  slotConfigSnapshotHash?: string | null
  slotBlockerMessage?: string | null
  routeBindingId?: number
  routeBindingSnapshotHash?: string
  batchRecordSort?: number
  instanceScope?: 'PROCESS' | 'BATCH_SHARED' | string
  sharedFormKey?: string
  fillableScopeJson?: string
  executionMode?: 'SEQUENTIAL' | 'PARALLEL'
  available?: boolean
  gateMessage?: string
  currentUserRole?: string
  allowedActions?: string[]
  disabledReason?: string
  activeWorkTaskId?: number
  activeWorkTaskType?: string
  activeWorkTaskActionUrl?: string
  executionId?: number
  executionCode?: string
  status?: number
  requiredFlag?: boolean
  signatureStatus?: string
  approvalStatus?: string
  auditChainStatus?: string
  blockerCode?: string
  blockerMessage?: string
  canOpen?: boolean
  canViewSignature?: boolean
  canViewAudit?: boolean
  canViewApproval?: boolean
  canViewArchive?: boolean
  skippedBy?: number
  skippedAt?: string
  specialPayloadJson?: string
  pendingSpecialNodeAttachments?: EdhrBatchSpecialNodeAttachment[]
  fillableUsers?: EdhrBatchExecutionTaskFillableUserRespVO[]
}

export interface EdhrBatchExecutionRespVO {
  id: number
  batchExecutionCode?: string
  workOrderId?: number
  workOrderCode?: string
  batchCode?: string
  attemptNo?: number
  sourceRejectedBatchExecutionId?: number
  supersededByBatchExecutionId?: number
  reexecutedByChangeEventId?: number
  productId?: number
  productCode?: string
  productName?: string
  routeId?: number
  routeVersionId?: number
  routeVersionNo?: string
  routeCode?: string
  routeName?: string
  currentProcessRouteProcessId?: number
  currentProcessCode?: string
  currentProcessName?: string
  currentProcessProductionFillers?: EdhrBatchExecutionCurrentProcessFillerRespVO[]
  currentProcessEquipmentFillers?: EdhrBatchExecutionCurrentProcessFillerRespVO[]
  currentProcessQualityFillers?: EdhrBatchExecutionCurrentProcessFillerRespVO[]
  status?: number
  taskTotal?: number
  taskApprovedCount?: number
  blockedCount?: number
  mainStage?: string
  mainStageLabel?: string
  stageOwnerRole?: string
  stageBlockers?: string[]
  canClose?: boolean
  canArchive?: boolean
  closeBlockers?: string[]
  releaseActionLocked?: boolean
  releaseActionLockReason?: string
  pendingVoidChangeEventId?: number
  pendingVoidChangeCode?: string
  pendingVoidChangeStatus?: string
  pendingVoidProcessInstanceId?: string
  pendingVoidRequestedBy?: number
  pendingVoidRequestedAt?: string
  canWithdrawVoidRequest?: boolean
  closedBy?: number
  closedAt?: string
  closeSignatureId?: number
  rejectSignatureId?: number
  rejectedBy?: number
  rejectedAt?: string
  rejectReason?: string
  aggregateHash?: string
  createTime?: string
  updateTime?: string
  tasks?: EdhrBatchExecutionTaskRespVO[]
}

export interface EdhrBatchWorkbenchRespVO {
  batchExecutionId: number
  batchExecutionCode?: string
  workOrderCode?: string
  batchCode?: string
  productName?: string
  productCode?: string
  routeName?: string
  routeCode?: string
  batchStatus?: number
  mainStage?: string
  mainStageLabel?: string
  stageOwnerRole?: string
  requiredProgress?: number
  blockedCount?: number
  stageBlockers?: string[]
  taskSummary?: {
    totalCount?: number
    approvedCount?: number
    submittedCount?: number
    reworkCount?: number
    blockedCount?: number
  }
  releaseSummary?: {
    releaseTransactionId?: number
    releaseStatus?: string
    releaseStatusLabel?: string
    blockingCheckCount?: number
    failedCheckCount?: number
    precheckSummary?: string
    lastPrecheckAt?: string
  }
  auditSummary?: {
    latestOperationAuditId?: number
    latestOperationAt?: string
    fieldAuditBatchCount?: number
    latestFieldAuditAt?: string
    latestDomainTraceAt?: string | null
  }
}

export interface EdhrBatchReviewTimelineRespVO {
  batchExecutionId: number
  batchEvents?: EdhrBatchExecutionReviewBatchEvent[]
  taskEvents?: EdhrBatchExecutionReviewTaskEvent[]
  signatureRecords?: EdhrBatchExecutionReviewSignatureRecord[]
  approvalRecords?: EdhrBatchExecutionReviewApprovalRecord[]
  flowEvents?: EdhrBatchExecutionReviewFlowEvent[]
  archiveVersions?: EdhrBatchExecutionArchiveRespVO[]
  dossierItems?: EdhrBatchExecutionDossierItemRespVO[]
  executionReviews?: EdhrBatchExecutionReviewExecutionRespVO[]
  [key: string]: unknown
}

export interface EdhrBatchExecutionReviewBatchEvent {
  batchExecutionId?: number
  batchExecutionCode?: string
  status?: number
  aggregateHash?: string
  closedBy?: number
  closedAt?: string | number
  closeSignatureId?: number
  rejectSignatureId?: number
  rejectedBy?: number
  rejectedAt?: string | number
  rejectReason?: string
  createTime?: string | number
}

export interface EdhrBatchExecutionReviewTaskEvent {
  taskId?: number
  routeProcessSort?: number
  processCode?: string
  processName?: string
  batchRecordReportId?: string
  batchRecordReportCode?: string
  batchRecordReportName?: string
  formSlotType?: EdhrBatchFormSlotType
  formBindingKey?: string
  formTemplateId?: number
  formTemplateName?: string
  formTemplateVersionId?: number
  formTemplateVersionNo?: string
  formCenterInstanceId?: number
  recordCategory?: EdhrRecordCategory
  validationProfile?: EdhrValidationProfile
  requiredPolicy?: EdhrBatchRequiredPolicy
  ownerRoleKey?: EdhrBatchOwnerRoleKey
  archiveVisibility?: EdhrBatchArchiveVisibility
  permissionScopeId?: number | null
  slotConfigSnapshotHash?: string | null
  slotBlockerMessage?: string | null
  executionId?: number
  status?: number
  closedAt?: string | number
  blockerCode?: string
  blockerMessage?: string
  skippedBy?: number
  skippedAt?: string
  specialPayloadJson?: string
  openedAt?: string
  submittedAt?: string
  approvedAt?: string
}

export interface EdhrBatchExecutionReviewFlowEvent {
  id?: number
  interventionId?: number
  taskId?: string
  nodeKey?: string
  eventType?: string
  action?: string
  fromStatus?: string
  toStatus?: string
  actorUserId?: number
  targetUserId?: number
  permissionCode?: string
  permissionDecision?: string
  reason?: string
  signoffEvidenceHash?: string
  integrityCheckResult?: string
  eventSnapshotJson?: string
  evidenceHash?: string
  occurredAt?: string
}

export interface EdhrBatchExecutionDossierItemRespVO {
  id?: number
  itemType?: string
  itemKey?: string
  itemName?: string
  requiredFlag?: boolean
  itemStatus?: string
  sourceDocType?: string
  sourceDocId?: number
  sourceDocCode?: string
  sourceDocStatus?: string
  sourceDocResult?: string
  sourceDocHash?: string
  completedAt?: string
  verifiedAt?: string
}

export interface EdhrBatchExecutionReviewSignatureRecord {
  id?: number
  executionId?: number
  executionCode?: string
  actorId?: number
  actorName?: string
  actionType?: string
  signatureMode?: string
  passwordVerified?: boolean
  comment?: string
  aggregateHash?: string
  signedAt?: string
  selectedSignedAt?: string
  signatureDisplayAt?: string
  signatureTimeMode?: string
  selectedTimeZone?: string
  selectedTimeReason?: string
  selectedTimePolicyVersion?: string
  selectedTimeAuditHash?: string
}

export interface EdhrSignatureCellMarker {
  rowIndex: number
  columnIndex: number
  enabled?: boolean
  actionType?: string
  label?: string
  displayFormat?: string
  reviewSourceType?: 'POST' | 'ROLE' | 'USER' | 'DEPT' | 'ROLES' | 'USERS' | 'DEPTS'
  reviewSourceId?: number
  reviewSourceIds?: number[]
  reviewSourceName?: string
}

export interface EdhrBatchExecutionReviewFormViewModel {
  sheetLayoutJson?: string
  metaJson?: string
  executionSnapshotJson?: string
  cellValuesJson?: string
  remark?: string
  signatureCellMarkers?: EdhrSignatureCellMarker[]
}

export interface EdhrBatchExecutionTaskPreviewRespVO {
  batchExecutionId?: number
  taskId?: number
  executionId?: number
  taskStatus?: number
  executionCreated?: boolean
  formViewModel?: EdhrBatchExecutionReviewFormViewModel
}

export interface EdhrBatchExecutionReviewFieldAuditSummary {
  batchCount?: number
  revision?: number
  lastBatchId?: number
  headHash?: string
}

export interface EdhrBatchExecutionReviewSignatureSummary {
  totalCount?: number
  fieldChangeCount?: number
  formReviewCount?: number
  submitCount?: number
  approveCount?: number
  lastSignedAt?: string
}

export interface EdhrBatchExecutionReviewApprovalRecord {
  executionId?: number
  executionCode?: string
  processCode?: string
  processName?: string
  actorName?: string
  comment?: string
  bpmTaskId?: string
  bpmTaskName?: string
  approvalResult?: string
  signedAt?: string
}

export interface EdhrBatchExecutionReviewApprovalSummary {
  processInstanceId?: string
  approvalSnapshotStatus?: string
  currentBpmTaskId?: string
  approvedRecord?: EdhrBatchExecutionReviewApprovalRecord
}

export interface EdhrBatchExecutionReviewDomainTraceSummary {
  snapshotId?: number
  status?: string
  snapshotHash?: string
  verifiedAt?: string
}

export interface EdhrBatchExecutionReviewAttachmentSummary {
  id?: number
  executionId?: number
  batchTaskId?: number
  workTaskId?: number
  rowIndex?: number
  columnIndex?: number
  fieldKey?: string
  fieldPath?: string
  fieldLabel?: string
  attachmentType?: string
  attachmentGroupKey?: string
  attachmentAction?: string
  versionNo?: number
  fileId?: number
  fileUrl?: string
  storageConfigId?: number
  storagePath?: string
  fileName?: string
  contentType?: string
  fileSize?: number
  sha256?: string
  storageRetentionHash?: string
  auditBatchId?: number
  signatureId?: number
  attachmentHash?: string
  operatorId?: number
  operatorName?: string
  operatedAt?: string
}

export interface EdhrBatchExecutionReviewExecutionRespVO {
  taskId: number
  routeProcessId?: number
  routeProcessSort?: number
  processCode?: string
  processName?: string
  batchRecordReportId?: string
  batchRecordReportCode?: string
  batchRecordReportName?: string
  formSlotType?: EdhrBatchFormSlotType
  formBindingKey?: string
  formTemplateId?: number
  formTemplateName?: string
  formTemplateVersionId?: number
  formTemplateVersionNo?: string
  formCenterInstanceId?: number
  recordCategory?: EdhrRecordCategory
  validationProfile?: EdhrValidationProfile
  requiredPolicy?: EdhrBatchRequiredPolicy
  ownerRoleKey?: EdhrBatchOwnerRoleKey
  archiveVisibility?: EdhrBatchArchiveVisibility
  permissionScopeId?: number | null
  slotConfigSnapshotHash?: string | null
  slotBlockerMessage?: string | null
  executionId?: number
  executionCode?: string
  status?: number
  submittedAt?: string
  approvedAt?: string
  closedAt?: string
  formViewModel?: EdhrBatchExecutionReviewFormViewModel
  fieldAuditSummary?: EdhrBatchExecutionReviewFieldAuditSummary
  signatureSummary?: EdhrBatchExecutionReviewSignatureSummary
  signatureRecords?: EdhrBatchExecutionReviewSignatureRecord[]
  approvalSummary?: EdhrBatchExecutionReviewApprovalSummary
  domainTraceSummary?: EdhrBatchExecutionReviewDomainTraceSummary
  attachmentCount?: number
  attachmentSummaries?: EdhrBatchExecutionReviewAttachmentSummary[]
}

const BATCH_EXECUTION_BASE_URL = '/mes/pro/edhr-batch-execution'
const BATCH_ARCHIVE_BASE_URL = '/mes/pro/edhr-batch-execution-archive'

const resolveBatchArchiveDownloadName = (
  batchExecutionId: number,
  fallbackFileName?: string,
  artifactType?: string
) => {
  const fileName = fallbackFileName?.trim()
  if (fileName) return fileName
  const suffix = (artifactType || '').toUpperCase().includes('PDF') ? 'pdf' : 'bin'
  return `edhr-batch-archive-${batchExecutionId}.${suffix}`
}

export const getEdhrBatchExecutionPage = async (params: EdhrBatchExecutionPageReqVO) => {
  return await request.get<PageResult<EdhrBatchExecutionRespVO[]>>({
    url: `${BATCH_EXECUTION_BASE_URL}/page`,
    params
  })
}

export const getEdhrBatchExecution = async (id: EdhrRouteId) => {
  return await request.get<EdhrBatchExecutionRespVO>({
    url: `${BATCH_EXECUTION_BASE_URL}/get?id=${id}`
  })
}

export const getEdhrBatchWorkbench = async (id: EdhrRouteId) => {
  return await request.get<EdhrBatchWorkbenchRespVO>({
    url: `${BATCH_EXECUTION_BASE_URL}/workbench`,
    params: { id }
  })
}

export const getEdhrBatchExecutionRouteOptions = async (workOrderId: number) => {
  return await request.get<EdhrBatchExecutionRouteOptionRespVO[]>({
    url: `${BATCH_EXECUTION_BASE_URL}/work-order-route-options`,
    params: { workOrderId }
  })
}

export const openOrCreateEdhrBatchExecution = async (
  data: EdhrBatchExecutionOpenOrCreateReqVO
) => {
  return await request.post<EdhrBatchExecutionRespVO>({
    url: `${BATCH_EXECUTION_BASE_URL}/open-or-create`,
    data
  })
}

export const createEdhrLocalStateSample = async (data: EdhrLocalStateSampleReqVO) => {
  return await request.post<EdhrLocalStateSampleRespVO>({
    url: `${BATCH_EXECUTION_BASE_URL}/local-state-sample`,
    data
  })
}

export const getEdhrRehearsalReadiness = async (params: EdhrRehearsalReadinessReqVO) => {
  return await request.get<EdhrRehearsalReadinessResult>({
    url: `${BATCH_EXECUTION_BASE_URL}/rehearsal-readiness`,
    params
  })
}

export const openEdhrBatchTask = async (data: EdhrBatchExecutionTaskOpenReqVO) => {
  return await request.post<EdhrBatchExecutionTaskOpenRespVO>({
    url: `${BATCH_EXECUTION_BASE_URL}/task/open`,
    data
  })
}

export const getEdhrBatchTaskPreview = async (
  batchExecutionId: EdhrRouteId,
  taskId: EdhrRouteId
) => {
  return await request.get<EdhrBatchExecutionTaskPreviewRespVO>({
    url: `${BATCH_EXECUTION_BASE_URL}/task/preview`,
    params: { batchExecutionId, taskId }
  })
}

export const skipEdhrBatchSpecialNode = async (
  data: EdhrBatchExecutionSpecialNodeSkipReqVO
) => {
  return await request.post<EdhrBatchExecutionRespVO>({
    url: `${BATCH_EXECUTION_BASE_URL}/task/special-node/skip`,
    data
  })
}

export const completeEdhrBatchSpecialNode = async (
  data: EdhrBatchExecutionSpecialNodeCompleteReqVO
) => {
  return await request.post<EdhrBatchExecutionRespVO>({
    url: `${BATCH_EXECUTION_BASE_URL}/task/special-node/complete`,
    data
  })
}

interface EdhrBatchSpecialNodeAttachmentPrepareUploadApiResp {
  data: EdhrBatchSpecialNodeAttachment
}

export const prepareEdhrBatchSpecialNodeAttachmentUpload = async (
  data: EdhrBatchSpecialNodeAttachmentPrepareUploadReqVO,
  onUploadProgress?: Function
) => {
  const formData = new FormData()
  formData.append('taskId', String(data.taskId))
  formData.append('file', data.file)
  const response = await request.upload<EdhrBatchSpecialNodeAttachmentPrepareUploadApiResp>({
    url: `${BATCH_EXECUTION_BASE_URL}/task/special-node/attachment/prepare-upload`,
    data: formData,
    onUploadProgress
  })
  if (!response.data) {
    throw new Error('特殊节点附件预登记响应缺少 data，不能写入批次归档。')
  }
  return response.data
}

export const deleteEdhrBatchSpecialNodePendingAttachment = async (
  data: EdhrBatchExecutionSpecialNodeAttachmentDeletePendingReqVO
) => {
  return await request.post<boolean>({
    url: `${BATCH_EXECUTION_BASE_URL}/task/special-node/attachment/delete-pending`,
    data
  })
}

export const savePendingEdhrBatchSpecialNodeAttachments = async (batchExecutionId: EdhrRouteId) => {
  return await request.post<EdhrBatchExecutionRespVO>({
    url: `${BATCH_EXECUTION_BASE_URL}/task/special-node/attachment/save-pending`,
    data: { batchExecutionId }
  })
}

export const syncEdhrBatchExecutionStatus = async (id: EdhrRouteId) => {
  return await request.post<EdhrBatchExecutionRespVO>({
    url: `${BATCH_EXECUTION_BASE_URL}/sync-status?id=${id}`
  })
}

export const closeEdhrBatchExecution = async (data: EdhrBatchExecutionCloseReqVO) => {
  return await request.post<EdhrBatchExecutionRespVO>({
    url: `${BATCH_EXECUTION_BASE_URL}/close`,
    data
  })
}

export const qualityRejectEdhrBatchExecution = async (data: EdhrBatchExecutionQualityRejectReqVO) => {
  return await request.post<EdhrBatchExecutionRespVO>({
    url: `${BATCH_EXECUTION_BASE_URL}/quality-reject`,
    data
  })
}

export const reexecuteRejectedEdhrBatchExecution = async (data: EdhrBatchExecutionReexecuteReqVO) => {
  return await request.post<EdhrBatchExecutionRespVO>({
    url: `${BATCH_EXECUTION_BASE_URL}/reexecute-rejected-batch`,
    data
  })
}

export const getEdhrBatchReviewTimeline = async (id: EdhrRouteId) => {
  return await request.get<EdhrBatchReviewTimelineRespVO>({
    url: `${BATCH_EXECUTION_BASE_URL}/review-timeline`,
    params: { id }
  })
}

export const generateEdhrBatchArchive = async (data: EdhrBatchExecutionArchiveGenerateReqVO) => {
  return await request.post<EdhrBatchExecutionArchiveRespVO>({
    url: `${BATCH_ARCHIVE_BASE_URL}/generate`,
    data
  })
}

export const getLatestEdhrBatchArchive = async (batchExecutionId: EdhrRouteId) => {
  return await request.get<EdhrBatchExecutionArchiveRespVO>({
    url: `${BATCH_ARCHIVE_BASE_URL}/latest`,
    params: { batchExecutionId }
  })
}

export const downloadEdhrBatchArchive = async (
  archiveId: number,
  fallbackFileName?: string,
  artifactType?: string,
  contentType?: string
) => {
  const blob = await request.download<Blob>({
    url: `${BATCH_ARCHIVE_BASE_URL}/download?id=${archiveId}`
  })
  downloadByData(
    blob,
    resolveBatchArchiveDownloadName(archiveId, fallbackFileName, artifactType),
    contentType || blob.type || 'application/octet-stream'
  )
}

export const printEdhrBatchArchive = async (archiveId: number, fallbackFileName?: string) => {
  const blob = await request.download<Blob>({
    url: `${BATCH_ARCHIVE_BASE_URL}/download?id=${archiveId}`
  })
  const objectUrl = URL.createObjectURL(blob)
  const printWindow = window.open(objectUrl, '_blank')
  if (!printWindow) {
    URL.revokeObjectURL(objectUrl)
    throw new Error(`${fallbackFileName || '批次最终归档'} 打印窗口打开失败。`)
  }
  printWindow.addEventListener('load', () => {
    printWindow.focus()
    printWindow.print()
    URL.revokeObjectURL(objectUrl)
  })
}
