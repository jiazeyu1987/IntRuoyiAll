import request from '@/config/axios'

const EDHR_VALIDATION_PACKAGE_PAGE_URL = '/mes/pro/edhr-validation-package/page'
const EDHR_VALIDATION_PACKAGE_CREATE_URL = '/mes/pro/edhr-validation-package/create'
const EDHR_VALIDATION_PACKAGE_DETAIL_URL = '/mes/pro/edhr-validation-package/detail'
const EDHR_VALIDATION_PACKAGE_EVALUATE_TRACE_URL = '/mes/pro/edhr-validation-package/evaluate-trace'
const EDHR_VALIDATION_ITEM_PAGE_URL = '/mes/pro/edhr-validation-requirement-item/page'
const EDHR_VALIDATION_ITEM_CREATE_URL = '/mes/pro/edhr-validation-requirement-item/create'
const EDHR_VALIDATION_TRACE_LINK_CREATE_URL = '/mes/pro/edhr-validation-trace-link/create'

export const EDHR_VALIDATION_QUERY_PERMISSION = 'mes:pro-edhr-validation:query'
export const EDHR_VALIDATION_CREATE_PERMISSION = 'mes:pro-edhr-validation:create'
export const EDHR_VALIDATION_EVALUATE_PERMISSION = 'mes:pro-edhr-validation:evaluate-trace'

export type EdhrValidationStatus = 'BLOCKED' | 'PREPARED' | string
export type EdhrValidationItemType = 'URS' | 'FRS' | 'RISK' | 'IQ' | 'OQ' | 'PQ' | string
export type EdhrValidationItemStatus = 'DRAFT' | 'ACTIVE' | 'CLOSED' | string
export type EdhrValidationTraceStatus = 'ACTIVE' | 'BLOCKED' | 'READY' | string

export interface EdhrValidationPackageCreateReqVO {
  packageName: string
  customerProjectName: string
  customerName: string
  siteName: string
  systemScope: string
  validationScope: string
  releaseTag: string
  schemaVersion: string
  targetEnvironment: string
  validationOwnerName: string
  qaOwnerName: string
  remark?: string
}

export interface EdhrValidationPackagePageReqVO extends PageParam {
  packageCode?: string
  packageName?: string
  customerProjectName?: string
  validationStatus?: EdhrValidationStatus
}

export interface EdhrValidationPackageRespVO {
  id: number
  packageCode: string
  packageName: string
  customerProjectName: string
  customerName: string
  siteName: string
  systemScope: string
  validationScope: string
  releaseTag: string
  schemaVersion: string
  targetEnvironment: string
  validationStatus: EdhrValidationStatus
  oqReady: boolean
  validationOwnerName: string
  qaOwnerName: string
  blockedReason?: string
  traceSummaryJson?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrValidationRequirementItemCreateReqVO {
  packageId: number
  itemCode: string
  itemName: string
  itemType: EdhrValidationItemType
  itemVersion: string
  itemStatus: EdhrValidationItemStatus
  ownerName: string
  signoffRole: string
  sourceDocument: string
  businessProcess?: string
  acceptanceCriteria?: string
  sort?: number
  remark?: string
}

export interface EdhrValidationRequirementItemPageReqVO extends PageParam {
  packageId: number
  itemCode?: string
  itemType?: EdhrValidationItemType
  itemStatus?: EdhrValidationItemStatus
}

export interface EdhrValidationRequirementItemRespVO {
  id: number
  packageId: number
  itemCode: string
  itemName: string
  itemType: EdhrValidationItemType
  itemVersion: string
  itemStatus: EdhrValidationItemStatus
  ownerName: string
  signoffRole: string
  sourceDocument: string
  businessProcess?: string
  acceptanceCriteria?: string
  sort?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrValidationTraceLinkCreateReqVO {
  packageId: number
  sourceItemId: number
  targetItemId: number
  linkType: string
  ownerName: string
  nextAction: string
  remark?: string
}

export interface EdhrValidationTraceLinkRespVO {
  id: number
  packageId: number
  sourceItemId: number
  sourceItemCode: string
  sourceItemType: EdhrValidationItemType
  targetItemId: number
  targetItemCode: string
  targetItemType: EdhrValidationItemType
  linkType: string
  traceStatus: EdhrValidationTraceStatus
  ownerName: string
  nextAction: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrValidationTraceIssueRespVO {
  packageId: number
  sourceItemId?: number
  sourceItemCode: string
  sourceItemType: EdhrValidationItemType
  missingItemType: string
  missingItemName: string
  ownerName: string
  signoffRole: string
  nextAction: string
  blockingReason: string
  signoffImpact: string
}

export interface EdhrValidationTraceEvaluateRespVO {
  packageId: number
  packageCode: string
  validationStatus: EdhrValidationStatus
  oqReady: boolean
  traceStatus: EdhrValidationTraceStatus
  ursCount: number
  frsCount: number
  riskCount: number
  iqCount: number
  oqCount: number
  pqCount: number
  traceLinkCount: number
  brokenTraceCount: number
  brokenItems: EdhrValidationTraceIssueRespVO[]
  blockedReason: string
  summary: string
  nextAction: string
}

export const getEdhrValidationPackagePage = async (params: EdhrValidationPackagePageReqVO) => {
  return await request.get<PageResult<EdhrValidationPackageRespVO[]>>({
    url: EDHR_VALIDATION_PACKAGE_PAGE_URL,
    params
  })
}

export const createEdhrValidationPackage = async (data: EdhrValidationPackageCreateReqVO) => {
  return await request.post<EdhrValidationPackageRespVO>({
    url: EDHR_VALIDATION_PACKAGE_CREATE_URL,
    data
  })
}

export const getEdhrValidationPackageDetail = async (id: number) => {
  return await request.get<EdhrValidationPackageRespVO>({
    url: EDHR_VALIDATION_PACKAGE_DETAIL_URL,
    params: { id }
  })
}

export const evaluateEdhrValidationTrace = async (packageId: number) => {
  return await request.post<EdhrValidationTraceEvaluateRespVO>({
    url: EDHR_VALIDATION_PACKAGE_EVALUATE_TRACE_URL,
    params: { packageId }
  })
}

export const getEdhrValidationRequirementItemPage = async (
  params: EdhrValidationRequirementItemPageReqVO
) => {
  return await request.get<PageResult<EdhrValidationRequirementItemRespVO[]>>({
    url: EDHR_VALIDATION_ITEM_PAGE_URL,
    params
  })
}

export const createEdhrValidationRequirementItem = async (
  data: EdhrValidationRequirementItemCreateReqVO
) => {
  return await request.post<EdhrValidationRequirementItemRespVO>({
    url: EDHR_VALIDATION_ITEM_CREATE_URL,
    data
  })
}

export const createEdhrValidationTraceLink = async (data: EdhrValidationTraceLinkCreateReqVO) => {
  return await request.post<EdhrValidationTraceLinkRespVO>({
    url: EDHR_VALIDATION_TRACE_LINK_CREATE_URL,
    data
  })
}
