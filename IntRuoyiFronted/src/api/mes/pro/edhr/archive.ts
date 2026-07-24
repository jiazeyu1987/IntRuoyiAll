import request from '@/config/axios'
import { downloadByData } from '@/utils/filt'
import type { EdhrSignatureTimeReqVO } from './batchExecution'

export const EDHR_EXECUTION_ARCHIVE_ARTIFACT_PDF = 'PDF'
export const EDHR_EXECUTION_ARCHIVE_ARTIFACT_EXCEL = 'EXCEL'

export const EDHR_EXECUTION_ARCHIVE_STATUS_GENERATING = 'GENERATING'
export const EDHR_EXECUTION_ARCHIVE_STATUS_SEALED = 'SEALED'
export const EDHR_EXECUTION_ARCHIVE_STATUS_FAILED = 'FAILED'

export const EDHR_EXECUTION_ARCHIVE_NOT_EXISTS_MESSAGE = '批记录执行归档不存在'

export interface ProFeedbackEdhrArchiveGenerateReqVO {
  executionId: number
  artifactType: string
  sealPassword: string
  comment?: string
  signatureTime?: EdhrSignatureTimeReqVO
  regenerate?: boolean
}

export interface ProFeedbackEdhrArchivePageReqVO extends PageParam {
  executionId?: number
  workOrderId?: number
  workOrderCode?: string
  batchCode?: string
  artifactType?: string
  archiveStatus?: string
  generatedTimeStart?: string
  generatedTimeEnd?: string
}

export interface ProFeedbackEdhrExecutionArchiveRespVO {
  id: number
  executionId: number
  archiveCode?: string
  archiveVersion?: number
  artifactType?: string
  archiveStatus?: string
  fileId?: number
  fileName?: string
  contentType?: string
  fileSize?: number
  sha256?: string
  renderSourceVersion?: string
  executionSnapshotHash?: string
  cellValuesHash?: string
  fieldAuditRevision?: number
  fieldAuditHeadHash?: string
  signatureHash?: string
  approvalSnapshotId?: number
  approvalSnapshotHash?: string
  generatedBy?: number
  generatedAt?: string
  sealedBy?: number
  sealedAt?: string
  sealSignatureId?: number
  canDownloadArchive?: boolean
  failureReason?: string
  remark?: string
  created?: boolean
}

const resolveArchiveDownloadName = (
  id: number,
  fallbackFileName?: string,
  artifactType?: string
) => {
  const trimmedFallbackFileName = fallbackFileName?.trim()
  if (trimmedFallbackFileName) {
    return trimmedFallbackFileName
  }
  const normalizedArtifactType = (artifactType || '').trim().toUpperCase()
  if (normalizedArtifactType === EDHR_EXECUTION_ARCHIVE_ARTIFACT_EXCEL) {
    return `edhr-archive-${id}.xlsx`
  }
  return `edhr-archive-${id}.pdf`
}

export const generateEdhrExecutionArchive = async (data: ProFeedbackEdhrArchiveGenerateReqVO) => {
  return await request.post<ProFeedbackEdhrExecutionArchiveRespVO>({
    url: `/mes/pro/batch-record-execution-archive/generate`,
    data
  })
}

export const getEdhrExecutionArchivePage = async (params: ProFeedbackEdhrArchivePageReqVO) => {
  return await request.get<PageResult<ProFeedbackEdhrExecutionArchiveRespVO[]>>({
    url: `/mes/pro/batch-record-execution-archive/page`,
    params
  })
}

export const getLatestEdhrExecutionArchive = async (executionId: number, artifactType: string) => {
  return await request.get<ProFeedbackEdhrExecutionArchiveRespVO>({
    url: `/mes/pro/batch-record-execution-archive/latest`,
    params: {
      executionId,
      artifactType
    },
    ignoreErrorMessage: true
  })
}

export const downloadEdhrExecutionArchive = async (
  id: number,
  fallbackFileName?: string,
  artifactType?: string,
  contentType?: string
) => {
  const blob = await request.download<Blob>({
    url: `/mes/pro/batch-record-execution-archive/download?id=${id}`
  })
  const fileName = resolveArchiveDownloadName(id, fallbackFileName, artifactType)
  downloadByData(blob, fileName, contentType || blob.type || 'application/octet-stream')
}

export const isEdhrExecutionArchiveNotExistsMessage = (message: string) => {
  return message.trim() === EDHR_EXECUTION_ARCHIVE_NOT_EXISTS_MESSAGE
}
