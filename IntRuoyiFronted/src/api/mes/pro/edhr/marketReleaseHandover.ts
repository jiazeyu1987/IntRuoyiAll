import request from '@/config/axios'
export interface MarketReleaseAccess {
  visible: boolean
  writable: boolean
  original: boolean
  active: boolean
  handoverId?: string | number
  originalUserId?: string | number
  currentUserId?: string | number
  currentUserName?: string
}
export interface MarketReleaseHandoverContext {
  workTaskId?: string | number
  batchExecutionId: string | number
  version: string
  writable: boolean
  canHandover: boolean
  canRecall: boolean
  currentUserName?: string
  blocker: string
  candidates: { id: string | number; name: string }[]
}
export const getMarketReleaseAccess = (params: {batchExecutionId?: string | number; activeOrderId?: string | number}) =>
  request.get<MarketReleaseAccess>({url:'/mes/pro/edhr-release/handover/access',params,ignoreErrorMessage:true})
export const getMarketReleaseHandover = (batchExecutionId: string | number) =>
  request.get<MarketReleaseHandoverContext>({url:'/mes/pro/edhr-release/handover/context',params:{batchExecutionId},ignoreErrorMessage:true})
export const changeMarketReleaseHandover = (action: 'assign' | 'recall', data: {
  batchExecutionId: string | number
  recipientUserId?: string | number
  reason: string
  expectedVersion: string
  idempotencyKey: string
}) => request.post<MarketReleaseHandoverContext>({url:`/mes/pro/edhr-release/handover/${action}`,data,ignoreErrorMessage:true})
