import { shallowRef, watch } from 'vue'
import { authContextRevision, getAccessToken, getTenantId, getVisitTenantId } from '@/utils/auth'
import { useUserStoreWithOut } from '@/store/modules/user'

// Shared lifetime and consumer leases are distinct: a badge read cannot revoke a page read.
export const workbenchLifetime = shallowRef(0)
export const workbenchInvalidationReason = shallowRef('')
let lastIdentity = ''
let sessionIncarnation = 0
let directoryBinding = ''
const readIdentity = () => {
  const user = useUserStoreWithOut()
  return JSON.stringify([authContextRevision.value, Boolean(getAccessToken()), String(user.getUser.id), String(getTenantId() ?? ''),
    String(getVisitTenantId() ?? ''), [...user.getPermissions].sort(), [...user.getRoles].sort()])
}
export const workbenchContextIdentity = () => {
  const identity = readIdentity()
  if (identity !== lastIdentity) {
    lastIdentity = identity
    sessionIncarnation += 1
    directoryBinding = ''
    workbenchLifetime.value += 1
  }
  return String(sessionIncarnation)
}
watch(readIdentity, () => { workbenchContextIdentity() }, { flush: 'sync' })
export const invalidateWorkbenchContext = (reason = '') => {
  workbenchContextIdentity()
  workbenchInvalidationReason.value = reason
  workbenchLifetime.value += 1
}
export const bindWorkbenchDirectoryContext = (registryRevision: string, sources: string[]) => {
  const binding = JSON.stringify([registryRevision, sources])
  if (directoryBinding && binding !== directoryBinding) {
    invalidateWorkbenchContext('工作台授权来源已变化，旧查询已失效，请点击刷新重新加载。')
  }
  directoryBinding = binding
}
export interface WorkbenchReadLease {
  lifetime: number
  identity: string
  contextEpoch: string
}
export const beginWorkbenchRead = (): WorkbenchReadLease => {
  const identity = workbenchContextIdentity()
  return { lifetime: workbenchLifetime.value, identity, contextEpoch: crypto.randomUUID() }
}
export const isWorkbenchReadCurrent = (lease: WorkbenchReadLease) =>
  lease.identity === workbenchContextIdentity() && lease.lifetime === workbenchLifetime.value

// One DOM listener pair; the mounted page owns refresh, otherwise the menu owns it.
const focusConsumers = new Map<symbol, { page: boolean; refresh: () => void }>()
const dispatchFocusRefresh = () => {
  if (document.visibilityState !== 'visible') return
  const consumers = [...focusConsumers.values()]
  const consumer = consumers.find(value => value.page) ?? consumers[0]
  consumer?.refresh()
}
export const registerWorkbenchFocusRefresh = (page: boolean, refresh: () => void) => {
  const key = Symbol()
  if (!focusConsumers.size) {
    window.addEventListener('focus', dispatchFocusRefresh)
    document.addEventListener('visibilitychange', dispatchFocusRefresh)
  }
  focusConsumers.set(key, { page, refresh })
  return () => {
    focusConsumers.delete(key)
    if (!focusConsumers.size) {
      window.removeEventListener('focus', dispatchFocusRefresh)
      document.removeEventListener('visibilitychange', dispatchFocusRefresh)
    }
  }
}
