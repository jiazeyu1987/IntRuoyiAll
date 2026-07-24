import request from '@/config/axios'

export interface ConfigPackageSheetDiffVO {
  sheetName: string
  packageCount: number
  currentCount: number
  createCount: number
  updateCount: number
  deleteCount: number
}

export interface ConfigPackagePrecheckRespVO {
  valid: boolean
  packageSha256: string
  targetSnapshotSha256: string
  blockingErrors: string[]
  warnings: string[]
  sheetDiffs: ConfigPackageSheetDiffVO[]
}

export interface ConfigPackageImportRespVO {
  restored: boolean
  targetSnapshotSha256: string
  restoredCounts: Record<string, number>
}

interface ApiResponse<T> {
  data: T
}

const unwrapUploadData = <T>(response: ApiResponse<T>): T => {
  if (!response?.data) {
    throw new Error('配置包上传响应缺少 data')
  }
  return response.data
}

export const exportConfigPackage = () => {
  return request.download({
    url: '/system/config-package/export-excel'
  })
}

export const precheckConfigPackage = (file: File, availableComponents: string[]) => {
  const data = new FormData()
  data.append('file', file)
  data.append('availableComponents', availableComponents.join(','))
  return request.upload<ApiResponse<ConfigPackagePrecheckRespVO>>({
    url: '/system/config-package/precheck',
    data
  }).then(unwrapUploadData)
}

export const importConfigPackage = (
  file: File,
  availableComponents: string[],
  targetSnapshotSha256: string
) => {
  const data = new FormData()
  data.append('file', file)
  data.append('availableComponents', availableComponents.join(','))
  data.append('confirmed', 'true')
  data.append('targetSnapshotSha256', targetSnapshotSha256)
  return request.upload<ApiResponse<ConfigPackageImportRespVO>>({
    url: '/system/config-package/import',
    data
  }).then(unwrapUploadData)
}
