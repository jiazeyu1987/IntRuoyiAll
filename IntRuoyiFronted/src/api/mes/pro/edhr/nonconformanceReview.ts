import request from '@/config/axios'
import type { EdhrRouteId } from './batchExecution'

const EDHR_NONCONFORMANCE_REVIEW_BASE_URL = '/mes/pro/edhr-nonconformance-review'

export const SOURCE_TYPE_PQC_SUBMISSION = 'PQC_SUBMISSION'
export const SOURCE_TYPE_PQC_RELEASE = 'PQC_RELEASE'

export const REVIEW_STATUS_PENDING_REVIEW = 'pending_review'
export const REVIEW_STATUS_CLOSED = 'closed'

export const DISPOSITION_CONCESSION_RELEASE = 'concession_release'
export const DISPOSITION_REWORK = 'rework'
export const DISPOSITION_VOID = 'void'

export type EdhrNonconformanceReviewSourceType =
  | typeof SOURCE_TYPE_PQC_SUBMISSION
  | typeof SOURCE_TYPE_PQC_RELEASE

export type EdhrNonconformanceReviewStatus =
  | typeof REVIEW_STATUS_PENDING_REVIEW
  | typeof REVIEW_STATUS_CLOSED

export type EdhrNonconformanceReviewDisposition =
  | typeof DISPOSITION_CONCESSION_RELEASE
  | typeof DISPOSITION_REWORK
  | typeof DISPOSITION_VOID

export interface EdhrNonconformanceReviewCreateReqVO {
  sourceType: EdhrNonconformanceReviewSourceType
  sourceId?: EdhrRouteId
  batchExecutionId?: EdhrRouteId
  nonconformanceReason: string
  remark?: string
}

export interface EdhrNonconformanceReviewDisposeReqVO {
  id: EdhrRouteId
  disposition: EdhrNonconformanceReviewDisposition
  reviewMaterialUrl: string
  reviewOpinion: string
  qaSignature: string
}

export interface EdhrNonconformanceReviewPageReqVO extends PageParam {
  reviewCode?: string
  sourceType?: EdhrNonconformanceReviewSourceType
  batchExecutionId?: EdhrRouteId
  batchExecutionCode?: string
  workOrderCode?: string
  batchCode?: string
  reviewStatus?: EdhrNonconformanceReviewStatus
  disposition?: EdhrNonconformanceReviewDisposition
}

export interface EdhrNonconformanceReviewRespVO {
  id: number
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
  reviewOpinion?: string
  qaSignature?: string
  qaUserId?: number
  frozenAt?: string
  closedAt?: string
  unfrozenAt?: string
  voidedAt?: string
  disposition?: EdhrNonconformanceReviewDisposition
  traceSnapshotJson?: string
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

export const getNonconformanceReview = async (id: EdhrRouteId) => {
  return await request.get<EdhrNonconformanceReviewRespVO>({
    url: `${EDHR_NONCONFORMANCE_REVIEW_BASE_URL}/get`,
    params: { id }
  })
}

export const getBatchNonconformanceReviewList = async (batchExecutionId: EdhrRouteId) => {
  return await request.get<EdhrNonconformanceReviewRespVO[]>({
    url: `${EDHR_NONCONFORMANCE_REVIEW_BASE_URL}/batch-list`,
    params: { batchExecutionId }
  })
}
