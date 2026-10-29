import {
  getEdhrBatchHistoryPage,
  type EdhrBatchHistoryRowRespVO
} from '@/api/mes/pro/edhr/batchExecution'
import { type HistorySource } from './historyPagination'

export interface HistoryRow {
  key: string
  id: string | number
  recordCode?: string
  batchExecutionId?: number
  batchExecutionCode?: string
  releaseTransactionId?: number
  workOrderCode?: string
  batchCode?: string
  productName?: string
  startedAt?: string | number
  pqcReleasedAt?: string | number
  releaseApprovedAt?: string | number
  statusLabel?: string
}
export interface HistoryFilters {
  workOrderCode?: string
  batchCode?: string
  productName?: string
  releaseApprovedTime?: string[]
  tracePerson?: string
  traceEquipment?: string
  traceMaterial?: string
  traceMaterialLot?: string
}
export function buildHistorySources(
  filters: HistoryFilters,
  hasPermission: (permissions: string[]) => boolean
): HistorySource<HistoryRow>[] {
  if (!hasPermission(['mes:pro-edhr-batch-execution:query'])) {
    throw new Error('没有历史追溯记录的查询权限。')
  }
  return [
    {
      load: async (pageNo, pageSize) => {
        const data = await getEdhrBatchHistoryPage({
          pageNo,
          pageSize,
          completedTraceOnly: true,
          excludeStatuses: [50],
          releasedOnly: filters.releaseApprovedTime?.length ? true : undefined,
          workOrderCode: filters.workOrderCode?.trim() || undefined,
          productName: filters.productName?.trim() || undefined,
          batchCode: filters.batchCode?.trim() || undefined,
          releaseApprovedTime: filters.releaseApprovedTime,
          tracePerson: filters.tracePerson?.trim() || undefined,
          traceEquipment: filters.traceEquipment?.trim() || undefined,
          traceMaterial: filters.traceMaterial?.trim() || undefined,
          traceMaterialLot: filters.traceMaterialLot?.trim() || undefined
        })
        return {
          total: data.total,
          list: data.list.map((row: EdhrBatchHistoryRowRespVO): HistoryRow => {
            const status = Number(row.status)
            if (
              status === 50 ||
              (status !== 40 && status !== 60 && row.releaseStatus !== 'RELEASED')
            ) {
              throw new Error('历史追溯接口返回了非已放行或已作废的批记录。')
            }
            return {
              ...row,
              key: 'history:' + row.id,
              batchExecutionId: row.id,
              recordCode: row.batchExecutionCode,
              statusLabel: status === 60 ? '已作废' : '已放行'
            }
          })
        }
      }
    }
  ]
}
