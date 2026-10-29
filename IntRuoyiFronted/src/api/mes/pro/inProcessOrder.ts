import request from '@/config/axios'

export interface InProcessOrder {
  id: number | string
  workOrderCode: string
  productCode: string
  productName: string
  batchCode: string
  leaderUserId: number | string
  leaderName: string
  routeName: string
  quantity: number
  productionProgressPercent: number | null
  inspectionProgressPercent: number | null
  joinedAt: string
  businessStatus: string
  abnormal: boolean
  abnormalReason?: string
  readBlocked: boolean
  readBlockReason?: string
}

export const getInProcessOrderPage = (params: {
  pageNo: number
  pageSize: number
  workOrderCode?: string
  productName?: string
  batchCode?: string
  equipment?: string
}) =>
  request.get<{ list: InProcessOrder[]; total: number }>({
    url: '/mes/pro/in-process-order/page',
    params
  })
