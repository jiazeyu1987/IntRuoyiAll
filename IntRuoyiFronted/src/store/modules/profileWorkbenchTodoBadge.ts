import { defineStore } from 'pinia'
import { effectScope, watch } from 'vue'
import {
  getProfileWorkbenchTodoCount,
  type ProfileWorkbenchTodoSourceId
} from '@/api/system/profileWorkbenchTodo'
import { usePermissionStoreWithOut } from '@/store/modules/permission'
import { useUserStoreWithOut } from '@/store/modules/user'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { checkPermi } from '@/utils/permission'
import { store } from '../index'

const TODO_TOTAL_REFRESH_INTERVAL_MS = 30 * 1000

export interface ProfileWorkbenchBadgeScope {
  userId: string
  tenantId: string
  badgeSources: ProfileWorkbenchTodoSourceId[]
}

export interface ProfileWorkbenchBadgeToken extends ProfileWorkbenchBadgeScope {
  epoch: number
}

interface BadgePatch {
  todoTotal?: number
  loaded?: boolean
  loading?: boolean
  error?: string
  lastLoadedAt?: number
}

let pendingTodoTotalRequest:
  | { token: ProfileWorkbenchBadgeToken; promise: Promise<void> }
  | undefined
let contextWatchStarted = false

export const hasProfileWorkbenchTodoBadgeRoute = (routes: AppRouteRecordRaw[] = []): boolean =>
  routes.some((route) =>
    Boolean(
      route.meta?.personalWorkbenchTodoBadge ||
        hasProfileWorkbenchTodoBadgeRoute(route.children || [])
    )
  )

const hasRouteName = (routes: AppRouteRecordRaw[] = [], targetName: string): boolean =>
  routes.some((route) => route.name === targetName || hasRouteName(route.children || [], targetName))

export const normalizeProfileWorkbenchSources = (sources: ProfileWorkbenchTodoSourceId[]) =>
  [...new Set(sources)].sort()

export const resolveProfileWorkbenchBadgeScope = (): ProfileWorkbenchBadgeScope => {
  const userStore = useUserStoreWithOut()
  const permissionStore = usePermissionStoreWithOut()
  const sources: ProfileWorkbenchTodoSourceId[] = []
  if (checkPermi(['dcc:controlled-file:query'])) sources.push('DCC_DISTRIBUTION')
  if (checkPermi(['dcc:controlled-file:training:mine'])) sources.push('DCC_TRAINING')
  if (
    checkPermi(['mes:pro-edhr-work-task:query']) ||
    checkPermi(['mes:pro-edhr-batch-execution:query'])
  ) sources.push('EDHR_WORK_TASK')
  if (checkPermi(['mes:pro-work-order:query'])) sources.push('WORK_ORDER')
  if (
    userStore.getRoles.includes('super_admin') ||
    hasRouteName(permissionStore.getRouters, 'ShowroomAdminAssignment')
  ) sources.push('SHOWROOM_ASSIGNMENT')
  return {
    userId: String(userStore.getUser.id),
    tenantId: JSON.stringify([String(getTenantId() ?? ''), String(getVisitTenantId() ?? '')]),
    badgeSources: normalizeProfileWorkbenchSources(sources)
  }
}

export const profileWorkbenchBadgeScopeKey = (scope: ProfileWorkbenchBadgeScope) =>
  JSON.stringify([scope.userId, scope.tenantId, normalizeProfileWorkbenchSources(scope.badgeSources)])

export const requireProfileWorkbenchTodoTotal = (total: unknown): number => {
  if (typeof total !== 'number' || !Number.isSafeInteger(total) || total < 0) {
    throw new Error(`个人工作台待处理数量异常：${String(total)}`)
  }
  return total
}

const resolveTodoBadgeError = (error: unknown) => {
  if (error instanceof Error && error.message) return error.message
  if (typeof error === 'string') return error
  return '个人工作台待处理数量加载失败'
}

export const useProfileWorkbenchTodoBadgeStore = defineStore('profileWorkbenchTodoBadge', {
  state: () => ({
    todoTotal: 0,
    loaded: false,
    loading: false,
    error: '',
    lastLoadedAt: 0,
    badgeUpdateEpoch: 0,
    scopeKey: ''
  }),
  getters: {
    getHasVisibleTodoBadge: (state) =>
      state.loaded && state.todoTotal > 0 &&
      state.scopeKey === profileWorkbenchBadgeScopeKey(resolveProfileWorkbenchBadgeScope()),
    getTodoBadgeText: (state) =>
      state.loaded && state.todoTotal > 0 &&
      state.scopeKey === profileWorkbenchBadgeScopeKey(resolveProfileWorkbenchBadgeScope())
        ? String(state.todoTotal) : ''
  },
  actions: {
    ensureContextWatch() {
      if (contextWatchStarted) return
      contextWatchStarted = true
      // Epoch survives logout so a previous session's requests cannot become current again.
      effectScope(true).run(() => {
        watch(
          () => profileWorkbenchBadgeScopeKey(resolveProfileWorkbenchBadgeScope()),
          () => { this.beginBadgeUpdate() },
          { flush: 'sync' }
        )
      })
    },
    beginBadgeUpdate(): ProfileWorkbenchBadgeToken {
      this.ensureContextWatch()
      const scope = resolveProfileWorkbenchBadgeScope()
      const token = { ...scope, epoch: ++this.badgeUpdateEpoch }
      this.scopeKey = profileWorkbenchBadgeScopeKey(scope)
      this.tryCommit(token, { loaded: false, loading: false, error: '', lastLoadedAt: 0 })
      return token
    },
    isCurrentToken(token: ProfileWorkbenchBadgeToken) {
      return token.epoch === this.badgeUpdateEpoch &&
        profileWorkbenchBadgeScopeKey(token) === this.scopeKey &&
        this.scopeKey === profileWorkbenchBadgeScopeKey(resolveProfileWorkbenchBadgeScope())
    },
    tryCommit(token: ProfileWorkbenchBadgeToken, patch: BadgePatch) {
      if (!this.isCurrentToken(token)) return false
      this.$patch(patch)
      return true
    },
    applyTodoTotal(token: ProfileWorkbenchBadgeToken, total: unknown) {
      if (!this.isCurrentToken(token)) return false
      return this.tryCommit(token, {
        todoTotal: requireProfileWorkbenchTodoTotal(total),
        loaded: true,
        loading: false,
        error: '',
        lastLoadedAt: Date.now()
      })
    },
    async refreshTodoTotal() {
      if (pendingTodoTotalRequest && this.isCurrentToken(pendingTodoTotalRequest.token)) {
        return pendingTodoTotalRequest.promise
      }
      const token = this.beginBadgeUpdate()
      this.tryCommit(token, { loading: true })
      const promise = (async () => {
        try {
          const result = await getProfileWorkbenchTodoCount(token.badgeSources)
          this.applyTodoTotal(token, result.businessTotal)
        } catch (error) {
          this.tryCommit(token, { loaded: false, error: resolveTodoBadgeError(error) })
          throw error
        } finally {
          this.tryCommit(token, { loading: false })
          if (pendingTodoTotalRequest?.token === token) pendingTodoTotalRequest = undefined
        }
      })()
      pendingTodoTotalRequest = { token, promise }
      return promise
    },
    async ensureTodoTotalLoaded() {
      this.ensureContextWatch()
      const scopeKey = profileWorkbenchBadgeScopeKey(resolveProfileWorkbenchBadgeScope())
      if (
        this.loaded && this.scopeKey === scopeKey &&
        Date.now() - this.lastLoadedAt < TODO_TOTAL_REFRESH_INTERVAL_MS
      ) return
      await this.refreshTodoTotal()
    }
  }
})

export const useProfileWorkbenchTodoBadgeStoreWithOut = () =>
  useProfileWorkbenchTodoBadgeStore(store)
