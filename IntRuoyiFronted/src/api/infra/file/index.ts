import request from '@/config/axios'

// 文件预签名地址 Response VO
export interface FilePresignedUrlRespVO {
  // 文件配置编号
  configId: number
  // 文件上传 URL
  uploadUrl: string
  // 文件 URL
  url: string
  // 文件路径
  path: string
}

// 查询文件列表
export const getFilePage = (params: PageParam, isRequestContextCurrent?: () => boolean) => {
  return request.get({ url: '/infra/file/page', params, isRequestContextCurrent })
}

// Read through the authenticated tenant API; storage URLs are not authorization.
export const readFileContent = (
  file: { configId: number; path: string },
  isRequestContextCurrent?: () => boolean
) => {
  const path = file.path.split('/').map(encodeURIComponent).join('/')
  return request.download<Blob>({ url: `/infra/file/${file.configId}/get/${path}`, isRequestContextCurrent })
}

// 删除文件
export const deleteFile = (id: number, isRequestContextCurrent?: () => boolean) => {
  return request.delete({ url: '/infra/file/delete?id=' + id, isRequestContextCurrent })
}

// 批量删除文件
export const deleteFileList = (ids: number[], isRequestContextCurrent?: () => boolean) => {
  return request.delete({ url: '/infra/file/delete-list', params: { ids: ids.join(',') }, isRequestContextCurrent })
}

// 获取文件预签名地址
export const getFilePresignedUrl = (
  name: string,
  directory?: string,
  isRequestContextCurrent?: () => boolean
) => {
  return request.get<FilePresignedUrlRespVO>({
    url: '/infra/file/presigned-url',
    params: { name, directory },
    isRequestContextCurrent
  })
}

// 创建文件
export const createFile = (data: any, isRequestContextCurrent?: () => boolean) => {
  return request.post({ url: '/infra/file/create', data, isRequestContextCurrent })
}

// 上传文件
export const updateFile = (
  data: any,
  onUploadProgress?: Function,
  isRequestContextCurrent?: () => boolean
) => {
  return request.upload({ url: '/infra/file/upload', data, onUploadProgress, isRequestContextCurrent })
}
