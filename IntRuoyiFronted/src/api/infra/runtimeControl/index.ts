import request from '@/config/axios'

export type RuntimeControlDateTime = string | number | number[]
export type RuntimeOpsInspectionStatus = 'PASS' | 'WARN' | 'BLOCKED' | 'NO_GO'
export type RuntimeControlSiteMessageStatus = 'SENT' | 'FAILED' | 'BLOCKED'
export type RuntimeControlCandidateStatus = 'AVAILABLE' | 'BLOCKED'
export type RuntimeControlPublishScope = 'code-only' | 'with-data'
export type RuntimeControlTargetEnvironment = 'test' | 'prod' | 'backup'
export type RuntimeControlRootDiskTargetEnvironment = 'test' | 'prod' | 'backup'

export interface RuntimeControlOperationVO {
  operationId: string
  requestedBy: string
  requestedAt: RuntimeControlDateTime
  environment: string
  component: string
  action?: string
  actionLabel?: string
  parameters?: Record<string, string>
  reason: string
  status: string
  summary?: string
  resultLogPath?: string
}

export interface RuntimeControlStatusVO {
  status: string
  httpStatus?: string
  runtimeState?: string
  url?: string
  port?: number
  currentReleaseTag?: string
  lastOperation?: RuntimeControlOperationVO
  actionEnabled: boolean
  blockedReason?: string
}

export interface RuntimeControlOverviewVO {
  environments: string[]
  components: string[]
  statuses: Record<string, Record<string, RuntimeControlStatusVO>>
}

export interface RuntimeControlRestartReqVO {
  environment: string
  component: string
  reason: string
  prodConfirmText?: string
}

export interface RuntimeControlActionReqVO {
  action: string
  reason: string
  prodConfirmText?: string
  targetEnvironment?: RuntimeControlTargetEnvironment
  publishScope?: RuntimeControlPublishScope
  includeOnlyOffice?: boolean
  includeShowroomBuildPackage?: boolean
  enableSmartReleaseReport?: boolean
  releaseTag?: string
  testConclusion?: string
  sqlPath?: string
  selectedImageCandidateId?: string
  selectedRecoverySetCandidateId?: string
}

export interface RuntimeControlActionPreviewVO {
  action: string
  actionLabel?: string
  environment: string
  component: string
  scriptPath: string
  arguments: string[]
  parameters?: Record<string, string>
  enableSmartReleaseReport: boolean
  summary?: string
}

export interface RuntimeControlLogVO {
  operationId: string
  status: string
  content: string
  length: number
  truncated: boolean
  logPath?: string
}

export interface RuntimeControlAlertPageReqVO extends PageParam {
  environment?: string
  action?: string
  siteMessageStatus?: RuntimeControlSiteMessageStatus
}

export interface RuntimeControlAlertCreateReqVO {
  environment: string
  action: string
  severity: string
  title: string
  content: string
  notifyTemplateCode?: string
  templateParams?: Record<string, unknown>
}

export interface RuntimeControlAlertVO {
  id: number
  environment: string
  action: string
  severity: string
  title: string
  content: string
  notifyTemplateCode?: string
  templateParams?: Record<string, unknown>
  siteMessageStatus: RuntimeControlSiteMessageStatus
  notifyMessageId?: number
  siteMessageFailureReason?: string
  createdAt?: RuntimeControlDateTime
  sentAt?: RuntimeControlDateTime
  acknowledgedBy?: string
  acknowledgedAt?: RuntimeControlDateTime
}

export interface RuntimeControlOwnerMatrixReqVO {
  environment: string
  action: string
  role: string
  required: boolean
  ownerUserId?: number
  ownerName?: string
  escalationPath?: string
}

export interface RuntimeControlOwnerMatrixVO extends RuntimeControlOwnerMatrixReqVO {
  id: number
  updatedAt?: RuntimeControlDateTime
}

export interface RuntimeControlWizardScenarioVO {
  scenario: string
  label: string
  recommendedAction: string
  recommendedActionLabel: string
  symptoms?: string[]
  requiredEvidence?: string[]
  requiredOwnerRoles?: string[]
  blockingConditions?: string[]
}

export interface RuntimeControlWizardRecommendationReqVO {
  scenario: string
}

export interface RuntimeControlCandidateVO {
  candidateId: string
  backupId?: string
  releaseTag?: string
  imageTag?: string
  manifestPath?: string
  status: RuntimeControlCandidateStatus
  blockedReasons?: string[]
}

export interface RuntimeControlRollbackCandidateVO extends RuntimeControlCandidateVO {
  prodHistoryPath?: string
  compatibilityStatus?: string
  compatibilityEvidencePath?: string
  compatibilityCheckedAt?: string
  compatibilitySummary?: string
}

export interface RuntimeControlRestoreCandidateVO extends RuntimeControlCandidateVO {
  recoverySetId?: string
  recoverySetStatus?: RuntimeControlCandidateStatus
  programVersion?: string
  redisPolicy?: string
  configurationManifestPath?: string
  recoverySetManifestHash?: string
  componentSummary?: Record<string, string>
  dccBackupMode?: string
  dccChainStatus?: string
  dccChangeSummary?: Record<string, string>
  checksumPath?: string
  rehearsalReportPath?: string
  snapshotPath?: string
}

export interface RuntimeControlReleasePackageVO {
  releaseTag: string
  packageDirectoryName?: string
  manifestPath?: string
  builtAt?: string
  publishScope?: RuntimeControlPublishScope
  component?: string
  includeShowroomBuildPackage?: boolean
  onlyOfficeIncluded?: boolean
  imageTag?: string
  checksumPresent?: boolean
  tested?: boolean
  testedAt?: string
  operatorName?: string
  testedRecoverySetCandidateId?: string
  testedRecoverySetId?: string
  testedRecoverySetManifestHash?: string
  status?: RuntimeControlCandidateStatus
  blockedReasons?: string[]
}

export interface RuntimeControlReleaseStatusVO {
  releasePackages: RuntimeControlReleasePackageVO[]
  targetStates: Record<string, Record<string, RuntimeControlStatusVO>>
  recentOperations: RuntimeControlOperationVO[]
  testCurrentReleaseTag?: string
  latestTestedReleaseTag?: string
}

export interface RuntimeControlWizardRecommendationVO {
  scenario: string
  recommendedAction: string
  recommendedActionLabel: string
  requiredEvidence?: string[]
  requiredOwnerRoles?: string[]
  blockingReasons?: string[]
  rollbackCandidates?: RuntimeControlRollbackCandidateVO[]
  restoreCandidates?: RuntimeControlRestoreCandidateVO[]
}

export interface RuntimeControlInspectionCheckVO {
  code: string
  name: string
  status: RuntimeOpsInspectionStatus
  required: boolean
  evidence?: string
  reason?: string
  sampledAt?: RuntimeControlDateTime
}

export interface RuntimeControlInspectionRunVO {
  id: number
  status: RuntimeOpsInspectionStatus
  summary?: string
  startedAt?: RuntimeControlDateTime
  completedAt?: RuntimeControlDateTime
  checks?: RuntimeControlInspectionCheckVO[]
}

export interface RuntimeControlBusinessHealthItemVO {
  code: string
  name: string
  status: RuntimeOpsInspectionStatus
  evidence?: string
  reason?: string
  sampledAt?: RuntimeControlDateTime
}

export interface RuntimeControlBusinessHealthVO {
  status: RuntimeOpsInspectionStatus
  sampledAt?: RuntimeControlDateTime
  items?: RuntimeControlBusinessHealthItemVO[]
}

export interface RuntimeControlProbeVO {
  environment: string
  component: string
  probeType: string
  url?: string
  status: RuntimeOpsInspectionStatus
  httpStatusCode?: number
  durationMillis?: number
  error?: string
  sampledAt?: RuntimeControlDateTime
}

export interface RuntimeControlProbeLatestVO {
  status: RuntimeOpsInspectionStatus
  sampledAt?: RuntimeControlDateTime
  probes?: RuntimeControlProbeVO[]
  alert?: RuntimeControlAlertVO
}

export interface RuntimeControlStorageMetricVO {
  path?: string
  status: RuntimeOpsInspectionStatus
  totalBytes?: number
  usableBytes?: number
  usedBytes?: number
  usagePercent?: number
  sizeBytes?: number
  growthBytes?: number
  reason?: string
}

export interface RuntimeControlCapacityStatusVO {
  status: RuntimeOpsInspectionStatus
  sampledAt?: RuntimeControlDateTime
  disk?: RuntimeControlStorageMetricVO
  logDirectory?: RuntimeControlStorageMetricVO
  reasons?: string[]
  alert?: RuntimeControlAlertVO
}

export interface RuntimeControlRemoteRootDiskStatusVO {
  targetEnvironment: RuntimeControlRootDiskTargetEnvironment
  serverHost: string
  mountPoint: string
  filesystem?: string
  totalBytes?: number
  usedBytes?: number
  availableBytes?: number
  usagePercent?: number
  inodeTotal?: number
  inodeUsed?: number
  inodeAvailable?: number
  inodeUsagePercent?: number
  backupTempBytes?: number
  tmpBytes?: number
  sampledAt?: RuntimeControlDateTime
}

export interface RuntimeControlRemoteRootCleanupReqVO {
  targetEnvironment: RuntimeControlRootDiskTargetEnvironment
  reason: string
  prodConfirmText?: string
}

export interface RuntimeControlRemoteRootCleanupVO {
  targetEnvironment: RuntimeControlRootDiskTargetEnvironment
  serverHost: string
  cleanupPaths: string[]
  before?: RuntimeControlRemoteRootDiskStatusVO
  after?: RuntimeControlRemoteRootDiskStatusVO
  deletedEntryCount?: number
  requestedBy?: string
  reason?: string
  cleanedAt?: RuntimeControlDateTime
}

export interface RuntimeControlBackupPointVO {
  backupId: string
  manifestPath?: string
  checksumPath?: string
  rehearsalReportPath?: string
  snapshotPath?: string
  lastVerifiedAt?: RuntimeControlDateTime
  imageTag?: string
  backupMode?: string
  retentionKeepLast?: number
  retentionKeepDays?: number
  retentionMaxNasUsedPercent?: number
  objectAddedCount?: number
  objectModifiedCount?: number
  objectDeletedCount?: number
  objectReusedCount?: number
  recoverabilityStatus?: string
  dccBackupMode?: string
  dccChainStatus?: string
  dccChangeSummary?: Record<string, string>
  rehearsalStatus?: string
  unrecoverableReasons?: string[]
}

export interface RuntimeControlIncidentActionReqVO {
  action: string
  verificationResult: string
  evidence: string
}

export interface RuntimeControlIncidentActionVO extends RuntimeControlIncidentActionReqVO {
  operator?: string
  actedAt?: RuntimeControlDateTime
}

export interface RuntimeControlIncidentCreateReqVO {
  environment: string
  action: string
  severity: string
  title: string
  description: string
  sourceType: string
  sourceId?: string
}

export interface RuntimeControlIncidentCloseReqVO {
  ownerGateResult: string
  verificationResult: string
  remainingRisk: string
  postmortemStatus: string
  closeReason: string
}

export interface RuntimeControlIncidentPageReqVO extends PageParam {
  environment?: string
  status?: string
  sourceType?: string
}

export interface RuntimeControlIncidentVO {
  id: number
  environment: string
  action: string
  severity: string
  title: string
  description: string
  sourceType: string
  sourceId?: string
  status: string
  createdBy?: string
  createdAt?: RuntimeControlDateTime
  actions?: RuntimeControlIncidentActionVO[]
  ownerGateResult?: string
  verificationResult?: string
  remainingRisk?: string
  postmortemStatus?: string
  closeReason?: string
  closedBy?: string
  closedAt?: RuntimeControlDateTime
}

const RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT = 120000

export const getRuntimeControlOverview = () => {
  return request.get<RuntimeControlOverviewVO>({
    url: '/infra/runtime-control/overview',
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const restartRuntimeControl = (data: RuntimeControlRestartReqVO) => {
  return request.post<RuntimeControlOperationVO>({ url: '/infra/runtime-control/restart', data })
}

export const executeRuntimeControlAction = (data: RuntimeControlActionReqVO) => {
  return request.post<RuntimeControlOperationVO>({ url: '/infra/runtime-control/actions', data })
}

export const previewRuntimeControlAction = (data: RuntimeControlActionReqVO) => {
  return request.post<RuntimeControlActionPreviewVO>({
    url: '/infra/runtime-control/actions/preview',
    data,
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const getRuntimeControlOperations = () => {
  return request.get<RuntimeControlOperationVO[]>({
    url: '/infra/runtime-control/operations',
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const getRuntimeControlOperationLog = (operationId: string, maxBytes = 65536) => {
  return request.get<RuntimeControlLogVO>({
    url: `/infra/runtime-control/operations/${operationId}/log`,
    params: { maxBytes },
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const getRuntimeControlAlertsPage = (params: RuntimeControlAlertPageReqVO) => {
  return request.get<PageResult<RuntimeControlAlertVO[]>>({
    url: '/infra/runtime-control/alerts/page',
    params,
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const createRuntimeControlAlert = (data: RuntimeControlAlertCreateReqVO) => {
  return request.post<RuntimeControlAlertVO>({ url: '/infra/runtime-control/alerts', data })
}

export const resendRuntimeControlAlertSiteMessage = (id: number) => {
  return request.post<RuntimeControlAlertVO>({
    url: `/infra/runtime-control/alerts/${id}/resend-site-message`
  })
}

export const acknowledgeRuntimeControlAlert = (id: number) => {
  return request.post<RuntimeControlAlertVO>({
    url: `/infra/runtime-control/alerts/${id}/acknowledge`
  })
}

export const getRuntimeControlOwnerMatrix = (params?: { environment?: string; action?: string }) => {
  return request.get<RuntimeControlOwnerMatrixVO[]>({
    url: '/infra/runtime-control/owner-matrix',
    params,
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const createRuntimeControlOwnerMatrix = (data: RuntimeControlOwnerMatrixReqVO) => {
  return request.post<RuntimeControlOwnerMatrixVO>({
    url: '/infra/runtime-control/owner-matrix',
    data
  })
}

export const updateRuntimeControlOwnerMatrix = (id: number, data: RuntimeControlOwnerMatrixReqVO) => {
  return request.put<RuntimeControlOwnerMatrixVO>({
    url: `/infra/runtime-control/owner-matrix/${id}`,
    data
  })
}

export const getRuntimeControlWizardScenarios = () => {
  return request.get<RuntimeControlWizardScenarioVO[]>({
    url: '/infra/runtime-control/wizard/scenarios',
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const getRuntimeControlWizardRecommendation = (
  data: RuntimeControlWizardRecommendationReqVO
) => {
  return request.post<RuntimeControlWizardRecommendationVO>({
    url: '/infra/runtime-control/wizard/recommendation',
    data,
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const getRuntimeControlRollbackCandidates = () => {
  return request.get<RuntimeControlRollbackCandidateVO[]>({
    url: '/infra/runtime-control/rollback-candidates',
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const getRuntimeControlRestoreCandidates = () => {
  return request.get<RuntimeControlRestoreCandidateVO[]>({
    url: '/infra/runtime-control/restore-candidates',
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const getRuntimeControlReleasePackages = () => {
  return request.get<RuntimeControlReleasePackageVO[]>({
    url: '/infra/runtime-control/release-packages',
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const getRuntimeControlReleaseStatus = () => {
  return request.get<RuntimeControlReleaseStatusVO>({
    url: '/infra/runtime-control/release-status',
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const runRuntimeControlInspection = () => {
  return request.post<RuntimeControlInspectionRunVO>({
    url: '/infra/runtime-control/inspection-runs',
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const getRuntimeControlInspectionRun = (id: number) => {
  return request.get<RuntimeControlInspectionRunVO>({
    url: `/infra/runtime-control/inspection-runs/${id}`
  })
}

export const getRuntimeControlBusinessHealth = () => {
  return request.get<RuntimeControlBusinessHealthVO>({
    url: '/infra/runtime-control/business-health',
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const runRuntimeControlProbes = () => {
  return request.post<RuntimeControlProbeLatestVO>({
    url: '/infra/runtime-control/probes/run',
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const getRuntimeControlLatestProbes = () => {
  return request.get<RuntimeControlProbeLatestVO>({
    url: '/infra/runtime-control/probes/latest',
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const getRuntimeControlCapacityStatus = () => {
  return request.get<RuntimeControlCapacityStatusVO>({
    url: '/infra/runtime-control/capacity/status',
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const getRuntimeControlRemoteRootDiskStatus = (
  targetEnvironment: RuntimeControlRootDiskTargetEnvironment
) => {
  return request.get<RuntimeControlRemoteRootDiskStatusVO>({
    url: '/infra/runtime-control/remote-root-disk/status',
    params: { targetEnvironment },
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const cleanupRemoteRootTemporaryFiles = (data: RuntimeControlRemoteRootCleanupReqVO) => {
  return request.post<RuntimeControlRemoteRootCleanupVO>({
    url: '/infra/runtime-control/remote-root-disk/cleanup',
    data,
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const getRuntimeControlBackupPoints = () => {
  return request.get<RuntimeControlBackupPointVO[]>({
    url: '/infra/runtime-control/backup-points',
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const getRuntimeControlBackupPoint = (backupId: string) => {
  return request.get<RuntimeControlBackupPointVO>({
    url: `/infra/runtime-control/backup-points/${backupId}`
  })
}

export const getRuntimeControlIncidentsPage = (params: RuntimeControlIncidentPageReqVO) => {
  return request.get<PageResult<RuntimeControlIncidentVO[]>>({
    url: '/infra/runtime-control/incidents/page',
    params,
    timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT
  })
}

export const createRuntimeControlIncident = (data: RuntimeControlIncidentCreateReqVO) => {
  return request.post<RuntimeControlIncidentVO>({
    url: '/infra/runtime-control/incidents',
    data
  })
}

export const recordRuntimeControlIncidentAction = (
  id: number,
  data: RuntimeControlIncidentActionReqVO
) => {
  return request.post<RuntimeControlIncidentVO>({
    url: `/infra/runtime-control/incidents/${id}/actions`,
    data
  })
}

export const closeRuntimeControlIncident = (id: number, data: RuntimeControlIncidentCloseReqVO) => {
  return request.post<RuntimeControlIncidentVO>({
    url: `/infra/runtime-control/incidents/${id}/close`,
    data
  })
}
