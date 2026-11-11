import request from '@/config/axios'

export interface ApiAccessLogVO {
  id: number
  traceId: string
  userId: number
  userType: number
  applicationName: string
  requestMethod: string
  requestParams: string
  responseBody: string
  requestUrl: string
  userIp: string
  userAgent: string
  operateModule: string
  operateName: string
  operateType: number
  beginTime: Date
  endTime: Date
  duration: number
  resultCode: number
  resultMsg: string
  createTime: Date
}

// 查询列表API 访问日志
export const getApiAccessLogPage = (params: PageParam, isRequestContextCurrent?: () => boolean) => {
  return request.get({ url: '/infra/api-access-log/page', params, isRequestContextCurrent })
}

export const getApiAccessLog = (id: number, isRequestContextCurrent?: () => boolean) => {
  return request.get<ApiAccessLogVO>({ url: '/infra/api-access-log/get', params: { id }, isRequestContextCurrent })
}

// 导出API 访问日志
export const exportApiAccessLog = (params) => {
  return request.download({ url: '/infra/api-access-log/export-excel', params })
}
