import request from '@/config/axios'

export interface SrmOutsourceExecutionEventVO {
  id?: number
  eventNo?: string
  eventType?: string
  eventTypeLabel?: string
  beforeStatus?: string
  afterStatus?: string
  operatorName?: string
  eventRemark?: string
  eventPayload?: string
  eventTime?: string
}

export interface SrmOutsourceExecutionReconciliationVO {
  id?: number
  reconciliationNo?: string
  reconciliationStatus?: string
  reconciliationStatusLabel?: string
  unitPrice?: number
  receivedQuantity?: number
  qualifiedQuantity?: number
  diffQuantity?: number
  reconciliationAmount?: number
  diffAmount?: number
  confirmRemark?: string
  confirmedTime?: string
}

export interface SrmOutsourceExecutionVO {
  id?: number
  executionNo?: string
  sourcePurchaseOrderId?: number
  sourcePurchaseOrderNo?: string
  sourcePlanId?: number
  sourcePlanNo?: string
  supplierId?: number
  supplierName?: string
  executionStatus?: string
  executionStatusLabel?: string
  simulationSource?: string
  simulationLabel?: string
  simulationRemark?: string
  plannedQuantity?: number
  issueNoticeNo?: string
  issueQuantity?: number
  progressPercent?: number
  progressStage?: string
  receivedQuantity?: number
  qualifiedQuantity?: number
  unitPrice?: number
  issuedTime?: string
  deliveredTime?: string
  inspectedTime?: string
  createTime?: string
  reconciliation?: SrmOutsourceExecutionReconciliationVO
  events: SrmOutsourceExecutionEventVO[]
}

export interface SrmOutsourceExecutionPageReqVO extends PageParam {
  executionNo?: string
  purchaseOrderNo?: string
  supplierName?: string
  executionStatus?: string
}

export interface SrmOutsourceExecutionCreateReqVO {
  purchaseOrderId: number
  simulationRemark?: string
}

export interface SrmOutsourceExecutionIssueReqVO {
  id: number
  issueQuantity: number
  issueRemark?: string
}

export interface SrmOutsourceExecutionProgressReqVO {
  id: number
  progressPercent: number
  progressStage: string
  progressRemark?: string
}

export interface SrmOutsourceExecutionReceiveReqVO {
  id: number
  receivedQuantity: number
  receiveRemark?: string
}

export interface SrmOutsourceExecutionInspectReqVO {
  id: number
  qualifiedQuantity: number
  inspectRemark?: string
}

export interface SrmOutsourceExecutionReconcileReqVO {
  id: number
  confirmRemark?: string
}

export const srmOutsourceExecutionStatusOptions = [
  { label: '待发料', value: 'PENDING_ISSUE' },
  { label: '加工中', value: 'IN_PRODUCTION' },
  { label: '已送货待检验', value: 'DELIVERED' },
  { label: '已检验待对账', value: 'INSPECTED' },
  { label: '已对账', value: 'RECONCILED' }
] as const

export const SrmOutsourceExecutionApi = {
  createFromPurchaseOrder: async (data: SrmOutsourceExecutionCreateReqVO) => {
    return await request.post<number>({
      url: '/srm/outsource-execution/create-from-purchase-order',
      data
    })
  },

  getOutsourceExecutionPage: async (params: SrmOutsourceExecutionPageReqVO) => {
    return await request.get<PageResult<SrmOutsourceExecutionVO[]>>({
      url: '/srm/outsource-execution/page',
      params
    })
  },

  getOutsourceExecution: async (id: number) => {
    return await request.get<SrmOutsourceExecutionVO>({
      url: '/srm/outsource-execution/get',
      params: { id }
    })
  },

  getMyOutsourceExecutionPage: async (params: SrmOutsourceExecutionPageReqVO) => {
    return await request.get<PageResult<SrmOutsourceExecutionVO[]>>({
      url: '/srm/outsource-execution/my/page',
      params
    })
  },

  getMyOutsourceExecution: async (id: number) => {
    return await request.get<SrmOutsourceExecutionVO>({
      url: '/srm/outsource-execution/my/get',
      params: { id }
    })
  },

  issue: async (data: SrmOutsourceExecutionIssueReqVO) => {
    return await request.put<boolean>({
      url: '/srm/outsource-execution/issue',
      data
    })
  },

  updateProgress: async (data: SrmOutsourceExecutionProgressReqVO) => {
    return await request.put<boolean>({
      url: '/srm/outsource-execution/my/progress',
      data
    })
  },

  receive: async (data: SrmOutsourceExecutionReceiveReqVO) => {
    return await request.put<boolean>({
      url: '/srm/outsource-execution/my/receive',
      data
    })
  },

  inspect: async (data: SrmOutsourceExecutionInspectReqVO) => {
    return await request.put<boolean>({
      url: '/srm/outsource-execution/inspect',
      data
    })
  },

  reconcile: async (data: SrmOutsourceExecutionReconcileReqVO) => {
    return await request.put<boolean>({
      url: '/srm/outsource-execution/reconcile',
      data
    })
  }
}
