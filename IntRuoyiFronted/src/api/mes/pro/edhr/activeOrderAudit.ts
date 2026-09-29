import request from '@/config/axios'

export type ActiveOrderAuditScope = 'TEAM' | 'PQC' | 'BATCH'
export type BatchAuditQuery =
  | { activeOrderId: number | string; batchExecutionId?: undefined }
  | { batchExecutionId: number | string; activeOrderId?: undefined }
export type ActiveOrderAuditIdentity = number | string | BatchAuditQuery

export interface GxpAuditEventPageReqVO {
  pageNo: number
  pageSize: number
  operationId?: string
  action?: string
  resultStatus?: string
  signaturePresent?: boolean
  occurredAt?: string[]
}

export interface GxpAuditEventRelationRespVO {
  eventId?: number
  relationType?: string
  targetType?: string
  targetId?: string
  targetVersion?: string
  targetHash?: string
  createdAtUtc?: string
}

export interface GxpAuditEventRespVO {
  id: number
  ledgerSequence?: number
  operationId?: string
  domain?: string
  subjectType?: string
  subjectId?: string
  subjectVersion?: string
  action?: string
  reason?: string
  actorId?: number
  actorDisplayName?: string
  authenticatedActorJson?: string
  performedByJson?: string
  serverOccurredAt?: string
  beforeStateJson?: string
  afterStateJson?: string
  resultStatus?: string
  reasonCode?: string
  signatureRecordId?: string
  eventHash?: string
  previousEventHash?: string
  relationManifestJson?: string
  evidenceManifestJson?: string
  integrityStatus?: string
  relations?: GxpAuditEventRelationRespVO[]
}

const resolveAuditPageUrl = (scope: ActiveOrderAuditScope) => {
  if (scope === 'TEAM') return '/mes/pro/process-pool/team-leader/active-order/audit/page'
  if (scope === 'PQC') return '/mes/pro/production-release/pqc/audit/page'
  return '/mes/pro/edhr-batch-execution/audit/page'
}

const resolveAuditGetUrl = (scope: ActiveOrderAuditScope, eventId: number) => {
  if (scope === 'TEAM') {
    return `/mes/pro/process-pool/team-leader/active-order/audit/get?eventId=${eventId}`
  }
  if (scope === 'PQC') return `/mes/pro/production-release/pqc/audit/get?eventId=${eventId}`
  return `/mes/pro/edhr-batch-execution/audit/get?eventId=${eventId}`
}

const resolveAuditScopeParams = (scope: ActiveOrderAuditScope, scopeId: ActiveOrderAuditIdentity) => {
  if (scope === 'BATCH') {
    if (!scopeId || typeof scopeId !== 'object') throw new Error('批次审计必须提供明确查询身份')
    return scopeId
  }
  if (typeof scopeId !== 'number' && typeof scopeId !== 'string') {
    throw new Error('审计查询身份无效')
  }
  return scope === 'TEAM' ? { activeOrderId: scopeId } : { applicationId: scopeId }
}

export const getActiveOrderGxpAuditPage = async (
  scope: ActiveOrderAuditScope,
  scopeId: ActiveOrderAuditIdentity,
  params: GxpAuditEventPageReqVO
) => {
  const scopeParam = resolveAuditScopeParams(scope, scopeId)
  return await request.get<PageResult<GxpAuditEventRespVO[]>>({
    url: resolveAuditPageUrl(scope),
    params: { ...params, ...scopeParam }
  })
}

export const getActiveOrderGxpAuditEvent = async (
  scope: ActiveOrderAuditScope,
  scopeId: ActiveOrderAuditIdentity,
  eventId: number
) => {
  const scopeParam = resolveAuditScopeParams(scope, scopeId)
  return await request.get<GxpAuditEventRespVO>({
    url: resolveAuditGetUrl(scope, eventId),
    params: scopeParam
  })
}
