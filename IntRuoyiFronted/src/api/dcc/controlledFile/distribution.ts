import request from '@/config/axios'

interface DccDistributionTaskRequestOptions {
  ignoreErrorMessage?: boolean
}

export interface DistributionTaskVO {
  recipientId: number | string
  distributionId: number | string
  controlledFileId: number | string
  categoryId: number | string
  fileName?: string
  title?: string
  fileNumber?: string
  versionNo?: string
  fileStatus: string
  userId: number | string
  departmentId?: number | string
  distributionMedium: string
  readAt?: number
  acknowledgedAt?: number
  publishedTime?: number
  status: 'READY_TO_ACKNOWLEDGE'
}

export interface DistributionTaskPageReqVO extends PageParam {
  categoryId?: number
  status?: string
}

export const getMyDistributionTaskPage = async (
  params: DistributionTaskPageReqVO,
  options: DccDistributionTaskRequestOptions = {}
): Promise<PageResult<DistributionTaskVO[]>> => {
  return await request.get({ url: '/dcc/distribution-tasks/my-page', params, ...options })
}
