import request from '@/config/axios'
import type { BusinessActionContextVO } from './businessAction'

export type FormInstanceStatus =
  | 'DRAFT'
  | 'IN_APPROVAL'
  | 'REWORKING'
  | 'REJECTED'
  | 'ABANDONED'
  | 'PENDING_EFFECT'
  | 'EFFECTIVE'
  | 'EFFECT_FAILED_PENDING'

export interface FormInstanceVO {
  id: number
  instanceCode: string
  status: FormInstanceStatus
  bpmProcessInstanceId?: string
  context: BusinessActionContextVO
}

export interface CreateFormInstanceReqVO {
  context: BusinessActionContextVO
  idempotencyKey: string
  formData: Record<string, unknown>
}

export interface SaveFormDraftReqVO {
  formData: Record<string, unknown>
  attachmentIds?: string[]
}

export interface SubmitFormInstanceReqVO {
  formData: Record<string, unknown>
  startUserSelectAssignees?: Record<string, number[]>
}

export interface FormInstanceSnapshotVO {
  id: number
  instanceId: number
  snapshotType: 'DRAFT' | 'SUBMIT' | 'REWORK_SUBMIT'
  snapshotVersion: number
  formData: Record<string, unknown>
  context: BusinessActionContextVO
  attachmentIds: string[]
  createdTime?: string
}

export interface FormEffectPendingPageReqVO extends PageParam {
  instanceId?: number
}

export type FormEffectExecutionStatus = 'APPLIED' | 'FAILED_PENDING'

export interface FormEffectExecutionVO {
  id: number
  instanceId: number
  executionCode: string
  idempotencyKey: string
  status: FormEffectExecutionStatus
  resultRef?: string
  failureReason?: string
}

export const createFormInstance = (data: CreateFormInstanceReqVO) => {
  return request.post<FormInstanceVO>({
    url: '/form-center/instances',
    data
  })
}

export const findActiveBusinessAction = (data: BusinessActionContextVO) => {
  return request.post<FormInstanceVO | null>({
    url: '/form-center/actions/active-instance',
    data
  })
}

export const saveFormDraft = (instanceId: number, data: SaveFormDraftReqVO) => {
  return request.put<boolean>({
    url: `/form-center/instances/${instanceId}/draft`,
    data
  })
}

export const submitFormInstance = (instanceId: number, data: SubmitFormInstanceReqVO) => {
  return request.post<FormInstanceVO>({
    url: `/form-center/instances/${instanceId}/submit`,
    data
  })
}

export const reworkSubmitFormInstance = (instanceId: number, data: SubmitFormInstanceReqVO) => {
  return request.post<boolean>({
    url: `/form-center/instances/${instanceId}/rework-submit`,
    data
  })
}

export const abandonFormInstance = (instanceId: number) => {
  return request.post<boolean>({
    url: `/form-center/instances/${instanceId}/abandon`
  })
}

export const getInstanceSnapshots = (instanceId: number) => {
  return request.get<FormInstanceSnapshotVO[]>({
    url: `/form-center/instances/${instanceId}/snapshots`
  })
}

export const getPendingEffects = (params: FormEffectPendingPageReqVO) => {
  return request.get<PageResult<FormEffectExecutionVO[]>>({
    url: '/form-center/effects/pending',
    params
  })
}

export const retryEffect = (instanceId: number) => {
  return request.post<FormEffectExecutionVO>({
    url: `/form-center/effects/${instanceId}/retry`
  })
}
