import request from '@/config/axios'
import type { EdhrRecordCategory, EdhrRouteId } from './batchExecution'

export type EdhrOperationAuditPermissionDecision = 'ALLOW' | 'DENY'
export type EdhrOperationAuditResultStatus = 'SUCCESS' | 'FAILED' | 'REJECTED'

export interface EdhrOperationAuditPageReqVO extends PageParam {
  batchExecutionId?: EdhrRouteId
  executionId?: EdhrRouteId
  workTaskId?: EdhrRouteId
  routeId?: EdhrRouteId
  routeProcessId?: EdhrRouteId
  reportId?: string
  recordCategory?: EdhrRecordCategory
  objectType?: string
  objectId?: string
  operationType?: string
  actorUserId?: number
  permissionDecision?: EdhrOperationAuditPermissionDecision
  resultStatus?: EdhrOperationAuditResultStatus
  occurredAt?: string[]
}

export interface EdhrOperationAuditRespVO {
  id: number
  requestId?: string
  objectType: string
  objectId: string
  batchExecutionId?: number
  executionId?: number
  workTaskId?: number
  routeId?: number
  routeProcessId?: number
  reportId?: string
  recordCategory?: EdhrRecordCategory
  operationType?: string
  actionName?: string
  actorUserId?: number
  actorUsername?: string
  permissionCode?: string
  permissionDecision?: EdhrOperationAuditPermissionDecision
  matchedRuleIds?: string
  resultStatus?: EdhrOperationAuditResultStatus
  failureCode?: string
  failureMessage?: string
  beforeSummaryHash?: string
  afterSummaryHash?: string
  metadataJson?: string
  occurredAt?: string
  previousAuditHash?: string
  auditHash?: string
}

export const EdhrOperationAuditApi = {
  getPage: async (params: EdhrOperationAuditPageReqVO) =>
    await request.get<PageResult<EdhrOperationAuditRespVO[]>>({
      url: '/mes/pro/edhr-operation-audit/page',
      params
    }),

  get: async (id: number) =>
    await request.get<EdhrOperationAuditRespVO>({
      url: `/mes/pro/edhr-operation-audit/${id}`
    })
}
