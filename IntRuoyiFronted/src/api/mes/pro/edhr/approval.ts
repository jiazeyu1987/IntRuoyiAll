import request from '@/config/axios'
import type { EdhrSignatureSummaryVO } from './signatures'
import type { EdhrRecordCategory, EdhrRouteId, EdhrSignatureTimeReqVO, EdhrValidationProfile } from './batchExecution'

export const EDHR_EXECUTION_STATUS = {
  DRAFT: 0,
  SUBMITTED: 1,
  REJECTED: 2,
  APPROVED: 3,
  FILL_COMPLETED: 4
} as const

export type EdhrExecutionStatus =
  (typeof EDHR_EXECUTION_STATUS)[keyof typeof EDHR_EXECUTION_STATUS]

export const EDHR_APPROVAL_ACTION_RESULT_TYPE = {
  REVIEW_INTERMEDIATE: 'REVIEW_INTERMEDIATE',
  REVIEW_TO_APPROVE: 'REVIEW_TO_APPROVE',
  FINAL_APPROVED: 'FINAL_APPROVED',
  REVIEW_REJECTED_REWORK: 'REVIEW_REJECTED_REWORK'
} as const

export type EdhrApprovalActionResultType =
  (typeof EDHR_APPROVAL_ACTION_RESULT_TYPE)[keyof typeof EDHR_APPROVAL_ACTION_RESULT_TYPE]

export interface EdhrApprovalPageReqVO extends PageParam {
  executionCode?: string
  workOrderCode?: string
  batchCode?: string
  submittedBy?: number
  submittedAtStart?: string
  submittedAtEnd?: string
}

export interface EdhrApprovalRowVO {
  id: number
  executionId: number
  workTaskId?: number
  taskType?: 'REVIEW' | 'APPROVE'
  executionCode: string
  workOrderId?: number
  workOrderCode: string
  taskId?: number
  taskCode?: string
  batchCode: string
  routeProcessId?: number
  processName?: string
  workstationName?: string
  recordCategory?: EdhrRecordCategory
  validationProfile?: EdhrValidationProfile
  permissionScopeId?: number | null
  status: EdhrExecutionStatus
  processInstanceId: string
  bpmTaskId?: string
  bpmTaskName?: string
  taskName?: string
  bpmTaskDefinitionKey?: string
  taskDefinitionKey?: string
  signatureCellKey?: string
  signatureRowIndex?: number
  signatureColumnIndex?: number
  reviewSourceType?: 'POST' | 'ROLE' | 'USER' | 'DEPT' | 'ROLES' | 'USERS' | 'DEPTS'
  reviewSourceId?: number
  reviewSourceIds?: number[]
  reviewSourceName?: string
  submittedBy?: number
  submittedAt?: string
  decision?: string
  handledAt?: string
  actorId?: number
  actorName?: string
  approvalSnapshotId?: number
  approvalSnapshotHash?: string
  approvalSnapshotStatus?: 'SUBMITTED' | 'APPROVED' | 'REJECTED'
  canApprove?: boolean
  canReject?: boolean
  canViewTracking?: boolean
  canViewSignatures?: boolean
  lastActionType?: string
  lastActionAt?: string
  canGenerateArchive?: boolean
  canDownloadArchive?: boolean
  closedAt?: string
}

export interface EdhrApprovalDetailVO extends EdhrApprovalRowVO {
  executionSnapshotJson: string
  cellValues: Array<{ rowIndex: number; columnIndex: number; value: string }>
  approvalSnapshotStatus?: 'SUBMITTED' | 'APPROVED' | 'REJECTED'
  currentAssigneeNames?: string[]
  signatureSummaries?: EdhrSignatureSummaryVO[]
}

export interface EdhrApprovalActionReqVO {
  executionId: EdhrRouteId
  workTaskId: EdhrRouteId
  processInstanceId: string
  approvalSnapshotId: number
  approvalSnapshotHash: string
  bpmTaskId: string
  password: string
  comment?: string
  signatureTime?: EdhrSignatureTimeReqVO
}

export interface EdhrRejectReqVO extends EdhrApprovalActionReqVO {
  reason: string
}

export interface EdhrApprovalActionRespVO {
  executionId: number
  revisionExecutionId?: number
  reworkTaskId?: number
  approveTaskId?: number
  status: EdhrExecutionStatus
  resultType?: EdhrApprovalActionResultType
  processInstanceId: string
  bpmTaskId: string
  signatureId: number
  trackingEventId: number
  closedAt?: string
  rejectedAt?: string
}

export const getEdhrApprovalPendingPage = async (params: EdhrApprovalPageReqVO) => {
  return await request.get<PageResult<EdhrApprovalRowVO[]>>({
    url: '/mes/pro/batch-record-execution/approval-pending-page',
    params
  })
}

export const getEdhrApprovalDonePage = async (params: EdhrApprovalPageReqVO) => {
  return await request.get<PageResult<EdhrApprovalRowVO[]>>({
    url: '/mes/pro/batch-record-execution/approval-done-page',
    params
  })
}

export const getEdhrApprovalDetail = async (
  executionId: EdhrRouteId,
  workTaskId: EdhrRouteId,
  bpmTaskId?: string
) => {
  return await request.get<EdhrApprovalDetailVO>({
    url: '/mes/pro/batch-record-execution/approval-detail',
    params: { id: executionId, bpmTaskId, workTaskId }
  })
}

export const approveEdhrExecution = async (data: EdhrApprovalActionReqVO) => {
  return await request.put<EdhrApprovalActionRespVO>({
    url: '/mes/pro/batch-record-execution/approve',
    data
  })
}

export const rejectEdhrExecution = async (data: EdhrRejectReqVO) => {
  return await request.put<EdhrApprovalActionRespVO>({
    url: '/mes/pro/batch-record-execution/reject',
    data
  })
}
