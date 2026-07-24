import request from '@/config/axios'

export interface ProRouteResourceVO {
  rowKey: string
  resourceType: 'MACHINE' | 'WORKER' | 'UNCONFIGURED'
  routeProductId: number
  productId: number
  productCode: string
  productName: string
  routeId: number
  routeCode: string
  routeName: string
  routeProcessId: number
  processId: number
  processCode: string
  processName: string
  sort: number
  workstationId?: number
  workstationCode?: string
  workstationName?: string
  workstationMachineId?: number
  machineryId?: number
  machineryCode?: string
  machineryName?: string
  machineryQuantity?: number
  machineryStandardHourlyCapacity?: number
  workstationWorkerId?: number
  workerQuantity?: number
  singleStandardHourlyCapacity?: number
  budgetHourlyCapacity?: number
  budgetDailyCapacity?: number
  capacitySource?: string
}

export const ProRouteResourceApi = {
  getResourcePage: async (params: any) => {
    return await request.get({ url: '/mes/pro/route-resource/page', params })
  }
}
