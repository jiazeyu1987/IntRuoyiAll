import request from '@/config/axios'

export interface MesProcessMachineryVO {
  id?: number
  machinerySortNo?: number
  machineryCode?: string
  machineryName?: string
}

export interface MesProcessVO {
  id: number
  rowKey: string
  sourceFileName: string
  sourceSheetName: string
  sourceRowNo: number
  sortNo: number
  catalogCode: string
  productName: string
  sourceMachineryCodes: string
  mesProcessName: string
  sourceMachineryName: string
  sourceMachineryQuantity: string
  dailyCapacity10_5: string
  dailyWorkerQuantity: string
  mesProcessCode: string
  processPrice: string
  feedbackFlag: string
  batchRecordFlag: string
  batchRecordProcessName: string
  machineryList?: MesProcessMachineryVO[]
}

export const MesProcessApi = {
  getMesProcessPage: async (params: any) => {
    return await request.get({ url: '/mes/pro/mes-process/page', params })
  }
}