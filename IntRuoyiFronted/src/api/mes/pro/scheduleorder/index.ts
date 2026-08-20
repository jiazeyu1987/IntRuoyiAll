import request from '@/config/axios'
import { REPLAN_REQUEST_TIMEOUT } from '@/api/mes/pro/task/autoSchedule'
import type { TableQuickFilterValue } from '@/hooks/web/useTableQuickFilter'

export interface MesProScheduleOrderVO {
  id: number
  code: string
  workOrderId: number
  erpWorkOrderCode: string
  productionMaterialListCount?: number
  productionMaterialListSummary?: string
  productId: number
  productCode: string
  productName: string
  productSpecification: string
  quantity: number
  totalQuantity: number
  completedQuantity: number
  uncompletedQuantity: number
  effectiveCompletedQuantity?: number
  pendingApprovalQuantity?: number
  pendingInspectionQuantity?: number
  overReportedQuantity?: number
  promiseDate: string
  priorityNo: number
  status: number
  progressPercent: number
  diffStatus: number
  riskStatus: number
  blockingIssueCount?: number
  latestBlockingIssueMessage?: string
  frozen?: boolean
  frozenTime?: string
  frozenBy?: number
  freezeReason?: string
  manualFinished?: boolean
  manualFinishedTime?: string
  manualFinishedBy?: number
  manualFinishedReason?: string
  latestStartTime?: string
  plannedStartTime?: string
  plannedEndTime?: string
  routeId: number
  routeCode: string
  routeName: string
  routeVersion?: string
  scheduleConfigVersion?: string
  currentProcessId?: number
  currentRouteProcessId?: number
  currentProcessCode?: string
  currentProcessName?: string
  currentProcessProgressPercent?: number
  sourceSnapshotJson?: string
  capacitySnapshotJson?: string
  remark: string
  createTime: string
}

export interface MesProScheduleOrderProcessVO {
  id: number
  scheduleOrderId: number
  routeProcessId: number
  processId: number
  processCode: string
  processName: string
  sort: number
  enabled: boolean
  capacitySource: string
  capacityMode?: 'RESOURCE_CALCULATED' | 'MANUAL_OVERRIDE' | 'FINITE_HOURLY' | 'INFINITE_FORMULA' | string
  hourlyCapacityTotal?: number
  infiniteDurationQuantityFactor?: number
  infiniteDurationBaseMinutes?: number
  shiftHours?: number
  shiftCapacityTotal?: number
  nightShiftEnabled?: boolean
  productionQuantityFactor?: number
  resourceSnapshotJson?: string
  plannedQuantity: number
  reportedQuantity: number
  effectiveCompletedQuantity?: number
  pendingApprovalQuantity?: number
  pendingInspectionQuantity?: number
  overReportedQuantity?: number
  remainingQuantity: number
  progressPercent?: number
  feedbackCount?: number
  latestFeedbackTime?: string
  feedbackHistoryList?: MesProScheduleOrderProcessFeedbackHistoryVO[]
  plannedStartTime: string
  plannedEndTime: string
  bottleneckFlag: boolean
  keyProcessFlag: boolean
}

export interface MesProScheduleOrderProcessFeedbackHistoryVO {
  id: number
  code?: string
  feedbackTime?: string
  feedbackQuantity?: number
  qualifiedQuantity?: number
  unqualifiedQuantity?: number
  uncheckQuantity?: number
  feedbackUserId?: number
  feedbackUserNickname?: string
  status?: number
  statusName?: string
  remark?: string
}

export interface MesProScheduleOrderProcessWipVO {
  routeId: number
  routeCode: string
  routeName: string
  routeVersionId: number
  routeVersionNo?: string
  routeVersionStatus?: string
  routeProcessId: number
  processId: number
  processCode?: string
  processName?: string
  wipOrderCount: number
  shiftCapacityTotal?: number
  capacityMode?: 'RESOURCE_CALCULATED' | 'MANUAL_OVERRIDE' | 'FINITE_HOURLY' | 'INFINITE_FORMULA' | string
  capacitySource?: 'MACHINE' | 'WORKER' | 'MANUAL_OVERRIDE' | 'INFINITE_FORMULA' | 'UNCONFIGURED' | string
  resourceStatus?: 'NORMAL' | 'CAPACITY_MISSING' | string
  resourceStatusReason?: string
  shiftStatus?: '白班' | '夜班' | string
  nightShiftEnabled?: boolean
  plannedStartDate?: string
  plannedStartDateMixed?: boolean
  unfinishedDemandQuantity?: number
  estimatedStartTime?: string
  estimatedCompletionTime?: string
  todayFeedbackQuantity?: number
  scheduleOrderIds: number[]
}

export interface MesProScheduleOrderProcessWipSettingsReqVO {
  routeVersionId: number
  routeProcessId: number
  nightShiftEnabled?: boolean
  plannedStartDate?: string
  shiftCapacityTotal?: number
  reason: string
}

export interface MesProScheduleOrderDailyCompareVO {
  id?: number
  scheduleOrderId: number
  scheduleOrderProcessId: number
  processId: number
  planDate: string
  plannedQuantity: number
  actualQuantity: number
  diffQuantity: number
  status: number
  statusLabel?: string
  remark?: string
}

export interface MesProScheduleOrderIssueActionVO {
  actionLabel: string
  targetRouteName?: string
  targetQuery?: Record<string, any>
  requiredPermission?: string
}

export interface MesProScheduleOrderAdmissionDiffRowVO {
  workOrderId: number
  workOrderCode: string
  productId?: number
  productCode?: string
  productName?: string
  productSpecification?: string
  quantity: number
  requestDate?: string
  workOrderStatus?: number
  temporaryFrozen?: boolean
  scheduleOrderId?: number
  admissionStatus: string
  schedulableStatus: string
  reasonCode: string
  severity: string
  message: string
  ownerRole?: string
  selectable: boolean
  actions?: MesProScheduleOrderIssueActionVO[]
}

export interface MesProScheduleOrderAdmissionDiffSummaryVO {
  readyCount: number
  alreadyAdmittedCount: number
  warnCount: number
  blockedCount: number
}

export interface MesProScheduleOrderAdmissionDiffPageRespVO {
  total: number
  list: MesProScheduleOrderAdmissionDiffRowVO[]
  summary: MesProScheduleOrderAdmissionDiffSummaryVO
}

export interface MesProScheduleOrderAdmissionDiffPageReqVO {
  pageNo?: number
  pageSize?: number
  workOrderCode?: string
  productCode?: string
  productName?: string
  productSpecification?: string
  quantity?: number[]
  admissionStatus?: string
  reasonCode?: string
  message?: string
  ownerRole?: string
  requestDate?: string[]
  quickFilter?: TableQuickFilterValue
}

export interface MesProScheduleOrderPreflightReqVO {
  scopeType?: 'SELECTED'
  scheduleOrderIds: number[]
  includeAdmissionDiff?: boolean
  startTime?: string
  capacityMode?: 'PLANNED' | 'ACTUAL'
}

export interface MesProScheduleOrderCreateFromWorkOrdersReqVO {
  workOrderIds: number[]
}

export interface MesProScheduleOrderPreflightScopeVO {
  scopeType: string
  scheduleOrderCount: number
}

export interface MesProScheduleOrderPreflightSummaryVO {
  passCount: number
  warnCount: number
  blockedCount: number
}

export interface MesProScheduleOrderPreflightIssueVO {
  reasonCode: string
  severity: 'PASS' | 'WARN' | 'BLOCKED'
  objectType?: string
  objectId?: number
  workOrderId?: number
  workOrderCode?: string
  scheduleOrderId?: number
  scheduleOrderCode?: string
  productId?: number
  productCode?: string
  productName?: string
  processId?: number
  processName?: string
  message: string
  ownerRole?: string
  action?: MesProScheduleOrderIssueActionVO
}

export interface MesProScheduleOrderPreflightRespVO {
  result: 'PASS' | 'WARN' | 'BLOCKED'
  checkedAt: string
  scope: MesProScheduleOrderPreflightScopeVO
  summary: MesProScheduleOrderPreflightSummaryVO
  issues: MesProScheduleOrderPreflightIssueVO[]
}

export interface MesProScheduleOrderCreateFromWorkOrderReqVO {
  workOrderId: number
  promiseDate?: string
  priorityNo?: number
  remark?: string
}

export interface MesProScheduleOrderUpdatePriorityReqVO {
  id: number
  priorityNo: number
}

export interface MesProScheduleOrderUpdateReqVO {
  id: number
  promiseDate: string
  plannedStartTime?: string
  priorityNo: number
  remark?: string
  reason: string
}

export interface MesProScheduleOrderBatchReqVO {
  ids: number[]
  reason: string
}

export interface MesProScheduleOrderActionReqVO {
  id: number
  reason: string
}

export interface MesProScheduleOrderPageReqVO {
  pageNo?: number
  pageSize?: number
  code?: string
  erpWorkOrderCode?: string
  currentProcessId?: number
  productCode?: string
  productName?: string
  promiseDate?: string[]
  status?: number
  frozen?: boolean
  completionFilter?: 'INCOMPLETE' | 'ALL' | 'COMPLETED'
  sortField?: string
  sortOrder?: 'asc' | 'desc'
  exportColumns?: string[]
  quickFilter?: TableQuickFilterValue
}

export interface MesProScheduleOrderOperationLogVO {
  id: number
  scheduleOrderId: number
  scheduleOrderCode?: string
  operationType:
    | 'FREEZE'
    | 'UNFREEZE'
    | 'UPDATE'
    | 'DELETE'
    | 'MANUAL_FINISH'
    | 'REVOKE_MANUAL_FINISH'
    | string
  beforeSnapshotJson?: string
  afterSnapshotJson?: string
  reason: string
  operatorId?: number
  operatorName?: string
  createTime: string
}

const normalizeDateValue = (value: unknown): string => {
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  return typeof value === 'string' ? value : ''
}

const normalizeDateTimeValue = (value: unknown): string | undefined => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  if (Array.isArray(value) && value.length >= 5) {
    const [year, month, day, hour, minute, second = 0] = value
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`
  }
  if (typeof value === 'number' || /^\d+$/.test(String(value))) {
    const date = new Date(Number(value))
    if (Number.isNaN(date.getTime())) {
      throw new Error(`排产工单接口字段不是有效时间: ${String(value)}`)
    }
    const pad = (item: number) => String(item).padStart(2, '0')
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  }
  return typeof value === 'string' ? value : undefined
}

const requireNumber = (row: MesProScheduleOrderVO, field: keyof MesProScheduleOrderVO): number => {
  const value = row[field]
  if (value === undefined || value === null || value === '') {
    throw new Error(`排产工单接口缺少必需字段: ${String(field)}`)
  }
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    throw new Error(`排产工单接口字段不是有效数字: ${String(field)}`)
  }
  return numberValue
}

const normalizeScheduleOrder = (row: MesProScheduleOrderVO): MesProScheduleOrderVO => {
  return {
    ...row,
    promiseDate: normalizeDateValue(row.promiseDate),
    progressPercent: requireNumber(row, 'progressPercent'),
    totalQuantity: requireNumber(row, 'totalQuantity'),
    completedQuantity: requireNumber(row, 'completedQuantity'),
    uncompletedQuantity: requireNumber(row, 'uncompletedQuantity'),
    effectiveCompletedQuantity:
      row.effectiveCompletedQuantity === undefined
        ? undefined
        : Number(row.effectiveCompletedQuantity),
    pendingApprovalQuantity:
      row.pendingApprovalQuantity === undefined ? undefined : Number(row.pendingApprovalQuantity),
    pendingInspectionQuantity:
      row.pendingInspectionQuantity === undefined
        ? undefined
        : Number(row.pendingInspectionQuantity),
    overReportedQuantity:
      row.overReportedQuantity === undefined ? undefined : Number(row.overReportedQuantity),
    latestStartTime: normalizeDateTimeValue(row.latestStartTime),
    plannedStartTime: normalizeDateTimeValue(row.plannedStartTime),
    plannedEndTime: normalizeDateTimeValue(row.plannedEndTime),
    frozenTime: normalizeDateTimeValue(row.frozenTime),
    manualFinishedTime: normalizeDateTimeValue(row.manualFinishedTime)
  }
}

const normalizeProcessWipStatistics = (
  row: MesProScheduleOrderProcessWipVO
): MesProScheduleOrderProcessWipVO => {
  return {
    ...row,
    plannedStartDate:
      row.plannedStartDate === undefined || row.plannedStartDate === null
        ? undefined
        : normalizeDateValue(row.plannedStartDate),
    estimatedStartTime: normalizeDateTimeValue(row.estimatedStartTime),
    estimatedCompletionTime: normalizeDateTimeValue(row.estimatedCompletionTime)
  }
}

export const MesProScheduleOrderApi = {
  getScheduleOrderPage: async (params: MesProScheduleOrderPageReqVO) => {
    const data = await request.get({ url: '/mes/pro/schedule-order/page', params })
    return {
      ...data,
      list: Array.isArray(data.list) ? data.list.map(normalizeScheduleOrder) : []
    }
  },

  exportScheduleOrderExcel: async (
    params: MesProScheduleOrderPageReqVO & { exportColumns?: string[] }
  ) => {
    return await request.download({ url: '/mes/pro/schedule-order/export-excel', params })
  },

  getScheduleOrder: async (id: number) => {
    const data = await request.get({ url: `/mes/pro/schedule-order/get?id=${id}` })
    return data ? normalizeScheduleOrder(data) : data
  },

  getProcessList: async (scheduleOrderId: number) => {
    return await request.get({
      url: `/mes/pro/schedule-order/process-list?scheduleOrderId=${scheduleOrderId}`
    })
  },

  getProcessWipStatistics: async () => {
    const data = await request.get<MesProScheduleOrderProcessWipVO[]>({
      url: '/mes/pro/schedule-order/process-wip-statistics'
    })
    return Array.isArray(data) ? data.map(normalizeProcessWipStatistics) : []
  },

  saveProcessWipSettings: async (data: MesProScheduleOrderProcessWipSettingsReqVO) => {
    return await request.put({
      url: '/mes/pro/schedule-order/process-wip-settings',
      data
    })
  },

  createFromWorkOrder: async (data: MesProScheduleOrderCreateFromWorkOrderReqVO) => {
    return await request.post({ url: '/mes/pro/schedule-order/create-from-work-order', data })
  },

  createFromWorkOrders: async (data: MesProScheduleOrderCreateFromWorkOrdersReqVO) => {
    return await request.post({ url: '/mes/pro/schedule-order/create-from-work-orders', data })
  },

  getAdmissionDiff: async (params: MesProScheduleOrderAdmissionDiffPageReqVO) => {
    const data = await request.get<MesProScheduleOrderAdmissionDiffPageRespVO>({
      url: '/mes/pro/schedule-order/admission-diff',
      params
    })
    return {
      ...data,
      list: Array.isArray(data.list)
        ? data.list.map((row) => ({
            ...row,
            requestDate: normalizeDateTimeValue(row.requestDate)
          }))
        : [],
      summary: data.summary || {
        readyCount: 0,
        alreadyAdmittedCount: 0,
        warnCount: 0,
        blockedCount: 0
      }
    }
  },

  preflightScheduleOrders: async (data: MesProScheduleOrderPreflightReqVO) => {
    return await request.post<MesProScheduleOrderPreflightRespVO>({
      url: '/mes/pro/schedule-order/preflight',
      data,
      timeout: REPLAN_REQUEST_TIMEOUT
    })
  },

  updatePriority: async (data: MesProScheduleOrderUpdatePriorityReqVO) => {
    return await request.put({ url: '/mes/pro/schedule-order/priority', data })
  },

  updateScheduleOrder: async (data: MesProScheduleOrderUpdateReqVO) => {
    return await request.put({ url: '/mes/pro/schedule-order/update', data })
  },

  freezeScheduleOrders: async (data: MesProScheduleOrderBatchReqVO) => {
    return await request.post({ url: '/mes/pro/schedule-order/freeze', data })
  },

  unfreezeScheduleOrders: async (data: MesProScheduleOrderBatchReqVO) => {
    return await request.post({ url: '/mes/pro/schedule-order/unfreeze', data })
  },

  manualFinishScheduleOrder: async (data: MesProScheduleOrderActionReqVO) => {
    return await request.post({ url: '/mes/pro/schedule-order/manual-finish', data })
  },

  revokeManualFinishScheduleOrder: async (data: MesProScheduleOrderActionReqVO) => {
    return await request.post({ url: '/mes/pro/schedule-order/revoke-manual-finish', data })
  },

  deleteScheduleOrders: async (data: MesProScheduleOrderBatchReqVO) => {
    return await request.delete({ url: '/mes/pro/schedule-order/batch-delete', data })
  },

  getOperationLog: async (scheduleOrderId: number) => {
    return await request.get<MesProScheduleOrderOperationLogVO[]>({
      url: '/mes/pro/schedule-order/operation-log',
      params: { scheduleOrderId }
    })
  },

  syncProgress: async (scheduleOrderId: number) => {
    return await request.post({
      url: `/mes/pro/schedule-order/sync-progress?scheduleOrderId=${scheduleOrderId}`
    })
  },

  getDailyCompare: async (params: {
    scheduleOrderId: number
    startDate?: string
    endDate?: string
  }) => {
    return await request.get<MesProScheduleOrderDailyCompareVO[]>({
      url: '/mes/pro/schedule-order/daily-compare',
      params
    })
  }
}
