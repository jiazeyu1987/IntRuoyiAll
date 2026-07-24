export interface PuhuiScheduleLine {
  id: string
  name: string
  baseCapacity: number
  capacityOverrides: Record<string, number>
  enabled: boolean
}

export interface PuhuiScheduleOrder {
  id: string
  orderNo: string
  productName: string
  spec: string
  batchNo: string
  workloadDays: number
  completedDays: number
  dueDate: string
  releaseDate: string
  priority: 'NORMAL' | 'URGENT'
  orderSeq: number
  lineWorkloads: Record<string, number>
  linePlanDays: Record<string, number>
  linePlanQuantities: Record<string, number>
}

export interface PuhuiScheduleScenario {
  schemaVersion: number
  nextOrderSeq: number
  planningMode: 'QTY_CAPACITY' | 'DURATION_MANUAL_FINISH'
  horizonStart: string
  horizonDays: number
  skipStatutoryHolidays: boolean
  weekendRestMode: 'NONE' | 'SINGLE' | 'DOUBLE'
  dateWorkModeByDate: Record<string, 'REST' | 'WORK'>
  manualFinishByLineOrder: Record<string, string>
  lines: PuhuiScheduleLine[]
  orders: PuhuiScheduleOrder[]
  locks: unknown[]
  simulationLogs: Array<{ date: string; completedWorkload: number; note: string }>
}

