import request from '@/config/axios'

export type EdhrDhrTemplateStatus =
  | 'DRAFT'
  | 'PRECHECK_FAILED'
  | 'PENDING_REVIEW'
  | 'APPROVED'
  | 'SIGNOFF_PENDING'
  | 'EFFECTIVE'
  | 'SUSPENDED'
  | 'RETIRED'
  | 'OBSOLETE'

export interface EdhrDhrCatalogPageReqVO extends PageParam {
  catalogCode?: string
  catalogName?: string
  status?: string
}

export interface EdhrDhrCatalogCreateReqVO {
  catalogCode: string
  catalogName: string
  parentCatalogId?: number
  remark?: string
}

export interface EdhrDhrCatalogRespVO {
  id: number
  catalogCode: string
  catalogName: string
  parentCatalogId?: number
  status?: string
  remark?: string
  createTime?: string | number
}

export interface EdhrDhrTemplatePageReqVO extends PageParam {
  catalogId?: number
  templateCode?: string
  templateName?: string
  status?: EdhrDhrTemplateStatus
  reviewStatus?: string
  signoffStatus?: string
}

export interface EdhrDhrTemplateCreateReqVO {
  catalogId: number
  templateCode: string
  templateName: string
  currentVersion: string
  templateSnapshotJson: string
  productCode?: string
  routeCode?: string
  processCode?: string
  batchType?: string
  remark?: string
}

export interface EdhrDhrTemplateLifecycleReqVO {
  id: number
}

export interface EdhrDhrTemplateSignoffReqVO {
  id: number
  signoffEvidenceHash: string
}

export interface EdhrDhrTemplateImpactReqVO {
  id: number
  impactScopeJson: string
  impactConfirmed: boolean
}

export interface EdhrDhrTemplateVersionRespVO {
  id: number
  templateId: number
  versionNo: string
  templateSnapshotJson?: string
  changeSummary?: string
  createTime?: string | number
}

export interface EdhrDhrTemplateBindingRespVO {
  id: number
  templateId: number
  bindingType: 'PRODUCT' | 'ROUTE' | 'PROCESS' | 'BATCH_TYPE'
  bindingObjectId?: number
  bindingObjectCode?: string
  bindingObjectName?: string
}

export interface EdhrDhrTemplateRespVO {
  id: number
  catalogId: number
  templateCode: string
  templateName: string
  currentVersion: string
  status?: 'DRAFT' | 'PRECHECK_FAILED' | 'PENDING_REVIEW' | 'APPROVED' | 'SIGNOFF_PENDING' | 'EFFECTIVE' | 'SUSPENDED' | 'RETIRED' | 'OBSOLETE'
  reviewStatus?: string
  signoffStatus?: string
  bindingCount?: number
  integrityIssueCount?: number
  integrityIssueJson?: string
  signoffEvidenceHash?: string
  effectiveAt?: string | number
  retiredAt?: string | number
  voidedAt?: string | number
  remark?: string
  createTime?: string | number
  versions?: EdhrDhrTemplateVersionRespVO[]
  bindings?: EdhrDhrTemplateBindingRespVO[]
}

export interface EdhrDhrTemplateImpactPageReqVO extends PageParam {
  templateId?: number
  actionType?: 'RETIRE' | 'VOID'
}

export interface EdhrDhrTemplateImpactRespVO {
  id: number
  templateId: number
  actionType: 'RETIRE' | 'VOID'
  impactScopeJson: string
  impactConfirmed: boolean
  confirmedBy?: number
  confirmedAt?: string | number
  createTime?: string | number
}

const CATALOG_PAGE_URL = '/mes/pro/edhr-dhr-template/catalog/page'
const CATALOG_CREATE_URL = '/mes/pro/edhr-dhr-template/catalog/create'
const TEMPLATE_PAGE_URL = '/mes/pro/edhr-dhr-template/page'
const TEMPLATE_CREATE_URL = '/mes/pro/edhr-dhr-template/create'
const TEMPLATE_INTEGRITY_CHECK_URL = '/mes/pro/edhr-dhr-template/integrity-check'
const TEMPLATE_APPROVE_URL = '/mes/pro/edhr-dhr-template/approve'
const TEMPLATE_SIGNOFF_URL = '/mes/pro/edhr-dhr-template/signoff'
const TEMPLATE_ACTIVATE_URL = '/mes/pro/edhr-dhr-template/activate'
const TEMPLATE_RETIRE_URL = '/mes/pro/edhr-dhr-template/retire'
const TEMPLATE_VOID_URL = '/mes/pro/edhr-dhr-template/void'
const TEMPLATE_IMPACT_PAGE_URL = '/mes/pro/edhr-dhr-template/impact/page'

export const getCatalogPage = async (params: EdhrDhrCatalogPageReqVO) =>
  await request.get<PageResult<EdhrDhrCatalogRespVO[]>>({
    url: CATALOG_PAGE_URL,
    params
  })

export const createCatalog = async (data: EdhrDhrCatalogCreateReqVO) =>
  await request.post<EdhrDhrCatalogRespVO>({
    url: CATALOG_CREATE_URL,
    data
  })

export const getTemplatePage = async (params: EdhrDhrTemplatePageReqVO) =>
  await request.get<PageResult<EdhrDhrTemplateRespVO[]>>({
    url: TEMPLATE_PAGE_URL,
    params
  })

export const createTemplate = async (data: EdhrDhrTemplateCreateReqVO) =>
  await request.post<EdhrDhrTemplateRespVO>({
    url: TEMPLATE_CREATE_URL,
    data
  })

export const runIntegrityCheck = async (data: EdhrDhrTemplateLifecycleReqVO) =>
  await request.post<EdhrDhrTemplateRespVO>({
    url: TEMPLATE_INTEGRITY_CHECK_URL,
    data
  })

export const approveTemplate = async (data: EdhrDhrTemplateLifecycleReqVO) =>
  await request.post<EdhrDhrTemplateRespVO>({
    url: TEMPLATE_APPROVE_URL,
    data
  })

export const signoffTemplate = async (data: EdhrDhrTemplateSignoffReqVO) =>
  await request.post<EdhrDhrTemplateRespVO>({
    url: TEMPLATE_SIGNOFF_URL,
    data
  })

export const activateTemplate = async (data: EdhrDhrTemplateLifecycleReqVO) =>
  await request.post<EdhrDhrTemplateRespVO>({
    url: TEMPLATE_ACTIVATE_URL,
    data
  })

export const retireTemplate = async (data: EdhrDhrTemplateImpactReqVO) =>
  await request.post<EdhrDhrTemplateRespVO>({
    url: TEMPLATE_RETIRE_URL,
    data
  })

export const voidTemplate = async (data: EdhrDhrTemplateImpactReqVO) =>
  await request.post<EdhrDhrTemplateRespVO>({
    url: TEMPLATE_VOID_URL,
    data
  })

export const getImpactPage = async (params: EdhrDhrTemplateImpactPageReqVO) =>
  await request.get<PageResult<EdhrDhrTemplateImpactRespVO[]>>({
    url: TEMPLATE_IMPACT_PAGE_URL,
    params
  })

export const EdhrDhrTemplateApi = {
  getCatalogPage,
  createCatalog,
  getTemplatePage,
  createTemplate,
  runIntegrityCheck,
  approveTemplate,
  signoffTemplate,
  activateTemplate,
  retireTemplate,
  voidTemplate,
  getImpactPage
}
