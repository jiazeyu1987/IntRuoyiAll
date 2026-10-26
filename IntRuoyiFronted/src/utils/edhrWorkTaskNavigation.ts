import type { Router } from 'vue-router'
import { requireExactJavaLong } from '@/utils/exactJavaLong'
import { openEdhrBatchTask } from '@/api/mes/pro/edhr/batchExecution'
import type { EdhrBatchExecutionTaskOpenRespVO } from '@/api/mes/pro/edhr/batchExecution'

export const EDHR_BATCH_EXECUTION_DETAIL_PATH = '/mes/pro/feedback/edhr-batch-execution/detail'
export const EDHR_BATCH_EXECUTION_LIST_PATH = '/mes/pro/feedback/edhr-batch-execution'
export const EDHR_EXECUTION_DETAIL_PATH = '/mes/pro/feedback/edhr-execution/detail'
export const EDHR_EXECUTION_FORM_PATH = '/mes/pro/feedback/edhr-execution/form'
export const EDHR_APPROVAL_DETAIL_PATH = '/mes/pro/feedback/edhr-approval/detail'
export const PQC_PRODUCTION_RELEASE_PATH = '/mes/production-release/pqc'
export const EDHR_FILL_CARRIER_FORM = 'FORM'
export const EDHR_RECORD_CATEGORY_BATCH_RECORD = 'BATCH_RECORD'
export const EDHR_FORM_SLOT_TYPE_MAIN = 'MAIN'

export const EDHR_WORK_TASK_NOTIFY_PATHS = new Set([
  EDHR_BATCH_EXECUTION_LIST_PATH,
  EDHR_BATCH_EXECUTION_DETAIL_PATH,
  EDHR_EXECUTION_DETAIL_PATH,
  EDHR_EXECUTION_FORM_PATH,
  EDHR_APPROVAL_DETAIL_PATH,
  PQC_PRODUCTION_RELEASE_PATH
])

export type EdhrWorkTaskRouteLike = {
  id?: number | string
  taskType?: string
  actionUrl?: string
  batchExecutionId?: number | string
  batchTaskId?: number | string
  executionId?: number | string
  businessScopeType?: string
  businessScopeId?: number | string
}

export type NormalizedEdhrWorkTaskRoute = {
  path: string
  query: Record<string, string>
  href: string
}

const EDHR_EXECUTION_PATHS = new Set([EDHR_EXECUTION_DETAIL_PATH, EDHR_EXECUTION_FORM_PATH])

const normalizeTaskType = (taskType?: string) => String(taskType || '').toUpperCase()

const isFillTask = (taskType?: string) => normalizeTaskType(taskType) === 'FILL'

const isFillOrReworkTask = (taskType?: string) => {
  const normalized = normalizeTaskType(taskType)
  return normalized === 'FILL' || normalized === 'REWORK'
}

const isRouteValuePresent = (value?: number | string | null) =>
  value !== undefined && value !== null && value !== ''

const setSearchParamIfPresent = (
  params: URLSearchParams,
  key: string,
  value?: number | string | null
) => {
  if (!isRouteValuePresent(value)) return
  params.set(key, String(value))
}

const parseInternalActionUrl = (item: EdhrWorkTaskRouteLike, origin: string) => {
  if (!item.actionUrl) {
    throw new Error(`eDHR 工作任务 ${item.id || ''} 缺少处理入口。`)
  }
  const url = new URL(item.actionUrl, origin)
  if (url.origin !== origin) {
    throw new Error(`eDHR 工作任务 ${item.id || ''} 处理入口不是当前系统路由。`)
  }
  return url
}

const buildStructuredBatchFillTaskUrl = (item: EdhrWorkTaskRouteLike, origin: string) => {
  const url = new URL(EDHR_BATCH_EXECUTION_DETAIL_PATH, origin)
  setSearchParamIfPresent(url.searchParams, 'batchExecutionId', item.batchExecutionId)
  setSearchParamIfPresent(url.searchParams, 'batchTaskId', item.batchTaskId)
  setSearchParamIfPresent(url.searchParams, 'workTaskId', item.id)
  return url
}

const hasStructuredBatchFillTaskContext = (item: EdhrWorkTaskRouteLike) =>
  isRouteValuePresent(item.batchExecutionId) && isRouteValuePresent(item.batchTaskId)

const requireTaskIdentity = (value: number | string | null | undefined, label: string) => {
  return requireExactJavaLong(value, `eDHR 工作任务${label}`)
}

const resolveMarketReleaseTaskUrl = (item: EdhrWorkTaskRouteLike, url: URL, origin: string) => {
  const actions = url.searchParams.getAll('action')
  if (actions.length > 1) {
    throw new Error('工作任务入口动作必须唯一。')
  }
  const isReleaseTask = normalizeTaskType(item.taskType) === 'RELEASE_APPROVE'
  const isMarketEntry = url.pathname === EDHR_BATCH_EXECUTION_LIST_PATH &&
    actions.includes('marketRelease')
  if (!isReleaseTask && !isMarketEntry) return url
  if (isMarketEntry && item.taskType && !isReleaseTask) {
    throw new Error('上市放行入口与明确的工作任务类型不一致。')
  }
  if (isReleaseTask && item.businessScopeType !== 'RELEASE_TRANSACTION') {
    throw new Error('上市放行任务缺少正式放行事务范围。')
  }
  if (isReleaseTask && ![EDHR_BATCH_EXECUTION_DETAIL_PATH, EDHR_BATCH_EXECUTION_LIST_PATH].includes(url.pathname)) {
    throw new Error('上市放行任务处理入口与正式任务类型不一致。')
  }
  const batchExecutionId = requireTaskIdentity(
    isReleaseTask ? item.batchExecutionId : url.searchParams.get('batchExecutionId'), '批次执行编号'
  )
  const releaseTransactionId = requireTaskIdentity(
    isReleaseTask ? item.businessScopeId : url.searchParams.get('releaseTransactionId'), '放行事务编号'
  )
  const workTaskId = requireTaskIdentity(isReleaseTask ? item.id : url.searchParams.get('workTaskId'), '工作任务编号')
  const identities = { batchExecutionId, releaseTransactionId, workTaskId }
  for (const [key, expected] of Object.entries(identities)) {
    const values = url.searchParams.getAll(key)
    if (values.length > 1 || (values.length === 1 && values[0] !== expected)) {
      throw new Error(`上市放行任务入口${key}与正式任务身份不一致。`)
    }
  }
  const legacyIds = url.searchParams.getAll('id')
  if (legacyIds.length > 1 || (legacyIds.length === 1 && legacyIds[0] !== batchExecutionId)) {
    throw new Error('上市放行任务入口批次与正式任务身份不一致。')
  }
  if (actions.length === 1 && actions[0] !== 'marketRelease') {
    throw new Error('上市放行任务入口动作不唯一或不一致。')
  }
  const target = new URL(EDHR_BATCH_EXECUTION_LIST_PATH, origin)
  Object.entries(identities).forEach(([key, value]) => target.searchParams.set(key, value))
  target.searchParams.set('action', 'marketRelease')
  return target
}

const resolveTaskNavigationUrl = (item: EdhrWorkTaskRouteLike, origin: string) => {
  if (item.actionUrl) {
    const url = resolveMarketReleaseTaskUrl(item, parseInternalActionUrl(item, origin), origin)
    if (isRouteValuePresent(item.id)) {
      const workTaskId = requireTaskIdentity(item.id, '工作任务编号')
      const existing = url.searchParams.getAll('workTaskId')
      if (existing.length > 1 || (existing.length === 1 && existing[0] !== workTaskId)) {
        throw new Error('工作任务入口与正式任务编号不一致。')
      }
      url.searchParams.set('workTaskId', workTaskId)
    }
    if (normalizeTaskType(item.taskType) === 'PQC_PRODUCTION_RELEASE') {
      const applicationId = requireTaskIdentity(item.businessScopeId, '生产放行申请编号')
      if (item.businessScopeType !== 'RELEASE_APPLICATION' || url.pathname !== PQC_PRODUCTION_RELEASE_PATH ||
        url.searchParams.getAll('applicationId').length !== 1 || url.searchParams.get('applicationId') !== applicationId) {
        throw new Error('生产放行入口与正式申请身份不一致。')
      }
    }
    return url
  }
  if (isFillTask(item.taskType) && hasStructuredBatchFillTaskContext(item)) {
    return buildStructuredBatchFillTaskUrl(item, origin)
  }
  return parseInternalActionUrl(item, origin)
}

const resolveExecutionId = (item: EdhrWorkTaskRouteLike, url: URL) =>
  item.executionId ||
  url.searchParams.get('executionId') ||
  (EDHR_EXECUTION_PATHS.has(url.pathname) ? url.searchParams.get('id') : null)

const resolveBatchExecutionId = (item: EdhrWorkTaskRouteLike, url: URL) =>
  item.batchExecutionId ||
  url.searchParams.get('batchExecutionId') ||
  (url.pathname === EDHR_BATCH_EXECUTION_DETAIL_PATH ? url.searchParams.get('id') : null)

const resolveBatchTaskId = (item: EdhrWorkTaskRouteLike, url: URL) =>
  item.batchTaskId || url.searchParams.get('batchTaskId')

const resolveWorkTaskId = (item: EdhrWorkTaskRouteLike, url: URL) =>
  item.id || url.searchParams.get('workTaskId')

const toPositiveNumber = (value?: number | string | null) => {
  if (!isRouteValuePresent(value)) return null
  // The formal wrapper accepts EdhrRouteId (string | number); keep Long identity exact.
  return requireTaskIdentity(value, '正式打开编号')
}

const shouldOpenBatchFillTask = (item: EdhrWorkTaskRouteLike, url: URL) =>
  (isFillOrReworkTask(item.taskType) || (!item.taskType && url.pathname === EDHR_BATCH_EXECUTION_DETAIL_PATH &&
    !url.searchParams.has('nodeType') && !url.searchParams.has('action'))) &&
  !resolveExecutionId(item, url) &&
  Boolean(resolveBatchExecutionId(item, url)) &&
  Boolean(resolveBatchTaskId(item, url))

export const stringifyEdhrExecutionPageQuery = (value?: Record<string, unknown>) => {
  const query: Record<string, string> = {}
  Object.entries(value || {}).forEach(([key, entryValue]) => {
    if (key === 'assistRows') {
      if (Array.isArray(entryValue)) {
        query.assistRows = JSON.stringify(entryValue)
        return
      }
      if (typeof entryValue === 'string' && entryValue.trim()) {
        query.assistRows = entryValue
        return
      }
    }
    if (
      (typeof entryValue === 'string' ||
        typeof entryValue === 'number' ||
        typeof entryValue === 'boolean') &&
      entryValue !== ''
    ) {
      query[key] = String(entryValue)
    }
  })
  return query
}

const resolveOpenedFormSlotType = (opened?: EdhrBatchExecutionTaskOpenRespVO) =>
  String(opened?.executionPageQuery?.formSlotType || '').toUpperCase()

const shouldOpenRouteFormDrawer = (opened?: EdhrBatchExecutionTaskOpenRespVO) => {
  const formSlotType = resolveOpenedFormSlotType(opened)
  return Boolean(opened?.formCenterInstanceId && opened?.formTemplateId && formSlotType) &&
    formSlotType !== EDHR_FORM_SLOT_TYPE_MAIN
}

export const normalizeEdhrWorkTaskRouteParts = (
  item: EdhrWorkTaskRouteLike,
  origin = window.location.origin
): NormalizedEdhrWorkTaskRoute => {
  const url = resolveTaskNavigationUrl(item, origin)
  if (isFillOrReworkTask(item.taskType)) {
    const executionId = resolveExecutionId(item, url)
    if (executionId) {
      url.pathname = EDHR_EXECUTION_FORM_PATH
      setSearchParamIfPresent(url.searchParams, 'id', executionId)
      setSearchParamIfPresent(url.searchParams, 'executionId', executionId)
      setSearchParamIfPresent(url.searchParams, 'workTaskId', resolveWorkTaskId(item, url))
      setSearchParamIfPresent(url.searchParams, 'batchExecutionId', resolveBatchExecutionId(item, url))
      setSearchParamIfPresent(url.searchParams, 'batchTaskId', resolveBatchTaskId(item, url))
      url.searchParams.set('fillCarrier', EDHR_FILL_CARRIER_FORM)
      url.searchParams.set('recordCategory', EDHR_RECORD_CATEGORY_BATCH_RECORD)
    }
  }
  return {
    path: url.pathname,
    query: Object.fromEntries(url.searchParams.entries()),
    href: `${url.pathname}${url.search}${url.hash}`
  }
}

export const normalizeEdhrWorkTaskRoute = (
  item: EdhrWorkTaskRouteLike,
  origin = window.location.origin
) => normalizeEdhrWorkTaskRouteParts(item, origin).href

export const navigateToEdhrWorkTask = async (
  router: Router,
  item: EdhrWorkTaskRouteLike,
  origin = window.location.origin,
  isCurrent: () => boolean = () => true
) => {
  if (!isCurrent()) throw new Error('eDHR 工作任务导航上下文已变化。')
  const url = resolveTaskNavigationUrl(item, origin)
  if (shouldOpenBatchFillTask(item, url)) {
    const batchExecutionId = toPositiveNumber(resolveBatchExecutionId(item, url))
    const batchTaskId = toPositiveNumber(resolveBatchTaskId(item, url))
    const workTaskId = toPositiveNumber(resolveWorkTaskId(item, url))
    if (!batchExecutionId || !batchTaskId || !workTaskId) {
      throw new Error(`eDHR 工作任务 ${item.id || ''} 缺少批次填写上下文。`)
    }
    const opened = await openEdhrBatchTask({
      batchExecutionId,
      taskId: batchTaskId,
      workTaskId
    })
    if (!isCurrent()) throw new Error('eDHR 工作任务导航上下文已变化。')
    // Validate the formal response before any String conversion or route construction.
    for (const key of ['taskId', 'executionId', 'workTaskId'] as const) {
      if (opened[key] !== undefined && opened[key] !== null) requireTaskIdentity(opened[key], key)
    }
    for (const key of ['id', 'executionId', 'batchExecutionId', 'batchTaskId', 'workTaskId']) {
      const value = opened.executionPageQuery?.[key]
      if (value !== undefined && value !== null) requireExactJavaLong(value, `正式打开${key}`)
    }
    if (shouldOpenRouteFormDrawer(opened)) {
      const query = stringifyEdhrExecutionPageQuery(opened?.executionPageQuery)
      const openedWorkTaskId = opened?.workTaskId || workTaskId
      await router.push({
        path: EDHR_BATCH_EXECUTION_DETAIL_PATH,
        query: {
          ...query,
          id: String(batchExecutionId),
          batchExecutionId: String(
            opened?.executionPageQuery?.batchExecutionId || batchExecutionId
          ),
          batchTaskId: String(opened?.executionPageQuery?.batchTaskId || opened?.taskId || batchTaskId),
          ...(openedWorkTaskId ? { workTaskId: String(openedWorkTaskId) } : {}),
          openRouteForm: '1'
        }
      })
      return
    }
    const executionId = opened?.executionId || resolveExecutionId(item, url)
    if (!executionId) {
      throw new Error('填写任务尚未生成执行记录，无法进入填写工作区。')
    }
    const query = stringifyEdhrExecutionPageQuery(opened?.executionPageQuery)
    query.id = String(executionId)
    query.executionId = String(executionId)
    const openedWorkTaskId = opened?.workTaskId || workTaskId
    if (openedWorkTaskId) {
      query.workTaskId = String(openedWorkTaskId)
    }
    query.batchExecutionId = String(
      opened?.executionPageQuery?.batchExecutionId || batchExecutionId
    )
    query.batchTaskId = String(opened?.executionPageQuery?.batchTaskId || batchTaskId)
    query.fillCarrier = EDHR_FILL_CARRIER_FORM
    query.recordCategory = EDHR_RECORD_CATEGORY_BATCH_RECORD
    await router.push({
      path: EDHR_EXECUTION_FORM_PATH,
      query
    })
    return
  }
  const normalized = normalizeEdhrWorkTaskRouteParts(item, origin)
  await router.push({
    path: normalized.path,
    query: normalized.query
  })
}
