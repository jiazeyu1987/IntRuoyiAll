import type { LocationQuery } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import type { EdhrBatchExecutionPageReqVO } from '@/api/mes/pro/edhr/batchExecution'
import type {
  ReverseTraceCatalogItem, ReverseTraceCategory, ReverseTraceQueryRequest,
  ReverseTraceQueryResponse, ReverseTraceEvidenceResponse
} from '@/api/mes/pro/edhr/reverseTrace'

export const reverseTraceHistoryPath = '/mes/pro/feedback/edhr-batch-history'
export const reverseTraceDetailPath = '/mes/pro/feedback/edhr-batch-execution/active-order-detail'
export const canReverseTrace = () => {
  const permissions = useUserStore().getPermissions
  return permissions.has('*:*:*') || permissions.has('mes:pro-edhr-batch-execution:query')
}
export const reverseTraceIdentity = () => {
  const user = useUserStore()
  const tenant = getTenantId()
  if (!user.getUser.id || tenant == null || !canReverseTrace()) return ''
  return JSON.stringify([String(user.getUser.id), String(tenant), getVisitTenantId() ?? '', [...user.getPermissions].sort()])
}

export interface ReverseTraceConditionRow {
  conditionId: string
  category: ReverseTraceCategory
  item: ReverseTraceCatalogItem
  operator: string
  value: string
}
export interface ReverseTraceSavedState {
  anchorBatchExecutionId: string
  catalogVersion: string
  releaseApprovedTime?: string[]
  activeCategory: ReverseTraceCategory
  conditions: ReverseTraceConditionRow[]
  successfulQuery?: ReverseTraceQueryRequest
  queryResponse?: ReverseTraceQueryResponse
  evidenceResponse?: ReverseTraceEvidenceResponse
  evidenceTarget: string
  evidenceOpen: string[]
  resultPage: number
  resultPageSize: number
  evidencePage: number
  evidencePageSize: number
  scrollTop: number
  dirty: boolean
  origin: { path: string; query: LocationQuery }
  historyListState?: EdhrBatchExecutionPageReqVO
}

// ponytail: page memory only; reload deliberately requires a fresh query, never persisted credentials.
const states = new Map<string, { owner: string; expiresAt: number; state: ReverseTraceSavedState }>()
export const copyReverseTraceState = <T>(value: T): T => JSON.parse(JSON.stringify(value))

export const saveReverseTraceState = (state: ReverseTraceSavedState) => {
  const owner = reverseTraceIdentity()
  if (!owner) throw new Error('当前身份或权限不可用，无法保留反查状态。')
  if (![reverseTraceHistoryPath, reverseTraceDetailPath].includes(state.origin.path)) {
    throw new Error('反查来源页面不在允许的返回范围内。')
  }
  for (const [key, entry] of states) {
    if (entry.expiresAt <= Date.now() || entry.owner !== owner) states.delete(key)
  }
  const key = crypto.randomUUID()
  states.set(key, { owner, expiresAt: Date.now() + 30 * 60 * 1000, state: copyReverseTraceState(state) })
  return key
}

export const readReverseTraceState = (key: unknown) => {
  if (typeof key !== 'string') return undefined
  const entry = states.get(key)
  if (!entry) return undefined
  if (!reverseTraceIdentity() || entry.owner !== reverseTraceIdentity() || entry.expiresAt <= Date.now()) {
    states.delete(key)
    return undefined
  }
  return copyReverseTraceState(entry.state)
}

export const revokeReverseTraceSnapshot = (request: ReverseTraceQueryRequest, queryHash: string) => {
  const owner = reverseTraceIdentity()
  for (const [key, entry] of states) {
    if (entry.owner === owner && entry.state.anchorBatchExecutionId === request.anchorBatchExecutionId
      && entry.state.catalogVersion === request.catalogVersion && entry.state.queryResponse?.queryHash === queryHash) {
      states.delete(key)
    }
  }
}
