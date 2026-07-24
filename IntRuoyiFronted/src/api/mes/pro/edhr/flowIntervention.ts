import request from '@/config/axios'

export type EdhrFlowInterventionAction =
  | 'RETURN'
  | 'WITHDRAW'
  | 'TRANSFER'
  | 'ADD_SIGN'
  | 'ADMIN_INTERVENE'
export type EdhrFlowInterventionStatus = 'RECORDED' | 'CANCELLED'
export type EdhrFlowInterventionIntegrityResult = 'RECHECK_REQUIRED' | 'PASS' | 'FAIL'

export interface EdhrFlowInterventionPageReqVO extends PageParam {
  businessObjectType?: string
  businessObjectId?: string
  businessObjectCode?: string
  flowInstanceId?: string
  interventionAction?: EdhrFlowInterventionAction | ''
  interventionStatus?: EdhrFlowInterventionStatus | ''
}

export interface EdhrFlowInterventionRespVO {
  id: number
  interventionCode: string
  businessObjectType: string
  businessObjectId: string
  businessObjectCode?: string
  flowInstanceId?: string
  interventionAction: EdhrFlowInterventionAction
  interventionStatus: EdhrFlowInterventionStatus
  fromStatus: string
  toStatus: string
  sourceTaskId?: string
  targetTaskId?: string
  nodeKey?: string
  targetUserId?: number
  requestedBy?: number
  requestedAt?: string | number
  reasonCategory?: string
  reason: string
  authorizationBasis?: string
  signoffEvidenceHash: string
  idempotencyKey: string
  integrityCheckResult: EdhrFlowInterventionIntegrityResult
  integrityCheckSnapshotJson?: string
  evidenceHash: string
}

export interface EdhrFlowInterventionBaseReqVO {
  businessObjectType: string
  businessObjectId: string
  businessObjectCode?: string
  flowInstanceId?: string
  taskId?: string
  nodeKey?: string
  fromStatus: string
  toStatus: string
  targetTaskId?: string
  targetUserId?: number
  reasonCategory?: string
  reason: string
  signoffEvidenceHash: string
  idempotencyKey: string
}

export interface EdhrFlowInterventionReturnReqVO extends EdhrFlowInterventionBaseReqVO {}
export interface EdhrFlowInterventionWithdrawReqVO extends EdhrFlowInterventionBaseReqVO {}
export interface EdhrFlowInterventionTransferReqVO extends EdhrFlowInterventionBaseReqVO {
  targetUserId: number
}
export interface EdhrFlowInterventionAddSignReqVO extends EdhrFlowInterventionBaseReqVO {
  targetUserId: number
}
export interface EdhrFlowInterventionAdminReqVO extends EdhrFlowInterventionBaseReqVO {
  authorizationBasis: string
}

export interface EdhrFlowEventPageReqVO extends PageParam {
  businessObjectType: string
  businessObjectId: string
  flowInstanceId?: string
  eventType?: string
}

export interface EdhrFlowEventRespVO {
  id: number
  businessObjectType: string
  businessObjectId: string
  businessObjectCode?: string
  interventionId?: number
  flowInstanceId?: string
  taskId?: string
  nodeKey?: string
  eventType: string
  fromStatus: string
  toStatus: string
  actorUserId?: number
  targetUserId?: number
  permissionCode: string
  permissionDecision: string
  reason?: string
  signoffEvidenceHash?: string
  integrityCheckResult: EdhrFlowInterventionIntegrityResult
  integrityCheckSnapshotJson?: string
  eventSnapshotJson?: string
  evidenceHash: string
  occurredAt: string | number
}

export const getEdhrFlowInterventionPage = async (params: EdhrFlowInterventionPageReqVO) => {
  return await request.get<PageResult<EdhrFlowInterventionRespVO[]>>({
    url: '/mes/pro/edhr-flow-intervention/page',
    params
  })
}

export const getEdhrFlowEventPage = async (params: EdhrFlowEventPageReqVO) => {
  return await request.get<PageResult<EdhrFlowEventRespVO[]>>({
    url: '/mes/pro/edhr-flow-intervention/event/page',
    params
  })
}

export const submitReturnIntervention = async (data: EdhrFlowInterventionReturnReqVO) => {
  return await request.post<EdhrFlowInterventionRespVO>({
    url: '/mes/pro/edhr-flow-intervention/return',
    data
  })
}

export const submitWithdrawIntervention = async (data: EdhrFlowInterventionWithdrawReqVO) => {
  return await request.post<EdhrFlowInterventionRespVO>({
    url: '/mes/pro/edhr-flow-intervention/withdraw',
    data
  })
}

export const submitTransferIntervention = async (data: EdhrFlowInterventionTransferReqVO) => {
  return await request.post<EdhrFlowInterventionRespVO>({
    url: '/mes/pro/edhr-flow-intervention/transfer',
    data
  })
}

export const submitAddSignIntervention = async (data: EdhrFlowInterventionAddSignReqVO) => {
  return await request.post<EdhrFlowInterventionRespVO>({
    url: '/mes/pro/edhr-flow-intervention/add-sign',
    data
  })
}

export const submitAdminIntervention = async (data: EdhrFlowInterventionAdminReqVO) => {
  return await request.post<EdhrFlowInterventionRespVO>({
    url: '/mes/pro/edhr-flow-intervention/admin-intervene',
    data
  })
}
