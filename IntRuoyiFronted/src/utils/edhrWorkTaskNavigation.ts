import type { Router } from 'vue-router'
import { openEdhrBatchTask } from '@/api/mes/pro/edhr/batchExecution'

export const EDHR_BATCH_EXECUTION_DETAIL_PATH = '/mes/pro/feedback/edhr-batch-execution/detail'
export const EDHR_EXECUTION_DETAIL_PATH = '/mes/pro/feedback/edhr-execution/detail'
export const EDHR_EXECUTION_FORM_PATH = '/mes/pro/feedback/edhr-execution/form'
export const EDHR_APPROVAL_DETAIL_PATH = '/mes/pro/feedback/edhr-approval/detail'
export const EDHR_FILL_CARRIER_FORM = 'FORM'
export const EDHR_RECORD_CATEGORY_BATCH_RECORD = 'BATCH_RECORD'

export const EDHR_WORK_TASK_NOTIFY_PATHS = new Set([
  EDHR_BATCH_EXECUTION_DETAIL_PATH,
  EDHR_EXECUTION_DETAIL_PATH,
  EDHR_EXECUTION_FORM_PATH,
  EDHR_APPROVAL_DETAIL_PATH
])

export type EdhrWorkTaskRouteLike = {
  id?: number | string
  taskType?: string
  actionUrl?: string
  batchExecutionId?: number | string
  batchTaskId?: number | string
  executionId?: number | string
}

export type NormalizedEdhrWorkTaskRoute = {
  path: string
  query: Record<string, string>
  href: string
}

const EDHR_EXECUTION_PATHS = new Set([EDHR_EXECUTION_DETAIL_PATH, EDHR_EXECUTION_FORM_PATH])

const isFillOrReworkTask = (taskType?: string) => {
  const normalized = String(taskType || '').toUpperCase()
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
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
}

const shouldOpenBatchFillTask = (item: EdhrWorkTaskRouteLike, url: URL) =>
  (isFillOrReworkTask(item.taskType) || url.pathname === EDHR_BATCH_EXECUTION_DETAIL_PATH) &&
  !resolveExecutionId(item, url) &&
  Boolean(resolveBatchExecutionId(item, url)) &&
  Boolean(resolveBatchTaskId(item, url))

const stringifyQuery = (value?: Record<string, unknown>) => {
  const query: Record<string, string> = {}
  Object.entries(value || {}).forEach(([key, entryValue]) => {
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

export const normalizeEdhrWorkTaskRouteParts = (
  item: EdhrWorkTaskRouteLike,
  origin = window.location.origin
): NormalizedEdhrWorkTaskRoute => {
  const url = parseInternalActionUrl(item, origin)
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
  origin = window.location.origin
) => {
  const url = parseInternalActionUrl(item, origin)
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
    if (opened?.formCenterInstanceId && opened?.formTemplateId) {
      const query = stringifyQuery(opened?.executionPageQuery)
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
    const query = stringifyQuery(opened?.executionPageQuery)
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
