import request from '@/config/axios'

export type FormTemplateStatus =
  | 'DRAFT'
  | 'PENDING_APPROVAL'
  | 'REJECTED'
  | 'READY'
  | 'PUBLISHED'
  | 'DISABLED'
  | 'OBSOLETE'

export interface FormRecognizedFieldVO {
  fieldCode: string
  label: string
  fieldType: string
  required: boolean
  confidence?: 'HIGH' | 'MEDIUM' | 'LOW'
}

export interface FormTemplateListItemVO {
  templateId: number
  templateName: string
  versionNo: string
  status: FormTemplateStatus
  updatedTime: string
  remark?: string
  slotCode?: string
  actionCode?: string
  recognizedFields?: FormRecognizedFieldVO[]
  jimuSchemaJson?: string
  sourceFileName?: string
}

export interface FormTemplateImportRespVO {
  templateId: number
  versionNo: string
  status: FormTemplateStatus
  importAction: 'CREATE' | 'UPGRADE'
  sourceTemplateId?: number
  approvalRequestId?: number
  approvalProcessInstanceId?: string
  recognizedFields: FormRecognizedFieldVO[]
  warnings: string[]
}

export interface FormTemplateObsoleteReqVO {
  reason: string
  startUserSelectAssignees?: Record<string, number[]>
}

export interface FormTemplateObsoleteRespVO {
  approvalRequestId: number
  approvalProcessInstanceId?: string
  status: FormTemplateStatus
}

export interface FormTemplateObsoletePendingRespVO {
  approvalRequestId: number
  approvalProcessInstanceId?: string
  applicantUserId: number
  canWithdraw: boolean
  objectState: FormTemplateStatus
  status: string
  reason?: string
}

export interface FormTemplatePoolPageReqVO extends PageParam {
  templateName?: string
  status?: FormTemplateStatus
}

export const getTemplatePool = (params: FormTemplatePoolPageReqVO) => {
  return request.get<PageResult<FormTemplateListItemVO[]>>({
    url: '/form-center/template-pool',
    params
  })
}

export const getTemplateVersion = (templateId: number, versionNo: string) => {
  return request.get<FormTemplateListItemVO>({
    url: `/form-center/templates/${templateId}/versions/${versionNo}`
  })
}

export const importTemplateDoc = (data: FormData) => {
  return request.upload<FormTemplateImportRespVO>({
    url: '/form-center/templates/import-doc',
    data
  })
}

export const saveTemplateJimuSchema = (templateId: number, versionNo: string, jimuSchema: string) => {
  return request.put<boolean>({
    url: `/form-center/templates/${templateId}/versions/${versionNo}/jimu-schema`,
    data: { jimuSchema }
  })
}

export const publishTemplateVersion = (templateId: number, versionNo: string) => {
  return request.post<boolean>({
    url: `/form-center/templates/${templateId}/versions/${versionNo}/publish`
  })
}

export const disableTemplateVersion = (templateId: number, versionNo: string) => {
  return request.post<boolean>({
    url: `/form-center/templates/${templateId}/versions/${versionNo}/disable`
  })
}

export const enableTemplateVersion = (templateId: number, versionNo: string) => {
  return request.post<boolean>({
    url: `/form-center/templates/${templateId}/versions/${versionNo}/enable`
  })
}

export const submitTemplateObsoleteRequest = (
  templateId: number,
  versionNo: string,
  data: FormTemplateObsoleteReqVO
) => {
  return request.post<FormTemplateObsoleteRespVO>({
    url: `/form-center/templates/${templateId}/versions/${versionNo}/obsolete-request`,
    data
  })
}

export const findTemplateObsoletePendingRequest = (templateId: number, versionNo: string) => {
  return request.get<FormTemplateObsoletePendingRespVO | null>({
    url: `/form-center/templates/${templateId}/versions/${versionNo}/obsolete-request/pending`
  })
}

export const withdrawTemplateObsoleteRequest = (
  templateId: number,
  versionNo: string,
  reason?: string
) => {
  return request.post<boolean>({
    url: `/form-center/templates/${templateId}/versions/${versionNo}/obsolete-request/withdraw`,
    data: { reason }
  })
}

export const downloadTemplateSourceFile = (templateId: number, versionNo: string) => {
  return request.download({
    url: `/form-center/templates/${templateId}/versions/${versionNo}/source-file`
  })
}
