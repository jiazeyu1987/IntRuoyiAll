import request from '@/config/axios'

export type VersionCenterTargetType = 'COMPANY' | 'PRODUCT'
export type VersionCenterReleaseStage = 'TEST' | 'PROD'

interface VersionCenterCommonResult<T> {
  code?: number | string
  msg?: string
  message?: string
  data?: T
  details?: unknown
}

export class VersionCenterApiError extends Error {
  code: number | string | null
  details: unknown

  constructor(message: string, code: number | string | null, details: unknown) {
    super(message)
    this.name = 'VersionCenterApiError'
    this.code = code
    this.details = details
  }
}

export interface VersionCenterReleaseScope {
  siteKey: string
  stage: VersionCenterReleaseStage
}

export interface VersionCenterHistoryQuery extends VersionCenterReleaseScope, Record<string, unknown> {
  targetType: VersionCenterTargetType
  targetId: number
}

export interface VersionCenterDetailQuery extends VersionCenterHistoryQuery {
  revisionId: number
}

export interface VersionCenterRepublishReqVO extends VersionCenterHistoryQuery {
  sourceRevisionId: number
}

const resolveVersionCenterError = (payload: unknown, defaultMessage: string) => {
  const result = (payload || {}) as VersionCenterCommonResult<unknown>
  const code = result.code ?? null
  const message =
    (typeof result.msg === 'string' && result.msg) ||
    (typeof result.message === 'string' && result.message) ||
    defaultMessage
  const details = result.details ?? result.data ?? null
  return new VersionCenterApiError(message, code, details)
}

const assertVersionCenterReleaseScope = (scope: VersionCenterReleaseScope) => {
  if (!scope.siteKey || !scope.siteKey.trim() || (scope.stage !== 'TEST' && scope.stage !== 'PROD')) {
    throw new VersionCenterApiError('版本中心缺少 scope：siteKey/stage', null, {
      siteKey: scope.siteKey,
      stage: scope.stage
    })
  }
}

const requestVersionCenter = async <T>(option: {
  method: 'GET' | 'POST'
  url: string
  params?: Record<string, unknown>
  data?: unknown
}) => {
  try {
    if (option.method === 'GET') {
      return (await request.get<T>({
        url: option.url,
        params: option.params,
        headers: {
          'Cache-Control': 'no-cache',
          Pragma: 'no-cache'
        }
      })) as T
    }
    return (await request.post<T>({
      url: option.url,
      data: option.data,
      headers: {
        'Cache-Control': 'no-cache',
        Pragma: 'no-cache'
      }
    })) as T
  } catch (error) {
    if (error instanceof VersionCenterApiError) {
      throw error
    }
    if (error && typeof error === 'object') {
      const errorRecord = error as Record<string, unknown>
      if (errorRecord.response && typeof errorRecord.response === 'object') {
        const responseRecord = errorRecord.response as Record<string, unknown>
        if (responseRecord.data) {
          throw resolveVersionCenterError(responseRecord.data, '版本中心请求失败')
        }
      }
      if (
        errorRecord.message ||
        errorRecord.code !== undefined ||
        errorRecord.msg ||
        errorRecord.details ||
        errorRecord.data
      ) {
        throw resolveVersionCenterError(errorRecord, '版本中心请求失败')
      }
    }
    throw new VersionCenterApiError(
      error instanceof Error ? error.message : '版本中心请求失败',
      null,
      null
    )
  }
}

export const getVersionCenterHistory = async (params: VersionCenterHistoryQuery) => {
  assertVersionCenterReleaseScope(params)
  return await requestVersionCenter({
    method: 'GET',
    url: '/showroom/version-center/history',
    params
  })
}

export const getVersionCenterDetail = async (params: VersionCenterDetailQuery) => {
  assertVersionCenterReleaseScope(params)
  return await requestVersionCenter({
    method: 'GET',
    url: '/showroom/version-center/detail',
    params
  })
}

export const republishVersionCenter = async (data: VersionCenterRepublishReqVO) => {
  assertVersionCenterReleaseScope(data)
  return await requestVersionCenter({
    method: 'POST',
    url: '/showroom/version-center/republish',
    data
  })
}
