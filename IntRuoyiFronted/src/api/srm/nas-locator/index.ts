import axios, { AxiosError } from 'axios'
import request from '@/config/axios'
import { config as axiosConfig } from '@/config/axios/config'
import { getAccessToken, getTenantId, getVisitTenantId } from '@/utils/auth'
import { downloadByData } from '@/utils/filt'

export interface SrmNasLocatorStatusRespVO {
  scopeShare: string
  rootPath: string
  latestTaskStatus: string
  latestSuccessTime?: number
  fileCount: number
  directoryCount: number
  message?: string
  runningShare?: string
  runningPath?: string
  runningDirectoryCount?: number
  runningFileCount?: number
  runningShareIndex?: number
  runningShareTotal?: number
}

export interface SrmNasLocatorPageReqVO extends PageParam {
  keyword?: string
}

export interface SrmNasLocatorBlacklistRespVO {
  patterns: string[]
}

export interface SrmNasLocatorBlacklistSaveReqVO {
  patterns: string[]
}

export interface SrmNasLocatorFileRespVO {
  id: number
  fileName: string
  directoryPath: string
  fullPath: string
  size?: number
  modifiedAt?: number
}

export interface SrmNasLocatorDownloadResult {
  blob: Blob
  fileName: string
}

const buildNasLocatorDownloadHeaders = () => {
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
  const tryDecode = (fileName: string) => {
    try {
      return decodeURIComponent(fileName)
    } catch {
      return fileName
    }
  }
  const utf8Match = contentDisposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return tryDecode(utf8Match[1].trim().replace(/^"(.*)"$/, '$1'))
  }
  const plainMatch = contentDisposition.match(/filename\s*=\s*("?)([^";]+)\1/i)
  return plainMatch?.[2] ? tryDecode(plainMatch[2].trim()) : null
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

const resolveBlobMessage = (payload: Record<string, unknown> | null, defaultMessage: string) => {
  const message = typeof payload?.message === 'string' ? payload.message.trim() : ''
  const msg = typeof payload?.msg === 'string' ? payload.msg.trim() : ''
  return message || msg || defaultMessage
}

const toNasLocatorDownloadError = async (error: unknown) => {
  if (error instanceof AxiosError) {
    const data = error.response?.data
    if (data instanceof Blob) {
      return new Error(resolveBlobMessage(await parseJsonBlob(data), 'NAS 文件下载失败，请稍后重试。'))
    }
    if (data && typeof data === 'object') {
      return new Error(resolveBlobMessage(data as Record<string, unknown>, 'NAS 文件下载失败，请稍后重试。'))
    }
    return new Error(error.message || 'NAS 文件下载失败，请稍后重试。')
  }
  if (error instanceof Error) {
    return error
  }
  return new Error('NAS 文件下载失败，请稍后重试。')
}

export const getNasLocatorStatus = async (): Promise<SrmNasLocatorStatusRespVO> => {
  return await request.get({ url: '/srm/nas-locator/status' })
}

export const getNasLocatorBlacklist = async (): Promise<SrmNasLocatorBlacklistRespVO> => {
  return await request.get({ url: '/srm/nas-locator/blacklist' })
}

export const saveNasLocatorBlacklist = async (
  data: SrmNasLocatorBlacklistSaveReqVO
): Promise<boolean> => {
  return await request.put({ url: '/srm/nas-locator/blacklist', data })
}

export const getNasLocatorPage = async (
  params: SrmNasLocatorPageReqVO
): Promise<PageResult<SrmNasLocatorFileRespVO[]>> => {
  return await request.get({ url: '/srm/nas-locator/page', params })
}

export const refreshNasLocator = async (): Promise<boolean> => {
  return await request.post({ url: '/srm/nas-locator/refresh' })
}

export const downloadNasLocatorFile = async (
  id: number | string
): Promise<SrmNasLocatorDownloadResult> => {
  try {
    const response = await axios.get<Blob>(`${axiosConfig.base_url}/srm/nas-locator/download`, {
      headers: buildNasLocatorDownloadHeaders(),
      params: { id },
      timeout: axiosConfig.request_timeout,
      responseType: 'blob'
    })
    const fileName = decodeContentDispositionFileName(
      response.headers?.['content-disposition'] || response.headers?.['Content-Disposition']
    )
    if (!fileName) {
      throw new Error('NAS 定位下载响应缺少文件名')
    }
    const blob = response.data
    const payload = await parseJsonBlob(blob)
    if (payload && typeof payload.code !== 'undefined' && payload.code !== 0 && payload.code !== 200) {
      throw new Error(resolveBlobMessage(payload, 'NAS 文件下载失败，请稍后重试。'))
    }
    downloadByData(blob, fileName, blob.type || 'application/octet-stream')
    return { blob, fileName }
  } catch (error) {
    throw await toNasLocatorDownloadError(error)
  }
}
