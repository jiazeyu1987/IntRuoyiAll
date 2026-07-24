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
