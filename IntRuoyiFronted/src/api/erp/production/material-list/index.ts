import request from '@/config/axios'

// ERP 生产用料清单 VO
export interface ErpProductionMaterialListVO {
  id: number
  sourceFormId: string
  sourceBillNo: string
  sourceEntryId: string
  productCode: string
  productionOrderNo: string
  productionOrderLineNo: number
  productionOrderStatus: string
  childMaterialCode: string
  childMaterialName: string
  childMaterialSpecification: string
  childMaterialType: string
  numerator: number
  denominator: number
  childUnitName: string
  requiredQuantity: number
  issueMethod: string
  demandTime: string
  workOrderId: number
  workOrderCode: string
  workOrderBomId: number
  productId: number
  childMaterialId: number
  sourceModifyTime: string
  lastSyncTime: string
  createTime: string
}

export interface ErpProductionMaterialListGroupVO {
  sourceBillNo: string
  lineCount: number
  sourceModifyTime: string
  lastSyncTime: string
  productionOrderCount: number
  productionOrderSummary: string
}

export interface ErpProductionMaterialListDetailVO {
  childMaterialCode: string
  childMaterialName: string
  childMaterialSpecification: string
  childMaterialType: string
  numerator: number
  denominator: number
  childUnitName: string
  productionOrderNo: string
  workOrderId: number
  workOrderCode: string
}

// ERP 生产用料清单 API
export const ErpProductionMaterialListApi = {
  // 查询 ERP 生产用料清单分页
  getPage: async (params: any) => {
    return await request.get({ url: `/erp/production-material-list/page`, params })
  },

  // 查询 ERP 生产用料清单单据汇总分页
  getGroupPage: async (params: any) => {
    return await request.get({ url: `/erp/production-material-list/group-page`, params })
  },

  // 查询 ERP 生产用料清单单据明细
  getDetailList: async (sourceBillNo: string) => {
    return await request.get({ url: `/erp/production-material-list/detail-list`, params: { sourceBillNo } })
  }
}
