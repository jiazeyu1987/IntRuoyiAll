import request from '@/config/axios'

export interface ActiveOrderReplenishmentLine {
  itemId: string
  documentId: string
  billNo: string
  productionOrderNo: string
  materialCode: string
  materialName: string
  lotNumber: string
  actualQuantity: number | null
  documentStatus: string
  sourceHash: string
}

export const getActiveOrderReplenishments = (activeOrderId: string) =>
  request.get<ActiveOrderReplenishmentLine[]>({
    url: '/mes/pro/process-pool/team-leader/active-order/replenishments',
    params: { activeOrderId }
  })
