import request from '@/config/axios'

export type ProScheduleCalendarWeekendRestMode = 'DOUBLE' | 'SINGLE' | 'NONE'
export type ProScheduleCalendarDateShiftMode = 'REST' | 'DAY'

export interface ProScheduleCalendarRulesRespVO {
  id?: number
  skipStatutoryHolidays: boolean
  weekendRestMode: ProScheduleCalendarWeekendRestMode
  dateShiftModeByDate: Record<string, ProScheduleCalendarDateShiftMode>
  simulationCurrentDate: string
  temporaryFreezeEnabled?: boolean
}

export interface ProScheduleCalendarRulesUpdateReqVO {
  skipStatutoryHolidays: boolean
  weekendRestMode: ProScheduleCalendarWeekendRestMode
  dateShiftModeByDate: Record<string, ProScheduleCalendarDateShiftMode>
}

export interface ProScheduleCalendarCapacityGenerateReqVO {
  startDate: string
  days: number
  lineIds?: number[]
}

export interface ProScheduleCalendarCapacityGenerateRespVO {
  startDate: string
  endDate: string
  lineCount: number
  generatedCount: number
  skippedExistingCount: number
  skippedRestCount: number
  skippedNoShiftCount: number
  skippedDetails?: ProScheduleCalendarCapacitySkippedDetailVO[]
}

export interface ProScheduleCalendarCapacitySkippedDetailVO {
  date: string
  lineCode?: string
  lineName?: string
  shiftName?: string
  reasonCode: string
  reasonText: string
}

export interface ProScheduleCalendarCurrentScheduleStatusVO {
  hasCurrentSchedule: boolean
  updatedAt?: string
  totalTaskCount: number
}

export interface ProScheduleCalendarMonthDayVO {
  date: string
  holiday: boolean
  dateShiftMode: ProScheduleCalendarDateShiftMode
  totalTaskCount: number
  totalOrderCount: number
  dayShiftTaskCount: number
  nightShiftTaskCount: number
  shortageCount: number
}

export interface ProScheduleCalendarMonthRespVO {
  month: string
  simulationCurrentDate: string
  currentScheduleStatus: ProScheduleCalendarCurrentScheduleStatusVO
  days: ProScheduleCalendarMonthDayVO[]
}

export interface ProScheduleCalendarTaskVO {
  taskId: number
  taskCode: string
  workOrderId: number
  workOrderCode: string
  routeId?: number
  routeName?: string
  processName?: string
  itemCode?: string
  itemName?: string
  shiftCode?: string
  quantity?: number
  dailyQuantity?: number
  reportedQuantity?: number
  pendingInspectionQuantity?: number
  executionStatus?: string
  startTime?: string
  endTime?: string
  scheduleSource?: string
  locked: boolean
  riskStatus?: string
  scheduleOrderFrozen?: boolean
  scheduleOrderFreezeReason?: string
}

export interface ProScheduleCalendarIssueItemVO {
  id?: number
  issueId?: number
  issueType?: string
  severity?: string
  objectType?: string
  objectId?: number
  workOrderId?: number
  workOrderCode?: string
  taskId?: number
  processId?: number
  processName?: string
  message?: string
  ownerRole?: string
  status?: string
  sourceType?: string
  sourceId?: number
}

export interface ProScheduleCalendarLineVO {
  lineId?: number | null
  lineCode?: string
  lineName?: string
  taskCount: number
  orderCount: number
  tasks: ProScheduleCalendarTaskVO[]
}

export interface ProScheduleCalendarWorkshopVO {
  workshopId?: number | null
  workshopCode?: string
  workshopName?: string
  taskCount: number
  orderCount: number
  busyLineCount: number
  lines: ProScheduleCalendarLineVO[]
}

export interface ProScheduleCalendarMaterialShortageItemVO {
  issueId: number
  severity?: string
  workOrderId?: number
  workOrderCode?: string
  materialId?: number
  materialCode?: string
  materialName?: string
  scheduledUsageQty?: number
  remainingAvailableQty?: number
  affectedWorkOrderCount: number
  requiredQty?: number
  availableQty?: number
  shortageQty?: number
  message?: string
}

export interface ProScheduleCalendarMaterialShortageSummaryVO {
  shortageCount: number
  totalShortageQty?: number
  items: ProScheduleCalendarMaterialShortageItemVO[]
}

export interface ProScheduleCalendarMaterialDemandTotalItemVO {
  materialId?: number
  materialCode?: string
  materialName?: string
  requiredQty?: number
  availableQty?: number
  shortageQty?: number
  affectedWorkOrderCount?: number
}

export interface ProScheduleCalendarMaterialDemandWorkOrderItemVO {
  workOrderId?: number
  workOrderCode?: string
  materialId?: number
  materialCode?: string
  materialName?: string
  requiredQty?: number
  availableQty?: number
  shortageQty?: number
}

export interface ProScheduleCalendarMaterialDemandSummaryVO {
  materialCount: number
  workOrderCount: number
  totalItems: ProScheduleCalendarMaterialDemandTotalItemVO[]
  workOrderItems: ProScheduleCalendarMaterialDemandWorkOrderItemVO[]
}

export interface ProScheduleCalendarIssueSummaryVO {
  openIssueCount: number
  blockingIssueCount: number
  items: ProScheduleCalendarIssueItemVO[]
}

export interface ProScheduleCalendarProcessCapacityItemVO {
  processId?: number
  processName?: string
  taskCount: number
  workOrderCount: number
  maxCapacity?: number
  scheduledQuantity?: number
  remainingCapacity?: number
  overCapacity?: number
  utilizationRate?: number
}

export interface ProScheduleCalendarProcessCapacitySummaryVO {
  processCount: number
  totalMaxCapacity?: number
  totalScheduledQuantity?: number
  totalRemainingCapacity?: number
  items: ProScheduleCalendarProcessCapacityItemVO[]
}

export interface ProScheduleCalendarDayDetailRespVO {
  date: string
  simulationCurrentDate: string
  holiday: boolean
  dateShiftMode: ProScheduleCalendarDateShiftMode
  dayShiftTaskCount: number
  nightShiftTaskCount: number
  workshops: ProScheduleCalendarWorkshopVO[]
  materialShortageSummary: ProScheduleCalendarMaterialShortageSummaryVO
  materialDemandSummary?: ProScheduleCalendarMaterialDemandSummaryVO
  scheduleIssueSummary?: ProScheduleCalendarIssueSummaryVO
  processCapacitySummary?: ProScheduleCalendarProcessCapacitySummaryVO
}

export interface ProScheduleCalendarWorkOrderAnalysisProcessVO {
  processId: number
  processName: string
  processSort: number
  scheduledQuantity?: number
  capacitySource: 'MACHINE' | 'WORKER'
  workstationCount: number
  workstationNames: string[]
  machineCount: number
  configuredWorkerCount: number
  currentWorkerCount: number
  effectiveHourlyCapacity?: number
  plannedDurationMinutes?: number
  startTime?: string
  endTime?: string
  bottleneck: boolean
}

export interface ProScheduleCalendarWorkOrderAnalysisVO {
  workOrderId: number
  workOrderCode: string
  productId?: number
  productCode?: string
  productName?: string
  quantity?: number
  conflict: boolean
  conflictMessage?: string
  lineId?: number
  lineCode?: string
  lineName?: string
  startTime?: string
  endTime?: string
  bottleneckProcessId?: number
  bottleneckProcessName?: string
  bottleneckHourlyCapacity?: number
  processes: ProScheduleCalendarWorkOrderAnalysisProcessVO[]
}

export const ProScheduleCalendarApi = {
  getRules: async () => {
    return await request.get<ProScheduleCalendarRulesRespVO>({
      url: '/mes/pro/schedule-calendar/rules'
    })
  },

  updateRules: async (data: ProScheduleCalendarRulesUpdateReqVO) => {
    return await request.put({
      url: '/mes/pro/schedule-calendar/rules',
      data
    })
  },

  advanceSimulationDay: async () => {
    return await request.post({
      url: '/mes/pro/schedule-calendar/simulation/advance-day'
    })
  },

  advanceSimulationDays: async (data: { days: number }) => {
    return await request.post({
      url: '/mes/pro/schedule-calendar/simulation/advance-days',
      data
    })
  },

  resetSimulation: async () => {
    return await request.post({
      url: '/mes/pro/schedule-calendar/simulation/reset'
    })
  },

  generateCapacityPlans: async (data: ProScheduleCalendarCapacityGenerateReqVO) => {
    return await request.post<ProScheduleCalendarCapacityGenerateRespVO>({
      url: '/mes/pro/schedule-calendar/capacity/generate',
      data
    })
  },

  getMonthCalendar: async (params: { month: string }) => {
    return await request.get<ProScheduleCalendarMonthRespVO>({
      url: '/mes/pro/schedule-calendar/month',
      params
    })
  },

  getDayDetail: async (params: { date: string }) => {
    return await request.get<ProScheduleCalendarDayDetailRespVO>({
      url: '/mes/pro/schedule-calendar/day-detail',
      params
    })
  },

  getWorkOrderAnalysis: async (params: { workOrderId: number }) => {
    return await request.get<ProScheduleCalendarWorkOrderAnalysisVO>({
      url: '/mes/pro/schedule-calendar/work-order-analysis',
      params
    })
  }
}
