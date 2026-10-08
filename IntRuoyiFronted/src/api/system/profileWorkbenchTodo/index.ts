import request from '@/config/axios'
import qs from 'qs'
import type { EdhrWorkTaskRouteLike } from '@/utils/edhrWorkTaskNavigation'
import type { ProfileWorkbenchTaskVisibilitySaveReqVO } from '@/api/system/profileWorkbenchTaskVisibility'

export const PROFILE_WORKBENCH_TODO_SOURCES = [
  'DCC_DISTRIBUTION', 'DCC_TRAINING', 'EDHR_WORK_TASK', 'WORK_ORDER', 'SHOWROOM_ASSIGNMENT'
] as const

export type ProfileWorkbenchTodoSourceId = (typeof PROFILE_WORKBENCH_TODO_SOURCES)[number]
export type ProfileWorkbenchTodoTaskType = '文控' | '批记录' | '排产' | '展厅' | '行政'

export interface ProfileWorkbenchTodoQuery {
  pageNo: number
  pageSize: number
  visibility: 'visible' | 'hidden'
  enabledSources: ProfileWorkbenchTodoSourceId[]
  taskType?: ProfileWorkbenchTodoTaskType
  quickFilter?: {
    fieldKey: string
    operator: 'contains' | 'eq'
    value?: string
  }
  sort?: { key: string; order: 'asc' | 'desc' }
}

export type ProfileWorkbenchTodoNavigation = {
  [K in keyof EdhrWorkTaskRouteLike]?: string
} & {
  controlledFileId?: string
  distributionId?: string
  recipientId?: string
  progressId?: string
  code?: string
  assignmentId?: string
}

export interface ProfileWorkbenchTodoRow {
  sourceId: ProfileWorkbenchTodoSourceId
  taskKey: string
  businessId: string
  taskType: ProfileWorkbenchTodoTaskType
  source: string
  detail: string
  statusLabel: string
  createdAt?: string | number | null
  dueAt?: string | number | null
  navigation: ProfileWorkbenchTodoNavigation
}

export interface ProfileWorkbenchTodoPage {
  list: ProfileWorkbenchTodoRow[]
  total: number
  businessTotal: number
  hiddenTotal: number
  effectivePageNo: number
  pageSize: number
  readAt: string | number
}

export interface ProfileWorkbenchTodoCount {
  businessTotal: number
  readAt: string | number
}

// The controller accepts repeated fixed source parameters, never comma-packed values.
const serializeWorkbenchParams = (params: Record<string, unknown>) =>
  qs.stringify(params, { allowDots: true, arrayFormat: 'repeat' })

const requireCount = (value: unknown, field: string) => {
  if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < 0) {
    throw new Error(`个人工作台接口返回的 ${field} 不是有效数量。`)
  }
}

const requireReadAt = (value: unknown) => {
  if (!((typeof value === 'string' && value.trim()) ||
    (typeof value === 'number' && Number.isFinite(value)))) {
    throw new Error('个人工作台接口缺少读取时间。')
  }
}

const requireRow = (row: ProfileWorkbenchTodoRow, sources: ProfileWorkbenchTodoSourceId[]) => {
  if (!row || !PROFILE_WORKBENCH_TODO_SOURCES.includes(row.sourceId) ||
    !sources.includes(row.sourceId) || typeof row.taskKey !== 'string' || !row.taskKey ||
    typeof row.businessId !== 'string' || !/^[1-9]\d*$/.test(row.businessId) ||
    !['文控', '批记录', '排产', '展厅', '行政'].includes(row.taskType) ||
    typeof row.source !== 'string' || typeof row.detail !== 'string' ||
    typeof row.statusLabel !== 'string' || !row.navigation || typeof row.navigation !== 'object') {
    throw new Error('个人工作台接口返回的任务行不符合合同。')
  }
  const prefix: Record<ProfileWorkbenchTodoSourceId, string> = {
    DCC_DISTRIBUTION: '文控分发', DCC_TRAINING: '文控培训', EDHR_WORK_TASK: 'eDHR工作任务',
    WORK_ORDER: '待排产工单', SHOWROOM_ASSIGNMENT: '展厅补充指派'
  }
  if (row.taskKey !== `${prefix[row.sourceId]}:${row.businessId}`) {
    throw new Error('个人工作台任务标识与正式业务身份不一致。')
  }
  for (const value of Object.values(row.navigation)) {
    if (typeof value !== 'string') throw new Error('个人工作台导航载荷必须使用字符串。')
  }
}

export const getProfileWorkbenchTodoPage = async (query: ProfileWorkbenchTodoQuery) => {
  const params: Record<string, unknown> = {
    pageNo: query.pageNo, pageSize: query.pageSize, visibility: query.visibility,
    enabledSources: query.enabledSources.length ? query.enabledSources : undefined,
    taskType: query.taskType
  }
  if (query.quickFilter) {
    params['quickFilter.fieldKey'] = query.quickFilter.fieldKey
    params['quickFilter.operator'] = query.quickFilter.operator
    params['quickFilter.value'] = query.quickFilter.value
  }
  if (query.sort) {
    params['sort.key'] = query.sort.key
    params['sort.order'] = query.sort.order
  }
  const page = await request.get<ProfileWorkbenchTodoPage>({
    url: '/system/profile-workbench-todo/page', params,
    paramsSerializer: serializeWorkbenchParams, ignoreErrorMessage: true
  })
  if (!page || !Array.isArray(page.list)) {
    throw new Error('个人工作台接口返回缺少待办列表。')
  }
  for (const field of ['total', 'businessTotal', 'hiddenTotal'] as const) requireCount(page[field], field)
  if (!Number.isSafeInteger(page.effectivePageNo) || page.effectivePageNo < 1 ||
    page.pageSize !== query.pageSize || page.pageSize < 10 || page.pageSize > 100 ||
    page.list.length > page.pageSize || page.hiddenTotal > page.businessTotal ||
    page.total > page.businessTotal ||
    page.effectivePageNo > Math.max(1, Math.ceil(page.total / page.pageSize)) ||
    page.list.length !== Math.min(page.pageSize, Math.max(0,
      page.total - (page.effectivePageNo - 1) * page.pageSize))) {
    throw new Error('个人工作台接口返回的分页不符合合同。')
  }
  requireReadAt(page.readAt)
  const keys = new Set<string>()
  for (const row of page.list) {
    requireRow(row, query.enabledSources)
    if (keys.has(row.taskKey)) throw new Error('个人工作台接口返回重复任务。')
    keys.add(row.taskKey)
  }
  return page
}

export const getProfileWorkbenchTodoCount = async (enabledSources: ProfileWorkbenchTodoSourceId[]) => {
  const result = await request.get<ProfileWorkbenchTodoCount>({
    url: '/system/profile-workbench-todo/count',
    params: { enabledSources: enabledSources.length ? enabledSources : undefined },
    paramsSerializer: serializeWorkbenchParams,
    ignoreErrorMessage: true
  })
  if (!result) throw new Error('个人工作台接口缺少待处理数量响应。')
  requireCount(result.businessTotal, 'businessTotal')
  requireReadAt(result.readAt)
  return result
}

// The workbench handles write failures locally; other visibility callers keep their existing wrapper.
export const hideProfileWorkbenchTask = (data: ProfileWorkbenchTaskVisibilitySaveReqVO) =>
  request.put<boolean>({
    url: '/system/profile-workbench-task-visibility/hide', data, ignoreErrorMessage: true
  })

export const restoreProfileWorkbenchTask = (taskKey: string) =>
  request.delete<boolean>({
    url: '/system/profile-workbench-task-visibility/restore',
    params: { taskKey }, ignoreErrorMessage: true
  })
