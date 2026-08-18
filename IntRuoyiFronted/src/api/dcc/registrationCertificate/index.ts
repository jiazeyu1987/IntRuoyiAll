import request from '@/config/axios'

export type DccRegistrationCertificateStatus =
  | 'DRAFT'
  | 'PENDING_EFFECTIVE'
  | 'CURRENT'
  | 'OLD'
  | 'VOIDED'

export interface DccRegistrationCertificatePageReqVO extends PageParam {
  ownerCompanyId?: number | string
  productMasterId?: number | string
  status?: DccRegistrationCertificateStatus
  certificateNo?: string
  missingProjectCode?: boolean
  missingFile?: boolean
  firstObtainedStart?: string
  firstObtainedEnd?: string
  approvalStart?: string
  approvalEnd?: string
  effectiveStart?: string
  effectiveEnd?: string
  expiryStart?: string
  expiryEnd?: string
}

export interface DccRegistrationCertificatePageItemVO {
  certificateId: number | string
  versionId: number | string
  snapshotId?: number | string
  ownerCompanyId: number | string
  ownerCompanyName: string
  productName: string
  certificateNo: string
  versionNo: number
  status: DccRegistrationCertificateStatus
  hasProjectCode: boolean
  hasRegistrationFile: boolean
  firstObtainedDate?: string
  approvalDate?: string
  effectiveDate?: string
  expiryDate?: string
}

export interface DccRegistrationCertificateOldIndexItemVO {
  certificateId: number | string
  versionId: number | string
  ownerCompanyId: number | string
  ownerCompanyName: string
  productName: string
  certificateNo: string
  versionNo: number
  expiryDate?: string
  status: DccRegistrationCertificateStatus
}

export interface DccRegistrationCertificateDetailVO {
  certificateId: number | string
  versionId: number | string
  snapshotId: number | string
  ownerCompanyId: number | string
  ownerCompanyName: string
  productMasterId: number | string
  productName: string
  projectCodeId?: number | string
  certificateNo: string
  versionNo: number
  status: DccRegistrationCertificateStatus
  firstObtainedDate?: string
  approvalDate?: string
  effectiveDate?: string
  expiryDate?: string
  classification: string
  registrantName: string
  modelSpecification: string
  structureComposition: string
  intendedUse: string
  technicalRequirements: string
  residenceAddress: string
  productionAddress: string
  entrustedProduction: boolean
  selfProduction: boolean
  entrustedEnterprisesJson?: string
  hasRegistrationFile: boolean
}

export interface DccRegistrationCertificateHistoryItemVO {
  eventType: string
  itemType: string
  beforeValueJson?: string
  afterValueJson?: string
  actorId?: number | string
}

export const getRegistrationCertificatePage = async (
  params: DccRegistrationCertificatePageReqVO
) => {
  return await request.get<PageResult<DccRegistrationCertificatePageItemVO[]>>({
    url: '/dcc/registration-certificates/page',
    params
  })
}

export const getRegistrationCertificateOldIndexPage = async (
  params: DccRegistrationCertificatePageReqVO
) => {
  return await request.get<PageResult<DccRegistrationCertificateOldIndexItemVO[]>>({
    url: '/dcc/registration-certificates/old-index/page',
    params
  })
}

export const getRegistrationCertificateDetail = async (id: number | string) => {
  return await request.get<DccRegistrationCertificateDetailVO>({
    url: `/dcc/registration-certificates/${id}`
  })
}

export const getRegistrationCertificateHistory = async (id: number | string) => {
  return await request.get<DccRegistrationCertificateHistoryItemVO[]>({
    url: `/dcc/registration-certificates/${id}/history`
  })
}
