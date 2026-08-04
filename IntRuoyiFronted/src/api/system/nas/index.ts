import request from '@/config/axios'
import type { ControlledFileNasTransferRespVO } from '@/api/dcc/controlledFile/workflow'

export interface NasConfigVO {
  server: string
  port?: number
  share: string
  domain?: string
  username: string
  password: string
}

export interface NasConfigTestRespVO {
  rootPath: string
  itemCount: number
  message: string
}

export interface NasFileItemVO {
  name: string
  path: string
  dir: boolean
  size: number
  modifiedAt?: number
}

export interface NasDirectoryTreeSkippedVO {
  path: string
  reason: string
}

export interface NasFileListRespVO {
  currentPath: string
  parentPath?: string
  rootPath: string
  items: NasFileItemVO[]
}

export interface NasControlAuditTaskRespVO {
  taskId: number
  status: string
  nasShareName?: string
  scanRoots: string[]
  currentPath?: string
  scannedFileCount: number
  controlledFileCount: number
  notControlledFileCount: number
  ambiguousFileCount: number
  sourceMissingCount: number
  skippedDirectoryCount: number
  unscannedFileCountLabel: string
  reportFileName?: string
  startedAt?: string
  completedAt?: string
  failureReason?: string
}

export interface DccNasControlAuditFilePageReqVO extends PageParam {
  keyword?: string
  classificationStatus?: string
  downloadStatus?: string
  archiveStatus?: string
}

export interface DccNasControlAuditFileRespVO {
  auditFileId: number
  taskId: number
  nasShareName?: string
  rootPath?: string
  normalizedRelativePath: string
  pathHash?: string
  fileName: string
  fileSize?: number | null
  modifiedAt?: number
  sourceSignature: string
  controlStatus?: string
  classificationStatus: 'MATCHED' | 'UNCLASSIFIED_PENDING' | 'AMBIGUOUS' | string
  matchedProjectCodeId?: number | null
  matchedFileTypeTaxonomyId?: number | null
  matchedFileTypeLevel1?: string | null
  matchedFileTypeLevel2?: string | null
  matchedFileTypeLevel3?: string | null
  matchedFileTypeLevel4?: string | null
  matchedFileTypeLevel5?: string | null
  classificationReason?: string | null
  classificationCandidatesJson?: string | null
  expectedLocalRelativePath?: string | null
  downloadStatus?: string | null
  archiveStatus?: string | null
  localRelativePath?: string | null
  localWriteErrorCode?: string | null
  localWriteError?: string | null
  archiveErrorCode?: string | null
  archiveError?: string | null
  controlledFileId?: number | null
}

export interface DccNasControlAuditRecognizeRespVO {
  matchedCount: number
  unclassifiedPendingCount: number
  ambiguousCount: number
  skippedCount: number
}

export interface DccNasUncontrolledImportSelectedReqVO {
  selectionScope: string
  idempotencyKey: string
  selectedFiles: Array<{
    auditFileId: number
    sourceSignature: string
    localRelativePath: string
  }>
}

export interface DccNasUncontrolledImportLocalWriteResultReqVO {
  sourceSignature: string
  localRelativePath: string
  localWriteStatus: 'LOCAL_WRITTEN' | 'LOCAL_WRITE_FAILED'
  localWriteErrorCode?: string
  localWriteError?: string
}

export const getNasConfig = async () => {
  return await request.get<NasConfigVO>({ url: '/infra/file/nas-config' })
}

export const saveNasConfig = async (data: NasConfigVO) => {
  return await request.put({ url: '/infra/file/nas-config', data })
}

export const testNasConfig = async (data: NasConfigVO) => {
  return await request.post<NasConfigTestRespVO>({ url: '/infra/file/nas-config/test', data })
}

export const listNasFiles = async (path = '') => {
  return await request.get<NasFileListRespVO>({ url: '/infra/file/nas-files', params: { path } })
}

export const startNasControlAudit = async () => {
  return await request.post<NasControlAuditTaskRespVO>({
    url: '/dcc/controlled-files/nas-control-audit/start'
  })
}

export const getNasControlAuditTask = async (taskId: number) => {
  return await request.get<NasControlAuditTaskRespVO>({
    url: `/dcc/controlled-files/nas-control-audit/${taskId}`
  })
}

export const getNasControlAuditFiles = async (
  taskId: number,
  params: DccNasControlAuditFilePageReqVO
): Promise<PageResult<DccNasControlAuditFileRespVO[]>> => {
  return await request.get({
    url: `/dcc/controlled-files/nas-control-audit/${taskId}/files`,
    params
  })
}

export const recognizeNasControlAuditFiles = async (
  taskId: number
): Promise<DccNasControlAuditRecognizeRespVO> => {
  return await request.post({
    url: `/dcc/controlled-files/nas-control-audit/${taskId}/files/recognize`
  })
}

export const importSelectedNasUncontrolledFiles = async (
  taskId: number,
  data: DccNasUncontrolledImportSelectedReqVO
): Promise<ControlledFileNasTransferRespVO> => {
  return await request.post({
    url: `/dcc/controlled-files/nas-control-audit/${taskId}/import-selected`,
    data
  })
}

export const downloadNasUncontrolledImportContent = async (
  importTaskId: number,
  auditFileId: number,
  sourceSignature: string,
  localRelativePath: string
) => {
  return await request.download<Blob>({
    url: `/dcc/controlled-files/nas-uncontrolled-import/tasks/${importTaskId}/files/${auditFileId}/content`,
    params: { sourceSignature, localRelativePath }
  })
}

export const recordNasUncontrolledImportLocalWriteResult = async (
  importTaskId: number,
  auditFileId: number,
  data: DccNasUncontrolledImportLocalWriteResultReqVO
): Promise<ControlledFileNasTransferRespVO> => {
  return await request.post({
    url: `/dcc/controlled-files/nas-uncontrolled-import/tasks/${importTaskId}/files/${auditFileId}/local-write-result`,
    data: {
      sourceSignature: data.sourceSignature,
      localRelativePath: data.localRelativePath,
      localWriteStatus: data.localWriteStatus,
      localWriteErrorCode: data.localWriteErrorCode,
      localWriteError: data.localWriteError
    }
  })
}

export const downloadNasControlAuditReport = async (taskId: number) => {
  return await request.download<Blob>({
    url: `/dcc/controlled-files/nas-control-audit/${taskId}/download`
  })
}
