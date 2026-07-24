import request from '@/config/axios'

export type DccUploadSizePolicyScopeType = 'GLOBAL' | 'CATEGORY' | 'PURPOSE' | 'CATEGORY_PURPOSE'

export type DccUploadSizePolicyPurpose =
  | 'SOURCE'
  | 'DRAWING_PDF'
  | 'TRAINING_RECORD'
  | 'EXTERNAL_REVIEW_OUTPUT'
  | string

export interface DccUploadSizePolicyVO {
  id?: number
  policyCode: string
  scopeType: DccUploadSizePolicyScopeType
  categoryId?: number | null
  purpose?: DccUploadSizePolicyPurpose | null
  maxBytes: number
  enabled: boolean
  priority?: number
  policyVersion: string
  effectiveFrom?: string | null
  effectiveTo?: string | null
  changeReason: string
  createTime?: Date | string
}

export interface DccUploadSizePolicySaveReqVO {
  policyCode: string
  scopeType: DccUploadSizePolicyScopeType
  categoryId?: number | null
  purpose?: DccUploadSizePolicyPurpose | null
  maxBytes: number
  enabled: boolean
  policyVersion: string
  effectiveFrom?: string | null
  effectiveTo?: string | null
  changeReason: string
}

export interface DccUploadSizePolicyEffectiveReqVO {
  categoryId?: number
  purpose?: DccUploadSizePolicyPurpose
  fileSize?: number
}

export interface DccUploadSizePolicyEffectiveVO {
  policyId: number
  policyCode: string
  scopeType: DccUploadSizePolicyScopeType
  categoryId?: number | null
  purpose?: DccUploadSizePolicyPurpose | null
  maxBytes: number
  policyVersion: string
  policyPriority?: number
  scopePriority?: number
}

export const DCC_UPLOAD_SIZE_POLICY_PURPOSE_OPTIONS = [
  { label: '源文件', value: 'SOURCE' },
  { label: '图纸 PDF', value: 'DRAWING_PDF' },
  { label: '培训记录', value: 'TRAINING_RECORD' },
  { label: '外部审核输出', value: 'EXTERNAL_REVIEW_OUTPUT' }
] as const

export const getUploadSizePolicyList = async (): Promise<DccUploadSizePolicyVO[]> => {
  return await request.get({ url: '/dcc/protection/upload-size-policies' })
}

export const createUploadSizePolicy = async (data: DccUploadSizePolicySaveReqVO) => {
  return await request.post({ url: '/dcc/protection/upload-size-policies', data })
}

export const updateUploadSizePolicy = async (
  id: number,
  data: DccUploadSizePolicySaveReqVO
) => {
  return await request.put({ url: `/dcc/protection/upload-size-policies/${id}`, data })
}

export const getEffectiveUploadSizePolicy = async (
  params: DccUploadSizePolicyEffectiveReqVO
): Promise<DccUploadSizePolicyEffectiveVO> => {
  return await request.get({ url: '/dcc/protection/upload-size-policies/effective', params })
}
