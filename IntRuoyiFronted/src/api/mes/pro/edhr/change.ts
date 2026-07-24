import request from '@/config/axios'
import type { EdhrRouteId } from './batchExecution'

export const EDHR_CHANGE_TYPE_VOID = 'VOID'
export const EDHR_CHANGE_TYPE_REOPEN = 'REOPEN'
export const EDHR_CHANGE_TYPE_SUPPLEMENT = 'SUPPLEMENT'

export const EDHR_CHANGE_STATUS_DRAFT = 'DRAFT'
export const EDHR_CHANGE_STATUS_SUBMITTED = 'SUBMITTED'
export const EDHR_CHANGE_STATUS_APPROVED = 'APPROVED'
export const EDHR_CHANGE_STATUS_REJECTED = 'REJECTED'
export const EDHR_CHANGE_STATUS_EFFECTIVE = 'EFFECTIVE'

export interface EdhrRecordChangePageReqVO extends PageParam {
  changeType?: string
  targetScope?: string
  batchExecutionId?: EdhrRouteId
  executionId?: EdhrRouteId
  changeStatus?: string
}

export interface EdhrRecordChangeRequestReqVO {
  batchExecutionId?: number
  executionId?: number
  sourceArchiveId?: number
  reasonCategory?: string
  reasonText?: string
  password?: string
  comment?: string
  startUserSelectAssignees?: Record<string, number[]>
}

export interface EdhrRecordChangeApproveReqVO {
  changeEventId: number
  password?: string
  comment?: string
}

export interface EdhrRecordChangeRespVO {
  id: number
  changeCode?: string
  changeType?: string
  targetScope?: string
  batchExecutionId?: number
  executionId?: number
  sourceExecutionId?: number
  newExecutionId?: number
  changeStatus?: string
  reasonCategory?: string
  reasonText?: string
  requestSignatureId?: number
  approvalSignatureId?: number
  previousStatus?: string
  newStatus?: string
  previousHeadHash?: string
  newHeadHash?: string
  previousArchiveHash?: string
  newArchiveHash?: string
  requestedAt?: string
  approvedAt?: string
  effectiveAt?: string
  bpmProcessInstanceId?: string
  bpmTaskId?: string
}

export const requestVoidExecution = async (data: EdhrRecordChangeRequestReqVO) =>
  request.post<EdhrRecordChangeRespVO>({ url: '/mes/pro/edhr-change/void-execution/request', data })

export const approveVoidExecution = async (data: EdhrRecordChangeApproveReqVO) =>
  request.post<EdhrRecordChangeRespVO>({ url: '/mes/pro/edhr-change/void-execution/approve', data })

export const requestVoidBatchExecution = async (data: EdhrRecordChangeRequestReqVO) =>
  request.post<EdhrRecordChangeRespVO>({
    url: '/mes/pro/edhr-change/void-batch-execution/request',
    data
  })

export const withdrawVoidBatchExecution = async (data: EdhrRecordChangeApproveReqVO) =>
  request.post<EdhrRecordChangeRespVO>({
    url: '/mes/pro/edhr-change/void-batch-execution/withdraw',
    data
  })

export const requestReopenBatch = async (data: EdhrRecordChangeRequestReqVO) =>
  request.post<EdhrRecordChangeRespVO>({ url: '/mes/pro/edhr-change/reopen-batch/request', data })

export const approveReopenBatch = async (data: EdhrRecordChangeApproveReqVO) =>
  request.post<EdhrRecordChangeRespVO>({ url: '/mes/pro/edhr-change/reopen-batch/approve', data })

export const requestReopenExecution = async (data: EdhrRecordChangeRequestReqVO) =>
  request.post<EdhrRecordChangeRespVO>({ url: '/mes/pro/edhr-change/reopen-execution/request', data })

export const approveReopenExecution = async (data: EdhrRecordChangeApproveReqVO) =>
  request.post<EdhrRecordChangeRespVO>({ url: '/mes/pro/edhr-change/reopen-execution/approve', data })

export const requestSupplement = async (data: EdhrRecordChangeRequestReqVO) =>
  request.post<EdhrRecordChangeRespVO>({ url: '/mes/pro/edhr-change/supplement/request', data })

export const saveSupplementDraft = async (data: EdhrRecordChangeRequestReqVO) =>
  request.put<EdhrRecordChangeRespVO>({ url: '/mes/pro/edhr-change/supplement/save-draft', data })

export const submitSupplement = async (data: EdhrRecordChangeApproveReqVO) =>
  request.post<EdhrRecordChangeRespVO>({ url: '/mes/pro/edhr-change/supplement/submit', data })

export const approveSupplement = async (data: EdhrRecordChangeApproveReqVO) =>
  request.post<EdhrRecordChangeRespVO>({ url: '/mes/pro/edhr-change/supplement/approve', data })

export const getEdhrRecordChangePage = async (params: EdhrRecordChangePageReqVO) =>
  request.get<PageResult<EdhrRecordChangeRespVO[]>>({ url: '/mes/pro/edhr-change/page', params })

export const getEdhrRecordChange = async (id: number) =>
  request.get<EdhrRecordChangeRespVO>({ url: '/mes/pro/edhr-change/get', params: { id } })
