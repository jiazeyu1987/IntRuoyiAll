import axios, { AxiosError, type AxiosResponse } from 'axios'
import { ElMessageBox } from 'element-plus'
import request from '@/config/axios'
import { config as axiosConfig } from '@/config/axios/config'
import { getAccessToken, getTenantId, getVisitTenantId } from '@/utils/auth'
import { downloadByData } from '@/utils/filt'
import type { TableQuickFilterValue } from '@/hooks/web/useTableQuickFilter'
import type { FormInstanceVO } from '@/api/form-center/instance'

export type ControlledFileChangeType = 'NEW' | 'REVISION' | 'OBSOLETE'

export const DCC_CONTROLLED_FILE_ACTIONS = [
  'VIEW',
  'PREVIEW',
  'DOWNLOAD',
  'PRINT',
  'WITHDRAW',
  'OBSOLETE',
  'PUBLISH',
  'MANUAL_RELEASE',
  'DELETE_WITHDRAWN_FLOW',
  'RESUBMIT_WITHDRAWN_FLOW',
  'UPLOAD_TRAINING_RECORD',
  'ACKNOWLEDGE_TRAINING',
  'RETRY_FINALIZATION'
] as const

export type DccControlledFileAction = (typeof DCC_CONTROLLED_FILE_ACTIONS)[number]
export type DccRouteId = string | number

export interface DccControlledFileActionProjectionVO {
  actionLocked: boolean
  actionLockReason?: string | null
  allowedActions: DccControlledFileAction[]
  canWithdraw: boolean
  pendingRequestId?: number | null
  pendingVersionNo?: string | null
}

export interface ControlledFileSubmitReqVO {
  categoryId: number
  directoryId: number
  sessionId: string
  originalUploadTicket: string
  sourceUploadTicket?: string
  sourceFileName?: string
  drawingPdfUploadTicket?: string
  fileName: string
  fileNumber: string
  productMasterId?: null
  productCode?: string
  dccProjectCodeId?: number | null
  fileTypeTaxonomyId?: number | null
  revisionTargetControlledFileId?: number | null
  needTraining: boolean
  selectedSignoffUserIds?: number[]
  processType?: string
  changeType: ControlledFileChangeType
  versionNo: string
  effectiveDate: string
  remark?: string
}

export interface ControlledFileMetadataUpdateReqVO {
  assignmentId?: number
  changeReason?: string
  productMasterId?: null
  productName?: string
  dccProjectCodeId?: number | null
  needTraining: boolean
  fileTypeTaxonomyId?: number | null
  fileTypeLevel1?: string | null
  fileTypeLevel2?: string | null
  fileTypeLevel3?: string | null
  fileTypeLevel4?: string | null
  fileTypeLevel5?: string | null
  fileName: string
  productCode?: string
  fileNumber?: string | null
  categoryId: number
  directoryId?: number | null
}

export interface ControlledFileProjectCodeRecognitionRespVO {
  controlledFileId: number
  recognitionStatus:
    | 'SUCCESS'
    | 'NO_MATCH'
    | 'UNKNOWN_DCC_BASIC_DATA'
    | 'UNRECOGNIZED_PROJECT_NAME'
  dccProjectCodeId?: number | null
  projectName?: string | null
  projectCode?: string | null
  matchType?: string | null
  matchText?: string | null
}

export interface ExternalFileReviewSubmitReqVO extends ControlledFileSubmitReqVO {
  externalSource: string
  externalOwner: string
  reviewReason: string
  participantUserIds: number[]
}

export interface ControlledFileWithdrawReqVO {
  reason: string
}

export interface ControlledFileTrainingRecordReqVO {
  sessionId: string
  trainingRecordUploadTicket: string
}

export type ControlledFilePreviewKind =
  | 'PDF'
  | 'IMAGE'
  | 'TEXT'
  | 'OFFICE'
  | 'VIDEO'
  | 'AUDIO'
  | 'DOWNLOAD_ONLY'

export interface ControlledFileUploadRespVO {
  uploadTicket: string
  sessionId: string
  requestId: string
  fileName: string
  contentType: string
  previewKind?: ControlledFilePreviewKind
  onlyofficeBaseUrl?: string
  onlyofficeDocumentUrl?: string
  previewUnavailableReason?: string
  fileSize: number
  expireTime?: number
  watermarkTraceCode?: string | null
  watermark?: ControlledPreviewWatermark | null
}

export type UploadPreviewPurpose =
  | 'SOURCE'
  | 'DRAWING_PDF'
  | 'TRAINING_RECORD'
  | 'EXTERNAL_REVIEW_OUTPUT'

export interface ControlledFileUploadTemporaryStatusRespVO {
  requestId: string
  temporaryFileCount: number
  bindable: boolean
  sessionId?: string
  purpose?: string
  status?: string
  expireTime?: number
  cleanupStatus?: string
  cleanupReason?: string
  cleanupTime?: number
  cleanedCount?: number
}

export interface ControlledFileUploadPreviewContext {
  categoryId: number
  sessionId: string
}

export interface ControlledFilePreviewMetadataVO {
  previewKind: ControlledFilePreviewKind
  fileName: string
  contentType: string
  onlyofficeBaseUrl?: string
  onlyofficeDocumentUrl?: string
  previewUnavailableReason?: string
  viewerToken: string
  viewerTokenId: string
  viewerTokenNonce: string
  accessEventCode: string
  watermarkTraceCode: string
  watermark: ControlledPreviewWatermark
}

export interface ControlledFileUploadNameOptionVO {
  fileName: string
  currentVersionNo?: string | null
  controlledFileId?: number | null
  fileNumber?: string | null
}

export interface ControlledFileCurrentVersionRespVO {
  fileNumber: string
  matched: boolean
  currentControlledFileId?: number | null
  masterId?: number | null
  fileName?: string | null
  currentVersionNo?: string | null
  status?: string | null
  categoryId?: number | null
  directoryId?: number | null
  originalFileId?: number | null
  originalFileName?: string | null
  originalFilePath?: string | null
  sourceFileId?: number | null
  sourceFileName?: string | null
  sourceFilePath?: string | null
  publishedFileId?: number | null
  publishedFileName?: string | null
  publishedFilePath?: string | null
  stampedFileId?: number | null
  stampedFileName?: string | null
  stampedFilePath?: string | null
  productMasterId?: number | null
  productCode?: string | null
  productName?: string | null
  dccProjectCodeId?: number | null
  fileTypeTaxonomyId?: number | null
  fileTypeLevel1?: string | null
  fileTypeLevel2?: string | null
  fileTypeLevel3?: string | null
  fileTypeLevel4?: string | null
  fileTypeLevel5?: string | null
  modifying?: boolean | null
  actionProjection?: DccControlledFileActionProjectionVO | null
}

export interface ControlledFileUploadDirectoryNodeVO {
  id: number
  name: string
  leaf: boolean
  children?: ControlledFileUploadDirectoryNodeVO[]
}

export interface ControlledFileUploadDirectoryTreeVO {
  bindingDirectoryId: number
  bindingDirectoryPath: string
  leafBinding: boolean
  defaultUnclassified: boolean
  children: ControlledFileUploadDirectoryNodeVO[]
}

export interface ControlledPreviewWatermarkOverlay {
  textColor: string
  opacity: number
  rotationDeg: number
  gapX: number
  gapY: number
  fontSize: number
}

export interface ControlledPreviewWatermark {
  label: string
  text: string
  actorName: string
  actorAccount: string
  timestamp: string
  purpose: string
  traceCode?: string | null
  overlay: ControlledPreviewWatermarkOverlay
}

export interface ControlledFilePreviewWithWatermark {
  blob: Blob
  watermark: ControlledPreviewWatermark
}

export interface ControlledFileDownloadEvidence {
  downloadRequestId: string
  accessEventCode: string
  plainSha256: string
}

export interface ControlledFileDownloadResult {
  blob: Blob
  fileName: string
  evidence: ControlledFileDownloadEvidence
}

export interface ControlledFileDownloadOptions {
  nonControlledWarningConfirmed?: boolean
}

export interface ControlledFilePrintCreateReqVO {
  purpose: string
  copies: number
  receivingDepartment: string
  useLocation: string
}

export interface ControlledFilePrintRecordVO {
  id: number
  controlledFileId: number
  fileNumber: string
  versionNo: string
  printNo: string
  purpose: string
  copies: number
  receivingDepartment: string
  useLocation: string
  printUserId: number
  printUserName?: string | null
  printTime: number
  approvalStatus: string
  approvalUserId?: number | null
  approvalUserName?: string | null
  approvalTime?: number | null
}

export interface ControlledFilePrintHtmlVO {
  printRecordId: number
  printNo: string
  html: string
}

export interface ControlledFileRoutePreviewReqVO {
  categoryId: number
  selectedSignoffUserIds: number[]
}

export interface ControlledFileRoutePreviewVO {
  stageNo: number
  stageCode?: string
  stageName: string
  stageOrder?: number
  candidateSourceType: 'USER' | 'POSITION'
  candidateSourceId?: number
  candidateSourceIds: number[]
  approveMethod: 'ANY' | 'ALL'
  approveRatio?: number | null
  requireAllApprovals?: boolean
  resolvedUserIds: number[]
}

export interface ControlledFileRouteReadinessBlockerVO {
  reasonCode: string
  message: string
  stageNo?: number | null
  stageCode?: string | null
  stageName?: string | null
  userId?: number | null
  userName?: string | null
}

export interface ControlledFileRouteReadinessVO {
  ready: boolean
  nodes: ControlledFileRoutePreviewVO[]
  blockers: ControlledFileRouteReadinessBlockerVO[]
}

export interface ControlledFileRouteSnapshotVO {
  id: number
  routeVersionNo?: number
  stageNo: number
  stageCode?: string
  stageName?: string
  stageOrder?: number
  candidateSourceType: string
  candidateSourceId?: number
  candidateSourceIds: number[]
  approveMethod: string
  approveRatio?: number | null
  requireAllApprovals?: boolean
  resolvedUserIds: number[]
}

export interface ControlledFileVersionHistoryVO {
  id: number
  title: string
  fileNumber: string
  versionNo: string
  status: string
  publishedArtifactAvailable?: boolean
  stampedArtifactAvailable?: boolean
  currentActiveVersionNo?: string | null
  effectiveDate?: string
  publishedTime?: number
  obsoletedTime?: number
  supersededByFileId?: number | null
  remark?: string
  canPreview?: boolean
  previewUnavailableReason?: string
  canDownload?: boolean
  modifying?: boolean
}

export interface ControlledFileDistributionStatusVO {
  id: number
  departmentId: number
  distributionMedium?: 'PUBLIC_FOLDER' | 'PAPER'
  status: string
  acknowledgedBy?: number | null
  acknowledgedAt?: number | null
  recoveredBy?: number | null
  recoveredAt?: number | null
  recipientUserIds: number[]
  recipients?: ControlledFileDistributionRecipientStatusVO[]
}

export interface ControlledFileDistributionRecipientStatusVO {
  id: number
  userId: number
  readAt?: number | null
  acknowledgedAt?: number | null
  ackComment?: string | null
}

export type ControlledFileDistributionMedium = 'PUBLIC_FOLDER' | 'PAPER'

export interface ControlledFileDistributionScopeVO {
  departmentId: number
  distributionMedium: ControlledFileDistributionMedium
}

export interface ControlledFileDistributionRecipientAckReqVO {
  password: string
  comment?: string
}

export interface ControlledFileDistributionRecipientSignReqVO {
  userIds: number[]
  password: string
  comment?: string
}

export interface ControlledFilePaperDistributionIssueReqVO {
  recipientUserIds: number[]
}

export interface ControlledFilePaperDistributionRecordVO {
  distributionId: number
  controlledFileId: number
  fileNumber: string
  fileName: string
  versionNo: string
  issuerUserId?: number | null
  issuerName?: string | null
  recipientUserIds: number[]
  recipientNames: string[]
  issuedAt?: number | null
  recovererUserId?: number | null
  recovererName?: string | null
  recoveredAt?: number | null
  status: string
}

export interface ControlledFileTrainingAssignmentVO {
  id: number
  userId: number
  status: string
  acknowledgedAt?: number
  accumulatedViewSeconds?: number
  requiredViewSeconds?: number
  eligibleToAcknowledge?: boolean
}

export interface ControlledFileTrainingStatusVO {
  id: number
  departmentId: number
  status: string
  assignments: ControlledFileTrainingAssignmentVO[]
}

export interface DccAdminFullConfigPackageImportRespVO {
  approvalPositionCount: number
  directoryCount: number
  directoryAccessRuleCount: number
  categoryCount: number
  permissionRuleCount: number
  approvalMatrixRuleCount: number
  viewMatrixRuleCount: number
  distributionRuleCount: number
  trainingRuleCount: number
  removedApprovalPositionCount: number
  removedDirectoryCount: number
  removedCategoryCount: number
}

export interface ControlledFileSignatureSummaryVO {
  id: number
  taskId?: string
  taskActionResult?: string
  revisionId?: number
  versionNo?: string
  meaningCode?: string
  sourceFileHashShort?: string
  controlledCopyHashStatus?: string
  controlledCopyHashShort?: string
  evidenceStatus?: string
  evidenceHashShort?: string
  actorId: number
  actorUsernameSnapshot?: string
  actorNicknameSnapshot?: string
  actorDeptIdSnapshot?: number
  actorDeptNameSnapshot?: string
  actorPostNamesSnapshot?: string
  actorRoleNamesSnapshot?: string
  signaturePurpose?: string
  authorizationBasis?: string
  authenticationMethod?: string
  recordVersionSnapshot?: string
  recordHashSnapshot?: string
  clientIpSnapshot?: string
  userAgentSnapshot?: string
  snapshotStatus?: string
  actionType?: string
  signatureMode?: string
  comment?: string
  signedAt?: number
}

export type DccSignatureTaskActionResult = 'APPROVED' | 'REJECTED'

export interface DccSignatureActionRespVO {
  taskActionResult: DccSignatureTaskActionResult
  signatureId: number
  controlledFileId: number
  revisionId: number
  versionNo: string
  meaningCode: string
  controlledCopyHashStatus: string
  evidenceStatus: string
  evidenceHashShort: string
  signedAt: number
  nextStatus: string
}

export interface ControlledFileApproveTaskReqVO {
  taskId: string
  password: string
  reason?: string
  sessionId?: string
  stampedPdfUploadTicket?: string
  confirmedDirectoryId?: number
  selectedDistributionScopes?: ControlledFileDistributionScopeVO[]
}

export interface ControlledFileTaskReadinessReqVO {
  taskId: string
  sessionId?: string
  stampedPdfUploadTicket?: string
  confirmedDirectoryId?: number
  selectedDistributionScopes?: ControlledFileDistributionScopeVO[]
}

export interface ControlledFileTaskReadinessBlockerVO {
  reasonCode: string
  message: string
}

export interface ControlledFileTaskReadinessVO {
  ready: boolean
  finalApproval: boolean
  blockers: ControlledFileTaskReadinessBlockerVO[]
}

export interface ExternalFileReviewApproveTaskReqVO extends ControlledFileApproveTaskReqVO {
  reviewConclusion?: string
  conclusionComment?: string
  sessionId?: string
  outputUploadTicket?: string
}

export interface ControlledFileRejectTaskReqVO {
  taskId: string
  password: string
  reason: string
}

export interface ControlledFileReturnTaskReqVO {
  taskId: string
  targetTaskDefinitionKey: string
  password: string
  reason: string
}

export interface ControlledFileTransferTaskReqVO {
  taskId: string
  assigneeUserId: number
  password: string
  reason: string
}

export interface ControlledFileCreateSignTaskReqVO {
  taskId: string
  userIds: number[]
  type: 'before' | 'after'
  password: string
  reason: string
}

export interface ControlledFileVO {
  id: number
  masterId?: number | null
  productMasterId?: number | null
  dccProjectCodeId?: number | null
  categoryId: number
  directoryId: number
  directoryPath?: string | null
  productCode?: string
  productName?: string
  projectCodeRecognitionType?: string | null
  projectCodeRecognitionText?: string | null
  projectCodeRecognizedBy?: number | null
  projectCodeRecognizedTime?: number | null
  fileTypeTaxonomyId?: number | null
  fileTypeLevel1?: string | null
  fileTypeLevel2?: string | null
  fileTypeLevel3?: string | null
  fileTypeLevel4?: string | null
  fileTypeLevel5?: string | null
  needTraining?: boolean
  processType?: string
  title: string
  fileName?: string
  contentType?: string
  previewKind?: ControlledFilePreviewKind
  fileNumber?: string
  publishedArtifactAvailable?: boolean
  stampedArtifactAvailable?: boolean
  versionNo: string
  effectiveDate?: string
  remark?: string
  status: string
  requesterId: number
  processInstanceId?: string
  processDefinitionKey?: string
  submittedTime?: number
  approvedTime?: number
  publishedTime?: number
  rejectedTime?: number
  stampedTime?: number
  obsoletedBy?: number | null
  obsoletedTime?: number
  obsoleteReason?: string
  supersededByFileId?: number | null
  rejectReason?: string
  finalizationError?: string
  canPreview?: boolean
  previewUnavailableReason?: string
  canDownload?: boolean
  canPrint?: boolean
  accessExplanation?: ControlledFileAccessExplanationVO
  canObsolete?: boolean
  canPublish?: boolean
  canManualRelease?: boolean
  actionProjection?: DccControlledFileActionProjectionVO | null
  hasPendingTrainingAcknowledgement?: boolean
  externalReview?: ExternalFileReviewVO | null
  currentActiveVersionNo?: string | null
  systemRecordDownloadOpen?: boolean
  modifying?: boolean
  routeSnapshots: ControlledFileRouteSnapshotVO[]
  versionHistory?: ControlledFileVersionHistoryVO[]
  distributionStatuses?: ControlledFileDistributionStatusVO[]
  trainingStatuses?: ControlledFileTrainingStatusVO[]
  signatureSummaries?: ControlledFileSignatureSummaryVO[]
}

export interface ExternalFileReviewVO {
  controlledFileId: number
  externalSource: string
  externalOwner: string
  reviewReason: string
  participantUserIds: number[]
  reviewConclusion?: string | null
  conclusionComment?: string | null
  outputFileName?: string | null
  closedTime?: number | null
}

export interface ControlledFilePageReqVO extends PageParam {
  categoryId?: DccRouteId
  directoryId?: DccRouteId
  includeDescendantDirectories?: boolean
  status?: string
  keyword?: string
  latestVersionOnly?: boolean
  dccProjectCodeId?: number
  fileTypeTaxonomyId?: number
  fileTypeTaxonomyIds?: number[]
  recognitionStatus?: string
  batchRecognitionTaskId?: DccRouteId
  quickFilter?: TableQuickFilterValue
}

export interface ControlledFileBrowserExtensionBlacklistRespVO {
  extensionPatterns: string[]
}

export interface ControlledFileBrowserExtensionBlacklistSaveReqVO {
  extensionPatterns: string[]
}

export interface ControlledFileBatchRecognitionCreateReqVO {
  recognitionType: 'BASIC_INFO' | 'FILE_CATEGORY' | 'FILE_NUMBER'
  scope: 'CURRENT' | 'GLOBAL'
  directoryId?: DccRouteId
  includeDescendantDirectories?: boolean
  keyword?: string
  status?: string
  categoryId?: DccRouteId
  overwriteExisting: boolean
  existingRecordPolicy: string
  syncFileNameTitle: boolean
  workerCount?: number
}

export interface ControlledFileBatchRecognitionFailureSummaryVO {
  stage: string
  code: string
  reason: string
  count: number
}

export interface ControlledFileBatchRecognitionTaskRespVO {
  taskId: number
  status: string
  recognitionType: 'BASIC_INFO' | 'FILE_CATEGORY' | 'FILE_NUMBER'
  scope: 'CURRENT' | 'GLOBAL'
  directoryId?: number | null
  directoryPath?: string | null
  workerCount?: number | null
  activeWorkerCount?: number | null
  recordedFileCount?: number | null
  keyword?: string | null
  statusFilter?: string | null
  categoryId?: number | null
  overwriteExisting: boolean
  existingRecordPolicy: string
  syncFileNameTitle: boolean
  totalCount: number
  processedCount: number
  successCount: number
  failedCount: number
  skippedExistingCount: number
  unclassifiedCount: number
  ambiguousCount: number
  conflictCount: number
  remainingCount: number
  lastFailureMessage?: string | null
  failureSummaries?: ControlledFileBatchRecognitionFailureSummaryVO[]
  startedAt?: number | null
  completedAt?: number | null
}

export interface ControlledFileMetadataImportRowRespVO {
  rowNo: number
  controlledFileId?: number | null
  fileName?: string | null
  fileNumber?: string | null
  importAction: string
  failureReason?: string | null
}

export interface ControlledFileMetadataImportPreviewRespVO {
  totalCount: number
  updateCount: number
  unchangedCount: number
  failureCount: number
  rows: ControlledFileMetadataImportRowRespVO[]
}

export interface ControlledFileRecognitionMigrationImportRowRespVO {
  rowNo: number
  directoryPath?: string | null
  fileName?: string | null
  fileNumber?: string | null
  testControlledFileId?: number | null
  targetControlledFileId?: number | null
  targetFileName?: string | null
  targetFileNumber?: string | null
  recognitionStatus?: string | null
  importAction: string
  failureReason?: string | null
  productName?: string | null
  productCode?: string | null
  productMasterId?: number | null
  projectName?: string | null
  projectCode?: string | null
  dccProjectCodeId?: number | null
  fileTypeLevel1?: string | null
  fileTypeLevel2?: string | null
  fileTypeLevel3?: string | null
  fileTypeLevel4?: string | null
  fileTypeLevel5?: string | null
}

export interface ControlledFileRecognitionMigrationImportPreviewRespVO {
  totalCount: number
  applicableCount: number
  blockedCount: number
  failedRecognitionCount: number
  appliedCount?: number | null
  rows: ControlledFileRecognitionMigrationImportRowRespVO[]
}

export interface ControlledFileAccessExplanationVO {
  detailSource?: string
  detailReason?: string
  detailDeniedReason?: string
  publishedPreviewSource?: string
  publishedPreviewReason?: string
  pendingPreviewSource?: string
  pendingPreviewReason?: string
  downloadSource?: string
  downloadReason?: string
  downloadDeniedReason?: string
}

export interface ControlledFileObsoleteReqVO {
  reason: string
  idempotencyKey: string
  startUserSelectAssignees?: Record<string, number[]>
}

export interface ControlledFilePublishReqVO {
  reason: string
  idempotencyKey: string
  startUserSelectAssignees?: Record<string, number[]>
}

export interface ControlledFileNasTransferReqVO {
  selectedNasPaths: string[]
  templateCategoryId: number
  dccProjectCodeId: number
  productMasterId?: null
  effectiveDate: string
}

export interface ControlledFileLocalFolderImportSessionCreateReqVO {
  templateCategoryId: number
  dccProjectCodeId: number
  productMasterId?: null
  effectiveDate: string
  rootDirectoryName: string
  expectedFileCount: number
  expectedTotalBytes: number
}

export interface ControlledFileLocalFolderImportUploadFileStateVO {
  relativePath: string
  fileSize?: number | null
  totalChunks?: number | null
  uploadedChunkIndexes: number[]
  completed: boolean
}

export interface ControlledFileLocalFolderImportUploadStateRespVO {
  taskId: number
  rootDirectoryName: string
  status: string
  expectedFileCount: number
  expectedTotalBytes: number
  uploadedFileCount: number
  uploadedTotalBytes: number
  uploadedRelativePaths: string[]
  files: ControlledFileLocalFolderImportUploadFileStateVO[]
}

export type ControlledFileNasTransferSourceType = 'NAS' | 'LOCAL_FOLDER' | 'NAS_UNCONTROLLED_IMPORT' | 'NAS_ORIGINAL_PATH_SYNC'

export interface ControlledFileNasTransferFailureVO {
  nasPath: string
  stage: string
  reason: string
}

export interface ControlledFileNasTransferRespVO {
  taskId: number
  status: string
  sourceType: ControlledFileNasTransferSourceType
  selectedNasPaths: string[]
  expectedFileCount: number
  expectedTotalBytes: number
  uploadedFileCount: number
  uploadedTotalBytes: number
  uploadCompletedAt?: string | null
  createdDirectoryCount: number
  reusedDirectoryCount: number
  createdCategoryCount: number
  reusedCategoryCount: number
  createdFileCount: number
  failedFileCount: number
  skippedPreviewOnlyCount: number
  remainingPendingCount: number
  lastFailureMessage?: string | null
  completedAt?: string | null
  failureReportPath?: string | null
  failureReportGeneratedAt?: string | null
  failureReportError?: string | null
  failures: ControlledFileNasTransferFailureVO[]
}

export interface NasPermissionBlockerVO {
  code: string
  message: string
  principal?: string | null
  aceIndex?: number | null
}

export interface NasPermissionSnapshotSummaryVO {
  taskId: number
  snapshotStatus: string
  selectedNasPaths: string[]
  directorySnapshotCount: number
  aceCount: number
  unsupportedAceCount: number
  unmappedPrincipalCount: number
  blockerCount: number
  capturedAt?: number | null
  lastFailureMessage?: string | null
  restoreSupported: boolean
}

export interface NasPermissionSnapshotItemVO {
  taskItemId: number
  nasPath: string
  dccDirectoryId?: number | null
  snapshotStatus: string
  aceCount: number
  blockers: NasPermissionBlockerVO[]
}

export interface NasPermissionSnapshotPageReqVO {
  pageNo?: number
  pageSize?: number
  status?: string
}

export interface NasUnmappedPrincipalVO {
  sourceAuthority?: string | null
  sourceSid: string
  sourceName?: string | null
  aceCount: number
  firstNasPath?: string | null
}

export interface NasUnmappedPrincipalListVO {
  list: NasUnmappedPrincipalVO[]
}

export interface NasPrincipalMappingSaveReqVO {
  sourceAuthority?: string | null
  sourceSid: string
  sourceName?: string | null
  accountName?: string | null
  accountType: string
  targetSubjectType: string
  targetSubjectId: number
  active: boolean
  changeReason?: string | null
}

export interface NasPrincipalMappingRespVO {
  id: number
  sourceSid: string
  targetSubjectType: string
  targetSubjectId: number
  active: boolean
}

export interface NasPermissionRestoreBlockerVO {
  code: string
  message: string
  directorySnapshotId?: number | null
  nasPath?: string | null
  trusteeSid?: string | null
}

export interface NasPermissionRestoreRulePreviewVO {
  directoryId: number
  nasPath: string
  subjectType: string
  subjectId: number
  canQuery: boolean
  canPreview: boolean
  canDownload: boolean
}

export interface NasPermissionRestorePreviewVO {
  taskId: number
  canRestore: boolean
  planHash: string
  restoreMode: string
  directoryCount: number
  ruleCount: number
  runtimeEnforcementReady: boolean
  runtimeEnforcementBlocker?: string | null
  blockers: NasPermissionRestoreBlockerVO[]
  sampleRules: NasPermissionRestoreRulePreviewVO[]
}

export interface NasPermissionRestoreApplyReqVO {
  idempotencyKey: string
  planHash: string
  restoreMode: string
  changeReason?: string | null
}

export interface NasPermissionRestoreApplyRespVO {
  restoreId: number
  taskId: number
  status: string
  directoryCount: number
  ruleCount: number
  completedDirectoryCount: number
  failedDirectoryCount: number
}

export interface NasPermissionRestoreStatusVO extends NasPermissionRestoreApplyRespVO {
  lastFailureMessage?: string | null
  startedAt?: number | null
  completedAt?: number | null
}

export const CONTROLLED_FILE_PROCESS_DEFINITION_KEY = 'dcc-controlled-file-approval'
export const EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_KEY = 'dcc-external-file-review'
export const CONTROLLED_FILE_TASK_PASSWORD_INVALID_CODE = 1080000022
export const CONTROLLED_FILE_PREVIEW_WATERMARK_HEADER = 'x-dcc-preview-watermark'
export const DCC_VIEWER_TOKEN_HEADER = 'X-DCC-Viewer-Token'
export const DCC_VIEWER_TOKEN_ID_HEADER = 'X-DCC-Viewer-Token-Id'
export const DCC_VIEWER_TOKEN_NONCE_HEADER = 'X-DCC-Viewer-Token-Nonce'
export const DCC_ACCESS_EVENT_CODE_HEADER = 'X-DCC-Access-Event-Code'
export const DCC_WATERMARK_TRACE_CODE_HEADER = 'X-DCC-Watermark-Trace-Code'
export const DCC_REQUEST_ID_HEADER = 'X-DCC-Request-Id'
export const LOCAL_FOLDER_IMPORT_REQUEST_TIMEOUT = 0
export const DCC_ADMIN_FULL_CONFIG_IMPORT_REQUEST_TIMEOUT = 0
export const LOCAL_FOLDER_IMPORT_CHUNK_BYTES = 32 * 1024 * 1024
export const CONTROLLED_FILE_DOWNLOAD_CONFIRM_TITLE = '确认下载'
export const CONTROLLED_FILE_DOWNLOAD_CONFIRM_MESSAGE =
  '确认下载该受控文件？下载后的文件为非受控文件，系统将记录本次下载留痕。'

const CONTROLLED_FILE_PREVIEW_KINDS: readonly ControlledFilePreviewKind[] = [
  'PDF',
  'IMAGE',
  'TEXT',
  'OFFICE',
  'VIDEO',
  'AUDIO',
  'DOWNLOAD_ONLY'
]

const DCC_FORBIDDEN_FILE_CAPABILITY_FIELDS = [
  'fileId',
  'fileUrl',
  'path',
  'configId',
  'originalFileId',
  'sourceFileId',
  'drawingPdfFileId',
  'publishedFileId',
  'stampedPdfFileId',
  'stampedFileId',
  'trainingRecordFileId',
  'outputFileId'
] as const

const DCC_UPLOAD_RESPONSE_FORBIDDEN_FIELDS = [
  ...DCC_FORBIDDEN_FILE_CAPABILITY_FIELDS
] as const

const DCC_DOWNLOAD_REQUIRED_RESPONSE_HEADERS = [
  {
    name: 'X-DCC-Download-Request-Id',
    key: 'x-dcc-download-request-id',
    target: 'downloadRequestId'
  },
  {
    name: 'X-DCC-Access-Event-Code',
    key: 'x-dcc-access-event-code',
    target: 'accessEventCode'
  },
  {
    name: 'X-DCC-Plain-SHA256',
    key: 'x-dcc-plain-sha256',
    target: 'plainSha256'
  }
] as const

export class DccControlledFileContractError extends Error {
  details?: unknown

  constructor(message: string, details?: unknown) {
    super(message)
    this.name = 'DccControlledFileContractError'
    this.details = details
  }
}

const buildDccExplicitTenantHeaders = () => {
  const tenantId = getTenantId()
  if (typeof tenantId !== 'number' || !Number.isSafeInteger(tenantId) || tenantId <= 0) {
    throw new DccControlledFileContractError('DCC 请求缺少有效的系统租户，请重新登录')
  }
  return { 'tenant-id': String(tenantId) }
}

const isBlankString = (value: unknown) => typeof value === 'string' && value.trim().length === 0

const assertRecordPayload = (data: unknown, context: string): Record<string, unknown> => {
  if (!data || typeof data !== 'object' || Array.isArray(data)) {
    throw new DccControlledFileContractError(`${context} payload is not an object`)
  }
  return data as Record<string, unknown>
}

const assertNoForbiddenDccFileCapabilityFields = (
  payload: Record<string, unknown>,
  context: string,
  forbiddenFields: readonly string[] = DCC_FORBIDDEN_FILE_CAPABILITY_FIELDS
) => {
  const exposedFields = forbiddenFields.filter((field) =>
    Object.prototype.hasOwnProperty.call(payload, field)
  )
  if (exposedFields.length > 0) {
    throw new DccControlledFileContractError(
      `${context} response exposes forbidden file capability fields: ${exposedFields.join(', ')}`
    )
  }
}

const assertRequiredString = (
  payload: Record<string, unknown>,
  field: string,
  context: string
): string => {
  const value = payload[field]
  if (typeof value !== 'string' || !value.trim()) {
    throw new DccControlledFileContractError(`${context} response missing required field: ${field}`)
  }
  return value.trim()
}

const assertRequiredNumber = (
  payload: Record<string, unknown>,
  field: string,
  context: string
): number => {
  const value = payload[field]
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new DccControlledFileContractError(`${context} response missing required field: ${field}`)
  }
  return value
}

const assertRequiredBoolean = (
  payload: Record<string, unknown>,
  field: string,
  context: string
): boolean => {
  const value = payload[field]
  if (typeof value !== 'boolean') {
    throw new DccControlledFileContractError(`${context} response missing required field: ${field}`)
  }
  return value
}

const assertRequiredObject = (
  payload: Record<string, unknown>,
  field: string,
  context: string
): Record<string, unknown> => {
  const value = payload[field]
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new DccControlledFileContractError(`${context} response missing required field: ${field}`)
  }
  return value as Record<string, unknown>
}

const readOptionalString = (payload: Record<string, unknown>, field: string): string | undefined => {
  const value = payload[field]
  if (value === undefined || value === null || isBlankString(value)) {
    return undefined
  }
  if (typeof value !== 'string') {
    throw new DccControlledFileContractError(`DCC response field has invalid type: ${field}`)
  }
  return value.trim()
}

const readOptionalTimestamp = (payload: Record<string, unknown>, field: string): number | undefined => {
  const value = payload[field]
  if (value === undefined || value === null) {
    return undefined
  }
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new DccControlledFileContractError(`DCC response field has invalid type: ${field}`)
  }
  return value
}

const readOptionalNullableString = (
  payload: Record<string, unknown>,
  field: string
): string | null | undefined => {
  const value = payload[field]
  if (value === undefined) {
    return undefined
  }
  if (value === null || isBlankString(value)) {
    return null
  }
  if (typeof value !== 'string') {
    throw new DccControlledFileContractError(`DCC response field has invalid type: ${field}`)
  }
  return value.trim()
}

const readOptionalPreviewKind = (
  payload: Record<string, unknown>,
  field: string
): ControlledFilePreviewKind | undefined => {
  const value = payload[field]
  if (value === undefined || value === null || isBlankString(value)) {
    return undefined
  }
  if (
    typeof value !== 'string' ||
    !CONTROLLED_FILE_PREVIEW_KINDS.includes(value as ControlledFilePreviewKind)
  ) {
    throw new DccControlledFileContractError(`DCC response field has invalid preview kind: ${field}`)
  }
  return value as ControlledFilePreviewKind
}

const assertRequiredPreviewKind = (
  payload: Record<string, unknown>,
  field: string,
  context: string
): ControlledFilePreviewKind => {
  const value = assertRequiredString(payload, field, context)
  if (!CONTROLLED_FILE_PREVIEW_KINDS.includes(value as ControlledFilePreviewKind)) {
    throw new DccControlledFileContractError(`${context} response has invalid preview kind: ${value}`)
  }
  return value as ControlledFilePreviewKind
}

const parseControlledPreviewWatermark = (
  value: Record<string, unknown>,
  context: string
): ControlledPreviewWatermark => {
  assertRequiredString(value, 'label', context)
  assertRequiredString(value, 'text', context)
  assertRequiredString(value, 'actorName', context)
  assertRequiredString(value, 'actorAccount', context)
  assertRequiredString(value, 'timestamp', context)
  assertRequiredString(value, 'purpose', context)
  assertRequiredObject(value, 'overlay', context)
  return value as unknown as ControlledPreviewWatermark
}

export const parseControlledFileUploadTemporaryStatusResp = (
  data: unknown
): ControlledFileUploadTemporaryStatusRespVO => {
  const payload = assertRecordPayload(data, 'DCC upload temporary status')
  assertNoForbiddenDccFileCapabilityFields(payload, 'DCC upload temporary status')
  return {
    requestId: assertRequiredString(payload, 'requestId', 'DCC upload temporary status'),
    temporaryFileCount: assertRequiredNumber(
      payload,
      'temporaryFileCount',
      'DCC upload temporary status'
    ),
    bindable: assertRequiredBoolean(payload, 'bindable', 'DCC upload temporary status'),
    sessionId: readOptionalString(payload, 'sessionId'),
    purpose: readOptionalString(payload, 'purpose'),
    status: readOptionalString(payload, 'status'),
    expireTime: readOptionalTimestamp(payload, 'expireTime'),
    cleanupStatus: readOptionalString(payload, 'cleanupStatus'),
    cleanupReason: readOptionalString(payload, 'cleanupReason'),
    cleanupTime: readOptionalTimestamp(payload, 'cleanupTime'),
    cleanedCount:
      payload.cleanedCount === undefined || payload.cleanedCount === null
        ? undefined
        : assertRequiredNumber(payload, 'cleanedCount', 'DCC upload temporary status')
  }
}

export const parseControlledFileUploadResp = (data: unknown): ControlledFileUploadRespVO => {
  const payload = assertRecordPayload(data, 'DCC upload')
  assertNoForbiddenDccFileCapabilityFields(payload, 'DCC upload', DCC_UPLOAD_RESPONSE_FORBIDDEN_FIELDS)
  return {
    uploadTicket: assertRequiredString(payload, 'uploadTicket', 'DCC upload'),
    sessionId: assertRequiredString(payload, 'sessionId', 'DCC upload'),
    requestId: assertRequiredString(payload, 'requestId', 'DCC upload'),
    fileName: assertRequiredString(payload, 'fileName', 'DCC upload'),
    contentType: assertRequiredString(payload, 'contentType', 'DCC upload'),
    previewKind: readOptionalPreviewKind(payload, 'previewKind'),
    onlyofficeBaseUrl: readOptionalString(payload, 'onlyofficeBaseUrl'),
    onlyofficeDocumentUrl: readOptionalString(payload, 'onlyofficeDocumentUrl'),
    previewUnavailableReason: readOptionalString(payload, 'previewUnavailableReason'),
    fileSize: assertRequiredNumber(payload, 'fileSize', 'DCC upload'),
    expireTime: readOptionalTimestamp(payload, 'expireTime'),
    watermarkTraceCode: readOptionalNullableString(payload, 'watermarkTraceCode'),
    watermark:
      payload.watermark === undefined || payload.watermark === null
        ? null
        : parseControlledPreviewWatermark(
            assertRequiredObject(payload, 'watermark', 'DCC upload'),
            'DCC upload watermark'
          )
  }
}

export const parseControlledFilePreviewMetadata = (
  data: unknown
): ControlledFilePreviewMetadataVO => {
  const payload = assertRecordPayload(data, 'DCC preview metadata')
  assertNoForbiddenDccFileCapabilityFields(payload, 'DCC preview metadata')
  return {
    previewKind: assertRequiredPreviewKind(payload, 'previewKind', 'DCC preview metadata'),
    fileName: assertRequiredString(payload, 'fileName', 'DCC preview metadata'),
    contentType: assertRequiredString(payload, 'contentType', 'DCC preview metadata'),
    onlyofficeBaseUrl: readOptionalString(payload, 'onlyofficeBaseUrl'),
    onlyofficeDocumentUrl: readOptionalString(payload, 'onlyofficeDocumentUrl'),
    previewUnavailableReason: readOptionalString(payload, 'previewUnavailableReason'),
    viewerToken: assertRequiredString(payload, 'viewerToken', 'DCC preview metadata'),
    viewerTokenId: assertRequiredString(payload, 'viewerTokenId', 'DCC preview metadata'),
    viewerTokenNonce: assertRequiredString(payload, 'viewerTokenNonce', 'DCC preview metadata'),
    accessEventCode: assertRequiredString(payload, 'accessEventCode', 'DCC preview metadata'),
    watermarkTraceCode: assertRequiredString(payload, 'watermarkTraceCode', 'DCC preview metadata'),
    watermark: parseControlledPreviewWatermark(
      assertRequiredObject(payload, 'watermark', 'DCC preview metadata'),
      'DCC preview metadata watermark'
    )
  }
}

const assertNoForbiddenDccRequestFields = (
  payload: Record<string, unknown>,
  context: string
) => {
  const forbiddenFields = DCC_FORBIDDEN_FILE_CAPABILITY_FIELDS.filter((field) =>
    Object.prototype.hasOwnProperty.call(payload, field)
  )
  if (forbiddenFields.length > 0) {
    throw new DccControlledFileContractError(
      `${context} request uses forbidden file capability fields: ${forbiddenFields.join(', ')}`
    )
  }
}

const assertControlledFileSubmitRequest = (
  data: ControlledFileSubmitReqVO | ExternalFileReviewSubmitReqVO,
  context: string
) => {
  const payload = assertRecordPayload(data, context)
  assertNoForbiddenDccRequestFields(payload, context)
  if (payload.uploadTicket !== undefined && payload.uploadTicket !== null) {
    throw new DccControlledFileContractError(`${context} request uses obsolete field: uploadTicket`)
  }
  assertRequiredString(payload, 'sessionId', context)
  assertRequiredString(payload, 'originalUploadTicket', context)
  if (payload.sourceUploadTicket !== undefined && payload.sourceUploadTicket !== null) {
    assertRequiredString(payload, 'sourceUploadTicket', context)
  }
  if (payload.drawingPdfUploadTicket !== undefined && payload.drawingPdfUploadTicket !== null) {
    assertRequiredString(payload, 'drawingPdfUploadTicket', context)
  }
  assertRequiredString(payload, 'changeType', context)
  if (payload.processType !== 'EXTERNAL_REVIEW') {
    assertRequiredNumber(payload, 'dccProjectCodeId', context)
    assertRequiredNumber(payload, 'fileTypeTaxonomyId', context)
  }
}

const assertRequiredTicketRequest = (
  data: Record<string, unknown>,
  field: string,
  context: string
) => {
  assertNoForbiddenDccRequestFields(data, context)
  assertRequiredString(data, field, context)
}

export class DccTaskActionError extends Error {
  code?: number

  constructor(message: string, code?: number) {
    super(message)
    this.name = 'DccTaskActionError'
    this.code = code
  }
}

const DCC_SIGNATURE_ACTION_REQUIRED_FIELDS: ReadonlyArray<keyof DccSignatureActionRespVO> = [
  'taskActionResult',
  'signatureId',
  'controlledFileId',
  'revisionId',
  'versionNo',
  'meaningCode',
  'controlledCopyHashStatus',
  'evidenceStatus',
  'evidenceHashShort',
  'signedAt',
  'nextStatus'
]

const isMissingDccSignatureActionField = (value: unknown) => {
  return value === undefined || value === null || (typeof value === 'string' && !value.trim())
}

const parseDccSignatureActionResp = (
  data: unknown,
  expectedResult: DccSignatureTaskActionResult
): DccSignatureActionRespVO => {
  if (!data || typeof data !== 'object') {
    throw new DccTaskActionError('DCC 电子签名响应缺少签名结果数据')
  }
  const payload = data as Record<keyof DccSignatureActionRespVO, unknown>
  const missingFields = DCC_SIGNATURE_ACTION_REQUIRED_FIELDS.filter((field) =>
    isMissingDccSignatureActionField(payload[field])
  )
  if (missingFields.length > 0) {
    throw new DccTaskActionError(
      `DCC 电子签名响应缺少必需字段：${missingFields.join('、')}`
    )
  }
  if (payload.taskActionResult !== expectedResult) {
    throw new DccTaskActionError(
      `DCC 电子签名响应动作不匹配：期望 ${expectedResult}，实际 ${String(
        payload.taskActionResult
      )}`
    )
  }
  if (payload.evidenceStatus !== 'VALID') {
    throw new DccTaskActionError(
      `DCC 电子签名证据状态不是 VALID：${String(payload.evidenceStatus)}`
    )
  }
  return data as DccSignatureActionRespVO
}

const buildControlledFileTaskActionHeaders = () => {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  }
  const accessToken = getAccessToken()
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }
  const tenantId = getTenantId()
  if (tenantId) {
    headers['tenant-id'] = tenantId
  }
  const visitTenantId = getVisitTenantId()
  if (visitTenantId && accessToken) {
    headers['visit-tenant-id'] = visitTenantId
  }
  return headers
}

export const buildControlledFileBinaryHeaders = (controlledHeaders: Record<string, string> = {}) => {
  const headers: Record<string, string> = { ...controlledHeaders }
  const accessToken = getAccessToken()
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }
  const tenantId = getTenantId()
  if (tenantId) {
    headers['tenant-id'] = tenantId
  }
  const visitTenantId = getVisitTenantId()
  if (visitTenantId && accessToken) {
    headers['visit-tenant-id'] = visitTenantId
  }
  headers['Cache-Control'] = 'no-cache'
  headers.Pragma = 'no-cache'
  return headers
}

const resolveDccTaskActionErrorMessage = (
  responseData: { msg?: string; message?: string } | undefined,
  fallbackMessage: string
) => {
  return responseData?.msg?.trim() || responseData?.message?.trim() || fallbackMessage
}

const toDccTaskActionError = (error: unknown) => {
  if (error instanceof DccTaskActionError) {
    return error
  }
  if (error instanceof AxiosError) {
    const responseData = error.response?.data as
      | {
          code?: number
          msg?: string
          message?: string
        }
      | undefined
    return new DccTaskActionError(
      resolveDccTaskActionErrorMessage(
        responseData,
        error.message || 'DCC 审批任务操作失败'
      ),
      responseData?.code
    )
  }
  if (error instanceof Error) {
    return new DccTaskActionError(error.message)
  }
  return new DccTaskActionError('DCC 审批任务操作失败')
}

const postControlledFileTaskAction = async <T>(url: string, data: unknown): Promise<T> => {
  try {
    const response = await axios.post<{
      code?: number
      msg?: string
      message?: string
      data?: T
    }>(`${axiosConfig.base_url}${url}`, data, {
      headers: buildControlledFileTaskActionHeaders(),
      timeout: axiosConfig.request_timeout
    })
    const code = response.data?.code ?? axiosConfig.result_code
    if (code === 0 || code === 200) {
      return response.data?.data as T
    }
    throw new DccTaskActionError(
      resolveDccTaskActionErrorMessage(response.data, 'DCC 审批任务操作失败'),
      code as number
    )
  } catch (error) {
    throw toDccTaskActionError(error)
  }
}

export const isControlledFileTaskPasswordInvalidError = (error: unknown) => {
  return (
    error instanceof DccTaskActionError &&
    error.code === CONTROLLED_FILE_TASK_PASSWORD_INVALID_CODE
  )
}

export const submitControlledFile = async (data: ControlledFileSubmitReqVO): Promise<number> => {
  assertControlledFileSubmitRequest(data, 'DCC controlled file submit')
  return await request.post({ url: '/dcc/controlled-files/submit', data })
}

export const submitExternalFileReview = async (
  data: ExternalFileReviewSubmitReqVO
): Promise<number> => {
  assertControlledFileSubmitRequest(data, 'DCC external file review submit')
  return await request.post({ url: '/dcc/external-file-reviews/submit', data })
}

export const uploadControlledFilePreview = async (
  file: File,
  purpose: UploadPreviewPurpose,
  context: ControlledFileUploadPreviewContext
): Promise<ControlledFileUploadRespVO> => {
  const requestId = createControlledFileDownloadRequestId()
  const formData = new FormData()
  formData.append('files', file)
  formData.append('categoryId', String(context.categoryId))
  formData.append('sessionId', context.sessionId)
  formData.append('purpose', purpose)
  const res = await request.upload({
    url: '/dcc/controlled-files/upload-preview',
    data: formData,
    headers: {
      ...buildDccExplicitTenantHeaders(),
      [DCC_REQUEST_ID_HEADER]: requestId
    }
  })
  const uploadResp = parseControlledFileUploadResp((res as { data?: unknown }).data)
  if (uploadResp.requestId !== requestId) {
    throw new DccControlledFileContractError('DCC upload response request id mismatch')
  }
  return uploadResp
}

export const getControlledFileUploadTemporaryStatus = async (
  requestId: string
): Promise<ControlledFileUploadTemporaryStatusRespVO> => {
  return parseControlledFileUploadTemporaryStatusResp(
    await request.get({
      url: '/dcc/controlled-files/upload-temporary/status',
      params: { requestId },
      headers: { ...buildDccExplicitTenantHeaders() }
    })
  )
}

export const cleanupControlledFileUploadSession = async (
  sessionId: string,
  requestId?: string
): Promise<ControlledFileUploadTemporaryStatusRespVO> => {
  return parseControlledFileUploadTemporaryStatusResp(
    await request.post({
      url: '/dcc/controlled-files/upload-temporary/session-cleanup',
      data: { sessionId },
      headers: {
        ...buildDccExplicitTenantHeaders(),
        ...(requestId ? { [DCC_REQUEST_ID_HEADER]: requestId } : {})
      }
    })
  )
}

export const checkControlledFileRouteReadiness = async (
  data: ControlledFileRoutePreviewReqVO
): Promise<ControlledFileRouteReadinessVO> => {
  return await request.post({ url: '/dcc/controlled-files/route-preview', data })
}

export const getControlledFileTaskActionReadiness = async (
  id: number | string,
  data: ControlledFileTaskReadinessReqVO
): Promise<ControlledFileTaskReadinessVO> => {
  return await request.post({
    url: `/dcc/controlled-files/${id}/task-action-readiness`,
    data
  })
}

export const getControlledFileUploadNameOptions = async (params: {
  dccProjectCodeId: number
  fileTypeTaxonomyId: number
}): Promise<ControlledFileUploadNameOptionVO[]> => {
  return await request.get({ url: '/dcc/controlled-files/upload-name-options', params })
}

export const getControlledFileCurrentVersion = async (
  fileNumber: string
): Promise<ControlledFileCurrentVersionRespVO> => {
  return await request.get({
    url: '/dcc/controlled-files/current-version',
    params: { fileNumber }
  })
}

export const getControlledFileUploadDirectoryTree = async (
  categoryId: number
): Promise<ControlledFileUploadDirectoryTreeVO> => {
  return await request.get({
    url: '/dcc/controlled-files/upload-directory-tree',
    params: { categoryId }
  })
}

export const getControlledFileUploadRevisionCandidates = async (params: {
  dccProjectCodeId: number
  fileTypeTaxonomyId: number
  keyword?: string
  pageNo?: number
  pageSize?: number
}): Promise<PageResult<ControlledFileVO[]>> => {
  return await request.get({
    url: '/dcc/controlled-files/upload-revision-candidates',
    params
  })
}

export const getControlledFileBrowserPage = async (
  params: ControlledFilePageReqVO
): Promise<PageResult<ControlledFileVO[]>> => {
  return await request.get({ url: '/dcc/controlled-files/browser-page', params })
}

export const getControlledFileBrowserExtensionBlacklist = async (): Promise<ControlledFileBrowserExtensionBlacklistRespVO> => {
  return await request.get({ url: '/dcc/controlled-files/browser-extension-blacklist' })
}

export const saveControlledFileBrowserExtensionBlacklist = async (
  data: ControlledFileBrowserExtensionBlacklistSaveReqVO
): Promise<boolean> => {
  return await request.put({ url: '/dcc/controlled-files/browser-extension-blacklist', data })
}

export const getControlledFile = async (id: number | string): Promise<ControlledFileVO> => {
  return await request.get({ url: `/dcc/controlled-files/${id}` })
}

export const updateControlledFileMetadata = async (
  id: number | string,
  data: ControlledFileMetadataUpdateReqVO
): Promise<boolean> => {
  return await request.put({ url: `/dcc/controlled-files/${id}/metadata`, data })
}

export const exportControlledFileMetadataExcel = async (params: ControlledFilePageReqVO) => {
  return await request.download({ url: '/dcc/controlled-files/metadata/export-excel', params })
}

export const exportControlledFileRecognitionRecordExcel = async (params: ControlledFilePageReqVO) => {
  return await request.download({
    url: '/dcc/controlled-files/recognition-records/export-excel',
    params
  })
}

export const exportControlledFileRecognitionMigrationExcel = async (params: ControlledFilePageReqVO) => {
  return await request.download({
    url: '/dcc/controlled-files/recognition-records/migration-export-excel',
    params
  })
}

export const getControlledFileMetadataImportTemplate = async () => {
  return await request.download({ url: '/dcc/controlled-files/metadata/import-template' })
}

export const previewControlledFileMetadataImport = async (
  file: File
): Promise<ControlledFileMetadataImportPreviewRespVO> => {
  const data = new FormData()
  data.append('file', file)
  const result = await request.upload<{ data: ControlledFileMetadataImportPreviewRespVO }>({
    url: '/dcc/controlled-files/metadata/import-preview',
    data
  })
  return result.data
}

export const confirmControlledFileMetadataImport = async (
  file: File
): Promise<ControlledFileMetadataImportPreviewRespVO> => {
  const data = new FormData()
  data.append('file', file)
  const result = await request.upload<{ data: ControlledFileMetadataImportPreviewRespVO }>({
    url: '/dcc/controlled-files/metadata/import-confirm',
    data
  })
  return result.data
}

export const previewControlledFileRecognitionMigrationImport = async (
  file: File
): Promise<ControlledFileRecognitionMigrationImportPreviewRespVO> => {
  const data = new FormData()
  data.append('file', file)
  const result = await request.upload<{ data: ControlledFileRecognitionMigrationImportPreviewRespVO }>({
    url: '/dcc/controlled-files/recognition-records/migration-import-preview',
    data
  })
  return result.data
}

export const confirmControlledFileRecognitionMigrationImport = async (
  file: File
): Promise<ControlledFileRecognitionMigrationImportPreviewRespVO> => {
  const data = new FormData()
  data.append('file', file)
  const result = await request.upload<{ data: ControlledFileRecognitionMigrationImportPreviewRespVO }>({
    url: '/dcc/controlled-files/recognition-records/migration-import-confirm',
    data
  })
  return result.data
}

export const recognizeControlledFileProjectCode = async (
  id: number | string
): Promise<ControlledFileProjectCodeRecognitionRespVO> => {
  return await request.post({ url: `/dcc/controlled-files/${id}/recognize-project-code` })
}

export const createControlledFileBatchRecognitionTask = async (
  data: ControlledFileBatchRecognitionCreateReqVO
): Promise<ControlledFileBatchRecognitionTaskRespVO> => {
  return await request.post({ url: '/dcc/controlled-files/batch-recognition/tasks', data })
}

export const getControlledFileBatchRecognitionTask = async (
  taskId: number | string
): Promise<ControlledFileBatchRecognitionTaskRespVO> => {
  return await request.get({ url: `/dcc/controlled-files/batch-recognition/tasks/${taskId}` })
}

export const getLatestControlledFileBatchRecognitionTask = async (
  recognitionType: ControlledFileBatchRecognitionCreateReqVO['recognitionType']
): Promise<ControlledFileBatchRecognitionTaskRespVO | null> => {
  return await request.get({
    url: '/dcc/controlled-files/batch-recognition/tasks/latest',
    params: { recognitionType }
  })
}

export const stopControlledFileBatchRecognitionTask = async (
  taskId: number | string
): Promise<ControlledFileBatchRecognitionTaskRespVO> => {
  return await request.post({ url: `/dcc/controlled-files/batch-recognition/tasks/${taskId}/stop` })
}

export const withdrawControlledFile = async (id: number | string, data: ControlledFileWithdrawReqVO) => {
  return await request.post({ url: `/dcc/controlled-files/${id}/withdraw`, data })
}

export const deleteWithdrawnControlledFile = async (id: number | string): Promise<boolean> => {
  return await request.delete({ url: `/dcc/controlled-files/${id}/withdrawn-flow` })
}

export const resubmitWithdrawnControlledFile = async (id: number | string): Promise<number> => {
  return await request.post({ url: `/dcc/controlled-files/${id}/resubmit` })
}

export const uploadControlledFileTrainingRecord = async (
  id: number | string,
  data: ControlledFileTrainingRecordReqVO
): Promise<boolean> => {
  assertRequiredString(
    data as unknown as Record<string, unknown>,
    'sessionId',
    'DCC training record upload'
  )
  assertRequiredTicketRequest(
    data as unknown as Record<string, unknown>,
    'trainingRecordUploadTicket',
    'DCC training record upload'
  )
  return await request.post({ url: `/dcc/controlled-files/${id}/training-record`, data })
}

export const approveControlledFileTask = async (
  id: number | string,
  data: ControlledFileApproveTaskReqVO
): Promise<DccSignatureActionRespVO> => {
  assertNoForbiddenDccRequestFields(
    data as unknown as Record<string, unknown>,
    'DCC controlled file approve task'
  )
  const response = await postControlledFileTaskAction<unknown>(
    `/dcc/controlled-files/${id}/approve-task`,
    data
  )
  return parseDccSignatureActionResp(response, 'APPROVED')
}

export const approveExternalFileReviewTask = async (
  id: number | string,
  data: ExternalFileReviewApproveTaskReqVO
): Promise<boolean> => {
  assertNoForbiddenDccRequestFields(
    data as unknown as Record<string, unknown>,
    'DCC external file review approve task'
  )
  return await postControlledFileTaskAction<boolean>(
    `/dcc/external-file-reviews/${id}/approve-task`,
    data
  )
}

export const rejectControlledFileTask = async (
  id: number | string,
  data: ControlledFileRejectTaskReqVO
): Promise<DccSignatureActionRespVO> => {
  const response = await postControlledFileTaskAction<unknown>(
    `/dcc/controlled-files/${id}/reject-task`,
    data
  )
  return parseDccSignatureActionResp(response, 'REJECTED')
}

export const rejectExternalFileReviewTask = async (
  id: number | string,
  data: ControlledFileRejectTaskReqVO
): Promise<boolean> => {
  return await postControlledFileTaskAction<boolean>(
    `/dcc/external-file-reviews/${id}/reject-task`,
    data
  )
}

export const returnControlledFileTask = async (
  id: number | string,
  data: ControlledFileReturnTaskReqVO
): Promise<boolean> => {
  return await postControlledFileTaskAction<boolean>(`/dcc/controlled-files/${id}/return-task`, data)
}

export const returnExternalFileReviewTask = async (
  id: number | string,
  data: ControlledFileReturnTaskReqVO
): Promise<boolean> => {
  return await postControlledFileTaskAction<boolean>(
    `/dcc/external-file-reviews/${id}/return-task`,
    data
  )
}

export const transferControlledFileTask = async (
  id: number | string,
  data: ControlledFileTransferTaskReqVO
): Promise<boolean> => {
  return await postControlledFileTaskAction<boolean>(`/dcc/controlled-files/${id}/transfer-task`, data)
}

export const transferExternalFileReviewTask = async (
  id: number | string,
  data: ControlledFileTransferTaskReqVO
): Promise<boolean> => {
  return await postControlledFileTaskAction<boolean>(
    `/dcc/external-file-reviews/${id}/transfer-task`,
    data
  )
}

export const createControlledFileSignTask = async (
  id: number | string,
  data: ControlledFileCreateSignTaskReqVO
): Promise<boolean> => {
  return await postControlledFileTaskAction<boolean>(`/dcc/controlled-files/${id}/sign-task`, data)
}

export const createExternalFileReviewSignTask = async (
  id: number | string,
  data: ControlledFileCreateSignTaskReqVO
): Promise<boolean> => {
  return await postControlledFileTaskAction<boolean>(
    `/dcc/external-file-reviews/${id}/sign-task`,
    data
  )
}

export const retryControlledFileStamp = async (id: number | string) => {
  return await request.post({ url: `/dcc/controlled-files/${id}/stamp-retry` })
}

export const manualReleaseControlledFile = async (id: number | string): Promise<boolean> => {
  return await request.post({ url: `/dcc/controlled-files/${id}/manual-release` })
}

export const createControlledFilePrintRecord = async (
  id: number | string,
  data: ControlledFilePrintCreateReqVO
): Promise<ControlledFilePrintRecordVO> => {
  return await request.post({ url: `/dcc/controlled-files/${id}/controlled-print`, data })
}

export const getControlledFilePrintRecords = async (
  id: number | string
): Promise<ControlledFilePrintRecordVO[]> => {
  return await request.get({ url: `/dcc/controlled-files/${id}/controlled-print/records` })
}

export const getControlledFilePrintHtml = async (
  id: number | string,
  printRecordId: number | string
): Promise<ControlledFilePrintHtmlVO> => {
  return await request.get({
    url: `/dcc/controlled-files/${id}/controlled-print/print-html`,
    params: { printRecordId }
  })
}

export const getAxiosHeader = (headers: AxiosResponse['headers'], headerName: string): unknown => {
  const headerGetter = headers as { get?: (name: string) => unknown }
  const fromGetter = headerGetter.get?.(headerName)
  if (fromGetter !== undefined && fromGetter !== null && !isBlankString(fromGetter)) {
    return fromGetter
  }
  const headerMap = headers as Record<string, unknown>
  return headerMap[headerName] ?? headerMap[headerName.toLowerCase()]
}

export const decodePreviewWatermark = (rawHeader: unknown): ControlledPreviewWatermark => {
  const encoded = typeof rawHeader === 'string' ? rawHeader.trim() : ''
  if (!encoded) {
    throw new DccControlledFileContractError('DCC preview response missing required watermark header')
  }
  try {
    const normalized = encoded.replace(/-/g, '+').replace(/_/g, '/')
    const paddingLength = (4 - (normalized.length % 4 || 4)) % 4
    const padded = normalized.padEnd(normalized.length + paddingLength, '=')
    const decoded = atob(padded)
    const bytes = Uint8Array.from(decoded, (char) => char.charCodeAt(0))
    const text = new TextDecoder().decode(bytes)
    const payload = JSON.parse(text)
    return parseControlledPreviewWatermark(
      assertRecordPayload(payload, 'DCC preview watermark header'),
      'DCC preview watermark header'
    )
  } catch (error) {
    if (error instanceof DccControlledFileContractError) {
      throw error
    }
    throw new DccControlledFileContractError('DCC preview response watermark header is invalid', error)
  }
}

const buildControlledFileDownloadEndpoint = (id: number | string) =>
  `${axiosConfig.base_url}/dcc/controlled-files/${id}/download`

const decodeContentDispositionFileName = (rawHeader: unknown): string | null => {
  const contentDisposition = typeof rawHeader === 'string' ? rawHeader.trim() : ''
  if (!contentDisposition) {
    return null
  }
  const utf8Match = contentDisposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    const encodedFileName = utf8Match[1].trim().replace(/^"(.*)"$/, '$1')
    try {
      return decodeURIComponent(encodedFileName)
    } catch (error) {
      throw new DccControlledFileContractError('DCC download response filename is invalid', error)
    }
  }
  const plainMatch = contentDisposition.match(/filename\s*=\s*("?)([^";]+)\1/i)
  return plainMatch?.[2]?.trim() || null
}

const assertControlledFileDownloadHeaders = (
  response: AxiosResponse<Blob>,
  expectedDownloadRequestId: string
): { fileName: string; evidence: ControlledFileDownloadEvidence } => {
  const evidence = {} as Record<keyof ControlledFileDownloadEvidence, string>
  for (const header of DCC_DOWNLOAD_REQUIRED_RESPONSE_HEADERS) {
    const value = getAxiosHeader(response.headers, header.name)
    if (typeof value !== 'string' || !value.trim()) {
      throw new DccControlledFileContractError(`DCC download response missing required header: ${header.name}`)
    }
    evidence[header.target] = value.trim()
  }
  if (evidence.downloadRequestId !== expectedDownloadRequestId) {
    throw new DccControlledFileContractError('DCC download response request id mismatch')
  }
  const fileName = decodeContentDispositionFileName(
    getAxiosHeader(response.headers, 'Content-Disposition')
  )
  if (!fileName) {
    throw new DccControlledFileContractError('DCC download response missing required filename')
  }
  return {
    fileName,
    evidence: evidence as ControlledFileDownloadEvidence
  }
}

const assertNoClientProvidedDownloadName = (clientProvidedFileName: unknown) => {
  if (clientProvidedFileName !== undefined && clientProvidedFileName !== null) {
    throw new DccControlledFileContractError(
      'DCC download API no longer accepts a client-provided file name'
    )
  }
}

const createControlledFileDownloadRequestId = (): string => {
  const crypto = globalThis.crypto
  if (!crypto || typeof crypto.getRandomValues !== 'function') {
    throw new DccControlledFileContractError('DCC download request id requires crypto.getRandomValues()')
  }
  const bytes = new Uint8Array(16)
  crypto.getRandomValues(bytes)
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = Array.from(bytes, (item) => item.toString(16).padStart(2, '0'))
  return `${hex.slice(0, 4).join('')}-${hex.slice(4, 6).join('')}-${hex
    .slice(6, 8)
    .join('')}-${hex.slice(8, 10).join('')}-${hex.slice(10, 16).join('')}`
}

export const createControlledFileUploadSessionId = (): string =>
  `dcc-upload-${createControlledFileDownloadRequestId()}`

export const previewControlledFileWithWatermark = async (
  id: number | string,
  metadata?: ControlledFilePreviewMetadataVO
): Promise<ControlledFilePreviewWithWatermark> => {
  const resolvedMetadata = metadata || (await getControlledFilePreviewMetadata(id))
  const response = await axios.get<Blob>(`${axiosConfig.base_url}/dcc/controlled-files/${id}/preview`, {
    headers: buildControlledFileBinaryHeaders({
      [DCC_VIEWER_TOKEN_HEADER]: resolvedMetadata.viewerToken,
      [DCC_VIEWER_TOKEN_ID_HEADER]: resolvedMetadata.viewerTokenId,
      [DCC_VIEWER_TOKEN_NONCE_HEADER]: resolvedMetadata.viewerTokenNonce,
      [DCC_ACCESS_EVENT_CODE_HEADER]: resolvedMetadata.accessEventCode,
      [DCC_WATERMARK_TRACE_CODE_HEADER]: resolvedMetadata.watermarkTraceCode
    }),
    timeout: axiosConfig.request_timeout,
    responseType: 'blob'
  })
  const watermark = decodePreviewWatermark(
    getAxiosHeader(response.headers, CONTROLLED_FILE_PREVIEW_WATERMARK_HEADER)
  )
  return {
    blob: response.data,
    watermark: {
      ...watermark,
      traceCode: resolvedMetadata.watermarkTraceCode
    }
  }
}

export const previewControlledFile = async (id: number | string): Promise<Blob> => {
  return (await previewControlledFileWithWatermark(id)).blob
}

export const downloadControlledFile = async (id: number | string): Promise<Blob> => {
  return (await downloadControlledFileWithName(id)).blob
}

export const confirmControlledFileDownload = async (): Promise<boolean> => {
  try {
    await ElMessageBox.confirm(
      CONTROLLED_FILE_DOWNLOAD_CONFIRM_MESSAGE,
      CONTROLLED_FILE_DOWNLOAD_CONFIRM_TITLE,
      {
        confirmButtonText: '确认下载',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    return true
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return false
    }
    throw error
  }
}

export const downloadControlledFileWithName = async (
  id: number | string,
  clientProvidedFileName?: string,
  options: ControlledFileDownloadOptions = {}
): Promise<ControlledFileDownloadResult> => {
  assertNoClientProvidedDownloadName(clientProvidedFileName)
  const downloadRequestId = createControlledFileDownloadRequestId()
  const response = await axios.get<Blob>(buildControlledFileDownloadEndpoint(id), {
    headers: buildControlledFileBinaryHeaders(),
    params: {
      nonControlledWarningConfirmed: options.nonControlledWarningConfirmed ?? true,
      downloadRequestId
    },
    timeout: axiosConfig.request_timeout,
    responseType: 'blob'
  })
  const { fileName, evidence } = assertControlledFileDownloadHeaders(response, downloadRequestId)
  return {
    blob: response.data,
    fileName,
    evidence
  }
}

export const getControlledFilePreviewMetadata = async (
  id: number | string
): Promise<ControlledFilePreviewMetadataVO> => {
  return parseControlledFilePreviewMetadata(
    await request.get({ url: `/dcc/controlled-files/${id}/preview-metadata` })
  )
}

export const triggerControlledFileDownload = async (
  id: number | string,
  clientProvidedFileName?: string
): Promise<boolean> => {
  assertNoClientProvidedDownloadName(clientProvidedFileName)
  const confirmed = await confirmControlledFileDownload()
  if (!confirmed) {
    return false
  }
  const { blob, fileName } = await downloadControlledFileWithName(id, undefined, {
    nonControlledWarningConfirmed: true
  })
  downloadByData(blob, fileName, blob.type || 'application/octet-stream')
  return true
}

export const obsoleteControlledFile = async (
  id: number | string,
  data: ControlledFileObsoleteReqVO
): Promise<FormInstanceVO> => {
  return await request.post({ url: `/dcc/controlled-files/${id}/obsolete`, data })
}

export const publishControlledFile = async (
  id: number | string,
  data: ControlledFilePublishReqVO
): Promise<FormInstanceVO> => {
  return await request.post({ url: `/dcc/controlled-files/${id}/publish`, data })
}

export const acknowledgeControlledFileTraining = async (id: number | string): Promise<boolean> => {
  return await request.post({ url: `/dcc/controlled-files/${id}/training-acknowledge` })
}

export const transferNasDirectories = async (
  data: ControlledFileNasTransferReqVO
): Promise<ControlledFileNasTransferRespVO> => {
  return await request.post({ url: '/dcc/controlled-files/nas-transfer', data })
}

export const getControlledFileAccessExplanation = async (
  id: number | string
): Promise<ControlledFileAccessExplanationVO> => {
  return await request.get({ url: `/dcc/controlled-files/${id}/access-explanation` })
}

export const importLocalFolderToDcc = async (
  data: FormData
): Promise<ControlledFileNasTransferRespVO> => {
  const result = await request.upload<{ data: ControlledFileNasTransferRespVO }>({
    url: '/dcc/controlled-files/local-folder-import',
    data,
    timeout: LOCAL_FOLDER_IMPORT_REQUEST_TIMEOUT,
    headersType: 'multipart/form-data'
  })
  return result.data
}

export const createLocalFolderImportSession = async (
  data: ControlledFileLocalFolderImportSessionCreateReqVO
): Promise<ControlledFileNasTransferRespVO> => {
  return await request.post({ url: '/dcc/controlled-files/local-folder-import/sessions', data })
}

export const uploadLocalFolderImportBatch = async (
  taskId: number | string,
  data: FormData
): Promise<ControlledFileNasTransferRespVO> => {
  const result = await request.upload<{ data: ControlledFileNasTransferRespVO }>({
    url: `/dcc/controlled-files/local-folder-import/sessions/${taskId}/batches`,
    data,
    timeout: LOCAL_FOLDER_IMPORT_REQUEST_TIMEOUT,
    headersType: 'multipart/form-data'
  })
  return result.data
}

export const getLocalFolderImportUploadState = async (
  taskId: number | string
): Promise<ControlledFileLocalFolderImportUploadStateRespVO> => {
  return await request.get({
    url: `/dcc/controlled-files/local-folder-import/sessions/${taskId}/upload-state`
  })
}

export const uploadLocalFolderImportChunk = async (
  taskId: number | string,
  data: FormData
): Promise<{
  taskId: number
  relativePath: string
  uploadedChunkCount: number
  totalChunks: number
  fileCompleted: boolean
  task: ControlledFileNasTransferRespVO
}> => {
  const result = await request.upload<{
    data: {
      taskId: number
      relativePath: string
      uploadedChunkCount: number
      totalChunks: number
      fileCompleted: boolean
      task: ControlledFileNasTransferRespVO
    }
  }>({
    url: `/dcc/controlled-files/local-folder-import/sessions/${taskId}/chunks`,
    data,
    timeout: LOCAL_FOLDER_IMPORT_REQUEST_TIMEOUT,
    headersType: 'multipart/form-data'
  })
  return result.data
}

export const completeLocalFolderImportSession = async (
  taskId: number | string
): Promise<ControlledFileNasTransferRespVO> => {
  return await request.post({
    url: `/dcc/controlled-files/local-folder-import/sessions/${taskId}/complete`
  })
}

export const getNasTransferTaskState = async (
  taskId: number | string
): Promise<ControlledFileNasTransferRespVO> => {
  return await request.get({ url: `/dcc/controlled-files/nas-transfer/tasks/${taskId}` })
}

export const getNasPermissionSnapshotSummary = async (
  taskId: number | string
): Promise<NasPermissionSnapshotSummaryVO> => {
  return await request.get({
    url: `/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-snapshot`
  })
}

export const getNasPermissionSnapshotItems = async (
  taskId: number | string,
  params: NasPermissionSnapshotPageReqVO
): Promise<PageResult<NasPermissionSnapshotItemVO[]>> => {
  return await request.get({
    url: `/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-snapshot/items`,
    params
  })
}

export const getNasUnmappedPrincipals = async (
  taskId: number | string
): Promise<NasUnmappedPrincipalListVO> => {
  return await request.get({
    url: '/dcc/nas-permission/principals/unmapped',
    params: { taskId }
  })
}

export const saveNasPrincipalMapping = async (
  data: NasPrincipalMappingSaveReqVO
): Promise<NasPrincipalMappingRespVO> => {
  return await request.put({ url: '/dcc/nas-permission/principal-mappings', data })
}

export const previewNasPermissionRestore = async (
  taskId: number | string
): Promise<NasPermissionRestorePreviewVO> => {
  return await request.get({
    url: `/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-restore/preview`
  })
}

export const applyNasPermissionRestore = async (
  taskId: number | string,
  data: NasPermissionRestoreApplyReqVO
): Promise<NasPermissionRestoreApplyRespVO> => {
  return await request.post({
    url: `/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-restore`,
    data
  })
}

export const exportAdminConfigPackage = async (): Promise<Blob> => {
  return await request.download({ url: '/dcc/file-categories/admin-config-package/export' })
}

export const exportDmrSheetWorkbook = async (): Promise<Blob> => {
  return await request.download({
    url: '/dcc/controlled-files/dmr-sheet/export',
    timeout: DCC_ADMIN_FULL_CONFIG_IMPORT_REQUEST_TIMEOUT
  })
}

export const importAdminConfigPackage = async (
  data: FormData
): Promise<DccAdminFullConfigPackageImportRespVO> => {
  const result = await request.upload<{ data: DccAdminFullConfigPackageImportRespVO }>({
    url: '/dcc/file-categories/admin-config-package/import',
    data,
    timeout: DCC_ADMIN_FULL_CONFIG_IMPORT_REQUEST_TIMEOUT,
    headersType: 'multipart/form-data'
  })
  return result.data
}

export const getNasPermissionRestoreStatus = async (
  taskId: number | string,
  restoreId: number | string
): Promise<NasPermissionRestoreStatusVO> => {
  return await request.get({
    url: `/dcc/controlled-files/nas-transfer/tasks/${taskId}/permission-restore/${restoreId}`
  })
}

export const acknowledgePaperDistribution = async (
  id: number | string,
  distributionId: number | string,
  data: ControlledFilePaperDistributionIssueReqVO
): Promise<boolean> => {
  return await request.post({
    url: `/dcc/controlled-files/${id}/paper-distributions/${distributionId}/acknowledge`,
    data
  })
}

export const getPaperDistributionRecords = async (
  id: number | string
): Promise<ControlledFilePaperDistributionRecordVO[]> => {
  return await request.get({ url: `/dcc/controlled-files/${id}/paper-distributions/records` })
}

export const recoverPaperDistribution = async (
  id: number | string,
  distributionId: number | string
): Promise<boolean> => {
  return await request.post({
    url: `/dcc/controlled-files/${id}/paper-distributions/${distributionId}/recover`
  })
}

export const acknowledgeElectronicDistribution = async (
  id: number | string,
  distributionId: number | string,
  recipientId: number | string,
  data: ControlledFileDistributionRecipientAckReqVO
): Promise<boolean> => {
  return await postControlledFileTaskAction<boolean>(
    `/dcc/controlled-files/${id}/distributions/${distributionId}/recipients/${recipientId}/acknowledge`,
    data
  )
}

export const createDistributionRecipientSignTask = async (
  id: number | string,
  distributionId: number | string,
  recipientId: number | string,
  data: ControlledFileDistributionRecipientSignReqVO
): Promise<boolean> => {
  return await postControlledFileTaskAction<boolean>(
    `/dcc/controlled-files/${id}/distributions/${distributionId}/recipients/${recipientId}/sign`,
    data
  )
}

export const buildControlledFilePreviewUrl = (id: number | string) =>
  `${import.meta.env.VITE_BASE_URL}${import.meta.env.VITE_API_URL}/dcc/controlled-files/${id}/preview`
