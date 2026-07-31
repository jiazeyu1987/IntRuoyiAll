import { ProRouteResourceApi, type ProRouteResourceVO } from '@/api/mes/pro/route/resource'

export interface MesProcessMachineryVO {
  machineryId?: number
  machineryCode?: string
  machineryName?: string
  machineryQuantity?: number
  machineryStandardHourlyCapacity?: number
}

export interface MesProcessVO extends ProRouteResourceVO {
  mesProcessCode?: string
  mesProcessName?: string
  executionProcessName?: string
  dailyCapacity10_5?: number
  dailyWorkerQuantity?: number
  processPrice?: number
  feedbackEnabled?: boolean
  batchRecordEnabled?: boolean
  batchRecordProcessName?: string
  machineryList?: MesProcessMachineryVO[]
}

export const MesProcessApi = {
  getMesProcessPage: async (params: any) => {
    const data = await ProRouteResourceApi.getResourcePage(params)
    return {
      ...data,
      list: (data.list || []).map(toMesProcessRow)
    }
  }
}

const toMesProcessRow = (row: ProRouteResourceVO): MesProcessVO => ({
  ...row,
  mesProcessCode: row.processCode,
  mesProcessName: row.processName,
  executionProcessName: row.processName,
  dailyCapacity10_5: row.budgetDailyCapacity,
  dailyWorkerQuantity: row.workerQuantity,
  batchRecordEnabled: Boolean(row.batchRecordReportName || row.batchRecordReportId),
  batchRecordProcessName: row.batchRecordReportName,
  machineryList:
    row.resourceType === 'MACHINE'
      ? [
          {
            machineryId: row.machineryId,
            machineryCode: row.machineryCode,
            machineryName: row.machineryName,
            machineryQuantity: row.machineryQuantity,
            machineryStandardHourlyCapacity: row.machineryStandardHourlyCapacity
          }
        ]
      : []
})
