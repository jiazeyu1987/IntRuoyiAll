import request from '@/config/axios'

export interface ReplenishmentBindingLine {
  itemId: string
  billNo: string
  materialCode: string
  materialName: string
  lotNumber: string
  actualQuantity: number
  sourceHash: string
  frozenRouteProcessIds: string[]
  boundRouteProcessId?: string
  signatureId?: string
}
export interface ReplenishmentBindingPage {
  version: number
  editable: boolean
  processes: { routeProcessId: string; name: string }[]
  lines: ReplenishmentBindingLine[]
}
const url = '/mes/pro/process-pool/team-leader/active-order/replenishment-bindings'
export const getReplenishmentBindings = (activeOrderId: string) =>
  request.get<ReplenishmentBindingPage>({ url, params: { activeOrderId } })
export const bindReplenishmentProcess = (data: {
  activeOrderId: string
  expectedVersion: number
  itemId: string
  routeProcessId: string
  sourceHash: string
  reason: string
  password: string
}) => request.post({ url, data })
