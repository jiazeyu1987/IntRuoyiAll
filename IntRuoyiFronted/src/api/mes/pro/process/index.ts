import request from '@/config/axios'

export interface ProProcessRouteVO {
  id: number // 工艺路线编号
  routeProcessId?: number // 路线工序编号
  code: string // 工艺路线编码
  name: string // 工艺路线名称
  shiftCapacity?: number // 该路线工序排产班次产能
}

export interface ProProcessWorkstationVO {
  id: number // 工作站编号
  code: string // 工作站编码
  name: string // 工作站名称
}

export interface ProProcessBatchRecordFormLinkVO {
  reportId: string // 批记录报表编号
  reportName: string // 批记录报表名称
}

// MES 生产工序 VO
export interface ProProcessVO {
  id?: number // 编号
  productName?: string // 产品名称
  code: string // 工序编码
  name: string // 工序名称
  attention?: string // 工艺要求
  status: number // 状态
  manualShiftCapacity?: number // 人工班产能
  machineryQuantityTotal?: number // 关联设备台数
  availableShiftCapacityTotal?: number // 当前可用班产能
  capacitySource?: string // 产能来源
  productionQuantityFactor?: number // 生产系数
  shiftCapacity?: number // 路线工序班次产能
  routeList?: ProProcessRouteVO[] // 所属工艺路线
  routeCapacityConflict?: boolean // 多工艺路线排产产能是否不一致
  routeCapacityConflictMessage?: string // 多工艺路线排产产能不一致提示
  workstationNames?: string // 工作站
  workstations?: ProProcessWorkstationVO[] // 工作站列表
  batchRecordFormNames?: string // 批记录表单
  batchRecordForms?: ProProcessBatchRecordFormLinkVO[] // 批记录表单链接
  lossReportFormNames?: string // 损耗单
  lossReportForms?: ProProcessBatchRecordFormLinkVO[] // 损耗单链接
  processInspectionFormNames?: string // 过程检验单
  processInspectionForms?: ProProcessBatchRecordFormLinkVO[] // 过程检验单链接
  parameterRecordFormNames?: string // 参数记录表
  parameterRecordForms?: ProProcessBatchRecordFormLinkVO[] // 参数记录表链接
  remark?: string // 备注
  createTime?: Date // 创建时间
}

// MES 生产工序关联设备产能 VO
export interface ProProcessMachineryVO {
  machineryId: number // 设备编号
  machineryCode: string // 设备编码
  machineryName: string // 设备名称
  machineryStatus?: number // 设备状态
  shiftCapacity?: number // 单台班产能
  availableShiftCapacity?: number // 当前可用班产能
  underRepair?: boolean // 是否维修中或待验收
  availabilityStatus?: string // 可用状态
  availabilityReason?: string // 不可用原因
}

// MES 生产工序 API
export const ProProcessApi = {
  // 查询生产工序列表分页
  getProcessPage: async (params: any) => {
    return await request.get({ url: `/mes/pro/process/page`, params })
  },

  // 查询生产工序精简列表
  getProcessSimpleList: async () => {
    return await request.get({ url: `/mes/pro/process/simple-list` })
  },

  // 查询生产工序详情
  getProcess: async (id: number, params?: { routeId?: number }) => {
    return await request.get({ url: `/mes/pro/process/get`, params: { id, ...params } })
  },

  // 查询生产工序关联设备产能明细
  getProcessMachineryList: async (processId: number) => {
    return await request.get({ url: `/mes/pro/process/machinery-list`, params: { processId } })
  },

  // 新增生产工序
  createProcess: async (data: ProProcessVO) => {
    return await request.post({ url: `/mes/pro/process/create`, data })
  },

  // 修改生产工序
  updateProcess: async (data: ProProcessVO) => {
    return await request.put({ url: `/mes/pro/process/update`, data })
  },

  // 删除生产工序
  deleteProcess: async (id: number) => {
    return await request.delete({ url: `/mes/pro/process/delete?id=` + id })
  },

  // 导出生产工序 Excel
  exportProcess: async (params: any) => {
    return await request.download({ url: `/mes/pro/process/export-excel`, params })
  }
}
