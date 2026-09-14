import request from '@/config/axios'

export interface ErpKingdeeSyncRunVO {
  id: number
  syncType: string
  triggerType: string
  status: number
  windowStartTime?: string
  windowEndTime?: string
  startedAt?: string
  endedAt?: string
  createdCount?: number
  updatedCount?: number
  skippedCount?: number
  failedCount?: number
  failureMessage?: string
}

export interface ErpKingdeeSyncWatermarkVO {
  syncType: string
  lastSuccessTime?: string
}

export interface ErpKingdeeProductionOrderCreateReqVO {
  billNo: string
  materialNumber: string
  unitNumber: string
  quantity: number
  plannedStartDate: number
  plannedFinishDate: number
  sourceBillNo?: string
  batchNumber: string
}

export interface ErpKingdeeProductionOrderCreateRespVO {
  erpFid: string
  erpBillNo: string
  saved: boolean
  submitted: boolean
}

export interface ErpKingdeeProductionMaterialListSyncRespVO {
  createdCount: number
  updatedCount: number
  createdIds: number[]
  updatedIds: number[]
}

export interface ErpKingdeeFullSyncRespVO {
  syncType: string
  handlerName: string
  jobId: number
  message: string
}

export const ErpKingdeeSyncApi = {
  getRunPage: async (params: PageParam) => {
    return await request.get({ url: '/erp/kingdee-sync/run/page', params })
  },

  getWatermarkList: async (): Promise<ErpKingdeeSyncWatermarkVO[]> => {
    return await request.get({ url: '/erp/kingdee-sync/watermark/list' })
  },

  createProductionOrder: async (
    data: ErpKingdeeProductionOrderCreateReqVO
  ): Promise<ErpKingdeeProductionOrderCreateRespVO> => {
    return await request.post({ url: '/erp/kingdee-sync/production-order/create', data })
  },

  runIncrementalSync: async (syncType: string): Promise<ErpKingdeeFullSyncRespVO> => {
    return await request.post({
      url: '/erp/kingdee-sync/incremental-sync',
      data: { syncType }
    })
  },

  runFullSync: async (syncType: string): Promise<ErpKingdeeFullSyncRespVO> => {
    return await request.post({
      url: '/erp/kingdee-sync/full-sync',
      data: { syncType }
    })
  }
}
