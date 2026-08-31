import request from '@/config/axios'
import { generateUUID } from '@/utils'

const REGISTRATION_CERTIFICATE_UPLOAD_REQUEST_ID_HEADER = 'X-DCC-Request-Id'

export type DccRegistrationCertificateStatus =
  | 'DRAFT'
  | 'PENDING_EFFECTIVE'
  | 'CURRENT'
  | 'OLD'
  | 'VOIDED'

export type DccRegistrationCertificateReminderVisualState =
  | 'NONE'
  | 'CLEARED'
  | 'T_30'
  | 'T_8'
  | 'T_2'
  | 'T_1'

export type DccRegistrationCertificateReminderFilterState =
  | 'NORMAL'
  | 'T_30'
  | 'T_8'
  | 'T_2'
  | 'T_1'

export type RegistrationCertificateSortField =
  | 'certificateNo'
  | 'ownerCompanyName'
  | 'productName'
  | 'classification'
  | 'projectCode'
  | 'versionNo'
  | 'status'
  | 'hasProjectCode'
  | 'hasRegistrationFile'
  | 'approvalDate'
  | 'effectiveDate'
  | 'expiryDate'
  | 'reminder'
  | 'remark'

export type DccRegistrationCertificateLocalDateValue = string | [number, number, number]

export interface DccRegistrationCertificatePageReqVO extends PageParam {
  ownerCompanyId?: number | string
  productMasterId?: number | string
  projectCodeId?: number | string
  status?: DccRegistrationCertificateStatus
  certificateNo?: string
  ownerCompanyName?: string
  productName?: string
  classification?: string
  registrantName?: string
  modelSpecification?: string
  productionAddress?: string
  entrustedEnterpriseName?: string
  projectCode?: string
  missingProjectCode?: boolean
  missingFile?: boolean
  reminderState?: DccRegistrationCertificateReminderFilterState
  firstObtainedStart?: string
  firstObtainedEnd?: string
  approvalStart?: string
  approvalEnd?: string
  effectiveStart?: string
  effectiveEnd?: string
  expiryStart?: string
  expiryEnd?: string
  sortField?: RegistrationCertificateSortField
  sortOrder?: 'asc' | 'desc'
}

export interface DccRegistrationCertificatePageItemVO {
  certificateId: number | string
  rowVersion: number
  versionId: number | string
  snapshotId?: number | string
  ownerCompanyId: number | string
  ownerCompanyName: string
  productMasterId: number | string
  productName: string
  projectCodeId?: number | string
  projectCode?: string
  certificateNo: string
  versionNo: number
  status: DccRegistrationCertificateStatus
  classification: string
  remark?: string
  hasProjectCode: boolean
  hasRegistrationFile: boolean
  reminderColor: string
  visualState: DccRegistrationCertificateReminderVisualState
  firstObtainedDate?: DccRegistrationCertificateLocalDateValue
  approvalDate?: DccRegistrationCertificateLocalDateValue
  effectiveDate?: DccRegistrationCertificateLocalDateValue
  expiryDate?: DccRegistrationCertificateLocalDateValue
}

export interface DccRegistrationCertificateOldIndexItemVO {
  certificateId: number | string
  versionId: number | string
  ownerCompanyId: number | string
  ownerCompanyName: string
  productMasterId: number | string
  productName: string
  projectCodeId?: number | string
  projectCode?: string
  certificateNo: string
  versionNo: number
  classification: string
  expiryDate?: DccRegistrationCertificateLocalDateValue
  status: DccRegistrationCertificateStatus
}

export interface DccRegistrationCertificateDetailVO {
  certificateId: number | string
  rowVersion: number
  versionId: number | string
  snapshotId: number | string
  snapshotRevision: number
  ownerCompanyId: number | string
  ownerCompanyName: string
  productMasterId: number | string
  productName: string
  projectCodeId?: number | string
  projectCode?: string
  certificateNo: string
  versionNo: number
  status: DccRegistrationCertificateStatus
  firstObtainedDate?: DccRegistrationCertificateLocalDateValue
  approvalDate?: DccRegistrationCertificateLocalDateValue
  effectiveDate?: DccRegistrationCertificateLocalDateValue
  expiryDate?: DccRegistrationCertificateLocalDateValue
  classification: string
  remark?: string
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
  registrationFileId?: number | string
  registrationFileName?: string
  hasRegistrationFile: boolean
  reminderColor: string
  visualState: DccRegistrationCertificateReminderVisualState
}

export interface DccRegistrationCertificateHistoryItemVO {
  eventType: string
  itemType: string
  beforeValueJson?: string
  afterValueJson?: string
  actorId?: number | string
  businessFileId?: number | string
  fileKind?: string
  targetVersionId?: number | string
  versionNo?: number
  approvalDate?: DccRegistrationCertificateLocalDateValue
  effectiveDate?: DccRegistrationCertificateLocalDateValue
  expiryDate?: DccRegistrationCertificateLocalDateValue
  categoryChanged?: boolean
  certificateNo?: string
  classification?: string
  originalFileName?: string
  fileStatus?: string
  occurredAt?: string | number
}

export interface DccRegistrationCertificateDraftReqVO {
  ownerCompanyId: number | string
  productMasterId: number | string
  projectCodeId?: number | string
  firstObtainedDate: string
  certificateNo: string
  approvalDate?: string
  effectiveDate: string
  expiryDate: string
  classification: string
  registrantName?: string
  modelSpecification?: string
  structureComposition?: string
  intendedUse?: string
  technicalRequirements?: string
  residenceAddress?: string
  productionAddress?: string
  entrustedProduction?: boolean
  selfProduction?: boolean
  entrustedEnterpriseIds?: Array<number | string>
  remark?: string
}

export interface DccRegistrationCertificateUpdateDraftReqVO
  extends DccRegistrationCertificateDraftReqVO {
  expectedRowVersion: number
  expectedSnapshotRevision: number
}

export interface DccRegistrationCertificateFormalizeReqVO {
  expectedRowVersion: number
  expectedSnapshotRevision: number
  businessFileId?: number | string
}

export interface DccRegistrationCertificateRenewalVoidReqVO {
  expectedRowVersion: number
  voidReason: string
}

export interface DccRegistrationCertificateVoidReqVO {
  expectedRowVersion: number
  approvalDate: string
  voidReason: string
}

export interface DccRegistrationCertificateSupportingDocumentUploadReqVO {
  versionId: number | string
  businessFileId?: number | string
  documentType: string
}

export interface DccRegistrationCertificateUploadSubmitReqVO {
  companyId: number | string
  productName: string
  projectCodeId?: number | string
  certificateNo: string
  firstObtainedDate: string
  effectiveDate: string
  expiryDate: string
  classification: string
  entrustedProduction: boolean
  selfProduction: boolean
  entrustedEnterpriseIds: Array<number | string>
  remark?: string
}

export interface DccRegistrationCertificateUploadEntrustedEnterpriseRespVO {
  id: number | string
  enterpriseCode?: string
  name: string
}

export interface DccRegistrationCertificateUploadCompanyRespVO {
  id: number | string
  enterpriseCode?: string
  name: string
}

export interface DccRegistrationCertificateAccessRequestSubmitReqVO {
  certificateId: number | string
  requestType: 'VIEW_OLD_CERTIFICATE' | 'DOWNLOAD_FILE'
  purpose: string
  projectCodeId?: number | string
  businessFileIds?: Array<number | string>
}

export interface DccRegistrationCertificateAccessReasonReqVO {
  reason: string
}

export interface DccRegistrationCertificateGrantStatusVO {
  grantId: number | string
  requestFileId?: number | string
  businessFileId?: number | string
  grantType: string
  status: string
  grantedAt?: string
  expiresAt?: string
  revokedAt?: string
  revokeReason?: string
}

export interface DccRegistrationCertificateAccessRequestStatusVO {
  requestId: number | string
  certificateId: number | string
  ownerCompanyId: number | string
  requesterUserId: number | string
  requestType: string
  purpose: string
  projectCodeId?: number | string
  requestStatus: string
  bpmProcessInstanceId?: string
  bpmBindingStatus?: string
  requestedAt?: string
  completedAt?: string
  withdrawnAt?: string
  withdrawReason?: string
  rejectReason?: string
  grants: DccRegistrationCertificateGrantStatusVO[]
}

export interface DccRegistrationCertificateApprovalResultVO {
  requestId: number | string
  businessKey: string
  processInstanceId?: string
  status: string
  grantIds: Array<number | string>
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

export const getRegistrationCertificateDetail = async (
  id: number | string,
  versionId?: number | string
) => {
  return await request.get<DccRegistrationCertificateDetailVO>({
    url: `/dcc/registration-certificates/${id}`,
    params: versionId ? { versionId } : undefined
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

export const submitRegistrationCertificateUpload = async (
  data: FormData,
  idempotencyKey: string
) => {
  return await request.upload({
    url: '/dcc/registration-certificates/uploads',
    data,
    headers: {
      'Idempotency-Key': idempotencyKey,
      [REGISTRATION_CERTIFICATE_UPLOAD_REQUEST_ID_HEADER]: idempotencyKey
    }
  })
}

export const getUploadEntrustedEnterprises = async (params?: {
  keyword?: string
}) => {
  return await request.get<DccRegistrationCertificateUploadEntrustedEnterpriseRespVO[]>({
    url: '/dcc/registration-certificates/uploads/entrusted-enterprises',
    params
  })
}

export const getUploadOwnerCompanies = async (params?: { keyword?: string }) => {
  return await request.get<DccRegistrationCertificateUploadCompanyRespVO[]>({
    url: '/dcc/registration-certificates/uploads/owner-companies',
    params
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
  data: FormData,
  idempotencyKey: string
) => {
  return await request.upload({
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

export const getRegistrationCertificateAccessRequestStatus = async (
  requestId: number | string
) => {
  return await request.get<DccRegistrationCertificateAccessRequestStatusVO>({
    url: `/dcc/registration-certificates/access-requests/${requestId}`
  })
}

export const withdrawRegistrationCertificateAccessRequest = async (
  requestId: number | string,
  data: DccRegistrationCertificateAccessReasonReqVO
) => {
  return await request.post<DccRegistrationCertificateApprovalResultVO>({
    url: `/dcc/registration-certificates/access-requests/${requestId}/withdraw`,
    data
  })
}

export const revokeRegistrationCertificateGrant = async (
  grantId: number | string,
  data: DccRegistrationCertificateAccessReasonReqVO
) => {
  return await request.post<boolean>({
    url: `/dcc/registration-certificates/grants/${grantId}/revoke`,
    data
  })
}

export const downloadRegistrationCertificateFile = async (
  businessFileId: number | string,
  attemptKey?: string
): Promise<{ blob: Blob; fileName: string }> => {
  const resolvedAttemptKey = attemptKey?.trim() || `DCC-REG-CERT-DOWNLOAD-${generateUUID()}`
  const response = await request.downloadOriginal<any>({
    url: `/dcc/registration-certificates/files/${businessFileId}/download`,
    headers: { 'X-DCC-Download-Attempt-Key': resolvedAttemptKey }
  })
  const disposition = response.headers?.['content-disposition'] || response.headers?.['Content-Disposition']
  if (!disposition) {
    throw new Error('下载响应缺少服务端文件名，已拒绝保存文件。')
  }
  const encodedName = /filename\*=UTF-8''([^;]+)/i.exec(disposition)?.[1]
  const plainName = /filename="?([^";]+)"?/i.exec(disposition)?.[1]
  const fileName = encodedName ? decodeURIComponent(encodedName) : plainName
  if (!fileName || !fileName.trim()) {
    throw new Error('下载响应文件名无效，已拒绝保存文件。')
  }
  return { blob: response.data, fileName: fileName.trim() }
}

export const getRegistrationCertificateFilePreviewMetadata = async (
  businessFileId: number | string
) => {
  return await request.get<DccRegistrationCertificatePreviewMetadataVO>({
    url: `/dcc/registration-certificates/files/${businessFileId}/preview-metadata`
  })
}

export interface DccRegistrationCertificateRenewalUploadReqVO {
  expectedRowVersion: number
  currentVersionId: number | string
  categoryChanged: boolean
  certificateNo?: string
  classification?: string
  approvalDate: string
  effectiveDate: string
  expiryDate: string
}

export const submitRegistrationCertificateRenewal = async (
  certificateId: number | string,
  data: FormData,
  idempotencyKey: string
) => {
  return await request.upload<number | string>({
    url: `/dcc/registration-certificates/${certificateId}/renewals`,
    data,
    headers: { 'Idempotency-Key': idempotencyKey }
  })
}
