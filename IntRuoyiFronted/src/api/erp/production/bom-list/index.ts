import request from '@/config/axios'

export interface ErpBomListVO {
  id: number
  bomNumber: string
  bomType: string
  documentStatus: string
  parentMaterialCode: string
  parentMaterialName: string
  parentMaterialSpecification: string
  parentQuantity: number
  lineNo: number
  childMaterialCode: string
  childMaterialName: string
  childMaterialSpecification: string
  childUnitName: string
  numerator: number
  denominator: number
  sourceModifyTime: string
  lastSyncTime: string
}

export const ErpBomListApi = {
  getPage: async (params: any) => {
    return await request.get({ url: `/erp/bom-list/page`, params })
  }
}
