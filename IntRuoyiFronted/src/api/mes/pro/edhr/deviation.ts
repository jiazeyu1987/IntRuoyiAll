import request from '@/config/axios'

export interface DeviationPageReqVO extends PageParam {
  status?: 'OPEN' | 'CLOSED'
  level?: 'NORMAL' | 'CRITICAL'
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

export interface DeviationCreateReqVO {
  batchExecutionId: number
  level: 'NORMAL' | 'CRITICAL'
  categoryCodes: string[]
  description: string
  levelBasis: string
  discoveryLocation?: string
  emergencyAction?: string
  idempotencyKey: string
  signaturePassword: string
}

export interface DeviationRespVO {
  canTransferToNcr?: boolean
  transferToNcrBlockedReason?: string
  id: number
  deviationCode: string
  batchExecutionId: number
  batchExecutionCode?: string
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
  nonconformanceReviewCode?: string
  investigationStartedAt?: string
  plannedCompletedAt?: string
  completedAt?: string
  investigationMembersJson?: string
  rootCauseAnalysis?: string
  impactScope?: string
  riskAssessment?: string
  productDisposition?: string
  correctiveOwnerName?: string
  correctiveDueAt?: string
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
    signedAt?: string
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
    signedAt?: string
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
    occurredAt?: string
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

export const getDeviationPage = (params: DeviationPageReqVO) =>
  request.get({ url: '/mes/pro/edhr-deviation/page', params })

export const getDeviationBatchOptions = (params: PageParam & { search?: string }) =>
  request.get({ url: '/mes/pro/edhr-deviation/batch-options', params })

export const getDeviationBatchOptionsByActiveOrder = (activeOrderId: number | string) =>
  request.get<DeviationBatchOptionRespVO[]>({
    url: '/mes/pro/edhr-deviation/batch-options-by-active-order',
    params: { activeOrderId }
  })

export const createDeviation = (data: DeviationCreateReqVO) =>
  request.post({ url: '/mes/pro/edhr-deviation/create', data })

export const getDeviation = (id: number) =>
  request.get({ url: '/mes/pro/edhr-deviation/get', params: { id } })

export const getDeviationHandling = (deviationId: number) =>
  request.get({ url: '/mes/pro/edhr-deviation-handling/get', params: { deviationId } })

export const saveDeviationHandling = (deviationId: number, data: Record<string, unknown>) =>
  request.post({ url: '/mes/pro/edhr-deviation-handling/save', params: { deviationId }, data })

export const signDeviationHandling = (data: { deviationId: number; node: string; password: string; comment: string; expectedContentVersion: number; expectedContentHash: string }) =>
  request.post({ url: '/mes/pro/edhr-deviation-handling/sign', data })

export const closeDeviationHandling = (deviationId: number) =>
  request.post({ url: '/mes/pro/edhr-deviation-handling/close', data: { deviationId } })
