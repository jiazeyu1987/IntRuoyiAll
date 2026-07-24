import request from '@/config/axios'

export interface SaleOrderVO {
  id: number
  no: string
  customerId: number
  orderTime: Date
  totalCount: number
  totalPrice: number
  status: number
  remark: string
  outCount: number
  returnCount: number
}

export interface KingdeeSaleOrderSyncRespVO {
  createdCount: number
  skippedCount: number
  createdSaleOrderIds: number[]
  skippedSourceFids: string[]
}

export const SaleOrderApi = {
  getSaleOrderPage: async (params: any) => {
    return await request.get({ url: `/erp/sale-order/page`, params })
  },

  getSaleOrder: async (id: number) => {
    return await request.get({ url: `/erp/sale-order/get?id=` + id })
  },

  createSaleOrder: async (data: SaleOrderVO) => {
    return await request.post({ url: `/erp/sale-order/create`, data })
  },

  updateSaleOrder: async (data: SaleOrderVO) => {
    return await request.put({ url: `/erp/sale-order/update`, data })
  },

  updateSaleOrderStatus: async (id: number, status: number) => {
    return await request.put({
      url: `/erp/sale-order/update-status`,
      params: {
        id,
        status
      }
    })
  },

  deleteSaleOrder: async (ids: number[]) => {
    return await request.delete({
      url: `/erp/sale-order/delete`,
      params: {
        ids: ids.join(',')
      }
    })
  },

  exportSaleOrder: async (params: any) => {
    return await request.download({ url: `/erp/sale-order/export-excel`, params })
  },

  syncKingdeeSaleOrders: async (): Promise<KingdeeSaleOrderSyncRespVO> => {
    return await request.post({ url: `/erp/sale-order/sync-kingdee` })
  }
}
