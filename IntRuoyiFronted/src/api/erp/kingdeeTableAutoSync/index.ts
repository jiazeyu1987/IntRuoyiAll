import request from '@/config/axios'

export interface ErpKingdeeTableAutoSyncPlanItemVO {
  id?: number
  syncType: string
  enabled: boolean
  sortOrder?: number
}

export interface ErpKingdeeTableAutoSyncPlanVO {
  id?: number
  enabled: boolean
  dailyStartTime?: string
  cronExpression?: string
  jobId?: number
  lastAutoRunDate?: string
  lastRunTime?: string | number
  lastStatus?: string
  lastMessage?: string
  items: ErpKingdeeTableAutoSyncPlanItemVO[]
}

export interface ErpKingdeeTableAutoSyncPlanSaveReqVO {
  enabled: boolean
  dailyStartTime?: string
  items: ErpKingdeeTableAutoSyncPlanItemVO[]
}

export interface ErpKingdeeTableAutoSyncTypeVO {
  syncType: string
  label: string
  handlerName: string
}

export interface ErpKingdeeTableAutoSyncRunItemVO {
  syncType: string
  label: string
  handlerName: string
  status: string
  message?: string
}

export interface ErpKingdeeTableAutoSyncRunOnceRespVO {
  status: string
  totalSyncCount: number
  successSyncCount: number
  failureMessage?: string
  items?: ErpKingdeeTableAutoSyncRunItemVO[]
}

export interface ErpKingdeeTableAutoSyncRunVO {
  id: number
  syncType: string
  triggerType: string
  status: number
  windowStartTime?: string | number
  windowEndTime?: string | number
  startedAt?: string | number
  endedAt?: string | number
  createdCount?: number
  updatedCount?: number
  skippedCount?: number
  failedCount?: number
  failureMessage?: string
}

export interface ErpKingdeeTableAutoSyncWatermarkVO {
  syncType: string
  lastSuccessTime?: string | number
}

export const ErpKingdeeTableAutoSyncApi = {
  getPlan: async (): Promise<ErpKingdeeTableAutoSyncPlanVO> => {
    return await request.get({ url: '/erp/kingdee-table-auto-sync/plan/get' })
  },

  savePlan: async (
    data: ErpKingdeeTableAutoSyncPlanSaveReqVO
  ): Promise<ErpKingdeeTableAutoSyncPlanVO> => {
    return await request.put({ url: '/erp/kingdee-table-auto-sync/plan/save', data })
  },

  getSyncTypes: async (): Promise<ErpKingdeeTableAutoSyncTypeVO[]> => {
    return await request.get({ url: '/erp/kingdee-table-auto-sync/sync-types' })
  },

  runOnce: async (): Promise<ErpKingdeeTableAutoSyncRunOnceRespVO> => {
    return await request.post({ url: '/erp/kingdee-table-auto-sync/plan/run-once' })
  },

  getRunPage: async (params: PageParam): Promise<PageResult<ErpKingdeeTableAutoSyncRunVO[]>> => {
    return await request.get({ url: '/erp/kingdee-table-auto-sync/run/page', params })
  },

  getWatermarkList: async (): Promise<ErpKingdeeTableAutoSyncWatermarkVO[]> => {
    return await request.get({ url: '/erp/kingdee-table-auto-sync/watermark/list' })
  }
}
