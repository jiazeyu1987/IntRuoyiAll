import request from '@/config/axios'

const EDHR_DELIVERY_PROJECT_PAGE_URL = '/mes/pro/edhr-delivery-cockpit/project/page'
const EDHR_DELIVERY_PROJECT_CREATE_URL = '/mes/pro/edhr-delivery-cockpit/project/create'
const EDHR_DELIVERY_PROJECT_DETAIL_URL = '/mes/pro/edhr-delivery-cockpit/project/detail'
const EDHR_DELIVERY_PACKAGE_PAGE_URL = '/mes/pro/edhr-delivery-cockpit/evidence-package/page'
const EDHR_DELIVERY_GATE_SUMMARY_URL = '/mes/pro/edhr-delivery-cockpit/gate-summary'

export const EDHR_DELIVERY_QUERY_PERMISSION = 'mes:pro-edhr-delivery:query'
export const EDHR_DELIVERY_CREATE_PERMISSION = 'mes:pro-edhr-delivery:create'

export type EdhrDeliveryProjectStatus = 'BLOCKED' | 'READY' | 'SIGNED' | string
export type EdhrEvidencePackageStatus = 'MISSING' | 'PARTIAL' | 'READY' | 'ACCEPTED' | string
export type EdhrEvidenceStatus = 'MISSING' | 'PARTIAL' | 'READY' | string
export type EdhrDeliveryGateStatus = 'BLOCKED' | 'READY' | 'WAIVED' | string

export interface EdhrDeliveryProjectCreateReqVO {
  projectName: string
  customerName: string
  siteName: string
  systemScope: string
  validationScope: string
  releaseTag: string
  schemaVersion: string
  targetEnvironment: string
  ownerName: string
  ownerDepartment?: string
  remark?: string
}

export interface EdhrDeliveryProjectPageReqVO extends PageParam {
  projectCode?: string
  projectName?: string
  customerName?: string
  projectStatus?: EdhrDeliveryProjectStatus
}

export interface EdhrDeliveryProjectRespVO {
  id: number
  projectCode: string
  projectName: string
  customerName: string
  siteName: string
  systemScope: string
  validationScope: string
  releaseTag: string
  schemaVersion: string
  targetEnvironment: string
  projectStatus: EdhrDeliveryProjectStatus
  signoffAllowed: boolean
  ownerName: string
  ownerDepartment?: string
  blockedReason?: string
  gateSummaryJson?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrEvidencePackagePageReqVO extends PageParam {
  projectId: number
  packageStatus?: EdhrEvidencePackageStatus
  evidenceStatus?: EdhrEvidenceStatus
}

export interface EdhrEvidencePackageRespVO {
  id: number
  projectId: number
  packageCode: string
  packageName: string
  packageType: string
  packageStatus: EdhrEvidencePackageStatus
  evidenceStatus: EdhrEvidenceStatus
  ownerName: string
  ownerDepartment?: string
  requiredEvidenceJson?: string
  availableEvidenceJson?: string
  missingEvidenceJson?: string
  signoffImpact?: string
  nextAction?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface EdhrDeliveryGateItemRespVO {
  id: number
  projectId: number
  packageId: number
  gateCode: string
  gateName: string
  gateStatus: EdhrDeliveryGateStatus
  missingEvidence: string
  ownerName: string
  nextAction: string
  signoffImpact: string
  blockingFlag: boolean
  sort?: number
}

export interface EdhrDeliveryGateSummaryRespVO {
  projectId: number
  projectCode: string
  projectStatus: EdhrDeliveryProjectStatus
  signoffAllowed: boolean
  packageCount: number
  gateCount: number
  blockedCount: number
  gateStatus: EdhrDeliveryGateStatus
  summary: string
  gateItems: EdhrDeliveryGateItemRespVO[]
}

export const getEdhrDeliveryProjectPage = async (params: EdhrDeliveryProjectPageReqVO) => {
  return await request.get<PageResult<EdhrDeliveryProjectRespVO[]>>({
    url: EDHR_DELIVERY_PROJECT_PAGE_URL,
    params
  })
}

export const createEdhrDeliveryProject = async (data: EdhrDeliveryProjectCreateReqVO) => {
  return await request.post<EdhrDeliveryProjectRespVO>({
    url: EDHR_DELIVERY_PROJECT_CREATE_URL,
    data
  })
}

export const getEdhrDeliveryProjectDetail = async (id: number) => {
  return await request.get<EdhrDeliveryProjectRespVO>({
    url: EDHR_DELIVERY_PROJECT_DETAIL_URL,
    params: { id }
  })
}

export const getEdhrEvidencePackagePage = async (params: EdhrEvidencePackagePageReqVO) => {
  return await request.get<PageResult<EdhrEvidencePackageRespVO[]>>({
    url: EDHR_DELIVERY_PACKAGE_PAGE_URL,
    params
  })
}

export const getEdhrDeliveryGateSummary = async (projectId: number) => {
  return await request.get<EdhrDeliveryGateSummaryRespVO>({
    url: EDHR_DELIVERY_GATE_SUMMARY_URL,
    params: { projectId }
  })
}
