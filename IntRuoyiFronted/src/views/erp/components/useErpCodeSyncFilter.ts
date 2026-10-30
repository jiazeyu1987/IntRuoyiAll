import type { TableQuickFilterState } from '@/hooks/web/useTableQuickFilter'

export const useErpCodeSyncFilter = (
  query: Record<string, any>,
  quickFilterState: TableQuickFilterState,
  reload: () => Promise<void>,
  resetColumnFilters: () => void
) => {
  const clearFilters = () => {
    resetColumnFilters()
    for (const key of Object.keys(query)) {
      if (key !== 'pageNo' && key !== 'pageSize') query[key] = undefined
    }
    query.pageNo = 1
    Object.assign(quickFilterState, {
      value: undefined,
      conditions: [],
      appliedConditions: [],
      activeConditionId: undefined
    })
  }
  const handleCodeSynced = async (code: string) => {
    clearFilters()
    query.syncCode = code
    await reload()
  }
  const clearCodeSyncFilter = async () => {
    clearFilters()
    await reload()
  }
  return { handleCodeSynced, clearCodeSyncFilter }
}
