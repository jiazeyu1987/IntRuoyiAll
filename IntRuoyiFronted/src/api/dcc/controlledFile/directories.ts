import request from '@/config/axios'

export interface ControlledFileDirectoryVO {
  id?: number
  parentId?: number | null
  code: string
  name: string
  active: boolean
  sort: number
  hasChildren?: boolean
  directoryPath?: string
  remark?: string
  createTime?: number
  children?: ControlledFileDirectoryVO[]
}

export interface ControlledFileDirectoryAccessRuleVO {
  id?: number
  directoryId: number
  subjectType: string
  subjectId: number
  canQuery: boolean
  canPreview: boolean
  canDownload: boolean
  active: boolean
  changeReason?: string
}

export interface ControlledFileDirectoryAccessRuleDirectoryVO {
  id: number
  name: string
  directoryPath: string
}

export interface ControlledFileDirectoryImportRespVO {
  importedCount: number
  rootCount: number
}

export interface ControlledFileDirectoryDeleteSubtreeRespVO {
  directoryCount: number
  controlledFileCount: number
  masterCount: number
  infraFileCount: number
}

export interface ControlledFileDirectoryActiveNasTransferRespVO {
  active: boolean
  taskId?: number
  status?: string
  selectedNasPaths: string[]
  remainingPendingCount: number
  lastFailureMessage?: string
}

export const getDirectoryTree = async (): Promise<ControlledFileDirectoryVO[]> => {
  return await request.get({ url: '/dcc/directories/tree' })
}

export const getDirectoryChildren = async (
  parentId?: number | null
): Promise<ControlledFileDirectoryVO[]> => {
  return await request.get({
    url: '/dcc/directories/children',
    params: parentId === undefined || parentId === null ? undefined : { parentId }
  })
}

export const searchDirectories = async (
  keyword: string,
  limit = 50
): Promise<ControlledFileDirectoryVO[]> => {
  return await request.get({
    url: '/dcc/directories/search',
    params: { keyword, limit }
  })
}

export const getDirectory = async (id: number): Promise<ControlledFileDirectoryVO> => {
  return await request.get({ url: `/dcc/directories/${id}` })
}

export const createDirectory = async (data: ControlledFileDirectoryVO) => {
  return await request.post({ url: '/dcc/directories', data })
}

export const importDirectoriesFromIntAuth = async (): Promise<ControlledFileDirectoryImportRespVO> => {
  return await request.post({ url: '/dcc/directories/import-intauth' })
}

export const updateDirectory = async (id: number, data: ControlledFileDirectoryVO) => {
  return await request.put({ url: `/dcc/directories/${id}`, data })
}

export const deleteDirectorySubtree = async (
  id: number,
  data: { confirmText: string }
): Promise<ControlledFileDirectoryDeleteSubtreeRespVO> => {
  return await request.post({ url: `/dcc/directories/${id}/delete-subtree`, data })
}

export const getDirectoryActiveNasTransfer = async (
  id: number
): Promise<ControlledFileDirectoryActiveNasTransferRespVO> => {
  return await request.get({ url: `/dcc/directories/${id}/active-nas-transfer` })
}

export const stopDirectoryActiveNasTransfer = async (
  id: number
): Promise<ControlledFileDirectoryActiveNasTransferRespVO> => {
  return await request.post({ url: `/dcc/directories/${id}/active-nas-transfer/stop` })
}

export const getDirectoryAccessRules = async (
  id: number
): Promise<ControlledFileDirectoryAccessRuleVO[]> => {
  return await request.get({ url: `/dcc/directories/${id}/access-rules` })
}

export const getAccessRuleDirectories = async (): Promise<
  ControlledFileDirectoryAccessRuleDirectoryVO[]
> => {
  return await request.get({ url: '/dcc/directories/access-rule-directories' })
}

export const deleteDirectoryAccessRules = async (id: number) => {
  return await request.delete({ url: `/dcc/directories/${id}/access-rules` })
}

export const saveDirectoryAccessRules = async (
  id: number,
  data: ControlledFileDirectoryAccessRuleVO[]
) => {
  return await request.put({ url: `/dcc/directories/${id}/access-rules`, data })
}
