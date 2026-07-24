import request from '@/config/axios'

export const EDHR_FORM_TEMPLATE_STATUS_DRAFT = 'DRAFT'
export const EDHR_FORM_TEMPLATE_STATUS_ACTIVE = 'ACTIVE'
export const EDHR_FORM_INSTANCE_STATUS_DRAFT = 'DRAFT'
export const EDHR_FORM_INSTANCE_STATUS_SUBMITTED = 'SUBMITTED'

export type EdhrFormFieldType = 'text' | 'number' | 'enum' | 'date'

export interface EdhrFormFieldSpec {
  key: string
  label: string
  type: EdhrFormFieldType
  required?: boolean
  min?: number
  max?: number
  options?: string[]
}

export interface EdhrFormTemplatePageReqVO extends PageParam {
  templateCode?: string
  templateName?: string
  status?: 'DRAFT' | 'ACTIVE'
}

export interface EdhrFormTemplateCreateReqVO {
  templateCode: string
  templateName: string
  templateVersion: string
  fieldSchemaJson: string
  remark?: string
}

export interface EdhrFormActivateReqVO {
  id: number
  remark?: string
}

export interface EdhrFormTemplateRespVO {
  id: number
  templateCode?: string
  templateName?: string
  templateVersion?: string
  fieldSchemaJson?: string
  status?: 'DRAFT' | 'ACTIVE'
  activeBy?: number
  activeAt?: string | number
  remark?: string
  createTime?: string | number
  updateTime?: string | number
}

export interface EdhrFormCreateInstanceReqVO {
  templateId: number
  businessScope?: string
  businessObjectType?: string
  businessObjectId?: number
  businessObjectCode?: string
  remark?: string
}

export interface EdhrFormInstancePageReqVO extends PageParam {
  instanceCode?: string
  templateCode?: string
  status?: 'DRAFT' | 'SUBMITTED'
  businessScope?: string
  businessObjectCode?: string
}

export interface EdhrFormInstanceSaveDraftReqVO {
  id: number
  values: Record<string, unknown>
  remark?: string
}

export interface EdhrFormInstanceSubmitReqVO {
  id: number
  values: Record<string, unknown>
  remark?: string
}

export interface EdhrFormInstanceRespVO {
  id: number
  instanceCode?: string
  templateId?: number
  templateCode?: string
  templateName?: string
  templateVersion?: string
  fieldSchemaJson?: string
  status?: 'DRAFT' | 'SUBMITTED'
  version?: number
  businessScope?: string
  businessObjectType?: string
  businessObjectId?: number
  businessObjectCode?: string
  values?: Record<string, unknown>
  submittedBy?: number
  submittedAt?: string | number
  remark?: string
  createTime?: string | number
  updateTime?: string | number
}

export interface EdhrFormEventPageReqVO extends PageParam {
  instanceId?: number
  templateId?: number
  eventType?: string
  resultStatus?: string
}

export interface EdhrFormEventRespVO {
  id: number
  instanceId?: number
  templateId?: number
  instanceCode?: string
  eventType?: string
  resultStatus?: string
  failureReason?: string
  operatorUserId?: number
  operatorUsername?: string
  occurredAt?: string | number
  metadataJson?: string
}

const FORM_TEMPLATE_PAGE_URL = '/mes/pro/edhr-form-template/page'
const FORM_TEMPLATE_CREATE_URL = '/mes/pro/edhr-form-template/create'
const FORM_TEMPLATE_ACTIVATE_URL = '/mes/pro/edhr-form-template/activate'
const FORM_INSTANCE_PAGE_URL = '/mes/pro/edhr-form-instance/page'
const FORM_INSTANCE_GET_URL = '/mes/pro/edhr-form-instance/get'
const FORM_INSTANCE_CREATE_URL = '/mes/pro/edhr-form-instance/create'
const FORM_INSTANCE_SAVE_DRAFT_URL = '/mes/pro/edhr-form-instance/save-draft'
const FORM_INSTANCE_SUBMIT_URL = '/mes/pro/edhr-form-instance/submit'
const FORM_INSTANCE_EVENT_PAGE_URL = '/mes/pro/edhr-form-instance/event/page'

export const getEdhrFormTemplatePage = async (params: EdhrFormTemplatePageReqVO) => {
  return await request.get<PageResult<EdhrFormTemplateRespVO[]>>({
    url: FORM_TEMPLATE_PAGE_URL,
    params
  })
}

export const createEdhrFormTemplate = async (data: EdhrFormTemplateCreateReqVO) => {
  return await request.post<EdhrFormTemplateRespVO>({
    url: FORM_TEMPLATE_CREATE_URL,
    data
  })
}

export const activateEdhrFormTemplate = async (data: EdhrFormActivateReqVO) => {
  return await request.post<EdhrFormTemplateRespVO>({
    url: FORM_TEMPLATE_ACTIVATE_URL,
    data
  })
}

export const getEdhrFormInstancePage = async (params: EdhrFormInstancePageReqVO) => {
  return await request.get<PageResult<EdhrFormInstanceRespVO[]>>({
    url: FORM_INSTANCE_PAGE_URL,
    params
  })
}

export const getEdhrFormInstance = async (id: number) => {
  return await request.get<EdhrFormInstanceRespVO>({
    url: FORM_INSTANCE_GET_URL,
    params: { id }
  })
}

export const createEdhrFormInstance = async (data: EdhrFormCreateInstanceReqVO) => {
  return await request.post<EdhrFormInstanceRespVO>({
    url: FORM_INSTANCE_CREATE_URL,
    data
  })
}

export const saveEdhrFormInstanceDraft = async (data: EdhrFormInstanceSaveDraftReqVO) => {
  return await request.put<EdhrFormInstanceRespVO>({
    url: FORM_INSTANCE_SAVE_DRAFT_URL,
    data
  })
}

export const submitEdhrFormInstance = async (data: EdhrFormInstanceSubmitReqVO) => {
  return await request.put<EdhrFormInstanceRespVO>({
    url: FORM_INSTANCE_SUBMIT_URL,
    data
  })
}

export const getEdhrFormEventPage = async (params: EdhrFormEventPageReqVO) => {
  return await request.get<PageResult<EdhrFormEventRespVO[]>>({
    url: FORM_INSTANCE_EVENT_PAGE_URL,
    params
  })
}

export const createTemplate = createEdhrFormTemplate
export const activateTemplate = activateEdhrFormTemplate
export const createFormInstance = createEdhrFormInstance
export const saveFormInstanceDraft = saveEdhrFormInstanceDraft
export const submitFormInstance = submitEdhrFormInstance

export const EdhrFormApi = {
  getTemplatePage: getEdhrFormTemplatePage,
  createTemplate: createEdhrFormTemplate,
  activateTemplate: activateEdhrFormTemplate,
  getInstancePage: getEdhrFormInstancePage,
  getInstance: getEdhrFormInstance,
  createFormInstance: createEdhrFormInstance,
  saveFormInstanceDraft: saveEdhrFormInstanceDraft,
  submitFormInstance: submitEdhrFormInstance,
  getFormEventPage: getEdhrFormEventPage
}
