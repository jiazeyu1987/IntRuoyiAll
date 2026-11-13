import request from '@/config/axios'

export interface ErpKingdeeStockMoveItemVO {
  id: number
  sourceEntryId: string
  materialNumber: string
  materialName: string
  materialSpecification?: string
  unitName?: string
  quantity: number
  fromWarehouseNumber?: string
  fromWarehouseName?: string
  toWarehouseNumber?: string
  toWarehouseName?: string
  fromStockLocation?: string
  toStockLocation?: string
  lotNumber?: string
}

export interface ErpKingdeeStockMoveVO {
  id: number
  sourceFormId: string
  sourceFid: string
  sourceBillNo: string
  billDate?: string
  documentStatus?: string
  transferDirect?: string
  transferBizType?: string
  remark?: string
  sourceModifyTime?: string
  lastSyncTime?: string
  createTime?: string
  materialNames?: string
  items?: ErpKingdeeStockMoveItemVO[]
}

export interface ErpKingdeeStockMovePageReqVO extends PageParam {
  sourceBillNo?: string
  documentStatus?: string
  transferDirect?: string
  billDate?: string[]
}

export const ErpKingdeeStockMoveApi = {
  getStockMovePage: async (params: ErpKingdeeStockMovePageReqVO) => {
    return await request.get({ url: '/erp/kingdee-stock-move/page', params })
  }
}
