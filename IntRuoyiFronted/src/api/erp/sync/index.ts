import request from '@/config/axios'
import * as JobApi from '@/api/infra/job'

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

export interface ErpKingdeeIncrementalSyncRespVO {
  handlerName: string
  jobId: number
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

  runIncrementalSyncJob: async (handlerName: string): Promise<ErpKingdeeIncrementalSyncRespVO> => {
    const page = await JobApi.getJobPage({
      pageNo: 1,
      pageSize: 1,
      handlerName
    } as PageParam & { handlerName: string })
    const job = page.list?.[0]
    if (!job) {
      throw new Error(`未找到同步任务处理器：${handlerName}`)
    }
    await JobApi.runJob(job.id)
    return { handlerName, jobId: job.id }
  }
}
