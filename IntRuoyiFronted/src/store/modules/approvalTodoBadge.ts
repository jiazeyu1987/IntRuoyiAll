import { defineStore } from 'pinia'
import { getApprovalTaskPage } from '@/api/approval-center'
import { store } from '../index'

const TODO_TOTAL_REFRESH_INTERVAL_MS = 30 * 1000
const APPROVAL_TODO_BADGE_PAGE_SIZE = 10

let pendingTodoTotalRequest: Promise<void> | undefined

export const hasApprovalTodoBadgeRoute = (routes: AppRouteRecordRaw[] = []): boolean =>
  routes.some((route) => Boolean(route.meta?.approvalTodoBadge) || hasApprovalTodoBadgeRoute(route.children || []))

const normalizeTodoTotal = (total: unknown) => {
  const numericTotal = Number(total)
  if (!Number.isFinite(numericTotal) || numericTotal < 0) {
    throw new Error(`审批待办数量异常：${String(total)}`)
  }
  return Math.trunc(numericTotal)
}

const resolveTodoBadgeError = (error: unknown) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  if (typeof error === 'string') {
    return error
  }
  return '审批待办数量加载失败'
}

export const useApprovalTodoBadgeStore = defineStore('approvalTodoBadge', {
  state: () => ({
    todoTotal: 0,
    loaded: false,
    loading: false,
    error: '',
    lastLoadedAt: 0
  }),
  getters: {
    getHasVisibleTodoBadge: (state) => state.loaded && state.todoTotal > 0,
    getTodoBadgeText: (state) =>
      state.loaded && state.todoTotal > 0 ? String(state.todoTotal) : ''
  },
  actions: {
    applyTodoTotal(total: unknown) {
      this.todoTotal = normalizeTodoTotal(total)
      this.loaded = true
      this.error = ''
      this.lastLoadedAt = Date.now()
    },
    async refreshTodoTotal() {
      if (pendingTodoTotalRequest) {
        return pendingTodoTotalRequest
      }

      pendingTodoTotalRequest = (async () => {
        this.loading = true
        this.error = ''
        try {
          const data = await getApprovalTaskPage({
            pageNo: 1,
            pageSize: APPROVAL_TODO_BADGE_PAGE_SIZE,
            viewType: 'TODO'
          })
          this.applyTodoTotal(data.total)
        } catch (error) {
          this.error = resolveTodoBadgeError(error)
          throw error
        } finally {
          this.loading = false
          pendingTodoTotalRequest = undefined
        }
      })()

      return pendingTodoTotalRequest
    },
    async ensureTodoTotalLoaded() {
      if (this.loaded && Date.now() - this.lastLoadedAt < TODO_TOTAL_REFRESH_INTERVAL_MS) {
        return
      }
      await this.refreshTodoTotal()
    }
  }
})

export const useApprovalTodoBadgeStoreWithOut = () => {
  return useApprovalTodoBadgeStore(store)
}
