import request from '@/config/axios'

export interface ErpNasTableSyncPlanItemVO {
  id?: number
  syncType: string
  enabled: boolean
  sortOrder?: number
  sheetName?: string
}

export interface ErpNasTableSyncPlanVO {
  id?: number
  enabled: boolean
  dailyStartTime?: string
  cronExpression?: string
  nasDirectory?: string
  fileNamePattern?: string
  jobId?: number
  lastRunId?: number
  lastStatus?: string
  items: ErpNasTableSyncPlanItemVO[]
}

export interface ErpNasTableSyncPlanSaveReqVO {
  enabled: boolean
  dailyStartTime?: string
  nasDirectory?: string
  fileNamePattern?: string
  items: ErpNasTableSyncPlanItemVO[]
}

export interface ErpNasTableSyncTypeVO {
  syncType: string
  label: string
  defaultSheetName: string
}

export interface ErpNasTableSyncRunItemVO {
  syncType: string
  status: string
  sheetName: string
  rowCount: number
  failureMessage?: string
}

export interface ErpNasTableSyncRunVO {
  id: number
  planId: number
  triggerType: string
  status: string
  startedAt?: string
  endedAt?: string
  outputPath?: string
  totalTableCount: number
  successTableCount: number
  failedTableCount: number
  failureMessage?: string
  items?: ErpNasTableSyncRunItemVO[]
}

export interface ErpNasTableSyncTestWriteRespVO {
  outputPath: string
}

export interface ErpNasTableSyncRunOnceRespVO {
  runId: number
  status: string
  outputPath?: string
  failureMessage?: string
}

export const ErpNasTableSyncApi = {
  getPlan: async (): Promise<ErpNasTableSyncPlanVO> => {
    return await request.get({ url: '/erp/nas-table-sync/plan/get' })
  },

  savePlan: async (data: ErpNasTableSyncPlanSaveReqVO): Promise<ErpNasTableSyncPlanVO> => {
    return await request.put({ url: '/erp/nas-table-sync/plan/save', data })
  },

  getSyncTypes: async (): Promise<ErpNasTableSyncTypeVO[]> => {
    return await request.get({ url: '/erp/nas-table-sync/sync-types' })
  },

  testNasWrite: async (nasDirectory?: string): Promise<ErpNasTableSyncTestWriteRespVO> => {
    return await request.post({ url: '/erp/nas-table-sync/plan/test-nas-write', data: { nasDirectory } })
  },

  runOnce: async (): Promise<ErpNasTableSyncRunOnceRespVO> => {
    return await request.post({ url: '/erp/nas-table-sync/plan/run-once' })
  },

  getRunPage: async (params: PageParam): Promise<PageResult<ErpNasTableSyncRunVO[]>> => {
    return await request.get({ url: '/erp/nas-table-sync/run/page', params })
  }
}