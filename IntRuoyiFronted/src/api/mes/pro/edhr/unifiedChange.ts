import request from '@/config/axios'

export type EdhrUnifiedChangeObjectType =
  | 'FORM_TEMPLATE'
  | 'DHR_TEMPLATE'
  | 'RECORDBOOK_TEMPLATE'
export type EdhrUnifiedChangeStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'EFFECT_BLOCKED'
export type EdhrUnifiedChangeRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface EdhrUnifiedChangePageReqVO extends PageParam {
  controlledObjectType?: EdhrUnifiedChangeObjectType | ''
  controlledObjectId?: string
  controlledObjectCode?: string
  changeType?: string
  changeStatus?: EdhrUnifiedChangeStatus | ''
  riskLevel?: EdhrUnifiedChangeRiskLevel | ''
}

export interface EdhrUnifiedChangeRespVO {
  id: number
  changeCode: string
  controlledObjectType: EdhrUnifiedChangeObjectType
  controlledObjectId: string
  controlledObjectCode: string
  currentVersion: string
  targetVersion: string
  changeType: string
  changeStatus: EdhrUnifiedChangeStatus
  riskLevel: EdhrUnifiedChangeRiskLevel
  reasonCategory?: string
  reason: string
  diffSnapshotJson: string
  impactSummaryJson: string
  impactRecalculatedAt: string | number
  impactRecalculationHash: string
  requestedBy?: number
  requestedAt: string | number
  submittedBy?: number
  submittedAt?: string | number
  approvedBy?: number
  approvedAt?: string | number
  approvalOpinion?: string
  approvalSignoffEvidenceHash?: string
  effectRequestedBy?: number
  effectRequestedAt?: string | number
  effectSignoffEvidenceHash?: string
  idempotencyKey: string
  evidenceHash: string
}

export interface EdhrUnifiedChangeCreateReqVO {
  controlledObjectType: EdhrUnifiedChangeObjectType
  controlledObjectId: string
  controlledObjectCode: string
  currentVersion: string
  targetVersion: string
  changeType: string
  riskLevel: EdhrUnifiedChangeRiskLevel
  reasonCategory?: string
  reason: string
  diffSnapshotJson: string
  impactSummaryJson: string
  idempotencyKey: string
}

export interface EdhrUnifiedChangeSubmitReqVO {
  changeRequestId: number
  reason: string
  signoffEvidenceHash: string
  idempotencyKey: string
}

export interface EdhrUnifiedChangeRecalculateImpactReqVO {
  changeRequestId: number
  impactSummaryJson: string
  idempotencyKey: string
}

export interface EdhrUnifiedChangeApproveReqVO {
  changeRequestId: number
  approvalOpinion: string
  signoffEvidenceHash: string
  idempotencyKey: string
}

export interface EdhrUnifiedChangeEffectReqVO {
  changeRequestId: number
  reason?: string
  signoffEvidenceHash: string
  idempotencyKey: string
}

export interface EdhrUnifiedChangeImpactPageReqVO extends PageParam {
  changeRequestId: number
  impactType?: string
  impactObjectType?: string
  riskLevel?: EdhrUnifiedChangeRiskLevel | ''
}

export interface EdhrUnifiedChangeImpactRespVO {
  id: number
  changeRequestId: number
  impactType: string
  impactObjectType: string
  impactObjectId: string
  impactObjectCode?: string
  riskLevel: EdhrUnifiedChangeRiskLevel
  responsibilityModule: string
  requiresTraining: boolean
  requiresRevalidation: boolean
  requiresReleaseRecheck: boolean
  impactDetail: string
  nextAction: string
  evidenceHash: string
}

export interface EdhrUnifiedChangeEventPageReqVO extends PageParam {
  changeRequestId: number
  eventType?: string
}

export interface EdhrUnifiedChangeEventRespVO {
  id: number
  changeRequestId: number
  eventType: string
  fromStatus?: string
  toStatus: string
  actorUserId?: number
  reason?: string
  signoffEvidenceHash?: string
  eventSnapshotJson: string
  evidenceHash: string
  occurredAt: string | number
  idempotencyKey: string
}

export const getEdhrUnifiedChangePage = async (params: EdhrUnifiedChangePageReqVO) => {
  return await request.get<PageResult<EdhrUnifiedChangeRespVO[]>>({
    url: '/mes/pro/edhr-change/unified/page',
    params
  })
}

export const getEdhrUnifiedChangeImpactPage = async (params: EdhrUnifiedChangeImpactPageReqVO) => {
  return await request.get<PageResult<EdhrUnifiedChangeImpactRespVO[]>>({
    url: '/mes/pro/edhr-change/unified/impact/page',
    params
  })
}

export const getEdhrUnifiedChangeEventPage = async (params: EdhrUnifiedChangeEventPageReqVO) => {
  return await request.get<PageResult<EdhrUnifiedChangeEventRespVO[]>>({
    url: '/mes/pro/edhr-change/unified/event/page',
    params
  })
}

export const createEdhrUnifiedChange = async (data: EdhrUnifiedChangeCreateReqVO) => {
  return await request.post<EdhrUnifiedChangeRespVO>({
    url: '/mes/pro/edhr-change/unified/create',
    data
  })
}

export const submitEdhrUnifiedChange = async (data: EdhrUnifiedChangeSubmitReqVO) => {
  return await request.post<EdhrUnifiedChangeRespVO>({
    url: '/mes/pro/edhr-change/unified/submit',
    data
  })
}

export const recalculateEdhrUnifiedChangeImpact = async (
  data: EdhrUnifiedChangeRecalculateImpactReqVO
) => {
  return await request.post<EdhrUnifiedChangeRespVO>({
    url: '/mes/pro/edhr-change/unified/recalculate-impact',
    data
  })
}

export const approveEdhrUnifiedChange = async (data: EdhrUnifiedChangeApproveReqVO) => {
  return await request.post<EdhrUnifiedChangeRespVO>({
    url: '/mes/pro/edhr-change/unified/approve',
    data
  })
}

export const requestEdhrUnifiedChangeEffect = async (data: EdhrUnifiedChangeEffectReqVO) => {
  return await request.post<EdhrUnifiedChangeRespVO>({
    url: '/mes/pro/edhr-change/unified/effect',
    data
  })
}
