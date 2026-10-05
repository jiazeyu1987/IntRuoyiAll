import request from '@/config/axios'
import type { EdhrRouteId } from './batchExecution'
import type { TeamLeaderActiveOrderDetailRespVO } from '@/api/mes/pro/processpool/teamLeader'

const EDHR_NONCONFORMANCE_REVIEW_BASE_URL = '/mes/pro/edhr-nonconformance-review'

export const SOURCE_TYPE_PQC_SUBMISSION = 'PQC_SUBMISSION'
export const SOURCE_TYPE_PQC_RELEASE = 'PQC_RELEASE'
export const SOURCE_TYPE_ACTIVE_ORDER = 'ACTIVE_ORDER'
export const SOURCE_TYPE_DEVIATION = 'DEVIATION'

export const REVIEW_STATUS_PENDING_REVIEW = 'pending_review'
export const REVIEW_STATUS_CLOSED = 'closed'

export const DISPOSITION_CONCESSION_RELEASE = 'concession_release'
export const DISPOSITION_REWORK = 'rework'
export const DISPOSITION_VOID = 'void'

export type EdhrNonconformanceReviewSourceType =
  | typeof SOURCE_TYPE_PQC_SUBMISSION
  | typeof SOURCE_TYPE_PQC_RELEASE
  | typeof SOURCE_TYPE_ACTIVE_ORDER
  | typeof SOURCE_TYPE_DEVIATION

export type EdhrNonconformanceReviewStatus =
  | typeof REVIEW_STATUS_PENDING_REVIEW
  | typeof REVIEW_STATUS_CLOSED

export type EdhrNonconformanceReviewDisposition =
  | typeof DISPOSITION_CONCESSION_RELEASE
  | typeof DISPOSITION_REWORK
  | typeof DISPOSITION_VOID

export interface EdhrNonconformanceReviewCreateReqVO {
  activeOrderId?: EdhrRouteId
  sourceType?: EdhrNonconformanceReviewSourceType
  sourceId?: EdhrRouteId
  batchExecutionId?: EdhrRouteId
  nonconformanceReason: string
  signaturePassword?: string
  remark?: string
}

export interface EdhrNonconformanceReviewActiveOrderRespVO {
  id: number
  workOrderId?: number
  workOrderCode?: string
  batchCode?: string
  activeStatus?: string
  businessStatus?: string
}

export interface EdhrDeviationNcrCreateReqVO {
  batchExecutionId: EdhrRouteId
  deviationIds: EdhrRouteId[]
  nonconformanceReason: string
  remark?: string
  idempotencyKey: string
  signaturePassword: string
}

export interface EdhrBatchExecutionRejectReqVO {
  batchExecutionId: EdhrRouteId
  nonconformanceReason: string
  signaturePassword: string
}

export interface EdhrNonconformanceReviewDisposeReqVO {
  id: EdhrRouteId
  disposition: EdhrNonconformanceReviewDisposition
  reviewMaterialUrl?: string
  reviewMaterials: EdhrNonconformanceReviewMaterial[]
  reviewMaterialEvents?: EdhrNonconformanceReviewMaterialEvent[]
  reviewOpinion: string
  signaturePassword: string
}

export interface EdhrNonconformanceReviewMaterial {
  fileId: EdhrRouteId
  url: string
  fileName?: string
  sortNo?: number
}

export interface EdhrNonconformanceReviewMaterialEvent {
  fileId: EdhrRouteId
  action: 'UPLOAD' | 'DELETE'
  url: string
  fileName?: string
  sequence?: number
}

export const uploadNonconformanceReviewMaterial = async (reviewId: EdhrRouteId, file: File) => {
  const data = new FormData()
  data.append('file', file)
  const response = await request.upload<{ data: EdhrNonconformanceReviewMaterial }>({
    url: `${EDHR_NONCONFORMANCE_REVIEW_BASE_URL}/${reviewId}/materials/upload`,
    data
  })
  return response.data
}

export interface EdhrNonconformanceReviewPageReqVO extends PageParam {
  reviewCode?: string
  sourceType?: EdhrNonconformanceReviewSourceType
  sourceId?: EdhrRouteId
  batchExecutionId?: EdhrRouteId
  batchExecutionCode?: string
  workOrderCode?: string
  batchCode?: string
  reviewStatus?: EdhrNonconformanceReviewStatus
  disposition?: EdhrNonconformanceReviewDisposition
}

export interface EdhrNonconformanceReviewRespVO {
  id: number
  activeOrderId?: number
  reviewCode?: string
  sourceType?: EdhrNonconformanceReviewSourceType
  sourceId?: number
  batchExecutionId?: number
  batchExecutionCode?: string
  workOrderId?: number
  workOrderCode?: string
  batchCode?: string
  previousBatchStatus?: number
  reviewStatus?: EdhrNonconformanceReviewStatus
  nonconformanceReason?: string
  reviewMaterialUrl?: string
  reviewMaterialFileId?: number
  reviewMaterialsJson?: string
  reviewOpinion?: string
  qaSignature?: string
  qaUserId?: number
  frozenAt?: string
  closedAt?: string
  unfrozenAt?: string
  voidedAt?: string
  disposition?: EdhrNonconformanceReviewDisposition
  traceSnapshotJson?: string
  deviationIdsJson?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export const createNonconformanceReview = async (data: EdhrNonconformanceReviewCreateReqVO) => {
  return await request.post<EdhrNonconformanceReviewRespVO>({
    url: `${EDHR_NONCONFORMANCE_REVIEW_BASE_URL}/create`,
    data
  })
}

export const getNonconformanceReviewActiveOrderList = async () => {
  return await request.get<EdhrNonconformanceReviewActiveOrderRespVO[]>({
    url: `${EDHR_NONCONFORMANCE_REVIEW_BASE_URL}/active-order-list`
  })
}

export const createCriticalDeviationReview = async (data: EdhrDeviationNcrCreateReqVO) => {
  return await request.post<EdhrNonconformanceReviewRespVO>({
    url: `${EDHR_NONCONFORMANCE_REVIEW_BASE_URL}/create-from-critical-deviations`,
    data
  })
}

export const rejectEdhrBatchExecutionToNonconformanceReview = async (
  data: EdhrBatchExecutionRejectReqVO
) => {
  return await request.post<EdhrNonconformanceReviewRespVO>({
    url: `${EDHR_NONCONFORMANCE_REVIEW_BASE_URL}/reject-batch`,
    data
  })
}

export const disposeNonconformanceReview = async (data: EdhrNonconformanceReviewDisposeReqVO) => {
  return await request.post<EdhrNonconformanceReviewRespVO>({
    url: `${EDHR_NONCONFORMANCE_REVIEW_BASE_URL}/dispose`,
    data
  })
}

export const getPendingNonconformanceReviewPage = async (
  params: EdhrNonconformanceReviewPageReqVO
) => {
  return await request.get<PageResult<EdhrNonconformanceReviewRespVO[]>>({
    url: `${EDHR_NONCONFORMANCE_REVIEW_BASE_URL}/pending-page`,
    params
  })
}

export const getNonconformanceReviewPage = async (params: EdhrNonconformanceReviewPageReqVO) => {
  return await request.get<PageResult<EdhrNonconformanceReviewRespVO[]>>({
    url: `${EDHR_NONCONFORMANCE_REVIEW_BASE_URL}/page`,
    params
  })
}

export const getNonconformanceReview = async (id: EdhrRouteId) => {
  return await request.get<EdhrNonconformanceReviewRespVO>({
    url: `${EDHR_NONCONFORMANCE_REVIEW_BASE_URL}/get`,
    params: { id }
  })
}

export const getNonconformanceReviewActiveOrderDetail = async (reviewId: EdhrRouteId) => {
  return await request.get<TeamLeaderActiveOrderDetailRespVO>({
    url: `${EDHR_NONCONFORMANCE_REVIEW_BASE_URL}/active-order-detail`,
    params: { reviewId }
  })
}

export const getBatchNonconformanceReviewList = async (batchExecutionId: EdhrRouteId) => {
  return await request.get<EdhrNonconformanceReviewRespVO[]>({
    url: `${EDHR_NONCONFORMANCE_REVIEW_BASE_URL}/batch-list`,
    params: { batchExecutionId }
  })
}
