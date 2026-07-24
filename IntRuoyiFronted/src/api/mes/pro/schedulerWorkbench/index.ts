import request from '@/config/axios'

export const SCHEDULER_WORKBENCH_IMPORT_TIMEOUT = 120000

export interface SchedulerWorkbenchStepVO {
  sort: number
  name: string
  description: string
  primaryPath: string
  primaryMetricName: string
  primaryMetricValue: string
}

export interface SchedulerWorkbenchBottleneckVO {
  scheduleOrderProcessId?: number
  routeId?: number
  routeProcessId?: number
  scheduleOrderCode?: string
  workOrderCode?: string
  processCode?: string
  processName?: string
  workstationName?: string
  resourceType?: string
  todayCapacity?: number
  demandQuantity?: number
  scheduledQuantity?: number
  gapQuantity?: number
  reason?: string
  targetPath?: string
}

export interface SchedulerWorkbenchReportedDeviationDetailVO {
  scheduleOrderId?: number
  scheduleOrderProcessId?: number
  scheduleOrderCode?: string
  workOrderCode?: string
  processCode?: string
  processName?: string
  plannedQuantity?: number
  reportedQuantity?: number
  deviationQuantity?: number
  processStatus?: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | string
}

export interface SchedulerWorkbenchRouteActiveProductVO {
  productId?: number
  productCode?: string
  productName?: string
  wipOrderCount?: number
}

export interface SchedulerWorkbenchRouteActiveOrderVO {
  routeId?: number
  routeCode?: string
  routeName?: string
  wipOrderCount?: number
  products?: SchedulerWorkbenchRouteActiveProductVO[]
}

export interface SchedulerWorkbenchSummaryVO {
  date: string
  pendingScheduleOrderCount: number
  todayScheduledTaskCount: number
  todayPlannedCapacity: number
  todayFeedbackCount: number
  todayFeedbackQuantity: number
  pendingApprovalFeedbackCount: number
  currentSchedulePlannedQuantity: number
  currentScheduleReportedQuantity: number
  reportedDeviationQuantity: number
  reportedDeviationText: string
  todayAvailableCapacity: number
  repairingMachineryCount: number
  resourceUnconfiguredCount: number
  blockingIssueCount: number
  materialShortageCount: number
  nightlyReplanText: string
  todayActionSuggestion: string
  currentScheduleScopeText: string
  globalRiskScopeText: string
  steps: SchedulerWorkbenchStepVO[]
  bottlenecks: SchedulerWorkbenchBottleneckVO[]
  reportedDeviationDetails: SchedulerWorkbenchReportedDeviationDetailVO[]
  routeActiveOrders: SchedulerWorkbenchRouteActiveOrderVO[]
}

export interface SchedulerWorkbenchCapacityUnificationAuditIssueVO {
  code: string
  message?: string
  routeScheduleConfigId?: number
  routeProcessId?: number
  capacityMode?: string
  manualHourlyCapacity?: number
  resourceCapacityHourly?: number
  capacitySource?: string
  workstationId?: number
  workstationCode?: string
  machineryId?: number
  machineryCode?: string
}

export interface SchedulerWorkbenchCapacityUnificationAuditVO {
  enabled: boolean
  legacyFiniteHourlyConfigCount: number
  manualOverrideDiffCount: number
  resourceMissingCount: number
  machineryProcessCapacityMissingCount: number
  totalIssueCount: number
  issues: SchedulerWorkbenchCapacityUnificationAuditIssueVO[]
}
export interface SchedulerWorkbenchSmokeTestStatusVO {
  status: 'IDLE' | 'RUNNING' | 'STOPPED' | 'FAILED'
  running: boolean
  feedbackApprovalEnabled: boolean
  runId?: string
  osName?: string
  pid?: number
  startedAt?: string
  stoppedAt?: string
  finishedAt?: string
  exitCode?: number
  frontendDirectory?: string
  scriptName?: string
  commandText?: string
  logFile?: string
  message?: string
}

export interface SchedulerWorkbenchSmokeTestStartReqVO {
  feedbackApprovalEnabled: boolean
}

export interface SchedulerWorkbenchShiftHoursVO {
  shiftHours?: number
  workstationCount: number
  configuredWorkstationCount: number
  missingWorkstationCount: number
  distinctShiftHoursCount: number
  updatedWorkstationCount: number
}

export interface SchedulerWorkbenchShiftHoursSaveReqVO {
  shiftHours: number
}

export interface SchedulerWorkbenchPolicySettingsVO {
  erpWorkOrderSyncTime: string
  nightlyReplanTime: string
  priorityRule: 'PROMISE_DATE' | 'ORDER_PRIORITY' | 'CREATED_TIME'
  protectReportedTasks: boolean
  protectCompletedTasks: boolean
  protectLockedTasks: boolean
  defaultScheduleUseEnabled: boolean
  defaultScheduleCapacityMode: 'RESOURCE_CALCULATED' | 'MANUAL_OVERRIDE' | 'INFINITE_FORMULA'
  defaultFiniteHourlyCapacity?: number
  defaultInfiniteDurationQuantityFactorHours?: number
  defaultInfiniteDurationBaseHours?: number
  defaultNightShiftEnabled: boolean
  defaultWorkerQuantity: number
  defaultWorkerSingleHourlyCapacity: number
}

export interface SchedulerWorkbenchRouteConfigImportRespVO {
  routeCount: number
  flowConfigProcessCount: number
  scheduleConfigCount: number
  resourceCount: number
}

export interface SchedulerWorkbenchFullConfigImportRespVO {
  userRoleBindingCount: number
  assignedRoleCount: number
}

export const SchedulerWorkbenchApi = {
  getSummary: async (date: string): Promise<SchedulerWorkbenchSummaryVO> => {
    return await request.get({ url: '/mes/pro/scheduler-workbench/summary', params: { date } })
  },
  getShiftHoursSetting: async (): Promise<SchedulerWorkbenchShiftHoursVO> => {
    return await request.get({ url: '/mes/pro/scheduler-workbench/shift-hours' })
  },
  saveShiftHoursSetting: async (
    data: SchedulerWorkbenchShiftHoursSaveReqVO
  ): Promise<SchedulerWorkbenchShiftHoursVO> => {
    return await request.put({ url: '/mes/pro/scheduler-workbench/shift-hours', data })
  },
  getCapacityUnificationAudit: async (): Promise<SchedulerWorkbenchCapacityUnificationAuditVO> => {
    return await request.get({ url: '/mes/pro/scheduler-workbench/capacity-unification-audit' })
  },  getPolicySettings: async (): Promise<SchedulerWorkbenchPolicySettingsVO> => {
    return await request.get({ url: '/mes/pro/scheduler-workbench/policy-settings' })
  },
  savePolicySettings: async (
    data: SchedulerWorkbenchPolicySettingsVO
  ): Promise<SchedulerWorkbenchPolicySettingsVO> => {
    return await request.put({ url: '/mes/pro/scheduler-workbench/policy-settings', data })
  },
  exportRouteConfigPackage: async (): Promise<Blob> => {
    return await request.download({ url: '/mes/pro/scheduler-workbench/route-config/export' })
  },
  importRouteConfigPackage: async (
    data: FormData
  ): Promise<SchedulerWorkbenchRouteConfigImportRespVO> => {
    const result = await request.upload<{ data: SchedulerWorkbenchRouteConfigImportRespVO }>({
      url: '/mes/pro/scheduler-workbench/route-config/import',
      data,
      timeout: SCHEDULER_WORKBENCH_IMPORT_TIMEOUT
    })
    return result.data
  },
  exportFullConfigPackage: async (): Promise<Blob> => {
    return await request.download({ url: '/mes/pro/scheduler-workbench/full-config/export' })
  },
  importFullConfigPackage: async (
    data: FormData
  ): Promise<SchedulerWorkbenchFullConfigImportRespVO> => {
    const result = await request.upload<{ data: SchedulerWorkbenchFullConfigImportRespVO }>({
      url: '/mes/pro/scheduler-workbench/full-config/import',
      data,
      timeout: SCHEDULER_WORKBENCH_IMPORT_TIMEOUT
    })
    return result.data
  },
  getSmokeTestStatus: async (): Promise<SchedulerWorkbenchSmokeTestStatusVO> => {
    return await request.get({ url: '/mes/pro/scheduler-workbench/smoke-test/status' })
  },
  startSmokeTest: async (
    data: SchedulerWorkbenchSmokeTestStartReqVO
  ): Promise<SchedulerWorkbenchSmokeTestStatusVO> => {
    return await request.post({ url: '/mes/pro/scheduler-workbench/smoke-test/start', data })
  },
  stopSmokeTest: async (): Promise<SchedulerWorkbenchSmokeTestStatusVO> => {
    return await request.post({ url: '/mes/pro/scheduler-workbench/smoke-test/stop' })
  }
}
