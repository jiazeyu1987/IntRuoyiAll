import request from '@/config/axios'

// MES 设备台账 VO
export interface DvMachineryVO {
  id: number
  code: string
  name: string
  brand: string
  specification: string
  machineryTypeId: number
  machineryTypeName: string
  workshopId: number
  workshopName: string
  processName?: string
  standardHourlyCapacity?: number
  status: number
  lastMaintenTime: Date
  lastCheckTime: Date
  remark: string
}

// MES 设备台账 API
export const DvMachineryApi = {
  getMachineryPage: async (params: any) => {
    return await request.get({ url: `/mes/dv/machinery/page`, params })
  },

  getMachinery: async (id: number) => {
    return await request.get({ url: `/mes/dv/machinery/get?id=` + id })
  },

  createMachinery: async (data: DvMachineryVO) => {
    return await request.post({ url: `/mes/dv/machinery/create`, data })
  },

  updateMachinery: async (data: DvMachineryVO) => {
    return await request.put({ url: `/mes/dv/machinery/update`, data })
  },

  deleteMachinery: async (id: number) => {
    return await request.delete({ url: `/mes/dv/machinery/delete?id=` + id })
  },

  exportMachinery: async (params: any) => {
    return await request.download({ url: `/mes/dv/machinery/export-excel`, params })
  },

  importTemplate: async () => {
    return await request.download({ url: `/mes/dv/machinery/get-import-template` })
  }
}
