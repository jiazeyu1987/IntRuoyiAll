import request from '@/config/axios'

export interface ControlledImportProject {
  id: number | string
  projectName: string
  projectCode: string
}

export interface ControlledImportOptions {
  projects: ControlledImportProject[]
  folders: Array<{ id: number | string; parentId: number | string; name: string }>
  fileTypes: Array<{ categoryId: number | string; fileTypeTaxonomyId: number | string; label: string; uploadBlocker?: string | null }>
  departments: Array<{ id: number | string; name: string }>
}

export interface ControlledImportResult {
  id: number | string
  importSource: 'CONTROLLED_IMPORT'
  importedBy: number | string
  importedTime: string
  controlledTime: string
  effectiveTime: string
  fileName: string
  versionNo: string
  status: string
}

export const getControlledImportOptions = async (
  dccProjectCodeId?: number | string
): Promise<ControlledImportOptions> => {
  const value = await request.get<ControlledImportOptions>({
    url: '/dcc/controlled-file-imports/options',
    params: dccProjectCodeId == null ? undefined : { dccProjectCodeId }
  })
  if (!value || !Array.isArray(value.projects) || !Array.isArray(value.folders) ||
      !Array.isArray(value.fileTypes) || !Array.isArray(value.departments)) {
    throw new Error('已受控文件上传候选响应格式无效')
  }
  return value
}

export const importControlledFile = async (data: FormData): Promise<ControlledImportResult> => {
  const result = await request.upload<{ data: ControlledImportResult }>({
    // Synchronous OnlyOffice conversion and PDF download each have a five-minute server bound.
    url: '/dcc/controlled-file-imports', data, timeout: 2 * 300_000
  })
  if (!result?.data?.id || result.data.importSource !== 'CONTROLLED_IMPORT') {
    throw new Error('已受控文件上传结果缺少正式文件身份或上传来源，请核对原提交记录')
  }
  return result.data
}

export const getControlledImportLibrary = async (scope: 'QMS' | 'GENERAL'): Promise<ControlledImportProject> => {
  const value = await request.get({ url: '/dcc/project-codes/library', params: { scope } })
  if (!value?.id || value.libraryScope !== scope || !value.canEdit || value.status !== 'ENABLE') {
    throw new Error('当前文件库不可上传或没有编辑授权')
  }
  return value
}
