import axios, { AxiosError } from 'axios'
import request from '@/config/axios'
import { config as axiosConfig } from '@/config/axios/config'
import { getAccessToken, getTenantId, getVisitTenantId } from '@/utils/auth'
import { downloadByData } from '@/utils/filt'
import type { TableQuickFilterValue } from '@/hooks/web/useTableQuickFilter'

export interface DccElectronicSignaturePageReqVO extends PageParam {
  controlledFileId?: number | string
  fileNumber?: string
  revisionId?: number
  versionNo?: string
  signerUserId?: number
  taskActionResult?: string
  meaningCode?: string
  controlledCopyHashStatus?: string
  evidenceStatus?: string
  evidenceHashShort?: string
  signedAt?: string[]
  quickFilter?: TableQuickFilterValue
}

export interface DccElectronicSignatureVO {
  id: number
  controlledFileId: number
  fileNumber: string
  fileName: string
  controlledFileStatus?: string
  taskId?: string
  revisionId: number
  versionNo: string
  signerUserId: number
  signerName?: string
  actorUsernameSnapshot?: string
  actorNicknameSnapshot?: string
  actorDeptIdSnapshot?: number
  actorDeptNameSnapshot?: string
  actorPostNamesSnapshot?: string
  actorRoleNamesSnapshot?: string
  signaturePurpose?: string
  authorizationBasis?: string
  authenticationMethod?: string
  recordVersionSnapshot?: string
  recordHashSnapshot?: string
  clientIpSnapshot?: string
  userAgentSnapshot?: string
  snapshotStatus?: string
  taskActionResult: string
  meaningCode: string
  sourceFileHash?: string
  sourceFileHashShort?: string
  sourceObjectKey?: string
  sourceVersionId?: string
  controlledCopyHashStatus: string
  controlledCopyHash?: string
  controlledCopyHashShort?: string
  controlledCopyObjectKey?: string
  controlledCopyVersionId?: string
  signatureImageId?: number
  signatureImageVersionNo?: number
  signatureImageFileId?: number
  signatureImageFileUrl?: string
  signatureImageSha256?: string
  signatureImageSha256Short?: string
  signatureImageContentType?: string
  signatureImageFileSize?: number
  signatureImageStatusSnapshot?: string
  signatureImageVerifiedStatus?: string
  payloadVersion?: string
  hashAlgorithm?: string
  keyVersion?: string
  evidenceHash?: string
  evidenceHashShort: string
  evidenceStatus: string
  signatureMode?: string
  passwordVerified?: boolean
  comment?: string
  signedAt?: string
}

export interface DccSignatureEvidenceRespVO {
  signatureId: number
  payloadVersion: string
  hashAlgorithm: string
  keyVersion: string
  controlledCopyHashStatus: string
  controlledCopyHash?: string
  controlledCopyHashShort?: string
  signatureImageId?: number
  signatureImageVersionNo?: number
  signatureImageFileId?: number
  signatureImageFileUrl?: string
  signatureImageSha256?: string
  signatureImageSha256Short?: string
  signatureImageContentType?: string
  signatureImageFileSize?: number
  signatureImageStatusSnapshot?: string
  signatureImageVerifiedStatus?: string
  canonicalPayloadFieldOrder: string[]
  canonicalPayload: string
  evidenceHash: string
  evidenceHashShort: string
  evidenceStatus: string
  verificationStatus: string
  verifiedAt?: string
}

export interface DccSignatureVerifyRespVO {
  signatureId: number
  storedEvidenceHash: string
  recomputedEvidenceHash: string
  evidenceHashShort: string
  verificationStatus: string
  verifiedAt: string
}

export interface DccSignatureEvidenceExportDownload {
  blob: Blob
  fileName: string
}

export type DccSignatureEvidencePdfArtifact = DccSignatureEvidenceExportDownload

export interface DccElectronicSignatureImageVO {
  id: number
  userId: number
  versionNo: number
  fileId: number
  fileUrl?: string
  fileName?: string
  contentType?: string
  fileSize?: number
  sha256: string
  sha256Short?: string
  imageStatus: string
  active: boolean
  uploadedBy?: number
  uploadedAt?: string | number
  enabledAt?: string | number
  disabledAt?: string | number
  disableReason?: string
  referencedCount?: number
}

export interface DccElectronicSignatureAuthorizationPageReqVO extends PageParam {
  username?: string
  mobile?: string
  status?: number
  authorizationState?: string
  locked?: boolean
}

export interface DccElectronicSignatureAuthorizationVO {
  userId: number
  userName: string
  username?: string
  nickname?: string
  deptName?: string
  mobile?: string
  status?: number
  loginDate?: string
  electronicSignatureEnabled: boolean
  authorizationState: string
  locked: boolean
  lockedUntil?: string | null
  latestAuditReason?: string
  latestAuditAt?: string
  latestAuditOperatorId?: number
  latestAuditOperatorName?: string
}

export interface DccElectronicSignatureAuthorizationUpdateReqVO {
  electronicSignatureEnabled: boolean
  reason: string
}

export interface DccElectronicSignatureAuthorizationUnlockReqVO {
  reason: string
}

export interface DccElectronicSignatureAuthorizationAuditPageReqVO extends PageParam {}

export interface DccElectronicSignatureAuthorizationAuditVO {
  id: number
  targetUserId: number
  operatorUserId: number
  operatorName?: string
  beforeState: string
  afterState: string
  reason: string
  operatedAt: string
}

export const getDccElectronicSignaturePage = async (
  params: DccElectronicSignaturePageReqVO
): Promise<PageResult<DccElectronicSignatureVO[]>> => {
  return await request.get({ url: '/dcc/electronic-signatures/page', params })
}

export const getDccElectronicSignatureAuthorizationPage = async (
  params: DccElectronicSignatureAuthorizationPageReqVO
): Promise<PageResult<DccElectronicSignatureAuthorizationVO[]>> => {
  return await request.get({ url: '/dcc/electronic-signature-authorizations/page', params })
}

export const updateDccElectronicSignatureAuthorization = async (
  userId: number,
  data: DccElectronicSignatureAuthorizationUpdateReqVO
): Promise<DccElectronicSignatureAuthorizationVO> => {
  return await request.put({ url: `/dcc/electronic-signature-authorizations/${userId}`, data })
}

export const unlockDccElectronicSignatureAuthorization = async (
  userId: number,
  data: DccElectronicSignatureAuthorizationUnlockReqVO
): Promise<DccElectronicSignatureAuthorizationVO> => {
  return await request.post({
    url: `/dcc/electronic-signature-authorizations/${userId}/unlock`,
    data
  })
}

export const getDccElectronicSignatureAuthorizationAuditPage = async (
  userId: number,
  params: DccElectronicSignatureAuthorizationAuditPageReqVO
): Promise<PageResult<DccElectronicSignatureAuthorizationAuditVO[]>> => {
  return await request.get({
    url: `/dcc/electronic-signature-authorizations/${userId}/audits/page`,
    params
  })
}

export const getMyDccElectronicSignatureImage =
  async (): Promise<DccElectronicSignatureImageVO | null> => {
    return await request.get({ url: '/dcc/electronic-signature-authorizations/my-image' })
  }

export const uploadDccElectronicSignatureImage = async (
  file: File,
  reason: string
): Promise<DccElectronicSignatureImageVO> => {
  const data = new FormData()
  data.append('file', file)
  data.append('reason', reason)
  const result = await request.upload<{ data: DccElectronicSignatureImageVO }>({
    url: '/dcc/electronic-signature-authorizations/my-image/upload',
    data
  })
  return result.data
}

export const enableDccElectronicSignatureImage = async (
  imageId: number,
  reason: string
): Promise<DccElectronicSignatureImageVO> => {
  return await request.post({
    url: `/dcc/electronic-signature-authorizations/my-image/${imageId}/enable`,
    params: { reason }
  })
}

export const disableDccElectronicSignatureImage = async (
  reason: string
): Promise<DccElectronicSignatureImageVO> => {
  return await request.post({
    url: '/dcc/electronic-signature-authorizations/my-image/disable',
    params: { reason }
  })
}

export const getDccElectronicSignatureEvidence = async (
  signatureId: number | string
): Promise<DccSignatureEvidenceRespVO> => {
  return await request.get({ url: `/dcc/electronic-signatures/${signatureId}/evidence` })
}

export const verifyDccElectronicSignatureEvidence = async (
  signatureId: number | string
): Promise<DccSignatureVerifyRespVO> => {
  return await request.post({ url: `/dcc/electronic-signatures/${signatureId}/verify` })
}

const buildDccSignatureEvidenceExportHeaders = () => {
  const headers: Record<string, string> = {}
  const accessToken = getAccessToken()
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }
  const tenantId = getTenantId()
  if (tenantId) {
    headers['tenant-id'] = tenantId
  }
  const visitTenantId = getVisitTenantId()
  if (visitTenantId && accessToken) {
    headers['visit-tenant-id'] = visitTenantId
  }
  headers['Cache-Control'] = 'no-cache'
  headers.Pragma = 'no-cache'
  return headers
}

const decodeContentDispositionFileName = (rawHeader: unknown): string | null => {
  const contentDisposition = typeof rawHeader === 'string' ? rawHeader.trim() : ''
  if (!contentDisposition) {
    return null
  }
  const utf8Match = contentDisposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    const encodedFileName = utf8Match[1].trim().replace(/^"(.*)"$/, '$1')
    try {
      return decodeURIComponent(encodedFileName)
    } catch {
      return encodedFileName
    }
  }
  const plainMatch = contentDisposition.match(/filename\s*=\s*("?)([^";]+)\1/i)
  return plainMatch?.[2]?.trim() || null
}

const parseJsonBlob = async (blob: Blob): Promise<Record<string, unknown> | null> => {
  try {
    const text = await blob.text()
    const payload = JSON.parse(text)
    return payload && typeof payload === 'object' ? (payload as Record<string, unknown>) : null
  } catch {
    return null
  }
}

const resolveExportBlockMessage = (payload: Record<string, unknown> | null) => {
  const message = typeof payload?.message === 'string' ? payload.message.trim() : ''
  const msg = typeof payload?.msg === 'string' ? payload.msg.trim() : ''
  return message || msg || '签名证据导出失败，请查看错误提示后重试。'
}

const assertDccSignatureEvidencePdfArtifact = async (blob: Blob, fileName: string) => {
  if (!fileName.toLowerCase().endsWith('.pdf')) {
    throw new Error('签名证据导出响应不是 PDF 文件')
  }
  if (blob.type && blob.type !== 'application/pdf') {
    throw new Error(resolveExportBlockMessage(await parseJsonBlob(blob)))
  }
}

const toDccSignatureEvidenceExportError = async (error: unknown) => {
  if (error instanceof AxiosError) {
    const data = error.response?.data
    if (data instanceof Blob) {
      return new Error(resolveExportBlockMessage(await parseJsonBlob(data)))
    }
    if (data && typeof data === 'object') {
      return new Error(resolveExportBlockMessage(data as Record<string, unknown>))
    }
    return new Error(error.message || '签名证据导出失败，请查看错误提示后重试。')
  }
  if (error instanceof Error) {
    return error
  }
  return new Error('签名证据导出失败，请查看错误提示后重试。')
}

export const fetchDccSignatureEvidencePdfArtifact = async (
  controlledFileId: number | string
): Promise<DccSignatureEvidencePdfArtifact> => {
  try {
    const response = await axios.get<Blob>(
      `${axiosConfig.base_url}/dcc/controlled-files/${controlledFileId}/signature-evidence-export`,
      {
        headers: buildDccSignatureEvidenceExportHeaders(),
        timeout: axiosConfig.request_timeout,
        responseType: 'blob'
      }
    )
    const fileName = decodeContentDispositionFileName(
      response.headers?.['content-disposition'] || response.headers?.['Content-Disposition']
    )
    if (!fileName) {
      throw new Error('签名证据导出响应缺少文件名')
    }
    const blob = response.data
    await assertDccSignatureEvidencePdfArtifact(blob, fileName)
    return { blob, fileName }
  } catch (error) {
    throw await toDccSignatureEvidenceExportError(error)
  }
}

export const downloadDccSignatureEvidenceExport = async (
  controlledFileId: number | string
): Promise<DccSignatureEvidenceExportDownload> => {
  const artifact = await fetchDccSignatureEvidencePdfArtifact(controlledFileId)
  const { blob, fileName } = artifact
  downloadByData(blob, fileName, 'application/pdf')
  return artifact
}
