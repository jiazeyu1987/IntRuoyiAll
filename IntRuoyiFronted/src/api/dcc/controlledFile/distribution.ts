import request from '@/config/axios'
import type { DistributionRecipientFact, AutomaticDistributionResult } from '@/views/dcc/controlled-file/workflow/distribution-facts'
import type { WorkflowId } from '@/views/dcc/controlled-file/workflow/workflow-actions'

export interface AutomaticDistributionRetryRequest {
  controlledFileId: WorkflowId
  batchId: WorkflowId
  versionNo: string
  approvalProcessInstanceId: string
}

const retryIdentity = (value: unknown): string => {
  if ((typeof value !== 'string' && typeof value !== 'number')
    || typeof value === 'number' && !Number.isSafeInteger(value)
    || !/^[1-9]\d*$/.test(String(value)) || BigInt(value) > 9223372036854775807n)
    throw new Error('缺少精确有效的自动下发批次或版本身份')
  return String(value)
}
const exactRetryText = (value: unknown): string => {
  if (typeof value !== 'string' || !value || value !== value.trim() || /[\x00-\x1f\x7f]/.test(value))
    throw new Error('缺少精确的下发版本或申请轮次')
  return value
}

export const retryAutomaticDistribution = async (
  input: AutomaticDistributionRetryRequest
): Promise<AutomaticDistributionResult> => {
  const keys = ['controlledFileId', 'batchId', 'versionNo', 'approvalProcessInstanceId']
  if (!input || typeof input !== 'object' || Object.keys(input).some(key => !keys.includes(key)))
    throw new Error('自动下发重试仅接受本批精确身份，名单和政策由服务器取得')
  const data = { controlledFileId: retryIdentity(input.controlledFileId), batchId: retryIdentity(input.batchId),
    versionNo: exactRetryText(input.versionNo), approvalProcessInstanceId: exactRetryText(input.approvalProcessInstanceId) }
  const result = await request.post<AutomaticDistributionResult>({
    url: '/dcc/distribution-tasks/automatic-retry', data, ignoreErrorMessage: true
  })
  if (!result || retryIdentity(result.controlledFileId) !== data.controlledFileId
    || retryIdentity(result.batchId) !== data.batchId || result.versionNo !== data.versionNo
    || result.approvalProcessInstanceId !== data.approvalProcessInstanceId
    || !['PENDING', 'FAILED', 'DISPATCH_READY', 'SENT'].includes(result.status)
    || result.status === 'FAILED' && (!result.errorCode || !result.errorMessage || !result.failureAt)
    || result.status === 'SENT' && !result.sentAt)
    throw new Error('自动下发重试回执与本批版本、轮次或真实状态不一致')
  return result
}

export const getAutomaticDistributionResult = (controlledFileId: string): Promise<AutomaticDistributionResult> => {
  if (typeof controlledFileId !== 'string' || !/^[1-9]\d*$/.test(controlledFileId)
    || BigInt(controlledFileId) > 9223372036854775807n) throw new Error('缺少精确有效的下发文件版本身份')
  return request.get({ url: '/dcc/distribution-tasks/automatic-result', params: { controlledFileId }, ignoreErrorMessage: true })
}

export const getDistributionRecipientReceipts = (controlledFileId: string): Promise<DistributionRecipientFact[]> => {
  if (typeof controlledFileId !== 'string' || !/^[1-9]\d*$/.test(controlledFileId)
    || BigInt(controlledFileId) > 9223372036854775807n) {
    throw new Error('缺少精确有效的下发文件版本身份')
  }
  return request.get({
    url: '/dcc/distribution-tasks/recipient-receipts',
    params: { controlledFileId },
    ignoreErrorMessage: true
  })
}

interface DccDistributionTaskRequestOptions {
  ignoreErrorMessage?: boolean
}

export interface DistributionTaskVO {
  recipientId: number | string
  distributionId: number | string
  controlledFileId: number | string
  categoryId: number | string
  fileName?: string
  title?: string
  fileNumber?: string
  versionNo?: string
  fileStatus: string
  userId: number | string
  departmentId?: number | string
  distributionMedium: string
  readAt?: number
  acknowledgedAt?: number
  publishedTime?: number
  status: 'READY_TO_ACKNOWLEDGE'
}

export interface DistributionTaskPageReqVO extends PageParam {
  categoryId?: number
  status?: string
}

export const getMyDistributionTaskPage = async (
  params: DistributionTaskPageReqVO,
  options: DccDistributionTaskRequestOptions = {}
): Promise<PageResult<DistributionTaskVO[]>> => {
  return await request.get({ url: '/dcc/distribution-tasks/my-page', params, ...options })
}
