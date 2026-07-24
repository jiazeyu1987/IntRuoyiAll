import request from '@/config/axios'

export interface StockVO {
  id: number
  productId: number
  warehouseId: number
  count: number
}

export interface KingdeeStockSyncRespVO {
  syncedCount: number
}

export const StockApi = {
  getStockPage: async (params: any) => {
    return await request.get({ url: `/erp/stock/page`, params })
  },

  getStock: async (id: number) => {
    return await request.get({ url: `/erp/stock/get?id=` + id })
  },

  getStock2: async (productId: number, warehouseId: number) => {
    return await request.get({ url: `/erp/stock/get`, params: { productId, warehouseId } })
  },

  getStockCount: async (productId: number) => {
    return await request.get({ url: `/erp/stock/get-count`, params: { productId } })
  },

  exportStock: async (params: any) => {
    return await request.download({ url: `/erp/stock/export-excel`, params })
  },

  syncKingdeeStocks: async (): Promise<KingdeeStockSyncRespVO> => {
    return await request.post({ url: `/erp/stock/sync-kingdee` })
  }
}
