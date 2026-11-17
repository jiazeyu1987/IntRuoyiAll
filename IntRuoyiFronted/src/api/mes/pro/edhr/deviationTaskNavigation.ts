import request from '@/config/axios'
import type { EdhrWorkTaskRouteLike } from '@/utils/edhrWorkTaskNavigation'

export const getDeviationTaskNavigation = (workTaskId: string) =>
  request.get<EdhrWorkTaskRouteLike>({
    url: '/mes/pro/edhr-deviation-review/task-navigation', params: { workTaskId }
  })
