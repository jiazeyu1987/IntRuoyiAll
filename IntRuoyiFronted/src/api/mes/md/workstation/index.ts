import request from '@/config/axios'

// MES 工作站 VO
export interface MdWorkstationVO {
  id: number // 工作站编号
  code: string // 工作站编码
  name: string // 工作站名称
  address: string // 工作站地点
  workshopId: number // 所在车间 ID
  workshopName: string // 所在车间名称
  processId: number // 工序 ID
  processName: string // 工序名称
  machinerySummary?: string // 绑定设备摘要
  machineryCount?: number // 绑定设备个数
  warehouseId: number // 线边库 ID
  locationId: number // 库区 ID
  areaId: number // 库位 ID
  singleStandardHourlyCapacity?: number // 单人标准小时产能
  shiftHours?: number // 班次小时数
  configuredWorkerCount?: number // 理论配置人数
  currentWorkerCount?: number // 当前在岗人数
  machineryStandardHourlyCapacity?: number // 设备标准小时产能
  todayCapacity?: number // 班次产能
  status: number // 状态
  remark: string // 备注
}

const normalizePositiveIdParam = (value: unknown) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const buildWorkstationPageParams = (params: any = {}) => ({
  ...params,
  processId: normalizePositiveIdParam(params?.processId)
})

// MES 工作站 API
export const MdWorkstationApi = {
  // 查询工作站分页
  getWorkstationPage: async (params: any) => {
    return await request.get({ url: `/mes/md-workstation/page`, params: buildWorkstationPageParams(params) })
  },

  // 查询工作站详情
  getWorkstation: async (id: number) => {
    return await request.get({ url: `/mes/md-workstation/get?id=` + id })
  },

  // 新增工作站
  createWorkstation: async (data: MdWorkstationVO) => {
    return await request.post({ url: `/mes/md-workstation/create`, data })
  },

  // 修改工作站
  updateWorkstation: async (data: MdWorkstationVO) => {
    return await request.put({ url: `/mes/md-workstation/update`, data })
  },

  // 删除工作站
  deleteWorkstation: async (id: number) => {
    return await request.delete({ url: `/mes/md-workstation/delete?id=` + id })
  },

  // 导出工作站 Excel
  exportWorkstation: async (params: any) => {
    return await request.download({ url: `/mes/md-workstation/export-excel`, params })
  }
}
