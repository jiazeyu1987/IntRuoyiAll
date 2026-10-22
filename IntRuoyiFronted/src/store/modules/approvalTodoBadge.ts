import { defineStore } from 'pinia'
import { getApprovalTaskPage } from '@/api/approval-center'
import { checkPermi } from '@/utils/permission'
import { store } from '../index'
import { effectScope, watch } from 'vue'
import { useApprovalCenterInvalidationStore } from './approvalCenterInvalidation'
import { createApprovalBadgeLoader } from './approvalCenterInvalidationState'

const TODO_TOTAL_REFRESH_INTERVAL_MS = 30 * 1000
const APPROVAL_TODO_BADGE_PAGE_SIZE = 10

let badgeLoader: ReturnType<typeof createApprovalBadgeLoader> | undefined
let badgeScopeInitialized = false

export const hasApprovalTodoBadgeRoute = (routes: AppRouteRecordRaw[] = []): boolean =>
  checkPermi(['bpm:task:query']) &&
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
    lastLoadedAt: 0,
    taskListRevision: 0
  }),
  getters: {
    getHasVisibleTodoBadge: (state) => state.loaded && state.todoTotal > 0,
    getTodoBadgeText: (state) =>
      state.loaded && state.todoTotal > 0 ? String(state.todoTotal) : ''
  },
  actions: {
    initializeScope() {
      const invalidation = useApprovalCenterInvalidationStore()
      invalidation.syncSession()
      if (badgeScopeInitialized) return
      badgeScopeInitialized = true
      badgeLoader = createApprovalBadgeLoader({
        contextKey: () => JSON.stringify([invalidation.syncSession().key, invalidation.todoRevision]),
        request: async (isRequestContextCurrent) => (await getApprovalTaskPage(
          { pageNo: 1, pageSize: APPROVAL_TODO_BADGE_PAGE_SIZE, viewType: 'TODO' },
          { ignoreErrorMessage: true, isRequestContextCurrent }
        )).total,
        commit: (total) => this.applyTodoTotal(total),
        start: () => { this.loading = true; this.error = '' },
        fail: (error) => { this.error = resolveTodoBadgeError(error) },
        finish: () => { this.loading = false }
      })
      // The badge belongs to the application session, beyond the first calling component.
      effectScope(true).run(() => {
        watch(() => invalidation.contextKey, () => {
          badgeLoader?.reset()
          this.$reset()
        }, { flush: 'sync' })
        watch(() => invalidation.todoRevision, () => { this.loaded = false }, { flush: 'sync' })
      })
    },
    invalidateTaskLists() {
      this.taskListRevision++
    },
    applyTodoTotal(total: unknown) {
      this.initializeScope()
      this.todoTotal = normalizeTodoTotal(total)
      this.loaded = true
      this.error = ''
      this.lastLoadedAt = Date.now()
    },
    async refreshTodoTotal() {
      this.initializeScope()
      if (!badgeLoader) throw new Error('审批待办徽标请求上下文未初始化')
      return badgeLoader.load()
    },
    async ensureTodoTotalLoaded() {
      this.initializeScope()
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
