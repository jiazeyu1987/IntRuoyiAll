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

export interface DccRegistrationCertificateDraftReqVO {
  ownerCompanyId: number | string
  productMasterId: number | string
  projectCodeId?: number | string
  firstObtainedDate: string
  certificateNo: string
  approvalDate: string
  effectiveDate: string
  expiryDate: string
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
  entrustedEnterpriseIds: Array<number | string>
}

export interface DccRegistrationCertificateUpdateDraftReqVO
  extends DccRegistrationCertificateDraftReqVO {
  expectedRowVersion: number
  expectedSnapshotRevision: number
}

export interface DccRegistrationCertificateFormalizeReqVO {
  expectedRowVersion: number
  expectedSnapshotRevision: number
  businessFileId: number | string
}

export interface DccRegistrationCertificateRenewalUploadReqVO {
  expectedRowVersion: number
  currentVersionId: number | string
  businessFileId: number | string
  categoryChanged: boolean
  certificateNo?: string
  classification?: string
  approvalDate: string
  effectiveDate: string
  expiryDate: string
}

export interface DccRegistrationCertificateRenewalVoidReqVO {
  expectedRowVersion: number
  voidReason: string
}

export interface DccRegistrationCertificateChangeApplyReqVO {
  expectedRowVersion: number
  approvalDate: string
  structuredValues?: Record<string, string>
  otherDescription?: string
  entrustedProduction?: boolean
  selfProduction?: boolean
  entrustedEnterprisesJson?: string
}

export interface DccRegistrationCertificateVoidReqVO {
  expectedRowVersion: number
  approvalDate: string
  voidReason: string
}

export interface DccRegistrationCertificateSupportingDocumentUploadReqVO {
  versionId: number | string
  businessFileId: number | string
  documentType: string
}

export interface DccRegistrationCertificateSupportingDocumentReviewReqVO
  extends DccRegistrationCertificateSupportingDocumentUploadReqVO {
  expectedRowVersion: number
  rejectReason?: string
}

export interface DccRegistrationCertificateAccessRequestSubmitReqVO {
  certificateId: number | string
  requestType: 'VIEW_OLD_CERTIFICATE' | 'DOWNLOAD_FILE'
  purpose: string
  projectCodeId?: number | string
  businessFileIds?: Array<number | string>
}

export interface DccRegistrationCertificatePreviewMetadataVO {
  previewKind?: string
  onlyofficeBaseUrl?: string
  onlyofficeDocumentUrl?: string
  viewerToken?: string
  accessEventCode?: string
  watermarkTraceCode?: string
  viewerTokenId?: string
  viewerTokenNonce?: string
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

export const createRegistrationCertificateDraft = async (
  data: DccRegistrationCertificateDraftReqVO,
  idempotencyKey: string
) => {
  return await request.post<number | string>({
    url: '/dcc/registration-certificates/drafts',
    data,
    headers: { 'Idempotency-Key': idempotencyKey }
  })
}

export const updateRegistrationCertificateDraft = async (
  id: number | string,
  data: DccRegistrationCertificateUpdateDraftReqVO,
  idempotencyKey: string
) => {
  return await request.put<number | string>({
    url: `/dcc/registration-certificates/drafts/${id}`,
    data,
    headers: { 'Idempotency-Key': idempotencyKey }
  })
}

export const deleteRegistrationCertificateDraft = async (
  id: number | string,
  expectedRowVersion: number,
  expectedSnapshotRevision: number,
  idempotencyKey: string
) => {
  return await request.delete<number | string>({
    url: `/dcc/registration-certificates/drafts/${id}`,
    params: { expectedRowVersion, expectedSnapshotRevision },
    headers: { 'Idempotency-Key': idempotencyKey }
  })
}

export const formalizeRegistrationCertificate = async (
  id: number | string,
  data: DccRegistrationCertificateFormalizeReqVO,
  idempotencyKey: string
) => {
  return await request.post<number | string>({
    url: `/dcc/registration-certificates/${id}/formalize`,
    data,
    headers: { 'Idempotency-Key': idempotencyKey }
  })
}

export const uploadRegistrationCertificateRenewalCandidate = async (
  certificateId: number | string,
  data: DccRegistrationCertificateRenewalUploadReqVO,
  idempotencyKey: string
) => {
  return await request.post({
    url: `/dcc/registration-certificates/${certificateId}/renewals`,
    data,
    headers: { 'Idempotency-Key': idempotencyKey }
  })
}

export const voidRegistrationCertificateRenewalCandidate = async (
  certificateId: number | string,
  pendingVersionId: number | string,
  data: DccRegistrationCertificateRenewalVoidReqVO,
  idempotencyKey: string
) => {
  return await request.post({
    url: `/dcc/registration-certificates/${certificateId}/renewals/${pendingVersionId}/void`,
    data,
    headers: { 'Idempotency-Key': idempotencyKey }
  })
}

export const submitRegistrationCertificateChange = async (
  certificateId: number | string,
  data: DccRegistrationCertificateChangeApplyReqVO,
  idempotencyKey: string
) => {
  return await request.post({
    url: `/dcc/registration-certificates/${certificateId}/changes`,
    data,
    headers: { 'Idempotency-Key': idempotencyKey }
  })
}

export const voidRegistrationCertificate = async (
  certificateId: number | string,
  data: DccRegistrationCertificateVoidReqVO,
  idempotencyKey: string
) => {
  return await request.post({
    url: `/dcc/registration-certificates/${certificateId}/changes/void`,
    data,
    headers: { 'Idempotency-Key': idempotencyKey }
  })
}

export const uploadRegistrationCertificateSupportingDocument = async (
  certificateId: number | string,
  data: DccRegistrationCertificateSupportingDocumentUploadReqVO,
  idempotencyKey: string
) => {
  return await request.post({
    url: `/dcc/registration-certificates/${certificateId}/supporting-documents`,
    data,
    headers: { 'Idempotency-Key': idempotencyKey }
  })
}

export const confirmRegistrationCertificateSupportingDocument = async (
  certificateId: number | string,
  supportingDocumentId: number | string,
  data: DccRegistrationCertificateSupportingDocumentReviewReqVO,
  idempotencyKey: string
) => {
  return await request.post({
    url: `/dcc/registration-certificates/${certificateId}/supporting-documents/${supportingDocumentId}/confirm`,
    data,
    headers: { 'Idempotency-Key': idempotencyKey }
  })
}

export const rejectRegistrationCertificateSupportingDocument = async (
  certificateId: number | string,
  supportingDocumentId: number | string,
  data: DccRegistrationCertificateSupportingDocumentReviewReqVO,
  idempotencyKey: string
) => {
  return await request.post({
    url: `/dcc/registration-certificates/${certificateId}/supporting-documents/${supportingDocumentId}/reject`,
    data,
    headers: { 'Idempotency-Key': idempotencyKey }
  })
}

export const submitRegistrationCertificateAccessRequest = async (
  data: DccRegistrationCertificateAccessRequestSubmitReqVO,
  idempotencyKey: string
) => {
  return await request.post<number | string>({
    url: '/dcc/registration-certificates/access-requests',
    data,
    headers: { 'Idempotency-Key': idempotencyKey }
  })
}

export const getRegistrationCertificateFilePreviewMetadata = async (
  businessFileId: number | string
) => {
  return await request.get<DccRegistrationCertificatePreviewMetadataVO>({
    url: `/dcc/registration-certificates/files/${businessFileId}/preview-metadata`
  })
}
