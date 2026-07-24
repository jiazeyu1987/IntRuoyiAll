import request from '@/config/axios'

export interface SrmSupplierRiskVO {
  id?: number
  supplierId: number
  supplierAccessId?: number
  supplierName?: string
  riskLevel: string
  riskLevelLabel?: string
  riskStatus?: string
  riskStatusLabel?: string
  sourceType: string
  sourceTypeLabel?: string
  sourceId?: number
  sourceCode?: string
  sourceName?: string
  riskDescription: string
  riskRemark?: string
  reportedName?: string
  reportedTime?: string
  resolvedName?: string
  resolvedTime?: string
  resolutionRemark?: string
}

export interface SrmSupplierRiskPageReqVO extends PageParam {
  supplierName?: string
  riskLevel?: string
  riskStatus?: string
}

export interface SrmSupplierRiskResolveReqVO {
  id: number
  resolutionRemark: string
}

export const srmSupplierRiskLevelOptions = [
  { label: '低', value: 'LOW' },
  { label: '中', value: 'MEDIUM' },
  { label: '高', value: 'HIGH' }
] as const

export const srmSupplierRiskStatusOptions = [
  { label: '未处理', value: 'OPEN' },
  { label: '已处理', value: 'RESOLVED' }
] as const

export const srmSupplierRiskSourceTypeOptions = [
  { label: '准入申请', value: 'ACCESS_REQUEST' },
  { label: '采购计划', value: 'PROCUREMENT_PLAN' },
  { label: '非招标项目', value: 'NON_TENDER_PROJECT' },
  { label: '招标项目', value: 'TENDER_PROJECT' },
  { label: '采购合同', value: 'PROCUREMENT_CONTRACT' }
] as const

export const SrmSupplierRiskApi = {
  getSupplierRiskPage: async (params: SrmSupplierRiskPageReqVO) => {
    return await request.get<PageResult<SrmSupplierRiskVO[]>>({
      url: '/srm/supplier-risk/page',
      params
    })
  },

  createSupplierRisk: async (data: SrmSupplierRiskVO) => {
    return await request.post<number>({
      url: '/srm/supplier-risk/create',
      data
    })
  },

  resolveSupplierRisk: async (data: SrmSupplierRiskResolveReqVO) => {
    return await request.put<boolean>({
      url: '/srm/supplier-risk/resolve',
      data
    })
  }
}
