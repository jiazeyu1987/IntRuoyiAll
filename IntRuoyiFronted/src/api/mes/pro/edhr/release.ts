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
  releaseTransactionId?: string
  releaseCode?: string
  batchExecutionId: string
  batchExecutionCode: string
  workOrderId?: string
  workOrderCode?: string
  batchCode: string
  productId?: string
  productCode?: string
  productName?: string
  routeId?: string
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
  submittedBy?: string
  submittedAt?: string | number
  approvalIdempotencyKey?: string
  approvedBy?: string
  approvedAt?: string | number
  approvalSignoffEvidenceHash?: string
  approvalOpinion?: string
  rejectedBy?: string
  rejectedAt?: string | number
  rejectReason?: string
  withdrawnBy?: string
  withdrawnAt?: string | number
  withdrawReason?: string
  releaseApprovalWorkTaskId?: string
  version: number
}

export interface EdhrReleasePrecheckReqVO {
  releaseTransactionId?: string
  batchExecutionId?: string | number
}

export interface EdhrReleaseCheckItemPageReqVO extends PageParam {
  releaseTransactionId: string
  checkCategory?: string
  checkResult?: EdhrReleaseCheckResult | ''
  itemStatus?: EdhrReleaseCheckItemStatus | ''
  sourceObjectCode?: string
}

export interface EdhrReleaseCheckItemVO {
  id: string
  releaseTransactionId: string
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
  releaseTransactionId: string
  idempotencyKey: string
  password: string
  submitReason?: string
}

export interface EdhrReleaseApproveReqVO {
  releaseTransactionId: string
  workTaskId: string
  expectedVersion: number
  idempotencyKey: string
  signoffEvidenceHash: string
  approvalOpinion?: string
}

export interface EdhrReleaseRejectReqVO {
  releaseTransactionId: string
  idempotencyKey: string
  rejectReason: string
}

export interface EdhrReleaseWithdrawReqVO {
  releaseTransactionId: string
  idempotencyKey: string
  withdrawReason: string
}

export interface EdhrReleaseEventPageReqVO extends PageParam {
  releaseTransactionId: string
  eventType?: EdhrReleaseEventType | ''
}

export interface EdhrReleaseEventRespVO {
  id: string
  releaseTransactionId: string
  eventType: EdhrReleaseEventType
  fromStatus: EdhrReleaseStatus
  toStatus: EdhrReleaseStatus
  actorUserId?: string
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

export const getEdhrRelease = async (id: string) => {
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
