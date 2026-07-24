import request from '@/config/axios'

export interface SrmPurchaseOrderLineVO {
  id?: number
  lineNo?: string
  sourcePlanLineId?: number
  materialId?: number
  materialCode?: string
  materialName?: string
  requestedQuantity?: number
  unit?: string
  requestedDeliveryDate?: string
  confirmedQuantity?: number
  confirmedDeliveryDate?: string
  supplierRemark?: string
  pendingChangedQuantity?: number
  pendingChangedDeliveryDate?: string
  pendingChangedRemark?: string
}

export interface SrmPurchaseOrderChangeVO {
  id?: number
  changeNo?: string
  changeStatus?: string
  changeStatusLabel?: string
  changeReason?: string
  changeRemark?: string
  confirmRemark?: string
  rejectRemark?: string
  withdrawRemark?: string
  submittedTime?: string
  confirmedTime?: string
  rejectedTime?: string
  withdrawnTime?: string
}

export interface SrmPurchaseOrderVO {
  id?: number
  orderNo?: string
  sourcePlanId?: number
  sourcePlanNo?: string
  supplierId?: number
  supplierName?: string
  orderStatus?: string
  orderStatusLabel?: string
  orderRemark?: string
  confirmedBy?: number
  confirmedName?: string
  confirmedTime?: string
  confirmRemark?: string
  createTime?: string
  latestChange?: SrmPurchaseOrderChangeVO
  lines: SrmPurchaseOrderLineVO[]
}

export interface SrmPurchaseOrderPageReqVO extends PageParam {
  orderNo?: string
  sourcePlanNo?: string
  supplierName?: string
  orderStatus?: string
}

export interface SrmPurchaseOrderCreateReqVO {
  sourcePlanId: number
  supplierId: number
  orderRemark?: string
}

export interface SrmPurchaseOrderConfirmReqVO {
  id: number
  confirmRemark?: string
  lines: Array<{
    orderLineId: number
    confirmedQuantity: number
    confirmedDeliveryDate: string
    supplierRemark?: string
  }>
}

export interface SrmPurchaseOrderChangeReqVO {
  orderId: number
  changeReason: string
  changeRemark?: string
  lines: Array<{
    orderLineId: number
    changedQuantity: number
    changedDeliveryDate: string
    changedSupplierRemark?: string
  }>
}

export interface SrmPurchaseOrderRejectChangeReqVO {
  changeId: number
  rejectRemark: string
}

export interface SrmPurchaseOrderWithdrawChangeReqVO {
  changeId: number
  withdrawRemark: string
}

export const srmPurchaseOrderStatusOptions = [
  { label: '待供应商确认', value: 'PENDING_CONFIRM' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '变更待确认', value: 'CHANGE_PENDING' },
  { label: '已取消', value: 'CANCELLED' }
] as const

export const srmPurchaseOrderChangeStatusOptions = [
  { label: '待供应商确认', value: 'PENDING_CONFIRM' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已撤回', value: 'WITHDRAWN' }
] as const

export const SrmPurchaseOrderApi = {
  createFromPlan: async (data: SrmPurchaseOrderCreateReqVO) => {
    return await request.post<number>({
      url: '/srm/purchase-order/create-from-plan',
      data
    })
  },

  getPurchaseOrderPage: async (params: SrmPurchaseOrderPageReqVO) => {
    return await request.get<PageResult<SrmPurchaseOrderVO[]>>({
      url: '/srm/purchase-order/page',
      params
    })
  },

  getPurchaseOrder: async (id: number) => {
    return await request.get<SrmPurchaseOrderVO>({
      url: '/srm/purchase-order/get',
      params: { id }
    })
  },

  getMyPurchaseOrderPage: async (params: SrmPurchaseOrderPageReqVO) => {
    return await request.get<PageResult<SrmPurchaseOrderVO[]>>({
      url: '/srm/purchase-order/my/page',
      params
    })
  },

  getMyPurchaseOrder: async (id: number) => {
    return await request.get<SrmPurchaseOrderVO>({
      url: '/srm/purchase-order/my/get',
      params: { id }
    })
  },

  confirmMyPurchaseOrder: async (data: SrmPurchaseOrderConfirmReqVO) => {
    return await request.put<boolean>({
      url: '/srm/purchase-order/confirm-my',
      data
    })
  },

  submitOrderChange: async (data: SrmPurchaseOrderChangeReqVO) => {
    return await request.post<number>({
      url: '/srm/purchase-order/change',
      data
    })
  },

  rejectMyOrderChange: async (data: SrmPurchaseOrderRejectChangeReqVO) => {
    return await request.put<boolean>({
      url: '/srm/purchase-order/change/reject-my',
      data
    })
  },

  withdrawOrderChange: async (data: SrmPurchaseOrderWithdrawChangeReqVO) => {
    return await request.put<boolean>({
      url: '/srm/purchase-order/change/withdraw',
      data
    })
  }
}
