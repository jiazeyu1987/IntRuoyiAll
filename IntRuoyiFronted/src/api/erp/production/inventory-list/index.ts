import request from '@/config/axios'

export interface ErpInventoryListVO {
  id: number
  materialNumber: string
  materialName: string
  materialSpecification: string
  warehouseNumber: string
  warehouseName: string
  lotNumber: string
  unitName: string
  quantity: number
  stockOrgNumber: string
  stockOrgName: string
  sourceModifyTime: string
  lastSyncTime: string
}

export const ErpInventoryListApi = {
  getPage: async (params: any) => {
    return await request.get({ url: `/erp/inventory-list/page`, params })
  }
}
