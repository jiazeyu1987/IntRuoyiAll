import request from '@/config/axios'

export interface DeviationPageReqVO extends PageParam {
  listFilters?: string
  status?: 'OPEN' | 'CLOSED'
  level?: 'NORMAL' | 'CRITICAL'
  activeOrderId?: number | string
  batchExecutionId?: number | string
  search?: string
  sortField?: string
  sortOrder?: 'asc' | 'desc'
  initiatedAtStart?: string
  initiatedAtEnd?: string
}

export interface DeviationBatchOptionRespVO {
  batchExecutionId: number | string
  batchExecutionCode?: string
  batchCode?: string
  workOrderCode?: string
  productName?: string
  batchStatus?: string
}

export interface DeviationOrderOptionRespVO {
  workOrderId: number | string
  workOrderCode: string
  batchCode?: string
}

export interface DeviationCreateReqVO {
  workOrderId?: number | string
  activeOrderId?: number | string
  batchExecutionId?: number
  level: 'NORMAL' | 'CRITICAL'
  categoryCodes: string[]
  description: string
  levelBasis: string
  discoveryLocation?: string
  emergencyAction?: string
  receiverId?: number | string
  receiverName?: string
  idempotencyKey: string
  signaturePassword: string
}

export interface DeviationRespVO {
  receiverId?: number | string
  version?: number
  canTransferToNcr?: boolean
  transferToNcrBlockedReason?: string
  id: number
  deviationCode: string
  activeOrderId?: number | string
  batchExecutionId?: number
  batchExecutionCode?: string
  workOrderCode?: string
  batchCode?: string
  level: 'NORMAL' | 'CRITICAL'
  status: 'OPEN' | 'CLOSED'
  initiatedAt?: string | number
  initiatorSignatureId?: number
  initiatorContentHash?: string
  description?: string
  discoveryDepartmentName?: string
  discovererName?: string
  discoveredAt?: string | number
  discoveryLocation?: string
  productName?: string
  productSpecification?: string
  equipmentOrSystem?: string
  reportedAt?: string | number
  receiverName?: string
  categoryCodesJson?: string
  emergencyAction?: string
  levelBasis?: string
  closeReason?: string
  closedAt?: string
  nonconformanceReviewId?: number
  nonconformanceReviewCode?: string
  nonconformanceReviewStatus?: string
  nonconformanceDisposition?: string
  nonconformanceClosedAt?: string
}

export interface DeviationHandlingRespVO {
  id: number
  deviationId: number
  contentVersion?: number
  contentHash?: string
  reviewerAssignments?: Record<string, number | string>
  reviewReady?: boolean
  investigationSubmitted?: boolean
  correctiveStatus?: 'DRAFT' | 'SUBMITTED' | 'RETURNED' | 'VERIFIED'
  correctiveResult?: string
  correctiveEvidenceUrlsJson?: string
  canEditInvestigation?: boolean
  canSubmitCorrective?: boolean
  canVerifyCorrective?: boolean
  effectiveSignatureIds?: Record<string, number | string>
  correctiveOwnerId?: number | string
  nonconformanceReviewCode?: string
  investigationStartedAt?: string | number
  plannedCompletedAt?: string | number
  completedAt?: string | number
  investigationMembersJson?: string
  rootCauseAnalysis?: string
  impactScope?: string
  riskAssessment?: string
  productDisposition?: string
  correctiveOwnerName?: string
  correctiveDueAt?: string | number
  capaRequired?: boolean
  capaCode?: string
  capaAttachmentsJson?: string
  handlingConclusion?: string
  verificationResult?: string
  verificationContent?: string
  signatureEvidence?: Array<{
    id: number
    node: string
    actorId?: number
    actorDisplayName?: string
    meaningLabel?: string
    signedAt?: string | number
    timeZone?: string
    timeEvidenceId?: string
    verificationStatus?: string
    contentHash?: string
    evidenceHash?: string
    policyVersion?: string
    subjectVersion?: string
    validityStatus?: string
  }>
  signatureHistory?: Array<{
    id: number
    node: string
    actorId?: number
    actorDisplayName?: string
    meaningLabel?: string
    signedAt?: string | number
    timeZone?: string
    timeEvidenceId?: string
    verificationStatus?: string
    contentHash?: string
    evidenceHash?: string
    policyVersion?: string
    subjectVersion?: string
    validityStatus?: string
  }>
  revisionHistory?: Array<{
    auditId: number
    handlingId: number
    revisionReason?: string
    beforeContent?: string
    afterContent?: string
    actorUserId?: number
    actorUsername?: string
    occurredAt?: string | number
    beforeSummaryHash?: string
    afterSummaryHash?: string
    previousAuditHash?: string
    auditHash?: string
    beforeContentVersion?: number
    afterContentVersion?: number
    beforeContentHash?: string
    afterContentHash?: string
  }>
}

export const getDeviationPage = (params: DeviationPageReqVO, options?: { ignoreErrorMessage?: boolean; isRequestContextCurrent?: () => boolean; isSuccessReceiptContextCurrent?: () => boolean }) =>
  request.get({ url: '/mes/pro/edhr-deviation/page', params, ignoreErrorMessage: options?.ignoreErrorMessage })

export const getDeviationOrderOptions = (params: PageParam & { search?: string }) =>
  request.get<{ list: DeviationOrderOptionRespVO[]; total: number }>({ url: '/mes/pro/edhr-deviation/order-options', params })

export const getDeviationBatchOptions = (params: PageParam & { search?: string }) =>
  request.get({ url: '/mes/pro/edhr-deviation/batch-options', params })

export const getDeviationBatchOptionsByActiveOrder = (activeOrderId: number | string) =>
  request.get<DeviationBatchOptionRespVO[]>({
    url: '/mes/pro/edhr-deviation/batch-options-by-active-order',
    params: { activeOrderId }
  })

export const createDeviation = (data: DeviationCreateReqVO) =>
  request.post({ url: '/mes/pro/edhr-deviation/create', data })

export const getDeviation = (id: number, options?: { ignoreErrorMessage?: boolean; isRequestContextCurrent?: () => boolean; isSuccessReceiptContextCurrent?: () => boolean }) =>
  request.get({ url: '/mes/pro/edhr-deviation/get', params: { id }, ignoreErrorMessage: options?.ignoreErrorMessage })

export const getDeviationHandling = (deviationId: number, options?: { ignoreErrorMessage?: boolean; isRequestContextCurrent?: () => boolean; isSuccessReceiptContextCurrent?: () => boolean }) =>
  request.get({ url: '/mes/pro/edhr-deviation-handling/get', params: { deviationId }, ignoreErrorMessage: options?.ignoreErrorMessage })

export const saveDeviationHandling = (deviationId: number, data: Record<string, unknown>) =>
  request.post({ url: '/mes/pro/edhr-deviation-handling/save', params: { deviationId }, data })

export const signDeviationHandling = (data: { deviationId: number; node: string; password: string; comment: string; expectedContentVersion: number; expectedContentHash: string }) =>
  request.post({ url: '/mes/pro/edhr-deviation-handling/sign', data })

export const closeDeviationHandling = (deviationId: number, expectedContentVersion: number, expectedContentHash: string) =>
  request.post({ url: '/mes/pro/edhr-deviation-handling/close', data: { deviationId, expectedContentVersion, expectedContentHash } })

export const getActiveOrderDeviationCreationBatches = (activeOrderId: number | string) =>
  request.get<DeviationBatchOptionRespVO[]>({
    url: '/mes/pro/edhr-deviation/creation-batch-options-by-active-order',
    params: { activeOrderId }
  })

export const createActiveOrderDeviation = (activeOrderId: number | string, data: DeviationCreateReqVO) =>
  request.post<DeviationRespVO>({
    url: '/mes/pro/edhr-deviation/create-by-active-order',
    params: { activeOrderId }, data
  })

export interface DeviationAdministratorOption {
  id: number | string
  username: string
  nickname: string
}
export const getDeviationReceiverCandidates = () =>
  request.get<DeviationAdministratorOption[]>({ url: '/mes/pro/edhr-deviation/receiver-candidates' })

export interface DeviationHandlingVersion {
  deviationId: number
  expectedContentVersion: number
  expectedContentHash: string
}
export const submitDeviationInvestigation = (data: DeviationHandlingVersion) =>
  request.post<DeviationHandlingRespVO>({ url: '/mes/pro/edhr-deviation-handling/submit-investigation', data })
export const submitDeviationCorrective = (data: DeviationHandlingVersion & { result: string; completedAt: string; evidenceUrls: string[] }) =>
  request.post<DeviationHandlingRespVO>({ url: '/mes/pro/edhr-deviation-handling/corrective-submit', data })
export const verifyDeviationCorrective = (data: DeviationHandlingVersion & { passed: boolean; comment: string }) =>
  request.post<DeviationHandlingRespVO>({ url: '/mes/pro/edhr-deviation-handling/corrective-verify', data })
