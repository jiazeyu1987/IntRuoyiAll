import request from '@/config/axios'
import type { ProjectAttributes } from '@/views/dcc/controlled-file/project-attributes/state'
import { parseReviewerConfiguration, reviewerIdentity, projectRequestContract } from '@/views/dcc/controlled-file/basic-data/components/project-reviewer'
import { requireProjectReviewerRole } from '@/views/dcc/controlled-file/basic-data/components/project-product-role'

export type DccProjectRequestId = number | string
export interface DccProjectReviewerRole {
  reviewContract: 'ROLE_SINGLE_REVIEW_V1'
  roleId: string
  roleCode: 'dcc_project_reviewer'
  roleName: string
  candidates: { userId: string; username: string; nickname: string }[]
  canReview: boolean
}

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
  projectLeaderUserId?: DccProjectRequestId
  defaultAttributes: ProjectAttributes
  folderTemplateId?: DccProjectRequestId
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
  reviewContract?: string | null
  reviewerRoleId?: string | null
  reviewerRoleCode?: string | null
  canReview?: boolean
  applicantUserId?: string | null
  configuredReviewerUserId?: string | null
  configuredReviewerUsername?: string | null
  configuredReviewerNickname?: string | null
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
export const getDccProjectReviewerRole = async (): Promise<DccProjectReviewerRole> => {
  const result = await request.get({ url: `${requestUrl}/reviewer-role`, ignoreErrorMessage: true })
  if (!result || typeof result.roleId !== 'string' || !Array.isArray(result.candidates)
    || result.candidates.some(candidate => !candidate || typeof candidate.userId !== 'string'))
    throw new Error('项目审核员角色或候选身份不是精确字符串')
  return requireProjectReviewerRole(result)
}

export const dccProjectProductRequestIdentity = (value: unknown): string => {
  if ((typeof value !== 'string' && typeof value !== 'number')
    || (typeof value === 'number' && !Number.isSafeInteger(value))
    || !/^[1-9][0-9]*$/.test(String(value)) || BigInt(value) > 9223372036854775807n) {
    throw new Error('项目及产品申请身份无效')
  }
  return String(value)
}

const validateRequestResponse = (result: DccProjectProductCreateRespVO, expectedId?: string): DccProjectProductCreateRespVO => {
  if (!result || typeof result !== 'object' || Array.isArray(result) || typeof result.id !== 'string'
    || (expectedId != null && dccProjectProductRequestIdentity(result.id) !== expectedId))
    throw new Error('项目及产品申请详情身份与请求不一致')
  dccProjectProductRequestIdentity(result.id)
  projectRequestContract(result)
  for (const key of ['projectLeaderUserId', 'folderTemplateId', 'reviewerRoleId', 'applicantUserId', 'configuredReviewerUserId',
    'reviewerUserId', 'approverUserId', 'generatedProjectCodeId', 'generatedProductCatalogId', 'relationId',
    'previousRequestId', 'resubmittedRequestId', 'writeOperatorUserId'] as const) {
    if (result[key] != null) {
      if (typeof result[key] !== 'string') throw new Error('项目申请返回身份不是精确字符串')
      dccProjectProductRequestIdentity(result[key])
    }
  }
  return result
}
const createPayload = (data: DccProjectProductCreateReqVO): DccProjectProductCreateReqVO => {
  for (const field of ['reviewContract', 'reviewerRoleId', 'reviewerRoleCode', 'configuredReviewerUserId',
    'configuredReviewerUsername', 'configuredReviewerNickname', 'reviewerUserId', 'approverUserId', 'canReview']) {
    if (Object.prototype.hasOwnProperty.call(data, field)) throw new Error('项目申请不能由客户端指定审核合同或审核人')
  }
  if (data.projectLeaderUserId != null) dccProjectProductRequestIdentity(data.projectLeaderUserId)
  if (data.folderTemplateId != null) dccProjectProductRequestIdentity(data.folderTemplateId)
  return data
}
const actionPayload = (data: DccProjectProductApprovalActionReqVO): DccProjectProductApprovalActionReqVO => {
  if (!data || typeof data.reason !== 'string' || !data.reason.trim() || data.reason.length > 500)
    throw new Error('请填写500字以内的真实审批或重试原因')
  return { reason: data.reason.trim() }
}

export const getDccProjectProductRequest = async (
  id: DccProjectRequestId
): Promise<DccProjectProductCreateRespVO> => {
  const requestId = dccProjectProductRequestIdentity(id)
  const result = await request.get({ url: `${requestUrl}/${requestId}`, ignoreErrorMessage: true })
  return validateRequestResponse(result, requestId)
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
): Promise<string> => {
  return dccProjectProductRequestIdentity(await request.post({ url: `${requestUrl}/create`, data: createPayload(data), ignoreErrorMessage: true }))
}

export const resubmitDccProjectProductRequest = async (
  rejectedRequestId: number | string, data: DccProjectProductCreateReqVO
): Promise<string> => dccProjectProductRequestIdentity(await request.post({ url: `${requestUrl}/${dccProjectProductRequestIdentity(rejectedRequestId)}/resubmit`, data: createPayload(data), ignoreErrorMessage: true }))

export const getDccProjectProductRequests = async (): Promise<DccProjectProductCreateRespVO[]> => {
  const rows = await request.get({ url: `${requestUrl}/pending`, ignoreErrorMessage: true })
  if (!Array.isArray(rows)) throw new Error('项目申请记录响应不完整')
  const seen = new Set<string>()
  return rows.map(row => {
    const result = validateRequestResponse(row)
    if (seen.has(result.id)) throw new Error('项目申请记录身份重复')
    seen.add(result.id); return result
  })
}

export const reviewDccProjectProductRequest = async (
  id: DccProjectRequestId,
  data: DccProjectProductApprovalActionReqVO,
  approve: boolean
): Promise<DccProjectProductCreateRespVO> => {
  const requestId = dccProjectProductRequestIdentity(id)
  const result = validateRequestResponse(await request.post({
    url: `${requestUrl}/${requestId}/review/${approve ? 'approve' : 'reject'}`,
    data: actionPayload(data), ignoreErrorMessage: true
  }), requestId)
  const expected = approve ? (result.reviewContract === 'ROLE_SINGLE_REVIEW_V1' ? 'COMPLETED' : 'PENDING_APPROVAL') : 'REJECTED'
  if (result.status !== expected) throw new Error('项目审核回执未确认本次处理结果')
  return result
}

export const approveDccProjectProductRequest = async (
  id: DccProjectRequestId,
  data: DccProjectProductApprovalActionReqVO,
  approve: boolean
): Promise<DccProjectProductCreateRespVO> => {
  const requestId = dccProjectProductRequestIdentity(id)
  const result = validateRequestResponse(await request.post({
    url: `${requestUrl}/${requestId}/approve/${approve ? 'approve' : 'reject'}`,
    data: actionPayload(data), ignoreErrorMessage: true
  }), requestId)
  if (result.reviewContract != null || !(approve ? ['COMPLETED', 'WRITE_FAILED'] : ['REJECTED']).includes(result.status)) throw new Error('旧批准回执与本次申请合同不一致')
  return result
}

export const retryDccProjectProductRequestWrite = async (
  id: DccProjectRequestId, data: DccProjectProductApprovalActionReqVO
): Promise<DccProjectProductCreateRespVO> => {
  const requestId = dccProjectProductRequestIdentity(id)
  const result = validateRequestResponse(await request.post({ url: `${requestUrl}/${requestId}/retry-write`, data: actionPayload(data), ignoreErrorMessage: true }), requestId)
  if (result.reviewContract != null || !['COMPLETED', 'WRITE_FAILED'].includes(result.status)) throw new Error('旧申请重试回执状态无效')
  return result
}
