import request from '@/config/axios'

export interface ApprovalDecisionReceipt {
  receiptId: string
  idempotencyKey: string
  prepareId: string
  signatureId: string
  evidenceHash: string
  resultCode: string
  resultingStatus: string
  committedAt: string
}
export interface ApprovalReviewPreview {
  prepareId: string
  expiresAt: string
  content: {
    contentPolicyVersion: string
    subjectVersion: string
    contentHash: string
    scopeCode: string
    scopeText: string
    canonicalBusinessContentJson: string
    allowedActions: string[]
    attachments: Array<{ fileId: string; fileName: string; version: string; sha256: string; fileSize: string; contentType: string; readUrl: string }>
  }
}
export const getApprovalReviewPreview = async (data: { moduleCode: ApprovalModuleCode; sourceTaskType: string; sourceTaskId: string; processInstanceId?: string }) =>
  await request.post<ApprovalReviewPreview>({ url: '/approval-center/tasks/review-preview', data })

export type ApprovalTaskViewType = 'TODO' | 'DONE' | 'MY_INITIATED' | 'CC'

export type ApprovalModuleCode = 'BPM' | 'DCC' | 'EDHR' | 'SHOWROOM' | 'SRM' | 'MES_FEEDBACK'

export type ApprovalTaskReviewResult = 'APPROVE' | 'REJECT'

export type ApprovalTaskCapability =
  | 'TIMELINE'
  | 'NOTIFICATION'
  | 'REMINDER'
  | 'AUDIT'
  | 'SIGNATURE_AUTHORIZATION'
  | 'EVIDENCE_LEDGER'

export interface ApprovalProviderDescriptorVO {
  moduleCode: ApprovalModuleCode
  moduleName: string
  providerCode: string
  providerVersion: string
  supportedViewTypes: ApprovalTaskViewType[]
  capabilities: ApprovalTaskCapability[]
}

export interface ApprovalTaskSummaryVO {
  id: string
  moduleCode: ApprovalModuleCode
  sourceTaskType: string
  sourceTaskId?: string
  businessKey?: string
  businessTitle: string
  businessCode?: string
  businessIdentifierHidden?: boolean
  businessStatus?: string
  businessContextTags?: string[]
  businessDeleted?: boolean
  currentNodeCode?: string
  currentNodeName?: string
  initiatorUserId?: number
  initiatorUserName?: string
  assigneeUserId?: number
  assigneeUserName?: string
  assigneeRoleCode?: string
  assigneeRoleName?: string
  processInstanceId?: string
  initiatedAt?: string
  taskCreatedAt?: string
  taskCompletedAt?: string
  taskCompletedByUserId?: number
  sortTimeMissing?: boolean
  approvalResult?: ApprovalTaskReviewResult
  approvalRemark?: string
  taskOutcome?: string
  sourceStatus?: string
  decisionState?: 'APPROVE' | 'REJECT' | 'UNKNOWN' | 'UNSUPPORTED'
  decisionSourceStatus?: string
  sourceRemark?: string
  terminationReason?: string
  decisionActorUserId?: number
  decisionAt?: string
  evidenceId?: string
  evidenceType?: string
  requiresSignature?: boolean
  detailRoute: string
  detailQuery?: Record<string, string>
  decisionDetailRoute?: string
  decisionDetailQuery?: Record<string, string>
  availableActions?: string[]
  capabilities?: ApprovalTaskCapability[]
}

export interface ApprovalTaskTimelineEntryVO {
  id: string
  moduleCode: ApprovalModuleCode
  sourceTaskType: string
  sourceTaskId?: string
  businessKey?: string
  nodeCode?: string
  nodeName?: string
  action?: string
  actionLabel?: string
  actorUserId?: number
  actedAt?: string
  comment?: string
  status?: string
  evidenceType?: string
  domainReferenceId?: string
  taskOutcome?: string
  taskCreatedAt?: string
  taskCompletedAt?: string
  taskCompletedByUserId?: number
  sourceStatus?: string
  decisionState?: 'APPROVE' | 'REJECT' | 'UNKNOWN' | 'UNSUPPORTED'
  decisionSourceStatus?: string
  sourceRemark?: string
  terminationReason?: string
  decisionActorUserId?: number
  decisionAt?: string
  evidenceId?: string
  businessStatus?: string
  approvalResult?: ApprovalTaskReviewResult
  approvalRemark?: string
}

export interface ApprovalTaskPageReqVO extends PageParam {
  viewType: ApprovalTaskViewType
  moduleCode?: ApprovalModuleCode
  keyword?: string
}

export interface ApprovalTaskTimelineReqVO {
  moduleCode: ApprovalModuleCode
  sourceTaskType: string
  sourceTaskId?: string
  businessKey?: string
  processInstanceId?: string
}

export interface ApprovalTaskReviewReqVO {
  moduleCode: ApprovalModuleCode
  sourceTaskType: string
  sourceTaskId?: string
  businessKey?: string
  processInstanceId?: string
  result: ApprovalTaskReviewResult
  reason?: string
  signaturePassword: string
  prepareId?: string
  idempotencyKey?: string
  expectedSubjectVersion?: string
  expectedContentHash?: string
  contentPolicyVersion?: string
}

export const getApprovalCenterModules = async (options?: { ignoreErrorMessage?: boolean; isRequestContextCurrent?: () => boolean; isSuccessReceiptContextCurrent?: () => boolean }) => {
  return await request.get<ApprovalProviderDescriptorVO[]>({
    ignoreErrorMessage: options?.ignoreErrorMessage,
    isRequestContextCurrent: options?.isRequestContextCurrent,
    isSuccessReceiptContextCurrent: options?.isSuccessReceiptContextCurrent,
    url: '/approval-center/modules'
  })
}

export const getApprovalTaskPage = async (params: ApprovalTaskPageReqVO, options?: { ignoreErrorMessage?: boolean; isRequestContextCurrent?: () => boolean; isSuccessReceiptContextCurrent?: () => boolean }) => {
  return await request.get<PageResult<ApprovalTaskSummaryVO[]>>({
    ignoreErrorMessage: options?.ignoreErrorMessage,
    isRequestContextCurrent: options?.isRequestContextCurrent,
    isSuccessReceiptContextCurrent: options?.isSuccessReceiptContextCurrent,
    url: '/approval-center/tasks/page',
    params
  })
}

export const getApprovalTaskTimeline = async (params: ApprovalTaskTimelineReqVO, options?: { ignoreErrorMessage?: boolean; isRequestContextCurrent?: () => boolean; isSuccessReceiptContextCurrent?: () => boolean }) => {
  return await request.get<ApprovalTaskTimelineEntryVO[]>({
    ignoreErrorMessage: options?.ignoreErrorMessage,
    isRequestContextCurrent: options?.isRequestContextCurrent,
    isSuccessReceiptContextCurrent: options?.isSuccessReceiptContextCurrent,
    url: '/approval-center/tasks/timeline',
    params
  })
}

export const reviewApprovalTask = async (data: ApprovalTaskReviewReqVO, options?: { ignoreErrorMessage?: boolean; isRequestContextCurrent?: () => boolean; isSuccessReceiptContextCurrent?: () => boolean }) => {
  return await request.post<boolean | ApprovalDecisionReceipt>({
    ignoreErrorMessage: options?.ignoreErrorMessage,
    isRequestContextCurrent: options?.isRequestContextCurrent,
    isSuccessReceiptContextCurrent: options?.isSuccessReceiptContextCurrent,
    url: '/approval-center/tasks/review',
    data
  })
}
