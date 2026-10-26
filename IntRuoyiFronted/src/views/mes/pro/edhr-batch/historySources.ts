import {
  getEdhrBatchExecutionPage,
  type EdhrBatchExecutionRespVO
} from '@/api/mes/pro/edhr/batchExecution'
import { type HistorySource } from './historyPagination'

export const historyTypes = [
  { value: 'execution', label: '批次执行', permission: 'mes:pro-edhr-batch-execution:query' },
  { value: 'history', label: '历史追溯', permission: 'mes:pro-edhr-batch-execution:query' }
] as const
export type HistoryType = (typeof historyTypes)[number]['value']
export interface HistoryRow {
  key: string
  id: string | number
  recordType: HistoryType
  typeLabel: string
  recordCode?: string
  batchExecutionId?: number
  batchExecutionCode?: string
  releaseTransactionId?: number
  workOrderCode?: string
  batchCode?: string
  productName?: string
  occurredAt?: string | number
  statusLabel?: string
}
export interface HistoryFilters {
  recordType: HistoryType | ''
  workOrderCode?: string
  batchCode?: string
  productName?: string
  releaseApprovedTime?: string[]
}
const batchStatuses: Record<number, string> = {
  0: '已创建',
  10: '执行中',
  15: '冻结中',
  20: '待关闭',
  25: '需返工',
  30: '已关闭',
  40: '已归档',
  50: '质量终态',
  60: '已作废'
}

export function buildHistorySources(
  filters: HistoryFilters,
  hasPermission: (permissions: string[]) => boolean
): HistorySource<HistoryRow>[] {
  const allowed = historyTypes.filter((item) => hasPermission([item.permission]))
  if (filters.recordType && !allowed.some((item) => item.value === filters.recordType))
    throw new Error('没有查询该类型记录的权限。')
  if (!allowed.length) throw new Error('没有历史追溯记录的查询权限。')
  const selected = filters.recordType
  const query = selected
    ? {
        workOrderCode: filters.workOrderCode?.trim() || undefined,
        batchCode: filters.batchCode?.trim() || undefined
      }
    : {}
  return allowed
    .filter((item) => !selected || selected === item.value)
    .map((item) => ({
      load: async (pageNo, pageSize) => {
        const page = { pageNo, pageSize }
        const wrap = (row: Omit<HistoryRow, 'key' | 'recordType' | 'typeLabel'>): HistoryRow => ({
          ...row,
          key: item.value + ':' + row.id,
          recordType: item.value,
          typeLabel: item.label
        })
        const data = await getEdhrBatchExecutionPage({
          ...page,
          ...query,
          productName: selected ? filters.productName?.trim() || undefined : undefined,
          ...(item.value === 'history'
            ? {
                releasedOnly: true,
                releaseApprovedTime: selected ? filters.releaseApprovedTime : undefined
              }
            : { excludeStatuses: [40, 50, 60], excludeReleased: true })
        })
        return {
          total: data.total,
          list: data.list.map((row: EdhrBatchExecutionRespVO) =>
            wrap({
              ...row,
              batchExecutionId: row.id,
              recordCode: row.batchExecutionCode,
              occurredAt: item.value === 'history' ? row.releaseApprovedAt : row.createTime,
              statusLabel:
                Number(row.status) === 40
                  ? '已归档'
                  : row.releaseStatus === 'RELEASED'
                    ? '已放行'
                    : batchStatuses[Number(row.status)] || String(row.status ?? '--')
            })
          )
        }
      }
    }))
}
