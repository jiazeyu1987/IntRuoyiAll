import request from '@/config/axios'

export interface ProfileWorkbenchTaskVisibilitySaveReqVO {
  taskKey: string
  taskType: string
  source: string
  businessId?: string
  detail?: string
}

export const getProfileWorkbenchHiddenTaskKeys = () => {
  return request.get<string[]>({
    url: '/system/profile-workbench-task-visibility/hidden-keys'
  })
}

export const hideProfileWorkbenchTask = (data: ProfileWorkbenchTaskVisibilitySaveReqVO) => {
  return request.put<boolean>({
    url: '/system/profile-workbench-task-visibility/hide',
    data
  })
}

export const restoreProfileWorkbenchTask = (taskKey: string) => {
  return request.delete<boolean>({
    url: '/system/profile-workbench-task-visibility/restore',
    params: { taskKey }
  })
}
