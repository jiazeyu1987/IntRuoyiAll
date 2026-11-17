import request from '@/config/axios'
import type { JobVO } from '@/api/infra/job'
export type { JobVO } from '@/api/infra/job'

export const getJobPage = (params: PageParam) =>
  request.get({ url: '/erp/kingdee-config/job/page', params })
export const updateJob = (data: JobVO) =>
  request.put({ url: '/erp/kingdee-config/job/update', data: { id: data.id, cronExpression: data.cronExpression } })
export const updateJobStatus = (id: number, status: number) =>
  request.put({ url: '/erp/kingdee-config/job/update-status', params: { id, status } })
