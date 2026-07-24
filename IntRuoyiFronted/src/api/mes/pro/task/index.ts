import request from '@/config/axios'

// MES 生产任务 VO
export interface ProTaskVO {
  id: number
  code: string
  name: string
  workOrderId: number
  workOrderCode: string
  workOrderName: string
  workstationId: number
  workstationCode: string
  workstationName: string
  routeId: number
  processId: number
  processName: string
  itemId: number
  itemName: string
  itemCode: string
  itemSpecification: string
  quantity: number
  producedQuantity: number
  qualifyQuantity: number
  unqualifyQuantity: number
  changedQuantity: number
  clientId: number
  clientName: string
  startTime: Date
  duration: number
  endTime: Date | number
  colorCode: string
  requestDate: Date
  finishDate: Date
  cancelDate: Date
  status: number
  checkFlag: boolean
  remark: string
}

export interface ProTaskLockReqVO {
  taskId: number
  lockedReason: string
}

// MES 生产任务 API
export const ProTaskApi = {
  getTaskPage: async (params: any) => {
    return await request.get({ url: `/mes/pro/task/page`, params })
  },

  getTask: async (id: number) => {
    return await request.get({ url: `/mes/pro/task/get?id=` + id })
  },

  createTask: async (data: ProTaskVO) => {
    return await request.post({ url: `/mes/pro/task/create`, data })
  },

  updateTask: async (data: ProTaskVO) => {
    return await request.put({ url: `/mes/pro/task/update`, data })
  },

  lockTask: async (data: ProTaskLockReqVO) => {
    return await request.put({ url: `/mes/pro/task/lock`, data })
  },

  unlockTask: async (taskId: number) => {
    return await request.put({ url: `/mes/pro/task/unlock?taskId=` + taskId })
  },

  deleteTask: async (id: number) => {
    return await request.delete({ url: `/mes/pro/task/delete?id=` + id })
  },

  exportTask: async (params: any) => {
    return await request.download({ url: `/mes/pro/task/export-excel`, params })
  },

  getGanttTaskList: async (params: any) => {
    return await request.get({ url: `/mes/pro/task/gantt-list`, params })
  }
}
