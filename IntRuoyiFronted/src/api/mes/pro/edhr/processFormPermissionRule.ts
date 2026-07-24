import request from '@/config/axios'

export type EdhrProcessFormCandidateSourceType =
  | 'USER'
  | 'USERS'
  | 'ROLE'
export type EdhrProcessFormCompletionPolicy = 'ANY_ONE' | 'ALL'
export type EdhrProcessFormSignatureRole = 'APPROVAL' | 'APPROVE' | 'REVIEW'

export interface EdhrProcessFormCandidateUser {
  userId: number
  displayName: string
}

export interface EdhrProcessFormCandidateRule {
  candidateSourceType: EdhrProcessFormCandidateSourceType
  candidateSourceIds: number[]
  completionPolicy: EdhrProcessFormCompletionPolicy
  dueMinutes?: number | null
  enabled?: boolean
  remark?: string | null
  candidateUsers?: EdhrProcessFormCandidateUser[]
}

export interface EdhrProcessFormSignatureRule {
  signatureCellKey: string
  signatureRole: EdhrProcessFormSignatureRole
  rule: EdhrProcessFormCandidateRule
}

export interface EdhrProcessFormPermissionRuleRespVO {
  routeProcessId?: number | null
  batchRecordReportId: string
  fillRuleStatus: 'CONFIGURED' | 'NOT_CONFIGURED' | 'CANDIDATE_EMPTY' | 'INCOMPLETE'
  signatureRuleStatus: 'CONFIGURED' | 'NOT_CONFIGURED' | 'CANDIDATE_EMPTY' | 'INCOMPLETE'
  permissionScopeId?: number | null
  fillRule?: EdhrProcessFormCandidateRule | null
  signatureRules: EdhrProcessFormSignatureRule[]
  updateTime?: string
  affectedRouteBindingCount?: number
}

export interface EdhrProcessFormPermissionRuleSaveVO {
  routeProcessId: number
  batchRecordReportId: string
  fillRule: EdhrProcessFormCandidateRule
  signatureRules?: EdhrProcessFormSignatureRule[]
}

export interface EdhrBatchRecordFormPermissionRuleSaveVO {
  batchRecordReportId: string
  fillRule: EdhrProcessFormCandidateRule
}

export const EdhrProcessFormPermissionRuleApi = {
  get: async (params: { routeProcessId: number; batchRecordReportId: string }) =>
    await request.get<EdhrProcessFormPermissionRuleRespVO>({
      url: '/mes/pro/edhr-process-form-permission-rule/get',
      params
    }),

  getByReport: async (batchRecordReportId: string) =>
    await request.get<EdhrProcessFormPermissionRuleRespVO>({
      url: '/mes/pro/edhr-process-form-permission-rule/get-by-report',
      params: { batchRecordReportId }
    }),

  save: async (data: EdhrProcessFormPermissionRuleSaveVO, options?: { ignoreErrorMessage?: boolean }) =>
    await request.post<EdhrProcessFormPermissionRuleRespVO>({
      url: '/mes/pro/edhr-process-form-permission-rule/save',
      data,
      ignoreErrorMessage: options?.ignoreErrorMessage
    }),

  saveByReport: async (
    data: EdhrBatchRecordFormPermissionRuleSaveVO,
    options?: { ignoreErrorMessage?: boolean }
  ) =>
    await request.post<EdhrProcessFormPermissionRuleRespVO>({
      url: '/mes/pro/edhr-process-form-permission-rule/save-by-report',
      data,
      ignoreErrorMessage: options?.ignoreErrorMessage
    })
}
