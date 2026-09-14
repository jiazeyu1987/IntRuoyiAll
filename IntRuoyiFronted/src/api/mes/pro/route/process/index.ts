import request from '@/config/axios'

// MES 工艺路线工序设备 VO
export interface ProRouteProcessMachineryVO {
  workstationMachineId: number // 工作站设备绑定编号
  workstationId: number // 工作站编号
  workstationCode?: string // 工作站编码
  workstationName?: string // 工作站名称
  machineryId: number // 设备编号
  machineryCode?: string // 设备编码
  machineryName?: string // 设备名称
  quantity: number // 设备数量
  machineryStandardHourlyCapacity?: number // 设备工序单台标准小时产能
  machineryHourlyCapacityTotal?: number // 设备工序总标准小时产能
  availableQuantity?: number // 今日可用设备数量
  availableHourlyCapacityTotal?: number // 今日可用设备小时产能
  availableShiftCapacityTotal?: number // 今日可用设备班次产能
  underRepair?: boolean // 是否维修中
  availabilityStatus?: 'NORMAL' | 'REPAIR' // 今日可用状态
  availabilityReason?: string // 今日可用原因
}

export interface ProRouteProcessRelationVO {
  routeProcessId: number
  processId: number
  processCode?: string
  processName?: string
}

export interface ProRouteBaseProcessVO {
  id?: number
  routeId: number
  processId: number
  processCode?: string
  processName?: string
  sort: number
  predecessor?: ProRouteProcessRelationVO
  predecessors?: ProRouteProcessRelationVO[]
  successors: ProRouteProcessRelationVO[]
  prepareTime?: number
  waitTime?: number
  colorCode?: string
  keyFlag?: boolean
  checkFlag?: boolean
  remark?: string
  createTime?: Date
}

// MES 工艺路线工序 VO
export interface ProRouteProcessVO {
  id?: number // 编号
  routeId: number // 工艺路线编号
  processId: number // 工序编号
  processCode?: string // 工序编码
  processName?: string // 工序名称
  processProductName?: string // 工序所属产品名称
  processAttention?: string // 工艺要求
  processStatus?: number // 工序状态
  processManualShiftCapacity?: number // 工序人工班次产能
  sort: number // 序号
  predecessor?: ProRouteProcessRelationVO // 直接前置工序
  predecessors?: ProRouteProcessRelationVO[] // 直接前置工序列表
  successors: ProRouteProcessRelationVO[] // 直接后续工序
  routeProcessWorkstationId?: number // 路线工序显式绑定工作站 ID
  workstationId?: number // 工作站 ID
  workstationCode?: string // 工作站编码
  workstationName?: string // 工作站名称
  machineryQuantityTotal?: number // 设备数量合计
  machineryList?: ProRouteProcessMachineryVO[] // 设备列表
  workerQuantityTotal?: number // 人工人数合计
  workstationWorkerId?: number // 工作站人员绑定编号
  processHourlyCapacityTotal?: number // 工序总标准小时产能
  processShiftCapacityTotal?: number // 工序总标准班次产能
  capacitySource?: 'MACHINE' | 'WORKER' | 'UNCONFIGURED' // 产能来源
  shiftHours?: number // 班次小时数
  todayAvailableResourceQuantityTotal?: number // 今日可用资源数量合计
  todayHourlyCapacityTotal?: number // 今日总小时产能
  todayShiftCapacityTotal?: number // 今日总班次产能
  resourceStatus?: 'NORMAL' | 'REPAIR' | 'CAPACITY_MISSING' | 'UNCONFIGURED' // 资源状态
  resourceStatusReason?: string // 资源状态原因
  workerSingleStandardHourlyCapacity?: number // 人工单人标准小时产能
  batchRecordReportId?: string // 默认批记录报表编号
  batchRecordReportCode?: string // 默认批记录报表编码
  batchRecordReportName?: string // 默认批记录报表名称
  prepareTime?: number // 准备时间（分钟）
  waitTime?: number // 等待时间（分钟）
  colorCode?: string // 甘特图显示颜色
  keyFlag?: boolean // 是否关键工序
  checkFlag?: boolean // 是否质检工序
  remark?: string // 备注
  createTime?: Date // 创建时间
}

// MES 工艺路线工序 API
export const ProRouteProcessApi = {
  getRouteBaseProcessListByRoute: async (routeId: number) => {
    return await request.get({ url: `/mes/pro/route-process/list-base-by-route?routeId=` + routeId })
  },

  getRouteProcessListByRoute: async (routeId: number) => {
    return await request.get({ url: `/mes/pro/route-process/list-by-route?routeId=` + routeId })
  },

  getRouteProcessListByProduct: async (productId: number) => {
    return await request.get({
      url: `/mes/pro/route-process/list-by-product?productId=` + productId
    })
  },

  getRouteProcess: async (id: number) => {
    return await request.get({ url: `/mes/pro/route-process/get?id=` + id })
  },

  getRouteProcessByRouteAndProcess: async (routeId: number, processId: number) => {
    return await request.get({
      url: `/mes/pro/route-process/get-by-route-and-process`,
      params: { routeId, processId }
    })
  },

  createRouteProcess: async (data: ProRouteProcessVO) => {
    return await request.post({ url: `/mes/pro/route-process/create`, data })
  },

  updateRouteProcess: async (data: ProRouteProcessVO) => {
    return await request.put({ url: `/mes/pro/route-process/update`, data })
  },

  deleteRouteProcess: async (id: number) => {
    return await request.delete({ url: `/mes/pro/route-process/delete?id=` + id })
  }
}
