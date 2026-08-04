import request from '@/config/axios'

export interface DistributionTaskVO {
  recipientId: number
  distributionId: number
  controlledFileId: number
  categoryId: number
  fileName?: string
  title?: string
  fileNumber?: string
  versionNo?: string
  fileStatus: string
  userId: number
  departmentId?: number
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
  params: DistributionTaskPageReqVO
): Promise<PageResult<DistributionTaskVO[]>> => {
  return await request.get({ url: '/dcc/distribution-tasks/my-page', params })
}
