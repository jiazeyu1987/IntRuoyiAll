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

export interface DccProjectProductCreateRespVO extends Omit<DccProjectProductCreateReqVO, 'defaultAttributes' | 'projectLeaderUserId' | 'folderTemplateId'> {
  id: string
  projectLeaderUserId?: string | null
  folderTemplateId?: string | null
  defaultAttributesJson?: string
  folderTemplateSnapshotJson?: string
  status: string
  applicantUserId?: string | null
  configuredReviewerUserId?: string | null
  configuredReviewerUsername?: string
  configuredReviewerNickname?: string
  reviewerUserId?: string | null
  approverUserId?: string | null
  reviewReason?: string
  approvalReason?: string
  rejectReason?: string
  writeErrorCode?: string
  writeErrorMessage?: string
  generatedProjectCodeId?: string | null
  generatedProductCatalogId?: string | null
  relationId?: string | null
  previousRequestId?: string | null
  resubmittedRequestId?: string | null
  submittedTime?: string
  reviewedTime?: string
  approvedTime?: string
  completedTime?: string
  failedTime?: string
  writeAttemptNo?: number
  writeReason?: string
  writeOperatorUserId?: string | null
}

export interface DccProjectProductApprovalActionReqVO {
  reason: string
}

const requestUrl = '/dcc/project-product-requests'

export const dccProjectProductRequestIdentity = (value: unknown): string => {
  if ((typeof value !== 'string' && typeof value !== 'number')
    || (typeof value === 'number' && !Number.isSafeInteger(value))
    || !/^[1-9][0-9]*$/.test(String(value)) || BigInt(value) > 9223372036854775807n) {
    throw new Error('项目及产品申请身份无效')
  }
  return String(value)
}

export const getDccProjectProductRequest = async (
  id: DccProjectRequestId
): Promise<DccProjectProductCreateRespVO> => {
  const requestId = dccProjectProductRequestIdentity(id)
  const result = await request.get({ url: `${requestUrl}/${requestId}`, ignoreErrorMessage: true })
  if (!result || typeof result !== 'object' || Array.isArray(result)
    || typeof result.id !== 'string' || dccProjectProductRequestIdentity(result.id) !== requestId) {
    throw new Error('项目及产品申请详情身份与请求不一致')
  }
  return result
}

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
