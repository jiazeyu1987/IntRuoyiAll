import request from '@/config/axios'

export interface SrmNonBiddingProjectLineVO {
  id: number
  sourcePlanLineId?: number
  lineNo?: string
  materialId: number
  materialCode: string
  materialName: string
  quantity: number
  unit: string
  requiredDate?: string
}

export interface SrmNonBiddingSupplierScopeVO {
  id?: number
  supplierId: number
  supplierName: string
}

export interface SrmNonBiddingQuoteLineVO {
  id?: number
  projectLineId: number
  materialId?: number
  materialCode?: string
  materialName?: string
  quantity?: number
  unit?: string
  unitPrice: number
  lineAmount: number
}

export interface SrmNonBiddingQuoteVO {
  id?: number
  supplierId: number
  supplierName?: string
  quoteAmount: number
  quoteStatus?: string
  attachmentUrl: string
  quotedName?: string
  quotedTime?: string
  lines: SrmNonBiddingQuoteLineVO[]
}

export interface SrmNonBiddingQuoteRankingVO {
  rankNo: number
  quoteId: number
  supplierId: number
  supplierName?: string
  quoteAmount: number
  quotedTime?: string
}

export interface SrmNonBiddingComparisonSummaryVO {
  supplierQuoteCount: number
  lowestQuoteAmount?: number
  lowestQuoteSupplierId?: number
  lowestQuoteSupplierName?: string
  highestQuoteAmount?: number
  averageQuoteAmount?: number
  quoteRankings: SrmNonBiddingQuoteRankingVO[]
}

export interface SrmNonBiddingPriceTrendPointVO {
  projectId: number
  projectNo?: string
  quoteId: number
  supplierId: number
  supplierName?: string
  unitPrice: number
  lineAmount: number
  quotedTime?: string
}

export interface SrmNonBiddingPriceTrendVO {
  materialId: number
  materialCode?: string
  materialName?: string
  points: SrmNonBiddingPriceTrendPointVO[]
}

export interface SrmNonBiddingProjectVO {
  id: number
  projectNo: string
  projectTitle: string
  projectType: string
  projectTypeLabel?: string
  projectStatus: string
  projectStatusLabel?: string
  sourcePlanId: number
  sourcePlanNo: string
  expectedAmount: number
  quoteMode?: string
  quoteModeLabel?: string
  quoteStartTime?: string
  quoteEndTime?: string
  publishAttachmentUrl?: string
  publishedTime?: string
  dealQuoteId?: number
  dealSupplierId?: number
  dealSupplierName?: string
  dealAmount?: number
  dealRemark?: string
  dealTime?: string
  contractId?: number
  createTime?: string
  lines: SrmNonBiddingProjectLineVO[]
  supplierScopes: SrmNonBiddingSupplierScopeVO[]
  quotes: SrmNonBiddingQuoteVO[]
  comparisonSummary?: SrmNonBiddingComparisonSummaryVO
  priceTrends: SrmNonBiddingPriceTrendVO[]
}

export interface SrmNonBiddingProjectPageReqVO extends PageParam {
  projectNo?: string
  projectTitle?: string
  projectStatus?: string
  supplierId?: number
}

export interface SrmNonBiddingPublishReqVO {
  projectId: number
  quoteMode: string
  quoteStartTime: number
  quoteEndTime: number
  attachmentUrl: string
  supplierIds: number[]
}

export const srmNonBiddingQuoteModeOptions = [
  { label: '邀请询价', value: 'INVITE' },
  { label: '公开询价', value: 'PUBLIC' }
] as const

export interface SrmNonBiddingQuoteReqVO {
  projectId: number
  supplierId: number
  quoteAmount: number
  attachmentUrl: string
  lines: Array<{
    projectLineId: number
    unitPrice: number
    lineAmount: number
  }>
}

export interface SrmNonBiddingDealReqVO {
  projectId: number
  quoteId: number
  dealRemark: string
}

export const srmNonBiddingProjectStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已成交', value: 'DEAL_CONFIRMED' },
  { label: '已建合同', value: 'CONTRACT_CREATED' }
] as const

export const SrmNonBiddingProjectApi = {
  getProjectPage: async (params: SrmNonBiddingProjectPageReqVO) => {
    return await request.get<PageResult<SrmNonBiddingProjectVO[]>>({
      url: '/srm/non-bidding-project/page',
      params
    })
  },

  getContractableProjectPage: async (params: SrmNonBiddingProjectPageReqVO) => {
    return await request.get<PageResult<SrmNonBiddingProjectVO[]>>({
      url: '/srm/non-bidding-project/contractable-page',
      params
    })
  },

  getProject: async (id: number) => {
    return await request.get<SrmNonBiddingProjectVO>({
      url: '/srm/non-bidding-project/get',
      params: { id }
    })
  },

  publishProject: async (data: SrmNonBiddingPublishReqVO) => {
    return await request.post<SrmNonBiddingProjectVO>({
      url: '/srm/non-bidding-project/publish',
      data
    })
  },

  submitQuote: async (data: SrmNonBiddingQuoteReqVO) => {
    return await request.post<SrmNonBiddingProjectVO>({
      url: '/srm/non-bidding-project/quote',
      data
    })
  },

  confirmDeal: async (data: SrmNonBiddingDealReqVO) => {
    return await request.post<SrmNonBiddingProjectVO>({
      url: '/srm/non-bidding-project/deal',
      data
    })
  }
}
