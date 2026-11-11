import request from '@/config/axios'

export interface ProductionQuantityDraft {
  id: number
  employeeId: number
  identityDomain: 'SYSTEM_USER' | 'MES_EMPLOYEE_PROFILE'
  activeOrderId: number
  workOrderId: number
  routeProcessId: number
  processId: number
  requestJson: string
  requestedQuantity: number
  previousQuantity: number
  upperQuantity: number
  status: 'BLOCKED' | 'READY'
  version: number
  decision?: 'CURRENT_ERROR' | 'HISTORY_ERROR'
  reviewReason?: string
  createTime: string
}

export const getOwnQuantityDrafts = () => request.get<ProductionQuantityDraft[]>({ url: '/mes/pro/production-quantity-draft/own' })
export const getLeaderQuantityDrafts = () => request.get<ProductionQuantityDraft[]>({ url: '/mes/pro/production-quantity-draft/leader' })
export const reviewQuantityDraft = (data: { id: number; expectedVersion: number; decision: string; reason: string; signaturePassword: string }) =>
  request.post<boolean>({ url: '/mes/pro/production-quantity-draft/review', data })
