export const MES_SCHEDULE_ORDER_REFRESH_EVENT = 'mes-schedule-order-refresh'
export const MES_PRO_TASK_GANTT_REFRESH_EVENT = 'mes-pro-task-gantt-refresh'

export interface MesScheduleOrderRefreshPayload {
  source?: 'DIRECT_WORK_REPORT' | 'FORMAL_FEEDBACK'
  scheduleOrderCodes?: string[]
  workOrderCodes?: string[]
}

export interface MesProTaskGanttRefreshPayload {
  source?: 'REPLAN_APPLY'
  scheduleOrderIds?: number[]
  scheduleOrderCodes?: string[]
  workOrderCodes?: string[]
}
