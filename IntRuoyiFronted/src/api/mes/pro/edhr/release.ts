import request from '@/config/axios'

export type EdhrReleaseStatus =
  | 'PRECHECK_REQUIRED'
  | 'PRECHECK_FAILED'
  | 'PRECHECK_PASSED'
  | 'PENDING_APPROVAL'
  | 'RELEASED'
  | 'REJECTED'
  | 'WITHDRAWN'
export type EdhrReleaseCheckResult = 'PASS' | 'FAIL' | 'BLOCKER' | 'NOT_APPLICABLE'
export type EdhrReleaseCheckItemStatus = 'OPEN' | 'SUPERSEDED' | 'RESOLVED'
export type EdhrReleaseEventType = 'PRECHECK' | 'SUBMIT' | 'APPROVE' | 'REJECT' | 'WITHDRAW'

export interface EdhrReleasePageReqVO extends PageParam {
  batchExecutionCode?: string
  workOrderCode?: string
  batchCode?: string
  productCode?: string
  batchExecutionStatuses?: number[]
  excludeBatchExecutionStatuses?: number[]
  completedTraceOnly?: boolean
  releaseStatus?: EdhrReleaseStatus | ''
  dhrStatus?: string
  inspectionStatus?: string
}

export interface EdhrReleaseRowVO {
  releaseTransactionId?: number
  releaseCode?: string
  batchExecutionId: number
  batchExecutionCode: string
  workOrderId?: number
  workOrderCode?: string
  batchCode: string
  productId?: number
  productCode?: string
  productName?: string
  routeId?: number
  routeCode?: string
  routeName?: string
  batchExecutionStatus?: number
  dhrStatus: string
  inspectionStatus: string
  deviationStatus: string
  reworkStatus: string
  scrapStatus: string
  inventoryStatus: string
  releaseStatus: EdhrReleaseStatus
  requiredCheckCount: number
  failedCheckCount: number
  blockingCheckCount: number
  lastPrecheckAt?: string | number
  precheckSummary?: string
  precheckSnapshotJson?: string
  submitIdempotencyKey?: string
  submittedBy?: number
  submittedAt?: string | number
  approvalIdempotencyKey?: string
  approvedBy?: number
  approvedAt?: string | number
  approvalSignoffEvidenceHash?: string
  approvalOpinion?: string
  rejectedBy?: number
  rejectedAt?: string | number
  rejectReason?: string
  withdrawnBy?: number
  withdrawnAt?: string | number
  withdrawReason?: string
}

export interface EdhrReleasePrecheckReqVO {
  releaseTransactionId?: number
  batchExecutionId?: number
}

export interface EdhrReleaseCheckItemPageReqVO extends PageParam {
  releaseTransactionId: number
  checkCategory?: string
  checkResult?: EdhrReleaseCheckResult | ''
  itemStatus?: EdhrReleaseCheckItemStatus | ''
  sourceObjectCode?: string
}

export interface EdhrReleaseCheckItemVO {
  id: number
  releaseTransactionId: number
  checkCode: string
  checkCategory: string
  checkName: string
  checkResult: EdhrReleaseCheckResult
  itemStatus: EdhrReleaseCheckItemStatus
  severity: string
  responsibilityModule: string
  sourceObjectType?: string
  sourceObjectId?: string
  sourceObjectCode?: string
  sourceRecordUrl?: string
  failureReason: string
  remediationSuggestion: string
  impactScopeJson?: string
  evidenceHash?: string
  checkedAt: string
}

export interface EdhrReleaseSubmitReqVO {
  releaseTransactionId: number
  idempotencyKey: string
  password: string
  submitReason?: string
}

export interface EdhrReleaseApproveReqVO {
  releaseTransactionId: number
  idempotencyKey: string
  signoffEvidenceHash: string
  approvalOpinion?: string
}

export interface EdhrReleaseRejectReqVO {
  releaseTransactionId: number
  idempotencyKey: string
  rejectReason: string
}

export interface EdhrReleaseWithdrawReqVO {
  releaseTransactionId: number
  idempotencyKey: string
  withdrawReason: string
}

export interface EdhrReleaseEventPageReqVO extends PageParam {
  releaseTransactionId: number
  eventType?: EdhrReleaseEventType | ''
}

export interface EdhrReleaseEventRespVO {
  id: number
  releaseTransactionId: number
  eventType: EdhrReleaseEventType
  fromStatus: EdhrReleaseStatus
  toStatus: EdhrReleaseStatus
  actorUserId?: number
  reason?: string
  opinion?: string
  idempotencyKey: string
  signoffEvidenceHash?: string
  eventSnapshotJson?: string
  evidenceHash: string
  occurredAt: string | number
}

export const getEdhrReleasePage = async (params: EdhrReleasePageReqVO) => {
  return await request.get<PageResult<EdhrReleaseRowVO[]>>({
    url: '/mes/pro/edhr-release/page',
    params
  })
}

export const getEdhrRelease = async (id: number) => {
  return await request.get<EdhrReleaseRowVO>({
    url: '/mes/pro/edhr-release/get',
    params: { id }
  })
}

export const precheckEdhrRelease = async (data: EdhrReleasePrecheckReqVO) => {
  return await request.post<EdhrReleaseRowVO>({
    url: '/mes/pro/edhr-release/precheck',
    data
  })
}

export const submitEdhrRelease = async (data: EdhrReleaseSubmitReqVO) => {
  return await request.post<EdhrReleaseRowVO>({
    url: '/mes/pro/edhr-release/submit',
    data
  })
}

export const approveEdhrRelease = async (data: EdhrReleaseApproveReqVO) => {
  return await request.post<EdhrReleaseRowVO>({
    url: '/mes/pro/edhr-release/approve',
    data
  })
}

export const rejectEdhrRelease = async (data: EdhrReleaseRejectReqVO) => {
  return await request.post<EdhrReleaseRowVO>({
    url: '/mes/pro/edhr-release/reject',
    data
  })
}

export const withdrawEdhrRelease = async (data: EdhrReleaseWithdrawReqVO) => {
  return await request.post<EdhrReleaseRowVO>({
    url: '/mes/pro/edhr-release/withdraw',
    data
  })
}

export const getEdhrReleaseCheckItemPage = async (params: EdhrReleaseCheckItemPageReqVO) => {
  return await request.get<PageResult<EdhrReleaseCheckItemVO[]>>({
    url: '/mes/pro/edhr-release/check-item/page',
    params
  })
}

export const getEdhrReleaseEventPage = async (params: EdhrReleaseEventPageReqVO) => {
  return await request.get<PageResult<EdhrReleaseEventRespVO[]>>({
    url: '/mes/pro/edhr-release/event/page',
    params
  })
}
