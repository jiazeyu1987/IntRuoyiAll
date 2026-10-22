import request from '@/config/axios'
import type { ActionEnvelope, SourceDescriptor } from '@/workbench/contracts'
import { workbenchQueryContractVersion, type WorkbenchQueryContractVersion } from '@/workbench/queryContract'

export type ProfileWorkbenchTodoSourceId = string
export interface ProfileWorkbenchTodoQuery {
  pageNo: number
  pageSize: number
  visibility: 'visible' | 'hidden'
  enabledSources: string[]
  taskType?: string
  quickFilter?: { fieldKey: string; operator: 'contains' | 'eq'; value?: string }
  sort?: { key: string; order: 'asc' | 'desc' }
}
export interface WorkbenchQuerySpec {
  visibility: 'visible' | 'hidden'
  taskType?: string | null
  quickFilter?: { fieldKey: string; operator: 'contains' | 'eq'; value: string } | null
  sort?: { key: string; order: 'asc' | 'desc' } | null
}
export interface WorkbenchContext {
  contractVersion: WorkbenchQueryContractVersion
  registryRevision: string
  sources: string[]
  contextEpoch: string
  visibilityVersion: string
  queryVersion: string
  expiresAt: string
  querySpec: WorkbenchQuerySpec
}
export interface ProfileWorkbenchTodoRow {
  sourceId: string
  taskKey: string
  businessId: string
  taskType: string
  source: string
  detail: string
  statusLabel: string
  createdAt: string | null
  dueAt: string | null
  action: ActionEnvelope
}
export interface ProfileWorkbenchTodoCount {
  contractVersion: WorkbenchQueryContractVersion
  registryRevision: string
  queryVersion: string
  contextEpoch: string
  visibilityVersion: string
  filteredTotal: number
  businessTotal: number
  hiddenTotal: number
  readAt: string
}
export interface ProfileWorkbenchTodoPage extends ProfileWorkbenchTodoCount {
  rows: ProfileWorkbenchTodoRow[]
  effectivePageNo: number
  pageSize: number
}
const requireCount = (value: unknown) => {
  if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < 0) throw new Error('工作台数量合同无效。')
}
const requireVersion = (value: unknown) => {
  if (typeof value !== 'string' || !/^[1-9]\d*$/.test(value)) throw new Error('工作台个人版本合同无效。')
}
const localDateTime = (value: unknown) => typeof value === 'string' &&
  /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?$/.test(value)
export const normalizeWorkbenchQuerySpec = (query: WorkbenchQuerySpec): WorkbenchQuerySpec => {
  if (!query || !['visible', 'hidden'].includes(query.visibility)) throw new Error('工作台显示分区无效。')
  if (query.taskType != null && (typeof query.taskType !== 'string' || !query.taskType.trim() || query.taskType.length > 100)) throw new Error('工作台任务类型无效。')
  const filter = query.quickFilter
  if (filter && (!['source', 'detail', 'statusLabel'].includes(filter.fieldKey) ||
    !['contains', 'eq'].includes(filter.operator) || typeof filter.value !== 'string' || filter.value.length > 1000)) throw new Error('工作台筛选条件无效。')
  if (query.sort && (!['taskType', 'source', 'detail', 'statusLabel'].includes(query.sort.key) ||
    !['asc', 'desc'].includes(query.sort.order))) throw new Error('工作台排序无效。')
  return { visibility: query.visibility, taskType: query.taskType ?? null,
    quickFilter: filter ? { ...filter, value: filter.value.trim() } : null, sort: query.sort ?? null }
}
export const initializeWorkbenchContext = async (registryRevision: string, sources: SourceDescriptor[], contextEpoch: string, querySpec: WorkbenchQuerySpec) => {
  const spec = normalizeWorkbenchQuerySpec(querySpec)
  const ids = [...sources].sort((a, b) => a.sortRank - b.sortRank).map(source => source.sourceId)
  const result = await request.post<WorkbenchContext>({ url: '/workbench/context',
    data: { registryRevision, sources: ids, contextEpoch, querySpec: spec }, ignoreErrorMessage: true, skipAuthRefreshReplay: true })
  if (!result || result.contractVersion !== workbenchQueryContractVersion || result.registryRevision !== registryRevision ||
    result.contextEpoch !== contextEpoch || !Array.isArray(result.sources) || result.sources.length !== ids.length ||
    result.sources.some((id, index) => id !== ids[index]) ||
    typeof result.queryVersion !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(result.queryVersion) ||
    typeof result.expiresAt !== 'string' || !/Z$/.test(result.expiresAt) || !Number.isFinite(Date.parse(result.expiresAt)) || Date.parse(result.expiresAt) <= Date.now() ||
    JSON.stringify(normalizeWorkbenchQuerySpec(result.querySpec)) !== JSON.stringify(spec)) throw new Error('工作台查询上下文合同不一致。')
  requireVersion(result.visibilityVersion)
  return result
}
const validateCount = (result: ProfileWorkbenchTodoCount, context: WorkbenchContext) => {
  if (!result || context.contractVersion !== workbenchQueryContractVersion || result.contractVersion !== context.contractVersion || result.registryRevision !== context.registryRevision ||
    result.queryVersion !== context.queryVersion || result.contextEpoch !== context.contextEpoch ||
    result.visibilityVersion !== context.visibilityVersion || !localDateTime(result.readAt)) throw new Error('工作台读取归属合同不一致。')
  for (const field of ['filteredTotal', 'businessTotal', 'hiddenTotal'] as const) requireCount(result[field])
  if (result.hiddenTotal > result.businessTotal || result.filteredTotal >
    (context.querySpec.visibility === 'hidden' ? result.hiddenTotal : result.businessTotal - result.hiddenTotal)) throw new Error('工作台数量分区合同无效。')
}
const paramsFor = (context: WorkbenchContext) => ({ queryVersion: context.queryVersion, contextEpoch: context.contextEpoch })
export const getProfileWorkbenchTodoPage = async (context: WorkbenchContext, pageNo: number, pageSize: number, sources: SourceDescriptor[]) => {
  const page = await request.get<ProfileWorkbenchTodoPage>({ url: '/workbench/tasks/page',
    params: { ...paramsFor(context), pageNo, pageSize }, ignoreErrorMessage: true, skipAuthRefreshReplay: true })
  validateCount(page, context)
  if (!Array.isArray(page.rows) || !Number.isSafeInteger(page.effectivePageNo) || page.effectivePageNo < 1 ||
    page.pageSize !== pageSize || pageSize < 10 || pageSize > 100 ||
    page.effectivePageNo > Math.max(1, Math.ceil(page.filteredTotal / pageSize)) ||
    page.rows.length !== Math.min(pageSize, Math.max(0, page.filteredTotal - (page.effectivePageNo - 1) * pageSize))) throw new Error('工作台分页合同无效。')
  const keys = new Set<string>()
  for (const row of page.rows) {
    const descriptor = sources.find(source => source.sourceId === row?.sourceId)
    if (!descriptor || !context.sources.includes(row.sourceId) || typeof row.businessId !== 'string' || !row.businessId.trim() ||
      row.taskKey !== `${row.sourceId}:${row.businessId}` || row.taskKey.length > 160 || keys.has(row.taskKey) ||
      row.taskType !== descriptor.taskType || row.source !== descriptor.label || typeof row.detail !== 'string' || typeof row.statusLabel !== 'string' ||
      ![row.createdAt, row.dueAt].every(value => value === null || localDateTime(value)) ||
      row.action?.actionKind !== descriptor.actionKind || row.action?.payloadVersion !== descriptor.payloadVersion ||
      !row.action.payload || typeof row.action.payload !== 'object' || Array.isArray(row.action.payload) ||
      Object.values(row.action.payload).some(value => typeof value !== 'string')) throw new Error('工作台任务行合同无效。')
    keys.add(row.taskKey)
  }
  return page
}
export const getProfileWorkbenchTodoCount = async (context: WorkbenchContext) => {
  const result = await request.get<ProfileWorkbenchTodoCount>({ url: '/workbench/tasks/count', params: paramsFor(context), ignoreErrorMessage: true, skipAuthRefreshReplay: true })
  validateCount(result, context)
  return result
}
export const setProfileWorkbenchTaskVisibility = async (context: WorkbenchContext, taskKey: string, operation: 'hide' | 'restore') => {
  const result = await request.post<{ changed: boolean; visibilityVersion: string; contextEpoch: string }>({
    url: '/workbench/tasks/visibility', data: { ...paramsFor(context), taskKey, operation }, ignoreErrorMessage: true, skipAuthRefreshReplay: true })
  if (!result || typeof result.changed !== 'boolean' || result.contextEpoch !== context.contextEpoch) throw new Error('工作台写入响应合同无效，请重新读取确认状态。')
  requireVersion(result.visibilityVersion)
  return result
}
