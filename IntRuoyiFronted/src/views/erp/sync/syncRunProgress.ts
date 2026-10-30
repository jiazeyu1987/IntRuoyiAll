import type { ErpKingdeeSyncRunVO } from '@/api/erp/sync'

export type SyncRunCountKey = 'createdCount' | 'updatedCount' | 'skippedCount' | 'failedCount'
type DisplayCount = number | '-' | '待统计' | '未统计'

const countKeys: SyncRunCountKey[] = [
  'createdCount',
  'updatedCount',
  'skippedCount',
  'failedCount'
]
const isCount = (value: unknown): value is number =>
  typeof value === 'number' && Number.isSafeInteger(value) && value >= 0

const hasReportedRows = (run: ErpKingdeeSyncRunVO) =>
  countKeys.some((key) => isCount(run[key]) && run[key]! > 0)

/** A newly started run contains zero placeholders until its first progress report. */
export const getSyncRunCount = (
  run: ErpKingdeeSyncRunVO | undefined,
  key: SyncRunCountKey
): DisplayCount => {
  if (!run) return '-'
  const count = run[key]
  if (!isCount(count)) return '未统计'
  if (run.status === 10 && !hasReportedRows(run)) return '待统计'
  // A request/transaction failure does not tell us how many source rows failed.
  if (run.status === 30 && key === 'failedCount' && count === 0) return '未统计'
  return count
}

export const getSyncRunProcessedCount = (run?: ErpKingdeeSyncRunVO): DisplayCount => {
  if (!run) return '-'
  const counts = [run.createdCount, run.updatedCount, run.skippedCount]
  if (!counts.every(isCount)) return '未统计'
  if (run.status === 10 && !hasReportedRows(run)) return '待统计'
  const total = counts.reduce((sum, count) => sum + count, 0)
  return isCount(total) ? total : '未统计'
}

export const describeSyncRunProgress = (run?: ErpKingdeeSyncRunVO): string => {
  if (!run) return '未执行'
  if (run.status === 10) {
    return hasReportedRows(run) ? '已上报部分计数，等待执行结果' : '计数待上报，尚不能判断已处理数量'
  }
  if (run.status === 30) return '任务失败；已提交数量保留，失败行数可能未统计'
  if (run.status === 20) return '执行结束，显示最终计数'
  return '运行状态未知'
}
