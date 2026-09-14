import request from '@/config/axios'

export interface ErpProductionReplenishmentListItemVO {
  id: number
  sourceEntryId: string
  materialNumber: string
  materialName: string
  materialSpecification?: string
  unitName?: string
  requestedQuantity: number
  actualQuantity: number
  baseActualQuantity?: number
  warehouseNumber?: string
  warehouseName?: string
  stockLocationNumber?: string
  stockLocationName?: string
  lotNumber?: string
  productionOrderNo?: string
  productionOrderLineNo?: number
  productionMaterialListNo?: string
  productionMaterialListLineNo?: number
  workshopNumber?: string
  workshopName?: string
  stockStatusNumber?: string
  stockStatusName?: string
}

export interface ErpProductionReplenishmentListVO {
  id: number
  sourceFormId: string
  sourceFid: string
  sourceBillNo: string
  billDate?: string
  documentStatus?: string
  stockOrgNumber?: string
  stockOrgName?: string
  productionOrgNumber?: string
  productionOrgName?: string
  ownerNumber?: string
  ownerName?: string
  departmentNumber?: string
  departmentName?: string
  description?: string
  sourceModifyTime?: string
  lastSyncTime?: string
  createTime?: string
  productionOrderNos?: string
  materialNames?: string
  items?: ErpProductionReplenishmentListItemVO[]
}

export const ErpProductionReplenishmentListApi = {
  getPage: async (params: PageParam) => {
    return await request.get({ url: '/erp/production-replenishment-list/page', params })
  }
}
