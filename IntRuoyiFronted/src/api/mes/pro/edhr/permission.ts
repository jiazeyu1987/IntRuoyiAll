import request from '@/config/axios'
import type { EdhrRecordCategory, EdhrRouteId } from './batchExecution'

export type EdhrPermissionAbility =
  | 'VIEW'
  | 'FILL'
  | 'SIGN'
  | 'APPROVE'
  | 'ARCHIVE'
  | 'AUDIT_VIEW'
  | 'ROUTE_EDIT'
  | 'PERMISSION_ADMIN'

export type EdhrPermissionDecision = 'ALLOW' | 'DENY'
export type EdhrPermissionSubjectType = 'USER' | 'ROLE' | 'DEPT'
export type EdhrPermissionRuleStatus = 'ENABLED' | 'DISABLED'

export interface EdhrPermissionEvaluateReqVO {
  scopeId?: EdhrRouteId
  objectType?: string
  objectId?: string
  batchExecutionId?: EdhrRouteId
  executionId?: EdhrRouteId
  workTaskId?: EdhrRouteId
  routeId?: EdhrRouteId
  routeProcessId?: EdhrRouteId
  reportId?: string
  recordCategory?: EdhrRecordCategory
  abilities: EdhrPermissionAbility[]
}

export interface EdhrPermissionEvaluateRespVO {
  scopeId?: number
  objectType?: string
  objectId?: string
  decisions: Partial<Record<EdhrPermissionAbility, EdhrPermissionDecision>>
  matchedRuleIds?: number[]
  operationAuditEventId?: number
}

export interface EdhrPermissionRuleSaveVO {
  subjectType: EdhrPermissionSubjectType
  subjectId: number
  ability: EdhrPermissionAbility
  decision: EdhrPermissionDecision
  priority?: number
  effectiveFrom?: string
  effectiveTo?: string
  status?: EdhrPermissionRuleStatus
}

export interface EdhrPermissionRuleRespVO extends EdhrPermissionRuleSaveVO {
  id?: number
  scopeId?: number
  version?: number
}

export interface EdhrPermissionScopeSaveVO {
  scopeId?: EdhrRouteId
  scopeName: string
  objectType: string
  objectId: string
  parentScopeId?: EdhrRouteId
  expectedVersion?: number
  rules: EdhrPermissionRuleSaveVO[]
}

export interface EdhrPermissionScopeDetailRespVO {
  scopeId?: number
  scopeName?: string
  objectType?: string
  objectId?: string
  parentScopeId?: number
  status?: string
  version?: number
  rules: EdhrPermissionRuleRespVO[]
  operationAuditEventId?: number
}

export const EdhrPermissionApi = {
  get: async (params: { scopeId?: EdhrRouteId; objectType?: string; objectId?: string }) =>
    await request.get<EdhrPermissionScopeDetailRespVO>({
      url: '/mes/pro/edhr-permission-scopes/get',
      params
    }),

  save: async (data: EdhrPermissionScopeSaveVO) =>
    await request.post<EdhrPermissionScopeDetailRespVO>({
      url: '/mes/pro/edhr-permission-scopes/save',
      data
    }),

  evaluate: async (data: EdhrPermissionEvaluateReqVO) =>
    await request.post<EdhrPermissionEvaluateRespVO>({
      url: '/mes/pro/edhr-permission-scopes/evaluate',
      data
    })
}
