import request from '@/config/axios'
import type { ProjectAttributes } from '@/views/dcc/controlled-file/project-attributes/state'
import { parseReviewerConfiguration, reviewerIdentity } from '@/views/dcc/controlled-file/basic-data/components/project-reviewer'

export type DccProjectRequestId = number | string

export interface DccProjectReviewerConfiguration {
  configured: boolean
  reviewerUserId: string | null
  reviewerUsername: string | null
  reviewerNickname: string | null
  enabled: boolean
}

export interface DccProjectProductCreateReqVO {
  projectName: string
  projectCode: string
  projectLeader?: string
  projectLeaderUserId?: number
  defaultAttributes: ProjectAttributes
  folderTemplateId?: number
  productCode: string
  productName: string
  classification: '一类' | '二类' | '三类'
  remark?: string
  creationReason?: string
  resubmissionReason?: string
}

export interface DccProjectProductCreateRespVO extends Omit<DccProjectProductCreateReqVO, 'defaultAttributes'> {
  id: DccProjectRequestId
  defaultAttributesJson?: string
  folderTemplateSnapshotJson?: string
  status: string
  applicantUserId?: DccProjectRequestId
  configuredReviewerUserId?: DccProjectRequestId
  configuredReviewerUsername?: string
  configuredReviewerNickname?: string
  reviewerUserId?: number
  approverUserId?: number
  reviewReason?: string
  approvalReason?: string
  rejectReason?: string
  writeErrorCode?: string
  writeErrorMessage?: string
  generatedProjectCodeId?: number
  generatedProductCatalogId?: number
  relationId?: number
  previousRequestId?: number
  resubmittedRequestId?: number
  submittedTime?: string
  reviewedTime?: string
  approvedTime?: string
  completedTime?: string
  failedTime?: string
  writeAttemptNo?: number
  writeReason?: string
  writeOperatorUserId?: number
}

export interface DccProjectProductApprovalActionReqVO {
  reason: string
}

const requestUrl = '/dcc/project-product-requests'

export const getDccProjectReviewerConfiguration = async (): Promise<DccProjectReviewerConfiguration> =>
  parseReviewerConfiguration(await request.get({ url: `${requestUrl}/reviewer-config`, ignoreErrorMessage: true }))

export const configureDccProjectReviewer = async (
  data: { reviewerUserId: DccProjectRequestId; reason: string }
): Promise<DccProjectReviewerConfiguration> => {
  const id = reviewerIdentity(data.reviewerUserId)
  if (typeof data.reason !== 'string' || !data.reason.trim() || data.reason.length > 500)
    throw new Error('请填写500字以内的审核人配置原因')
  const result = parseReviewerConfiguration(await request.put({
    url: `${requestUrl}/reviewer-config`, data: { reviewerUserId: id, reason: data.reason.trim() }, ignoreErrorMessage: true
  }))
  if (!result.configured || !result.enabled || result.reviewerUserId !== id)
    throw new Error('审核人配置保存结果不一致，请刷新核对')
  return result
}

export const createDccProjectProductRequest = async (
  data: DccProjectProductCreateReqVO
): Promise<number> => {
  return await request.post({ url: `${requestUrl}/create`, data, ignoreErrorMessage: true })
}

export const resubmitDccProjectProductRequest = (
  rejectedRequestId: number | string, data: DccProjectProductCreateReqVO
): Promise<number> => request.post({ url: `${requestUrl}/${rejectedRequestId}/resubmit`, data, ignoreErrorMessage: true })

export const getDccProjectProductRequests = async (): Promise<DccProjectProductCreateRespVO[]> => {
  return await request.get({ url: `${requestUrl}/pending` })
}

export const reviewDccProjectProductRequest = async (
  id: DccProjectRequestId,
  data: DccProjectProductApprovalActionReqVO,
  approve: boolean
): Promise<DccProjectProductCreateRespVO> => {
  return await request.post({
    url: `${requestUrl}/${id}/review/${approve ? 'approve' : 'reject'}`,
    data
  })
}

export const approveDccProjectProductRequest = async (
  id: DccProjectRequestId,
  data: DccProjectProductApprovalActionReqVO,
  approve: boolean
): Promise<DccProjectProductCreateRespVO> => {
  return await request.post({
    url: `${requestUrl}/${id}/approve/${approve ? 'approve' : 'reject'}`,
    data
  })
}

export const retryDccProjectProductRequestWrite = async (
  id: DccProjectRequestId, data: DccProjectProductApprovalActionReqVO
): Promise<DccProjectProductCreateRespVO> => {
  return await request.post({ url: `${requestUrl}/${id}/retry-write`, data, ignoreErrorMessage: true })
}
