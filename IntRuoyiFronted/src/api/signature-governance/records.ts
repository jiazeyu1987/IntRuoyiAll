import request from '@/config/axios'
import { downloadByData } from '@/utils/filt'

export type SignatureGovernanceRecordSourceCode =
  | 'FILE'
  | 'BATCH_RECORD'
  | 'SHOWROOM'
  | 'BPM'
  | 'MES_FEEDBACK'
  | 'SCHEDULING'
  | 'DOCUMENT_CONTROL'

export interface SignatureGovernanceRecordPageReqVO extends PageParam {
  sourceCodes?: SignatureGovernanceRecordSourceCode[]
  keyword?: string
  signerUserId?: number
  signerKeyword?: string
  actionCode?: string
  evidenceHash?: string
  signedAt?: string[]
}

export interface SignatureGovernanceRecordRespVO {
  globalId: string
  sourceCode: SignatureGovernanceRecordSourceCode
  sourceLabel: string
  sourceTable: string
  sourceRecordId: number
  businessRecordId?: number
  businessRecordCode?: string
  businessRecordName?: string
  signerUserId?: number
  signerName?: string
  actorUsernameSnapshot?: string
  actorNicknameSnapshot?: string
  actorDeptNameSnapshot?: string
  actorPostNamesSnapshot?: string
  actorRoleNamesSnapshot?: string
  actionCode?: string
  actionLabel?: string
  meaningCode?: string
  meaningLabel?: string
  comment?: string
  signedAt?: number
  evidenceHash?: string
  evidenceStatus?: string
  detailRouteName?: string
  detailPath?: string
}

export interface SignatureGovernanceRecordPdfArtifact {
  blob: Blob
  fileName: string
}

export const getSignatureGovernanceRecordPage = async (
  params: SignatureGovernanceRecordPageReqVO
): Promise<PageResult<SignatureGovernanceRecordRespVO[]>> => {
  return await request.get({ url: '/signature-governance/signature-records/page', params })
}

export const getMySignatureGovernanceRecordPage = async (
  params: SignatureGovernanceRecordPageReqVO
): Promise<PageResult<SignatureGovernanceRecordRespVO[]>> => {
  return await request.get({ url: '/signature-governance/my-signature-records/page', params })
}

const sanitizeSignatureRecordFileNameSegment = (value: string) => {
  const text = value.trim()
  if (!text) {
    throw new Error('电子签名记录 PDF 缺少全局记录 ID')
  }
  return text.replace(/[\\/:*?"<>|\s]+/g, '_')
}

const assertSignatureGovernanceRecordPdfArtifact = (blob: Blob, fileName: string) => {
  if (!fileName.toLowerCase().endsWith('.pdf')) {
    throw new Error('电子签名记录 PDF 响应文件名无效')
  }
  if (blob.type && blob.type !== 'application/pdf') {
    throw new Error('电子签名记录 PDF 响应不是 PDF 文件')
  }
}

export const fetchSignatureGovernanceRecordPdfArtifact = async (
  globalId: number | string
): Promise<SignatureGovernanceRecordPdfArtifact> => {
  const normalizedGlobalId = String(globalId ?? '').trim()
  const fileName = `electronic-signature-${sanitizeSignatureRecordFileNameSegment(normalizedGlobalId)}.pdf`
  const blob = await request.download<Blob>({
    url: `/signature-governance/signature-records/${encodeURIComponent(String(normalizedGlobalId))}/pdf`,
    responseType: 'blob'
  })
  assertSignatureGovernanceRecordPdfArtifact(blob, fileName)
  return { blob, fileName }
}

export const downloadSignatureGovernanceRecordPdf = async (
  globalId: number | string
): Promise<SignatureGovernanceRecordPdfArtifact> => {
  const artifact = await fetchSignatureGovernanceRecordPdfArtifact(globalId)
  const { blob, fileName } = artifact
  downloadByData(blob, fileName, 'application/pdf')
  return artifact
}
