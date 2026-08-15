import request from '@/config/axios'
import type { ProScheduleCalendarWorkOrderAnalysisVO } from '@/api/mes/pro/scheduleCalendar'

export const REPLAN_REQUEST_TIMEOUT = 180000

export interface ProTaskAutoSchedulePreviewReqVO {
  scheduleOrderIds: number[]
  startTime: string
  runtimeCapacityBasis: 'PLANNED' | 'ACTUAL'
  preserveManualLockedTasks?: boolean
  reason?: string
  erpSourceRiskConfirmed?: boolean
}

export interface ProTaskAutoScheduleApplyReqVO extends ProTaskAutoSchedulePreviewReqVO {
  calendarContextToken: string
}

export interface ProTaskAutoScheduleReplanApplyReqVO extends ProTaskAutoScheduleApplyReqVO {
  idempotencyKey: string
}

export interface ProTaskAutoScheduleIssueVO {
  id?: number
  issueType: string
  severity: string
  workOrderId?: number
  workOrderCode?: string
  taskId?: number
  processId?: number
  processName?: string
  workstationId?: number
  workstationName?: string
  materialId?: number
  materialCode?: string
  materialName?: string
  calendarDate?: string
  shiftId?: number
  shiftName?: string
  requiredQty?: number
  availableQty?: number
  shortageQty?: number
  message: string
}

export interface ProTaskAutoScheduleIssueCreateReqVO {
  issueType: string
  severity: string
  workOrderId?: number
  taskId?: number
  processId?: number
  workstationId?: number
  occurredAt?: string
  sourceType?: string
  sourceId?: number
  message: string
}

export interface ProTaskAutoScheduleIssueResolveReqVO {
  id: number
  resolutionReason: string
}

export interface ProTaskAutoScheduleCancelNightShiftReqVO {
  taskId: number
  reason: string
}

export interface ProTaskAutoScheduleSummaryVO {
  workOrderCount: number
  generatedTaskCount: number
  preservedTaskCount: number
  blockingIssueCount: number
  appliedWorkOrderCount: number
  blockedWorkOrderCount: number
  skippedWorkOrderCount: number
  shortageCount: number
  startTime?: string
  endTime?: string
}

export interface ProTaskAutoSchedulePreviewRespVO {
  previewOnly: boolean
  calendarContextToken: string
  calendarContextMode?: string
  calendarContextDate?: string
  summary: ProTaskAutoScheduleSummaryVO
  tasks: any[]
  links: any[]
  issues: ProTaskAutoScheduleIssueVO[]
  workOrderAnalyses: ProScheduleCalendarWorkOrderAnalysisVO[]
}

export interface ProTaskAutoScheduleProtectedTaskVO {
  taskId: number
  taskCode?: string
  taskName?: string
  workOrderCode?: string
  processName?: string
  workstationName?: string
  scheduleOrderId?: number
  scheduleOrderCode?: string
  protectionReason: string
  message?: string
}

export interface ProTaskAutoScheduleReplanPreviewRespVO extends ProTaskAutoSchedulePreviewRespVO {
  protectedTasks: ProTaskAutoScheduleProtectedTaskVO[]
}

export interface ProTaskAutoScheduleApplyRespVO {
  applied: boolean
  summary: ProTaskAutoScheduleSummaryVO
  createdTaskIds: number[]
  deletedTaskIds: number[]
  preservedTaskIds: number[]
  issues: ProTaskAutoScheduleIssueVO[]
}

export interface ProTaskLatestScheduleApplyRespVO {
  hasData: boolean
  appliedAt?: string
  operationType?: 'AUTO_APPLY' | 'REPLAN_APPLY'
  scheduleOrderId?: number
  scheduleOrderCode?: string
  operatorId?: number
  operatorName?: string
  reason?: string
}

export interface ProTaskReplanExplanationSummaryVO {
  scheduleOrderCount: number
  workOrderCount: number
  routeCount: number
  processCount: number
  generatedTaskCount: number
  deletedTaskCount: number
  preservedTaskCount: number
  blockingIssueCount: number
  warningIssueCount: number
  shortageCount: number
  startTime?: string
  endTime?: string
}

export interface ProTaskReplanExplanationOrderVO {
  rank: number
  scheduleOrderId: number
  scheduleOrderCode?: string
  workOrderId?: number
  workOrderCode?: string
  productId?: number
  productCode?: string
  productName?: string
  quantity?: number
  promiseDate?: string
  priorityNo?: number
  routeId?: number
  routeCode?: string
  routeName?: string
  processCount: number
}

export interface ProTaskReplanExplanationProcessVO {
  processId?: number
  processName?: string
  processSort?: number
  scheduledQuantity?: number
  capacitySource?: string
  shiftNames?: string[]
  workstationCount?: number
  workstationNames?: string[]
  machineCount?: number
  configuredWorkerCount?: number
  currentWorkerCount?: number
  effectiveHourlyCapacity?: number
  plannedDurationMinutes?: number
  startTime?: string
  endTime?: string
  bottleneck?: boolean
}

export interface ProTaskReplanExplanationWorkOrderVO {
  workOrderId?: number
  workOrderCode?: string
  productId?: number
  productCode?: string
  productName?: string
  quantity?: number
  routeId?: number
  routeCode?: string
  routeName?: string
  startTime?: string
  endTime?: string
  bottleneckProcessId?: number
  bottleneckProcessName?: string
  bottleneckHourlyCapacity?: number
  processes: ProTaskReplanExplanationProcessVO[]
}

export interface ProTaskReplanExplanationMaterialContributionVO {
  scheduleOrderId?: number
  scheduleOrderCode?: string
  workOrderId?: number
  workOrderCode?: string
  requiredQty?: number
}

export interface ProTaskReplanExplanationMaterialVO {
  materialId: number
  materialCode?: string
  materialName?: string
  requiredQty?: number
  availableQty?: number
  shortageQty?: number
  orderContributions: ProTaskReplanExplanationMaterialContributionVO[]
}

export interface ProTaskReplanExplanationProtectionSummaryVO {
  totalCount: number
  feedbackCount: number
  inProgressCount: number
  finishedCount: number
  lockedCount: number
  manualCount: number
  otherCount: number
}

export interface ProTaskReplanExplanationRespVO {
  hasData: boolean
  requestId?: string
  triggerSource?: 'MANUAL' | 'NIGHTLY'
  capacityMode?: string
  reason?: string
  operatorId?: number
  operatorName?: string
  requestStartTime?: string
  appliedAt?: string
  summary?: ProTaskReplanExplanationSummaryVO
  orders?: ProTaskReplanExplanationOrderVO[]
  workOrders?: ProTaskReplanExplanationWorkOrderVO[]
  materials?: ProTaskReplanExplanationMaterialVO[]
  protectionSummary?: ProTaskReplanExplanationProtectionSummaryVO
  protectedTasks?: ProTaskAutoScheduleProtectedTaskVO[]
  issues?: ProTaskAutoScheduleIssueVO[]
}

export const ProTaskAutoScheduleApi = {
  preview: async (data: ProTaskAutoSchedulePreviewReqVO) => {
    return await request.post<ProTaskAutoSchedulePreviewRespVO>({
      url: '/mes/pro/auto-schedule/preview',
      data
    })
  },

  apply: async (data: ProTaskAutoScheduleApplyReqVO) => {
    return await request.post<ProTaskAutoScheduleApplyRespVO>({
      url: '/mes/pro/auto-schedule/apply',
      data
    })
  },

  replanPreview: async (data: ProTaskAutoSchedulePreviewReqVO) => {
    return await request.post<ProTaskAutoScheduleReplanPreviewRespVO>({
      url: '/mes/pro/auto-schedule/replan/preview',
      data,
      timeout: REPLAN_REQUEST_TIMEOUT
    })
  },

  replanApply: async (data: ProTaskAutoScheduleReplanApplyReqVO): Promise<ProTaskAutoScheduleApplyRespVO> => {
    return await request.post<ProTaskAutoScheduleApplyRespVO>({
      url: '/mes/pro/auto-schedule/replan/apply',
      data,
      timeout: REPLAN_REQUEST_TIMEOUT
    })
  },

  getLatestReplanExplanation: async () => {
    return await request.get<ProTaskReplanExplanationRespVO>({
      url: '/mes/pro/auto-schedule/replan/explanation/latest'
    })
  },

  getLatestSuccessfulScheduleApply: async () => {
    return await request.get<ProTaskLatestScheduleApplyRespVO>({
      url: '/mes/pro/auto-schedule/apply/latest-success'
    })
  },

  getIssues: async (params: any) => {
    return await request.get<ProTaskAutoScheduleIssueVO[]>({
      url: '/mes/pro/auto-schedule/issues',
      params
    })
  },

  createIssue: async (data: ProTaskAutoScheduleIssueCreateReqVO) => {
    return await request.post<number>({
      url: '/mes/pro/auto-schedule/issues',
      data
    })
  },

  resolveIssue: async (data: ProTaskAutoScheduleIssueResolveReqVO) => {
    return await request.put<boolean>({
      url: '/mes/pro/auto-schedule/issues/resolve',
      data
    })
  },

  cancelNightShift: async (data: ProTaskAutoScheduleCancelNightShiftReqVO) => {
    return await request.post<number>({
      url: '/mes/pro/auto-schedule/issues/cancel-night-shift',
      data
    })
  },

  getDependencies: async (data: { workOrderIds?: number[]; taskIds?: number[] }) => {
    return await request.post<any[]>({
      url: '/mes/pro/auto-schedule/dependencies',
      data
    })
  }
}
