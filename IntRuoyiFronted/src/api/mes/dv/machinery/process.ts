import request from '@/config/axios'

export interface DvMachineryProcessVO {
  id: number | string
  machineryId: number | string
  processId?: number | string
  machineryCode: string
  lineName: string
  processName: string
  deviceName: string
  deviceQuantity: number
  tenHalfHourDailyCapacity: number
  standardHourlyCapacity: number
  sourceRowNo: number
  remark?: string
}

export const DvMachineryProcessApi = {
  saveCapacity: async (data: MachineryProcessCapacitySaveVO) => {
    return await request.put({ url: '/mes/dv/machinery-process/capacity', data })
  },
  getMachineryProcessList: async (
    machineryId: number | string
  ): Promise<DvMachineryProcessVO[]> => {
    return await request.get({
      url: `/mes/dv/machinery-process/list-by-machinery?machineryId=` + machineryId
    })
  }
}

export interface MachineryProcessCapacitySaveVO {
  machineryId: number | string
  processId: number | string
  standardHourlyCapacity: number
  remark: string
}
