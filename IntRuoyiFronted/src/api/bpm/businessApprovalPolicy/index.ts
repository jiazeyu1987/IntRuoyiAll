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
  formPolicyType?: string | null
  formSlotsJson?: string | null
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
  approvalSwitchScope?: boolean
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

export interface BusinessApprovalPolicyTransfer {
  schemaVersion: 2
  format: 'BUSINESS_APPROVAL_POLICIES'
  policies: Omit<BusinessApprovalPolicyVO, 'id' | 'updatedTime'>[]
  processes: BusinessApprovalProcessTransfer[]
}

export interface BusinessApprovalProcessTransfer {
  key: string
  name: string
  category: Record<string, unknown>
  bpmnXml: string
  metaInfo: Record<string, unknown>
  roles: { code: string; name: string; category: Record<string, unknown> }[]
  startUsernames: string[]
}

export const BusinessApprovalPolicyApi = {
  exportPolicies: (isRequestContextCurrent: () => boolean) => request.get<BusinessApprovalPolicyTransfer>({
    url: `${BASE_URL}/export`, isRequestContextCurrent
  }),
  importPolicies: (data: BusinessApprovalPolicyTransfer, isRequestContextCurrent: () => boolean) =>
    request.post<number>({ url: `${BASE_URL}/import`, data, isRequestContextCurrent, skipAuthRefreshReplay: true }),
  getPolicyPage: async (params: BusinessApprovalPolicyPageReqVO) => {
    return await request.get<PageResult<BusinessApprovalPolicyVO[]>>({ url: BASE_URL, params })
  },

  savePolicy: async (data: BusinessApprovalPolicySaveReqVO) => {
    return await request.post<BusinessApprovalPolicyVO>({ url: BASE_URL, data })
  },

  deleteDraftPolicy: async (id: number) => {
    return await request.delete<boolean>({ url: `${BASE_URL}/${id}` })
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
