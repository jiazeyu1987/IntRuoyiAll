import request from '@/config/axios'
import type { FormTemplateVersionRefVO } from './policy'

export type FormPolicyType = 'NONE' | 'OPTIONAL' | 'REQUIRED' | 'PACKAGE'

export interface BusinessAttachmentMetadataVO {
  fileId: string
  fileName: string
  mimeType: string
  size: number
  sha256: string
  source: string
  controlled: boolean
}

export interface BusinessActionContextVO {
  tenantId?: number
  dataDomain: string
  systemCode: string
  objectType: string
  objectId: string
  objectVersion: string
  actionCode: string
  objectState: string
  orgCode: string
  deptCode: string
  roleCodes: string[]
  productCode: string
  categoryCode: string
  reason: string
  attachmentMetadata?: BusinessAttachmentMetadataVO[]
}

export interface FormActionSlotVO {
  slotCode: string
  required: boolean
  templateVersionRef: FormTemplateVersionRefVO
}

export interface FormActionResolutionVO {
  policyId: number
  policyType: FormPolicyType
  requiresForm: boolean
  requiresBpm: boolean
  bpmProcessKey?: string
  slots: FormActionSlotVO[]
  duplicateDecision?: {
    type: 'ALLOW_CREATE' | 'RETURN_EXISTING_DRAFT' | 'BLOCK_ACTIVE'
    existingInstanceId?: number
  }
}

export const resolveBusinessAction = (data: BusinessActionContextVO) => {
  return request.post<FormActionResolutionVO>({
    url: '/form-center/actions/resolve',
    data
  })
}
