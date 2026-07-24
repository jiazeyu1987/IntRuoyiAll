import request from '@/config/axios'

export type BusinessApprovalPolicyMode = 'BPM_REQUIRED' | 'SIGNATURE_REQUIRED' | 'DIRECT'

export type BusinessApprovalPolicyStatus = 'DRAFT' | 'PUBLISHED' | 'DISABLED'

export interface BusinessApprovalPolicyVO {
  id: number
  dataDomain: string
  systemCode: string
  objectType: string
  actionCode: string
  objectState: string
  policyMode: BusinessApprovalPolicyMode
  processDefinitionKey?: string
  effectExecutorCode: string
  status: BusinessApprovalPolicyStatus
  remark?: string
  updatedTime?: string
}

export interface BusinessApprovalPolicyPageReqVO extends PageParam {
  tenantId?: number
  dataDomain?: string
  systemCode?: string
  objectType?: string
  actionCode?: string
  objectState?: string
  policyMode?: BusinessApprovalPolicyMode
  status?: BusinessApprovalPolicyStatus
}

export interface BusinessApprovalPolicySaveReqVO {
  dataDomain: string
  systemCode: string
  objectType: string
  actionCode: string
  objectState: string
  policyMode: BusinessApprovalPolicyMode
  processDefinitionKey?: string
  effectExecutorCode: string
  remark?: string
}

export interface BusinessApprovalPolicySwitchModeReqVO {
  policyMode: BusinessApprovalPolicyMode
  signaturePassword?: string
}

const BASE_URL = '/business-approval/policies'

export const BusinessApprovalPolicyApi = {
  getPolicyPage: async (params: BusinessApprovalPolicyPageReqVO) => {
    return await request.get<PageResult<BusinessApprovalPolicyVO[]>>({ url: BASE_URL, params })
  },

  savePolicy: async (data: BusinessApprovalPolicySaveReqVO) => {
    return await request.post<BusinessApprovalPolicyVO>({ url: BASE_URL, data })
  },

  publishPolicy: async (id: number) => {
    return await request.post<boolean>({ url: `${BASE_URL}/${id}/publish` })
  },

  disablePolicy: async (id: number) => {
    return await request.post<boolean>({ url: `${BASE_URL}/${id}/disable` })
  },

  switchPolicyMode: async (id: number, data: BusinessApprovalPolicySwitchModeReqVO) => {
    return await request.post<BusinessApprovalPolicyVO>({ url: `${BASE_URL}/${id}/switch-mode`, data })
  }
}
