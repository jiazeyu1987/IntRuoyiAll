import { defineStore } from 'pinia'
import { effectScope, watch } from 'vue'
import { getProfileWorkbenchTodoCount, initializeWorkbenchContext } from '@/api/system/profileWorkbenchTodo'
import { authorizedWorkbenchSources, refreshWorkbenchDirectory, workbenchDirectoryRevision, workbenchIdentityKey } from '@/workbench/registry'
import { beginWorkbenchRead, isWorkbenchReadCurrent, workbenchInvalidationReason, workbenchLifetime, type WorkbenchReadLease } from '@/workbench/queryContext'
import { store } from '../index'

export const hasProfileWorkbenchTodoBadgeRoute = (routes: AppRouteRecordRaw[] = []): boolean =>
  routes.some(route => Boolean(route.meta?.personalWorkbenchTodoBadge || hasProfileWorkbenchTodoBadgeRoute(route.children || [])))
export const normalizeProfileWorkbenchSources = (sources: string[]) => [...new Set(sources)].sort()
export interface ProfileWorkbenchBadgeScope {
  badgeSources: string[]
  registryRevision: string
  authorizationKey: string
}
export interface ProfileWorkbenchBadgeToken extends ProfileWorkbenchBadgeScope {
  epoch: number
  lease: WorkbenchReadLease
}
interface BadgePatch {
  todoTotal?: number
  loaded?: boolean
  loading?: boolean
  error?: string
  lastLoadedAt?: number
}
export const resolveProfileWorkbenchBadgeScope = (): ProfileWorkbenchBadgeScope => ({
  badgeSources: normalizeProfileWorkbenchSources(authorizedWorkbenchSources().map(source => source.sourceId)),
  registryRevision: workbenchDirectoryRevision(), authorizationKey: workbenchIdentityKey()
})
export const profileWorkbenchBadgeScopeKey = (scope: ProfileWorkbenchBadgeScope) =>
  JSON.stringify([scope.badgeSources, scope.registryRevision, scope.authorizationKey])
export const requireProfileWorkbenchTodoTotal = (total: unknown): number => {
  if (typeof total !== 'number' || !Number.isSafeInteger(total) || total < 0) throw new Error('工作台待处理数量无效。')
  return total
}
const errorMessage = (error: unknown) => error instanceof Error ? error.message : String(error)
export const useProfileWorkbenchTodoBadgeStore = defineStore('profileWorkbenchTodoBadge', {
  state: () => ({
    todoTotal: undefined as number | undefined, loaded: false, loading: false, error: '',
    lastLoadedAt: 0, badgeUpdateEpoch: 0, scopeKey: '', watching: false
  }),
  getters: {
    getHasVisibleTodoBadge: state => state.loaded && state.todoTotal !== undefined && state.todoTotal > 0 &&
      state.scopeKey === profileWorkbenchBadgeScopeKey(resolveProfileWorkbenchBadgeScope()),
    getTodoBadgeText: state => state.loaded && state.todoTotal !== undefined && state.todoTotal > 0 &&
      state.scopeKey === profileWorkbenchBadgeScopeKey(resolveProfileWorkbenchBadgeScope()) ? String(state.todoTotal) : ''
  },
  actions: {
    ensureContextWatch() {
      if (this.watching) return
      this.watching = true
      effectScope(true).run(() => {
        watch(workbenchLifetime, () => {
          this.beginBadgeUpdate()
          this.error = workbenchInvalidationReason.value
        }, { flush: 'sync' })
      })
    },
    beginBadgeUpdate(): ProfileWorkbenchBadgeToken {
      this.ensureContextWatch()
      const lease = beginWorkbenchRead()
      const scope = resolveProfileWorkbenchBadgeScope()
      const token = { ...scope, lease, epoch: ++this.badgeUpdateEpoch }
      this.scopeKey = profileWorkbenchBadgeScopeKey(scope)
      this.todoTotal = undefined
      this.loaded = false
      this.loading = false
      this.error = ''
      this.lastLoadedAt = 0
      return token
    },
    isCurrentToken(token: ProfileWorkbenchBadgeToken) {
      return isWorkbenchReadCurrent(token.lease) && token.epoch === this.badgeUpdateEpoch &&
        profileWorkbenchBadgeScopeKey(token) === this.scopeKey
    },
    bindDirectory(token: ProfileWorkbenchBadgeToken) {
      if (!this.isCurrentToken(token)) return false
      Object.assign(token, resolveProfileWorkbenchBadgeScope())
      this.scopeKey = profileWorkbenchBadgeScopeKey(token)
      return true
    },
    tryCommit(token: ProfileWorkbenchBadgeToken, patch: BadgePatch) {
      if (!this.isCurrentToken(token)) return false
      this.$patch(patch)
      return true
    },
    applyTodoTotal(token: ProfileWorkbenchBadgeToken, total: unknown) {
      return this.tryCommit(token, { todoTotal: requireProfileWorkbenchTodoTotal(total),
        loaded: true, loading: false, error: '', lastLoadedAt: Date.now() })
    },
    async refreshTodoTotal() {
      const token = this.beginBadgeUpdate()
      this.tryCommit(token, { loading: true })
      try {
        const directory = await refreshWorkbenchDirectory()
        if (!this.bindDirectory(token)) return
        const context = await initializeWorkbenchContext(directory.registryRevision, directory.sources,
          token.lease.contextEpoch, { visibility: 'visible' })
        if (!this.isCurrentToken(token)) return
        const result = await getProfileWorkbenchTodoCount(context)
        if (!this.isCurrentToken(token)) return
        this.applyTodoTotal(token, result.businessTotal)
      } catch (error) {
        if (!this.isCurrentToken(token)) return
        this.tryCommit(token, { loaded: false, error: errorMessage(error) })
        throw error
      } finally {
        this.tryCommit(token, { loading: false })
      }
    },
    async ensureTodoTotalLoaded() {
      this.ensureContextWatch()
      if (this.loaded && this.scopeKey === profileWorkbenchBadgeScopeKey(resolveProfileWorkbenchBadgeScope()) &&
        Date.now() - this.lastLoadedAt < 30 * 1000) return
      await this.refreshTodoTotal()
    }
  }
})
export const useProfileWorkbenchTodoBadgeStoreWithOut = () => useProfileWorkbenchTodoBadgeStore(store)
