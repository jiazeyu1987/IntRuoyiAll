import request from '@/config/axios'

export interface SrmPaymentExecutionEventVO {
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

export interface SrmPaymentExecutionVO {
  id?: number
  paymentNo?: string
  reconciliationId?: number
  reconciliationNo?: string
  executionId?: number
  executionNo?: string
  contractId?: number
  contractNo?: string
  supplierId?: number
  supplierName?: string
  paymentStatus?: string
  paymentStatusLabel?: string
  simulationSource?: string
  simulationLabel?: string
  paymentStage?: string
  paymentRatio?: number
  dueDate?: string
  paymentTermSummary?: string
  reconciliationAmount?: number
  applyAmount?: number
  paymentRemark?: string
  rejectRemark?: string
  pushRemark?: string
  submittedTime?: string
  approvedTime?: string
  rejectedTime?: string
  pushedTime?: string
  createTime?: string
  events: SrmPaymentExecutionEventVO[]
}

export interface SrmPaymentExecutionPageReqVO extends PageParam {
  paymentNo?: string
  reconciliationNo?: string
  supplierName?: string
  paymentStatus?: string
}

export interface SrmPaymentExecutionCreateReqVO {
  reconciliationId: number
  contractId: number
  paymentRemark?: string
}

export interface SrmPaymentExecutionSubmitReqVO {
  id: number
  submitRemark?: string
}

export interface SrmPaymentExecutionApproveReqVO {
  id: number
  approveRemark?: string
}

export interface SrmPaymentExecutionRejectReqVO {
  id: number
  rejectRemark?: string
  pushSuccess?: boolean
  pushRemark?: string
}

export const srmPaymentExecutionStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审批', value: 'PENDING_APPROVAL' },
  { label: '审批通过', value: 'APPROVED' },
  { label: '审批驳回', value: 'REJECTED' },
  { label: '财务推送成功', value: 'PUSH_SUCCESS' },
  { label: '财务推送失败', value: 'PUSH_FAILED' }
] as const

export const SrmPaymentExecutionApi = {
  createFromReconciliation: async (data: SrmPaymentExecutionCreateReqVO) => {
    return await request.post<number>({
      url: '/srm/payment-execution/create-from-reconciliation',
      data
    })
  },

  getPaymentExecutionPage: async (params: SrmPaymentExecutionPageReqVO) => {
    return await request.get<PageResult<SrmPaymentExecutionVO[]>>({
      url: '/srm/payment-execution/page',
      params
    })
  },

  getPaymentExecution: async (id: number) => {
    return await request.get<SrmPaymentExecutionVO>({
      url: '/srm/payment-execution/get',
      params: { id }
    })
  },

  submit: async (data: SrmPaymentExecutionSubmitReqVO) => {
    return await request.put<boolean>({
      url: '/srm/payment-execution/submit',
      data
    })
  },

  approve: async (data: SrmPaymentExecutionApproveReqVO) => {
    return await request.put<boolean>({
      url: '/srm/payment-execution/approve',
      data
    })
  },

  reject: async (data: SrmPaymentExecutionRejectReqVO) => {
    return await request.put<boolean>({
      url: '/srm/payment-execution/reject',
      data
    })
  },

  financePush: async (data: SrmPaymentExecutionRejectReqVO) => {
    return await request.put<boolean>({
      url: '/srm/payment-execution/finance-push',
      data
    })
  }
}
