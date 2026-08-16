import request from '@/config/axios'

export const EDHR_WORK_TASK_TYPE_FILL = 'FILL'
export const EDHR_WORK_TASK_TYPE_REVIEW = 'REVIEW'
export const EDHR_WORK_TASK_TYPE_APPROVE = 'APPROVE'
export const EDHR_WORK_TASK_TYPE_REWORK = 'REWORK'
export const EDHR_WORK_TASK_TYPE_ARCHIVE = 'ARCHIVE'
export const EDHR_WORK_TASK_TYPE_RELEASE_APPROVE = 'RELEASE_APPROVE'
export const EDHR_WORK_TASK_TYPE_PQC_PRODUCTION_RELEASE = 'PQC_PRODUCTION_RELEASE'
export const EDHR_WORK_TASK_STATUS_TODO = 'TODO'
export const EDHR_WORK_TASK_STATUS_DOING = 'DOING'
export const EDHR_WORK_TASK_STATUS_DONE = 'DONE'
export const EDHR_WORK_TASK_STATUS_CANCELED = 'CANCELED'
export const EDHR_WORK_TASK_STATUS_OVERDUE = 'OVERDUE'
export const EDHR_PRODUCTION_RELEASE_REPORT_NODE_TYPES = [
  'INCOMING_INSPECTION_REPORT',
  'STERILIZATION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_RECORD'
] as const

export interface EdhrWorkTaskPageReqVO extends PageParam {
  taskType?: string
  status?: string
  workOrderCode?: string
  batchCode?: string
  processName?: string
  nodeTypes?: string[]
  batchExecutionId?: string
}

export interface EdhrWorkTaskRespVO {
  id: string
  taskCode?: string
  taskType: string
  status: string
  batchExecutionId: string
  batchTaskId?: string
  businessScopeType?: string
  businessScopeId?: string
  executionId?: string
  sourceExecutionId?: string
  workOrderId?: string
  workOrderCode?: string
  batchCode?: string
  routeProcessId?: string
  processName?: string
  assigneeUserId?: string
  assigneeUserName?: string
  candidateSourceType?: 'USER' | 'USER_GROUP' | 'ROLE_GROUP' | 'DEPT_GROUP'
  candidateSourceId?: string
  candidatePoolName?: string
  candidateUserSnapshot?: string
  candidateSnapshotDisplay?: string
  sourceUserId?: string
  sourceUserName?: string
  responsibilitySource?: string
  inactionReason?: string
  signatureCellKey?: string
  signatureRowIndex?: number
  signatureColumnIndex?: number
  reviewSourceType?: 'POST' | 'ROLE' | 'USER' | 'DEPT' | 'ROLES' | 'USERS' | 'DEPTS'
  reviewSourceId?: string
  reviewSourceIds?: string[]
  reviewSourceName?: string
  bpmTaskId?: string
  nodeType?: string
  nodeName?: string
  version?: number
  actionUrl: string
  reason?: string
  remark?: string
  createTime?: string
  dueTime?: string
  overdueAt?: string
  overdueReason?: string
  completedAt?: string
}

export interface EdhrWorkTaskStatsRespVO {
  todoCount: number
  fillCount: number
  reviewCount: number
  reworkCount: number
  archiveCount: number
  overdueCount: number
  doneCount: number
}

export interface EdhrWorkTaskArchiveRuleReqVO {
  routeId: number
  assigneeUserId: number
  dueMinutes: number
  enabled: boolean
  remark?: string
}

export interface EdhrWorkTaskCloseRuleReqVO {
  routeId: number
  assigneeUserId: number
  dueMinutes: number
  enabled: boolean
  remark?: string
}

export type EdhrWorkTaskReleaseApprovalCandidateSourceType = 'USER' | 'ROLE_GROUP'

export interface EdhrWorkTaskReleaseApprovalRuleReqVO {
  routeId: number
  candidateSourceType: EdhrWorkTaskReleaseApprovalCandidateSourceType
  candidateSourceId: number
  enabled: boolean
  remark?: string
}

export interface EdhrWorkTaskAssignmentRuleRespVO {
  id: number
  routeProcessId?: number
  scopeType: string
  scopeId: number
  taskType: string
  assigneeUserId?: number
  reviewUserId?: number
  candidateSourceType?: 'USER' | 'USER_GROUP' | 'ROLE_GROUP' | 'DEPT_GROUP'
  candidateSourceId?: number
  dueMinutes?: number
  enabled: boolean
  remark?: string
  createTime?: string
  updateTime?: string
}

export const getEdhrWorkTaskMyPage = async (params: EdhrWorkTaskPageReqVO) => {
  return await request.get<PageResult<EdhrWorkTaskRespVO[]>>({
    url: '/mes/pro/edhr-work-task/my-page',
    params
  })
}

export const getEdhrWorkTaskDonePage = async (params: EdhrWorkTaskPageReqVO) => {
  return await request.get<PageResult<EdhrWorkTaskRespVO[]>>({
    url: '/mes/pro/edhr-work-task/done-page',
    params
  })
}

export const getEdhrWorkTaskCandidateTodoPage = async (params: EdhrWorkTaskPageReqVO) => {
  return await request.get<PageResult<EdhrWorkTaskRespVO[]>>({
    url: '/mes/pro/edhr-work-task/candidate-todo-page',
    params
  })
}

export const getEdhrWorkTaskStats = async () => {
  return await request.get<EdhrWorkTaskStatsRespVO>({
    url: '/mes/pro/edhr-work-task/stats'
  })
}

export const getEdhrRouteArchiveRule = async (routeId: number) => {
  return await request.get<EdhrWorkTaskAssignmentRuleRespVO>({
    url: '/mes/pro/edhr-work-task/route-archive-rule',
    params: { routeId }
  })
}

export const saveEdhrRouteArchiveRule = async (data: EdhrWorkTaskArchiveRuleReqVO) => {
  return await request.post<EdhrWorkTaskAssignmentRuleRespVO>({
    url: '/mes/pro/edhr-work-task/route-archive-rule',
    data
  })
}

export const getEdhrRouteCloseRule = async (routeId: number) => {
  return await request.get<EdhrWorkTaskAssignmentRuleRespVO>({
    url: '/mes/pro/edhr-work-task/route-close-rule',
    params: { routeId }
  })
}

export const saveEdhrRouteCloseRule = async (data: EdhrWorkTaskCloseRuleReqVO) => {
  return await request.post<EdhrWorkTaskAssignmentRuleRespVO>({
    url: '/mes/pro/edhr-work-task/route-close-rule',
    data
  })
}

export const getEdhrRouteReleaseApprovalRule = async (routeId: number) => {
  return await request.get<EdhrWorkTaskAssignmentRuleRespVO>({
    url: '/mes/pro/edhr-work-task/route-release-approval-rule',
    params: { routeId }
  })
}

export const saveEdhrRouteReleaseApprovalRule = async (
  data: EdhrWorkTaskReleaseApprovalRuleReqVO
) => {
  return await request.post<EdhrWorkTaskAssignmentRuleRespVO>({
    url: '/mes/pro/edhr-work-task/route-release-approval-rule',
    data
  })
}

export const completeEdhrCandidateSignatureTask = async (
  workTaskId: string,
  executionId: string
) => {
  return await request.post<boolean>({
    url: '/mes/pro/edhr-work-task/candidate-signature/complete',
    params: { workTaskId, executionId }
  })
}
