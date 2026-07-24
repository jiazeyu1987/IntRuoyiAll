import request from '@/config/axios'
import type { TableQuickFilterValue } from '@/hooks/web/useTableQuickFilter'

export interface ProWorkOrderVO {
  id: number
  code: string
  name: string
  type: number
  orderSourceType: number
  orderSourceCode: string
  productId: number
  productName: string
  productCode: string
  productSpecification: string
  unitMeasureName: string
  quantity: number
  quantityProduced: number
  quantityChanged: number
  quantityScheduled: number
  clientId: number
  clientCode: string
  clientName: string
  vendorId: number
  vendorName: string
  vendorCode: string
  batchCode: string
  workshopName: string
  bomVersion: string
  pickMode: string
  auxiliaryCode: string
  businessStatus: string
  drawingNumber: string
  scheduleStatus: string
  plannedStartTime: Date
  plannedEndTime: Date
  requestDate: Date
  parentId: number
  parentCode: string
  finishDate: Date
  cancelDate: Date
  status: number
  temporaryFrozen: boolean
  remark: string
  productionMaterialListCount: number
  productionMaterialListSummary: string
}

export interface ProWorkOrderTemporaryFreezeStatusVO {
  enabled: boolean
  totalWorkOrderCount: number
  frozenWorkOrderCount: number
  unfrozenWorkOrderCount: number
  clearedTaskCount: number
}

export interface KingdeeProductionOrderSyncRespVO {
  createdCount: number
  updatedCount: number
  skippedCount: number
  createdWorkOrderIds: number[]
  updatedWorkOrderIds: number[]
  skippedSourceKeys: string[]
}

export interface KingdeeWorkOrderBomSyncRespVO {
  workOrderId: number
  erpBomVersion: string
  syncedBomCount: number
}

export interface KingdeeProductionOrderCreateRespVO {
  workOrderId: number
  erpFid: string
  erpBillNo: string
  saved: boolean
  submitted: boolean
}

export interface ProWorkOrderKingdeeSyncStatusVO {
  syncType: string
  autoSyncConfigured: boolean
  autoSyncEnabled?: boolean
  autoSyncJobId?: number
  autoSyncJobName?: string
  autoSyncCronExpression?: string
  latestStatus: string
  latestRunTime?: string
  latestFinishedTime?: string
  latestTriggerType?: string
  latestCreatedCount?: number
  latestUpdatedCount?: number
  latestSkippedCount?: number
  latestFailedCount?: number
  latestFailureMessage?: string
  lastSuccessTime?: string
}

export interface ProWorkOrderPageReqVO extends PageParam {
  code?: string
  productNameKeyword?: string
  productCodeKeyword?: string
  requestDate?: string[]
  quickFilter?: TableQuickFilterValue
}

export const ProWorkOrderApi = {
  getWorkOrderPage: async (params: ProWorkOrderPageReqVO) => {
    return await request.get({ url: `/mes/pro/work-order/page`, params })
  },

  getWorkOrderProductNameOptions: async (keyword?: string): Promise<string[]> => {
    return await request.get({ url: `/mes/pro/work-order/product-name-options`, params: { keyword } })
  },

  getWorkOrder: async (id: number) => {
    return await request.get({ url: `/mes/pro/work-order/get?id=` + id })
  },

  createWorkOrder: async (data: ProWorkOrderVO) => {
    return await request.post({ url: `/mes/pro/work-order/create`, data })
  },

  updateWorkOrder: async (data: ProWorkOrderVO) => {
    return await request.put({ url: `/mes/pro/work-order/update`, data })
  },

  deleteWorkOrder: async (id: number) => {
    return await request.delete({ url: `/mes/pro/work-order/delete?id=` + id })
  },

  exportWorkOrder: async (params: any) => {
    return await request.download({ url: `/mes/pro/work-order/export-excel`, params })
  },

  finishWorkOrder: async (id: number) => {
    return await request.put({ url: `/mes/pro/work-order/finish?id=` + id })
  },

  cancelWorkOrder: async (id: number) => {
    return await request.put({ url: `/mes/pro/work-order/cancel?id=` + id })
  },

  confirmWorkOrder: async (id: number) => {
    return await request.put({ url: `/mes/pro/work-order/confirm?id=` + id })
  },

  syncKingdeeWorkOrders: async (): Promise<KingdeeProductionOrderSyncRespVO> => {
    return await request.post({ url: `/mes/pro/work-order/sync-kingdee` })
  },

  getKingdeeSyncStatus: async (): Promise<ProWorkOrderKingdeeSyncStatusVO> => {
    return await request.get({ url: `/mes/pro/work-order/sync-status` })
  },

  createKingdeeProductionOrder: async (
    workOrderId: number
  ): Promise<KingdeeProductionOrderCreateRespVO> => {
    return await request.post({
      url: `/mes/pro/work-order/${workOrderId}/create-kingdee-production-order`
    })
  },

  syncErpBom: async (workOrderId: number): Promise<KingdeeWorkOrderBomSyncRespVO> => {
    return await request.post({ url: `/mes/pro/work-order/${workOrderId}/sync-erp-bom` })
  },

  getTemporaryFreezeStatus: async () => {
    return await request.get<ProWorkOrderTemporaryFreezeStatusVO>({
      url: `/mes/pro/work-order/temporary-freeze-status`
    })
  },

  updateTemporaryFreeze: async (data: { enabled: boolean }) => {
    return await request.put<ProWorkOrderTemporaryFreezeStatusVO>({
      url: `/mes/pro/work-order/temporary-freeze`,
      data
    })
  },

  updateWorkOrderTemporaryFrozen: async (data: { id: number; temporaryFrozen: boolean }) => {
    return await request.put({
      url: `/mes/pro/work-order/update-temporary-frozen`,
      data
    })
  }
}
