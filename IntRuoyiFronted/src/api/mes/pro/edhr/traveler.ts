import request from '@/config/axios'

export const EDHR_TRAVELER_TEMPLATE_STATUS_DRAFT = 'DRAFT'
export const EDHR_TRAVELER_TEMPLATE_STATUS_ACTIVE = 'ACTIVE'
export const EDHR_TRAVELER_STATUS_GENERATED = 'GENERATED'
export const EDHR_TRAVELER_PRINT_STATUS_NOT_PRINTED = 'NOT_PRINTED'
export const EDHR_TRAVELER_PRINT_STATUS_QUEUED = 'QUEUED'

export interface EdhrTravelerTemplatePageReqVO extends PageParam {
  templateCode?: string
  templateName?: string
  status?: string
  applicableProductCode?: string
  applicableRouteId?: number
  applicableProcessId?: number
  createTime?: string[]
}

export interface EdhrTravelerTemplateCreateReqVO {
  templateCode: string
  templateName: string
  templateVersion: string
  applicableProductCode?: string
  applicableRouteId?: number
  applicableRouteCode?: string
  applicableProcessId?: number
  applicableProcessCode?: string
  applicableProcessName?: string
  remark?: string
}

export interface EdhrTravelerActivateReqVO {
  id: number
}

export interface EdhrTravelerTemplateRespVO {
  id: number
  templateCode?: string
  templateName?: string
  templateVersion?: string
  status?: string
  applicableProductCode?: string
  applicableRouteId?: number
  applicableRouteCode?: string
  applicableProcessId?: number
  applicableProcessCode?: string
  applicableProcessName?: string
  activeAt?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrTravelerPageReqVO extends PageParam {
  travelerCode?: string
  batchExecutionId?: number
  batchExecutionCode?: string
  workOrderCode?: string
  batchCode?: string
  serialNo?: string
  routeProcessId?: number
  processCode?: string
  processName?: string
  status?: string
  printStatus?: string
  generatedAt?: string[]
}

export interface EdhrTravelerGenerateReqVO {
  templateId: number
  batchExecutionId: number
  routeProcessId: number
  serialNo?: string
  requestId?: string
  remark?: string
}

export interface EdhrTravelerRespVO {
  id: number
  travelerCode?: string
  templateId?: number
  templateCode?: string
  templateVersion?: string
  batchExecutionId?: number
  batchExecutionCode?: string
  workOrderId?: number
  workOrderCode?: string
  batchCode?: string
  productId?: number
  productCode?: string
  productName?: string
  serialNo?: string
  scopeType?: 'BATCH_LEVEL' | 'SN_LEVEL'
  routeId?: number
  routeCode?: string
  routeName?: string
  routeProcessId?: number
  routeProcessSort?: number
  processId?: number
  processCode?: string
  processName?: string
  status?: string
  printStatus?: 'NOT_PRINTED' | 'QUEUED'
  businessKeyHash?: string
  generatedBy?: number
  generatedAt?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrTravelerEventPageReqVO extends PageParam {
  travelerId?: number
  travelerCode?: string
  eventType?: string
  resultStatus?: string
  occurredAt?: string[]
}

export interface EdhrTravelerEventRespVO {
  id: number
  travelerId?: number
  travelerCode?: string
  eventType?: string
  resultStatus?: string
  failureReason?: string
  operatorUserId?: number
  operatorUsername?: string
  occurredAt?: string
  metadataJson?: string
}

export const getEdhrTravelerTemplatePage = async (params: EdhrTravelerTemplatePageReqVO) =>
  await request.get<PageResult<EdhrTravelerTemplateRespVO[]>>({
    url: '/mes/pro/edhr-traveler-template/page',
    params
  })

export const createEdhrTravelerTemplate = async (data: EdhrTravelerTemplateCreateReqVO) =>
  await request.post<EdhrTravelerTemplateRespVO>({
    url: '/mes/pro/edhr-traveler-template/create',
    data
  })

export const activateEdhrTravelerTemplate = async (data: EdhrTravelerActivateReqVO) =>
  await request.post<EdhrTravelerTemplateRespVO>({
    url: '/mes/pro/edhr-traveler-template/activate',
    data
  })

export const getEdhrTravelerPage = async (params: EdhrTravelerPageReqVO) =>
  await request.get<PageResult<EdhrTravelerRespVO[]>>({
    url: '/mes/pro/edhr-traveler/page',
    params
  })

export const getEdhrTraveler = async (id: number) =>
  await request.get<EdhrTravelerRespVO>({
    url: '/mes/pro/edhr-traveler/get',
    params: { id }
  })

export const generateEdhrTraveler = async (data: EdhrTravelerGenerateReqVO) =>
  await request.post<EdhrTravelerRespVO>({
    url: '/mes/pro/edhr-traveler/generate',
    data
  })

export const getEdhrTravelerEventPage = async (params: EdhrTravelerEventPageReqVO) =>
  await request.get<PageResult<EdhrTravelerEventRespVO[]>>({
    url: '/mes/pro/edhr-traveler/event/page',
    params
  })

export const EdhrTravelerApi = {
  getTemplatePage: getEdhrTravelerTemplatePage,
  createTemplate: createEdhrTravelerTemplate,
  activateTemplate: activateEdhrTravelerTemplate,
  getTravelerPage: getEdhrTravelerPage,
  getTraveler: getEdhrTraveler,
  generateTraveler: generateEdhrTraveler,
  getTravelerEventPage: getEdhrTravelerEventPage
}
