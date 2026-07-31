import request from '@/config/axios'

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

export const downloadNasControlAuditReport = async (taskId: number) => {
  return await request.download<Blob>({
    url: `/dcc/controlled-files/nas-control-audit/${taskId}/download`
  })
}
