import request from '@/config/axios'

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
  businessStatus?: string
  businessDeleted?: boolean
  currentNodeCode?: string
  currentNodeName?: string
  initiatorUserId?: number
  assigneeUserId?: number
  assigneeUserName?: string
  processInstanceId?: string
  initiatedAt?: string
  taskCreatedAt?: string
  taskCompletedAt?: string
  approvalResult?: ApprovalTaskReviewResult
  approvalRemark?: string
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
}

export const getApprovalCenterModules = async () => {
  return await request.get<ApprovalProviderDescriptorVO[]>({
    url: '/approval-center/modules'
  })
}

export const getApprovalTaskPage = async (params: ApprovalTaskPageReqVO) => {
  return await request.get<PageResult<ApprovalTaskSummaryVO[]>>({
    url: '/approval-center/tasks/page',
    params
  })
}

export const getApprovalTaskTimeline = async (params: ApprovalTaskTimelineReqVO) => {
  return await request.get<ApprovalTaskTimelineEntryVO[]>({
    url: '/approval-center/tasks/timeline',
    params
  })
}

export const reviewApprovalTask = async (data: ApprovalTaskReviewReqVO) => {
  return await request.post<boolean>({
    url: '/approval-center/tasks/review',
    data
  })
}
