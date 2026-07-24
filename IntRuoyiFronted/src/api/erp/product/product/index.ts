import request from '@/config/axios'

// ERP 产品 VO
export interface ProductVO {
  id: number
  name: string
  barCode: string
  categoryId: number
  unitId: number
  unitName?: string
  status: number
  standard: string
  remark: string
  expiryDay: number
  weight: number
  purchasePrice: number
  salePrice: number
  minPrice: number
}

export interface KingdeeProductSyncRespVO {
  createdCount: number
  updatedCount: number
  skippedCount: number
  createdProductCodes: string[]
  updatedProductCodes: string[]
  skippedProductCodes: string[]
}

// ERP 产品 API
export const ProductApi = {
  getProductPage: async (params: any) => {
    return await request.get({ url: `/erp/product/page`, params })
  },

  getProductSimpleList: async () => {
    return await request.get({ url: `/erp/product/simple-list` })
  },

  getProduct: async (id: number) => {
    return await request.get({ url: `/erp/product/get?id=` + id })
  },

  createProduct: async (data: ProductVO) => {
    return await request.post({ url: `/erp/product/create`, data })
  },

  updateProduct: async (data: ProductVO) => {
    return await request.put({ url: `/erp/product/update`, data })
  },

  deleteProduct: async (id: number) => {
    return await request.delete({ url: `/erp/product/delete?id=` + id })
  },

  exportProduct: async (params) => {
    return await request.download({ url: `/erp/product/export-excel`, params })
  },

  syncKingdeeProducts: async (): Promise<KingdeeProductSyncRespVO> => {
    return await request.post({ url: `/erp/product/sync-kingdee` })
  }
}
