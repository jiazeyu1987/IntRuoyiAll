import request from '@/config/axios'

export type FormPolicyStatus = 'DRAFT' | 'PUBLISHED'
export type FormPolicyType = 'NONE' | 'OPTIONAL' | 'REQUIRED' | 'PACKAGE'
export type FormApprovalMode = 'BPM_REQUIRED' | 'DIRECT'

export interface FormTemplateVersionRefVO {
  versionId: number
  templateCode: string
  versionNo: string
  templateName: string
}

export interface FormPolicySlotVO {
  slotCode: string
  required: boolean
  templateVersionRef: FormTemplateVersionRefVO
}

export interface FormPolicyListItemVO {
  id: number
  dataDomain: string
  systemCode: string
  objectType: string
  actionCode: string
  objectState: string
  policyType: FormPolicyType
  approvalMode: FormApprovalMode
  bpmProcessKey?: string
  effectExecutorCode: string
  status: FormPolicyStatus
  slots: FormPolicySlotVO[]
  remark?: string
  updatedTime: string
}

export interface FormPolicySaveReqVO {
  dataDomain: string
  systemCode: string
  objectType: string
  actionCode: string
  objectState: string
  policyType: FormPolicyType
  approvalMode?: FormApprovalMode
  bpmProcessKey?: string
  effectExecutorCode: string
  slots: Array<{
    slotCode: string
    required: boolean
    templateId: number
  }>
  remark?: string
}

export interface FormPolicySwitchApprovalModeReqVO {
  approvalMode: FormApprovalMode
  bpmProcessKey?: string
}

export const getPolicyPage = (params: PageParam) => {
  return request.get<PageResult<FormPolicyListItemVO[]>>({
    url: '/form-center/policies',
    params
  })
}

export const savePolicy = (data: FormPolicySaveReqVO) => {
  return request.post<FormPolicyListItemVO>({
    url: '/form-center/policies',
    data
  })
}

export const publishPolicy = (policyId: number) => {
  return request.post<boolean>({
    url: `/form-center/policies/${policyId}/publish`
  })
}

export const switchPolicyApprovalMode = (policyId: number, data: FormPolicySwitchApprovalModeReqVO) => {
  return request.post<FormPolicyListItemVO>({
    url: `/form-center/policies/${policyId}/switch-approval-mode`,
    data
  })
}
