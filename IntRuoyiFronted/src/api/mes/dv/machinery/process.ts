import request from '@/config/axios'

export interface DvMachineryProcessVO {
  id: number
  machineryId: number
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
  getMachineryProcessList: async (machineryId: number) => {
    return await request.get({
      url: `/mes/dv/machinery-process/list-by-machinery?machineryId=` + machineryId
    })
  }
}
