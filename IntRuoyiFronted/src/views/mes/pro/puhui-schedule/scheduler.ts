export const EPSILON = 1e-6

export const PLANNING_MODE = {
  QTY_CAPACITY: 'QTY_CAPACITY',
  DURATION_MANUAL_FINISH: 'DURATION_MANUAL_FINISH'
} as const

export const WEEKEND_REST_MODE = {
  NONE: 'NONE',
  SINGLE: 'SINGLE',
  DOUBLE: 'DOUBLE'
} as const

export const DATE_WORK_MODE = {
  REST: 'REST',
  WORK: 'WORK'
} as const

export const LITE_SCENARIO_STORAGE_KEY = 'liteScheduler.scenario.v1'
export const LITE_SNAPSHOT_STORAGE_KEY = 'liteScheduler.scenario.snapshots.v1'

type PlanningMode = (typeof PLANNING_MODE)[keyof typeof PLANNING_MODE]
type WeekendRestMode = (typeof WEEKEND_REST_MODE)[keyof typeof WEEKEND_REST_MODE]
type DateWorkMode = (typeof DATE_WORK_MODE)[keyof typeof DATE_WORK_MODE]

export interface LiteLine {
  id: string
  name: string
  baseCapacity: number
  capacityOverrides: Record<string, number>
  enabled: boolean
}

export interface LiteOrder {
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

export interface LiteLock {
  id: string
  orderId: string
  lineId: string
  startDate: string
  endDate: string
  workloadDays: number
  seq: number
}

export interface LiteScenario {
  schemaVersion: number
  nextOrderSeq: number
  planningMode: PlanningMode
  horizonStart: string
  horizonDays: number
  skipStatutoryHolidays: boolean
  weekendRestMode: WeekendRestMode
  dateWorkModeByDate: Record<string, DateWorkMode>
  manualFinishByLineOrder: Record<string, string>
  lines: LiteLine[]
  orders: LiteOrder[]
  locks: LiteLock[]
  simulationLogs: Array<{ date: string; completedWorkload: number; note: string }>
}

export interface LiteAllocation {
  id: string
  orderId: string
  lineId: string
  date: string
  workloadDays: number
  source: string
  lockId: string | null
  segmentStartDate?: string
  plannedEndDate?: string
  actualEndDate?: string
  finishSource?: string
  manualFinishDate?: string | null
}

export interface LiteLineRow {
  lineId: string
  lineName: string
  assignedTotal: number
  capacityTotal: number
  utilization: number
  daily: Record<string, { capacity: number; assigned: number; utilization: number; items: LiteAllocation[] }>
}

export interface LiteOrderRow extends LiteOrder {
  scheduledDays: number
  autoScheduledDays: number
  lockedScheduledDays: number
  remainingDays: number
  completionDate: string | null
  delayDays: number | null
  reason: string
  finishStatus?: string
  actualFinishDate?: string | null
}

export interface LiteSchedulePlan {
  scenario: LiteScenario
  dates: string[]
  allocations: LiteAllocation[]
  warnings: string[]
  lineRows: LiteLineRow[]
  orderRows: LiteOrderRow[]
  summary: {
    horizonStart: string
    horizonEnd: string
    totalOrders: number
    totalCapacity: number
    totalAssigned: number
    utilization: number
    delayedOrders: number
    totalRemaining: number
    bottleneckLineId: string | null
    bottleneckLineName: string | null
  }
}

export interface LiteSnapshot {
  id: string
  name: string
  createdAt: number
  updatedAt: number
  scenario: LiteScenario
}

export interface OrderModalForm {
  orderNo: string
  productName: string
  spec: string
  batchNo: string
  dueDate: string
  releaseDate: string
  priority: 'NORMAL'
  lineTotals: Record<string, string>
  linePlanDays: Record<string, string>
  linePlanQuantities: Record<string, string>
}

const DAY_MS = 24 * 60 * 60 * 1000

const CN_STATUTORY_HOLIDAY_DATES_BY_YEAR: Record<number, ReadonlyArray<string>> = Object.freeze({
  2024: Object.freeze([
    '2024-01-01',
    '2024-02-10',
    '2024-02-11',
    '2024-02-12',
    '2024-02-13',
    '2024-02-14',
    '2024-02-15',
    '2024-02-16',
    '2024-02-17',
    '2024-04-04',
    '2024-04-05',
    '2024-04-06',
    '2024-05-01',
    '2024-05-02',
    '2024-05-03',
    '2024-05-04',
    '2024-05-05',
    '2024-06-08',
    '2024-06-09',
    '2024-06-10',
    '2024-09-15',
    '2024-09-16',
    '2024-09-17',
    '2024-10-01',
    '2024-10-02',
    '2024-10-03',
    '2024-10-04',
    '2024-10-05',
    '2024-10-06',
    '2024-10-07'
  ]),
  2025: Object.freeze([
    '2025-01-01',
    '2025-01-28',
    '2025-01-29',
    '2025-01-30',
    '2025-01-31',
    '2025-02-01',
    '2025-02-02',
    '2025-02-03',
    '2025-02-04',
    '2025-04-04',
    '2025-04-05',
    '2025-04-06',
    '2025-05-01',
    '2025-05-02',
    '2025-05-03',
    '2025-05-04',
    '2025-05-05',
    '2025-05-31',
    '2025-06-01',
    '2025-06-02',
    '2025-10-01',
    '2025-10-02',
    '2025-10-03',
    '2025-10-04',
    '2025-10-05',
    '2025-10-06',
    '2025-10-07',
    '2025-10-08'
  ]),
  2026: Object.freeze([
    '2026-01-01',
    '2026-01-02',
    '2026-01-03',
    '2026-02-15',
    '2026-02-16',
    '2026-02-17',
    '2026-02-18',
    '2026-02-19',
    '2026-02-20',
    '2026-02-21',
    '2026-02-22',
    '2026-02-23',
    '2026-04-04',
    '2026-04-05',
    '2026-04-06',
    '2026-05-01',
    '2026-05-02',
    '2026-05-03',
    '2026-05-04',
    '2026-05-05',
    '2026-06-19',
    '2026-06-20',
    '2026-06-21',
    '2026-09-25',
    '2026-09-26',
    '2026-09-27',
    '2026-10-01',
    '2026-10-02',
    '2026-10-03',
    '2026-10-04',
    '2026-10-05',
    '2026-10-06',
    '2026-10-07'
  ]),
  2027: Object.freeze([]),
  2028: Object.freeze([])
})

const CN_STATUTORY_HOLIDAY_DATE_SET = new Set(
  Object.values(CN_STATUTORY_HOLIDAY_DATES_BY_YEAR).flat()
)

function pad2(value: number | string) {
  return String(value).padStart(2, '0')
}

function toIsoDateFromUtc(date: Date) {
  return `${date.getUTCFullYear()}-${pad2(date.getUTCMonth() + 1)}-${pad2(date.getUTCDate())}`
}

export function clampNumber(value: unknown, fallback = 0) {
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

export function round3(value: number) {
  return Math.round(value * 1000) / 1000
}

export function positiveOr(value: unknown, fallback = 0) {
  return Math.max(0, round3(clampNumber(value, fallback)))
}

export function makeId(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export function makeLineOrderKey(lineId: string, orderId: string) {
  return `${String(lineId || '').trim()}|${String(orderId || '').trim()}`
}

export function parseIsoDate(value: unknown) {
  const text = String(value || '').trim()
  const match = text.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) {
    return null
  }
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) {
    return null
  }
  if (month < 1 || month > 12 || day < 1 || day > 31) {
    return null
  }
  const ts = Date.UTC(year, month - 1, day)
  const date = new Date(ts)
  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() + 1 !== month ||
    date.getUTCDate() !== day
  ) {
    return null
  }
  return { year, month, day, ts }
}

function dayNumber(dateText: unknown) {
  const parsed = parseIsoDate(dateText)
  return parsed ? Math.round(parsed.ts / DAY_MS) : null
}

export function isoToday() {
  return toIsoDateFromUtc(new Date())
}

export function addDays(dateText: string, offset: number) {
  const parsed = parseIsoDate(dateText)
  if (!parsed) {
    return dateText
  }
  return toIsoDateFromUtc(new Date(parsed.ts + Number(offset || 0) * DAY_MS))
}

export function compareDate(a: unknown, b: unknown) {
  if (a === b) {
    return 0
  }
  const aDay = dayNumber(a)
  const bDay = dayNumber(b)
  if (aDay === null && bDay === null) {
    return String(a || '').localeCompare(String(b || ''))
  }
  if (aDay === null) {
    return -1
  }
  if (bDay === null) {
    return 1
  }
  return aDay - bDay
}

export function diffDays(laterDate: string, earlierDate: string) {
  const a = dayNumber(laterDate)
  const b = dayNumber(earlierDate)
  if (a === null || b === null) {
    return 0
  }
  return a - b
}

export function supportedCnHolidayYears() {
  return Object.keys(CN_STATUTORY_HOLIDAY_DATES_BY_YEAR)
    .map((year) => Number(year))
    .sort((a, b) => a - b)
}

export function isCnStatutoryHoliday(dateText: string) {
  const parsed = parseIsoDate(dateText)
  if (!parsed) {
    return false
  }
  const normalized = `${parsed.year}-${pad2(parsed.month)}-${pad2(parsed.day)}`
  return CN_STATUTORY_HOLIDAY_DATE_SET.has(normalized)
}

function isWeekendDate(dateText: string) {
  const parsed = parseIsoDate(dateText)
  if (!parsed) {
    return false
  }
  const weekday = new Date(parsed.ts).getUTCDay()
  return weekday === 0 || weekday === 6
}

function isSingleRestDay(dateText: string) {
  const parsed = parseIsoDate(dateText)
  if (!parsed) {
    return false
  }
  return new Date(parsed.ts).getUTCDay() === 0
}

export function normalizeWeekendRestMode(value: unknown): WeekendRestMode {
  const modeText = String(value || '').toUpperCase()
  if (modeText === WEEKEND_REST_MODE.NONE) {
    return WEEKEND_REST_MODE.NONE
  }
  if (modeText === WEEKEND_REST_MODE.SINGLE) {
    return WEEKEND_REST_MODE.SINGLE
  }
  return WEEKEND_REST_MODE.DOUBLE
}

export function normalizeDateWorkModeByDate(inputValue: unknown): Record<string, DateWorkMode> {
  const normalized: Record<string, DateWorkMode> = {}
  Object.entries((inputValue || {}) as Record<string, unknown>).forEach(([dateKey, modeRaw]) => {
    if (!parseIsoDate(dateKey)) {
      return
    }
    const mode = String(modeRaw || '').toUpperCase()
    if (mode === DATE_WORK_MODE.REST || mode === DATE_WORK_MODE.WORK) {
      normalized[dateKey] = mode
    }
  })
  return normalized
}

export function isSkippedPlanningDate(
  dateText: string,
  skipStatutoryHolidays: boolean,
  weekendRestMode: WeekendRestMode = WEEKEND_REST_MODE.DOUBLE,
  dateWorkModeByDate: Record<string, DateWorkMode> = {}
) {
  const dayMode = dateWorkModeByDate?.[dateText]
  if (dayMode === DATE_WORK_MODE.WORK) {
    return false
  }
  if (dayMode === DATE_WORK_MODE.REST) {
    return true
  }
  if (isCnStatutoryHoliday(dateText)) {
    return skipStatutoryHolidays
  }
  if (!skipStatutoryHolidays) {
    return false
  }
  if (weekendRestMode === WEEKEND_REST_MODE.NONE) {
    return false
  }
  if (weekendRestMode === WEEKEND_REST_MODE.SINGLE) {
    return isSingleRestDay(dateText)
  }
  return isWeekendDate(dateText)
}

export function nextBusinessDate(
  dateText: string,
  skipStatutoryHolidays: boolean,
  weekendRestMode: WeekendRestMode = WEEKEND_REST_MODE.DOUBLE,
  dateWorkModeByDate: Record<string, DateWorkMode> = {}
) {
  let nextDate = addDays(dateText, 1)
  let guard = 0
  while (
    isSkippedPlanningDate(nextDate, skipStatutoryHolidays, weekendRestMode, dateWorkModeByDate) &&
    guard < 370
  ) {
    const candidate = addDays(nextDate, 1)
    if (candidate === nextDate) {
      break
    }
    nextDate = candidate
    guard += 1
  }
  return nextDate
}

export function buildDateRange(
  startDate: string,
  horizonDays: number,
  skipStatutoryHolidays = false,
  weekendRestMode: WeekendRestMode = WEEKEND_REST_MODE.DOUBLE,
  dateWorkModeByDate: Record<string, DateWorkMode> = {}
) {
  const safeDays = Math.max(1, Math.round(clampNumber(horizonDays, 30)))
  const safeWeekendMode = normalizeWeekendRestMode(weekendRestMode)
  const safeDateWorkModeByDate = normalizeDateWorkModeByDate(dateWorkModeByDate)
  const parsedStart = parseIsoDate(startDate)
  const hasDateOverride = Object.keys(safeDateWorkModeByDate).length > 0
  if (!parsedStart || (!skipStatutoryHolidays && !hasDateOverride)) {
    return Array.from({ length: safeDays }, (_, idx) => addDays(startDate, idx))
  }

  const dates: string[] = []
  let cursor = `${parsedStart.year}-${pad2(parsedStart.month)}-${pad2(parsedStart.day)}`
  let guard = 0
  while (dates.length < safeDays && guard < safeDays * 400) {
    if (!isSkippedPlanningDate(cursor, skipStatutoryHolidays, safeWeekendMode, safeDateWorkModeByDate)) {
      dates.push(cursor)
    }
    const next = addDays(cursor, 1)
    if (next === cursor) {
      break
    }
    cursor = next
    guard += 1
  }
  return dates.length >= safeDays
    ? dates
    : Array.from({ length: safeDays }, (_, idx) => addDays(startDate, idx))
}

export function createDefaultLiteScenario(baseDate = isoToday()): LiteScenario {
  const start = parseIsoDate(baseDate) ? baseDate : isoToday()
  return {
    schemaVersion: 1,
    nextOrderSeq: 1,
    planningMode: PLANNING_MODE.QTY_CAPACITY,
    horizonStart: start,
    horizonDays: 30,
    skipStatutoryHolidays: false,
    weekendRestMode: WEEKEND_REST_MODE.DOUBLE,
    dateWorkModeByDate: {},
    manualFinishByLineOrder: {},
    lines: [
      {
        id: makeId('line'),
        name: '导管产线',
        baseCapacity: 300,
        capacityOverrides: {},
        enabled: true
      }
    ],
    orders: [],
    locks: [],
    simulationLogs: []
  }
}

function normalizePlanningMode(value: unknown): PlanningMode {
  return String(value || '').toUpperCase() === PLANNING_MODE.DURATION_MANUAL_FINISH
    ? PLANNING_MODE.DURATION_MANUAL_FINISH
    : PLANNING_MODE.QTY_CAPACITY
}

function normalizeLine(line: any, index: number): LiteLine {
  const id = String(line?.id || makeId('line'))
  const name = String(line?.name || '').trim() || `产线-${index + 1}`
  const baseCapacity = positiveOr(line?.baseCapacity, 300)
  const capacityOverrides: Record<string, number> = {}
  Object.entries((line?.capacityOverrides || {}) as Record<string, unknown>).forEach(
    ([dateKey, capacityValue]) => {
      if (parseIsoDate(dateKey)) {
        capacityOverrides[dateKey] = positiveOr(capacityValue, baseCapacity)
      }
    }
  )
  return { id, name, baseCapacity, capacityOverrides, enabled: line?.enabled !== false }
}

function extractTrailingNumber(text: string) {
  const match = String(text || '').match(/(\d+)(?!.*\d)/)
  if (!match) {
    return null
  }
  const n = Number(match[1])
  return Number.isFinite(n) ? n : null
}

function normalizeOrderLineWorkloads(inputValue: unknown) {
  const normalized: Record<string, number> = {}
  Object.entries((inputValue || {}) as Record<string, unknown>).forEach(([lineIdRaw, daysRaw]) => {
    const lineId = String(lineIdRaw || '').trim()
    const days = positiveOr(daysRaw, 0)
    if (lineId && days > EPSILON) {
      normalized[lineId] = round3((normalized[lineId] || 0) + days)
    }
  })
  return normalized
}

function normalizeOrderLinePlanDays(inputValue: unknown) {
  const normalized: Record<string, number> = {}
  Object.entries((inputValue || {}) as Record<string, unknown>).forEach(([lineIdRaw, daysRaw]) => {
    const lineId = String(lineIdRaw || '').trim()
    const days = Math.max(0, Math.round(clampNumber(daysRaw, 0)))
    if (lineId && days > 0) {
      normalized[lineId] = days
    }
  })
  return normalized
}

function normalizeOrderLinePlanQuantities(inputValue: unknown) {
  const normalized: Record<string, number> = {}
  Object.entries((inputValue || {}) as Record<string, unknown>).forEach(([lineIdRaw, qtyRaw]) => {
    const lineId = String(lineIdRaw || '').trim()
    const qty = Math.max(0, Math.round(clampNumber(qtyRaw, 0)))
    if (lineId && qty > 0) {
      normalized[lineId] = qty
    }
  })
  return normalized
}

function normalizeOrder(order: any, index: number, horizonStart: string): LiteOrder {
  const id = String(order?.id || makeId('order'))
  const orderNo = String(order?.orderNo || '').trim() || `SO-${pad2(index + 1)}`
  const productName = String(order?.productName ?? order?.product_name ?? order?.product_name_cn ?? '').trim()
  const spec = String(order?.spec ?? order?.specModel ?? order?.spec_model ?? '').trim()
  const batchNo = String(order?.batchNo ?? order?.batch_no ?? order?.production_batch_no ?? '').trim()
  const inferredSeq = extractTrailingNumber(orderNo) ?? index + 1
  const orderSeqRaw = Number(order?.orderSeq)
  const orderSeq = Number.isFinite(orderSeqRaw)
    ? Math.max(1, Math.round(orderSeqRaw))
    : Math.max(1, inferredSeq)
  const lineWorkloads = normalizeOrderLineWorkloads(order?.lineWorkloads)
  const linePlanDays = normalizeOrderLinePlanDays(order?.linePlanDays ?? order?.line_plan_days)
  const linePlanQuantities = normalizeOrderLinePlanQuantities(
    order?.linePlanQuantities ?? order?.line_plan_quantities
  )
  const lineWorkloadTotal = round3(Object.values(lineWorkloads).reduce((sum, value) => sum + value, 0))
  const linePlanDayMax = round3(
    Object.values(linePlanDays).reduce((maxValue, value) => Math.max(maxValue, value), 0)
  )
  const baseWorkloadDays = positiveOr(
    order?.workloadDays,
    lineWorkloadTotal > EPSILON ? lineWorkloadTotal : linePlanDayMax > EPSILON ? linePlanDayMax : 1
  )
  const workloadDays = Math.max(baseWorkloadDays, lineWorkloadTotal, linePlanDayMax)
  const completedDays = Math.min(workloadDays, positiveOr(order?.completedDays, 0))
  const dueDate = parseIsoDate(order?.dueDate) ? order.dueDate : addDays(horizonStart, 7)
  const releaseDate = parseIsoDate(order?.releaseDate) ? order.releaseDate : horizonStart
  return {
    id,
    orderNo,
    productName,
    spec,
    batchNo,
    workloadDays,
    completedDays,
    dueDate,
    releaseDate,
    priority: order?.priority === 'URGENT' ? 'URGENT' : 'NORMAL',
    orderSeq,
    lineWorkloads,
    linePlanDays,
    linePlanQuantities
  }
}

function inferNextOrderSeq(orders: LiteOrder[]) {
  let maxSeq = 0
  orders.forEach((order) => {
    const seq = Number(order.orderSeq)
    if (Number.isFinite(seq) && seq > maxSeq) {
      maxSeq = seq
    }
  })
  return Math.max(1, maxSeq + 1)
}

function normalizeLock(lock: any, index: number, fallbackStart: string): LiteLock {
  const id = String(lock?.id || makeId('lock'))
  const orderId = String(lock?.orderId || '').trim()
  const lineId = String(lock?.lineId || '').trim()
  const startDateRaw = parseIsoDate(lock?.startDate) ? lock.startDate : fallbackStart
  const endDateRaw = parseIsoDate(lock?.endDate) ? lock.endDate : startDateRaw
  const startDate = compareDate(startDateRaw, endDateRaw) <= 0 ? startDateRaw : endDateRaw
  const endDate = compareDate(startDateRaw, endDateRaw) <= 0 ? endDateRaw : startDateRaw
  return {
    id,
    orderId,
    lineId,
    startDate,
    endDate,
    workloadDays: positiveOr(lock?.workloadDays, 0),
    seq: Number(lock?.seq ?? index)
  }
}

function parseLineOrderKey(text: string) {
  const parts = String(text || '').split('|')
  if (parts.length !== 2) {
    return null
  }
  const lineId = String(parts[0] || '').trim()
  const orderId = String(parts[1] || '').trim()
  return lineId && orderId ? { lineId, orderId } : null
}

function normalizeManualFinishByLineOrder(
  inputValue: unknown,
  orderIdSet: Set<string>,
  lineIdSet: Set<string>
) {
  const normalized: Record<string, string> = {}
  Object.entries((inputValue || {}) as Record<string, unknown>).forEach(([keyRaw, dateRaw]) => {
    const parsedKey = parseLineOrderKey(keyRaw)
    const dateText = String(dateRaw || '')
    if (
      parsedKey &&
      lineIdSet.has(parsedKey.lineId) &&
      orderIdSet.has(parsedKey.orderId) &&
      parseIsoDate(dateText)
    ) {
      normalized[makeLineOrderKey(parsedKey.lineId, parsedKey.orderId)] = dateText
    }
  })
  return normalized
}

function sanitizeLinePlans(order: LiteOrder, lineIdSet: Set<string>) {
  const nextLineWorkloads: Record<string, number> = {}
  Object.entries(order.lineWorkloads || {}).forEach(([lineId, value]) => {
    if (lineIdSet.has(lineId) && value > EPSILON) {
      nextLineWorkloads[lineId] = value
    }
  })
  const nextLinePlanDays: Record<string, number> = {}
  Object.entries(order.linePlanDays || {}).forEach(([lineId, value]) => {
    const days = Math.max(0, Math.round(clampNumber(value, 0)))
    if (lineIdSet.has(lineId) && days > 0) {
      nextLinePlanDays[lineId] = days
    }
  })
  const nextLinePlanQuantities: Record<string, number> = {}
  Object.entries(order.linePlanQuantities || {}).forEach(([lineId, value]) => {
    const qty = Math.max(0, Math.round(clampNumber(value, 0)))
    if (lineIdSet.has(lineId) && qty > 0) {
      nextLinePlanQuantities[lineId] = qty
    }
  })
  const lineTotal = round3(Object.values(nextLineWorkloads).reduce((sum, value) => sum + value, 0))
  const linePlanTotal = round3(Object.values(nextLinePlanDays).reduce((sum, value) => sum + value, 0))
  const workloadDays = Math.max(order.workloadDays, lineTotal, linePlanTotal)
  return {
    workloadDays,
    completedDays: Math.min(workloadDays, order.completedDays),
    lineWorkloads: nextLineWorkloads,
    linePlanDays: nextLinePlanDays,
    linePlanQuantities: nextLinePlanQuantities
  }
}

export function normalizeLiteScenario(input: any): LiteScenario {
  const fallback = createDefaultLiteScenario()
  const planningMode = normalizePlanningMode(input?.planningMode)
  const horizonStart = parseIsoDate(input?.horizonStart) ? input.horizonStart : fallback.horizonStart
  const horizonDays = Math.max(1, Math.round(clampNumber(input?.horizonDays, 30)))
  const lines: LiteLine[] = Array.isArray(input?.lines)
    ? input.lines.map((line: any, index: number) => normalizeLine(line, index)).filter((line: LiteLine) => line.id)
    : []
  const safeLines: LiteLine[] = lines.length > 0 ? lines : fallback.lines
  const lineIdSet = new Set<string>(safeLines.map((line) => line.id))
  const rawOrders: LiteOrder[] = Array.isArray(input?.orders)
    ? input.orders.map((order: any, index: number) => normalizeOrder(order, index, horizonStart))
    : []
  const orders: LiteOrder[] = rawOrders.map((order) => ({ ...order, ...sanitizeLinePlans(order, lineIdSet) }))
  const orderIdSet = new Set<string>(orders.map((order) => order.id))
  const nextOrderSeqRaw = Number(input?.nextOrderSeq)
  const nextOrderSeq = Number.isFinite(nextOrderSeqRaw)
    ? Math.max(1, Math.round(nextOrderSeqRaw))
    : inferNextOrderSeq(orders)

  const locks = Array.isArray(input?.locks)
    ? input.locks
        .map((lock: any, index: number) => normalizeLock(lock, index, horizonStart))
        .filter(
          (lock: LiteLock) =>
            lock.workloadDays > EPSILON &&
            lock.orderId &&
            lock.lineId &&
            orderIdSet.has(lock.orderId) &&
            lineIdSet.has(lock.lineId)
        )
    : []

  const simulationLogs = Array.isArray(input?.simulationLogs)
    ? input.simulationLogs
        .map((row: any) => ({
          date: parseIsoDate(row?.date) ? row.date : horizonStart,
          completedWorkload: positiveOr(row?.completedWorkload, 0),
          note: String(row?.note || '').trim()
        }))
        .slice(0, 30)
    : []

  return {
    schemaVersion: 1,
    nextOrderSeq,
    planningMode,
    horizonStart,
    horizonDays,
    skipStatutoryHolidays: input?.skipStatutoryHolidays === true,
    weekendRestMode: normalizeWeekendRestMode(input?.weekendRestMode),
    dateWorkModeByDate: normalizeDateWorkModeByDate(input?.dateWorkModeByDate),
    manualFinishByLineOrder: normalizeManualFinishByLineOrder(
      input?.manualFinishByLineOrder,
      orderIdSet,
      lineIdSet
    ),
    lines: safeLines,
    orders,
    locks,
    simulationLogs
  }
}

export function resolveLineCapacity(line: LiteLine, date: string) {
  if (Object.prototype.hasOwnProperty.call(line.capacityOverrides, date)) {
    return positiveOr(line.capacityOverrides[date], line.baseCapacity)
  }
  return positiveOr(line.baseCapacity, 0)
}

export function orderSortForReplan(a: LiteOrder, b: LiteOrder) {
  const seqCmp = (Number(a.orderSeq) || 0) - (Number(b.orderSeq) || 0)
  return seqCmp !== 0 ? seqCmp : String(a.orderNo).localeCompare(String(b.orderNo), 'zh-Hans-CN')
}

function makeAllocation(
  orderId: string,
  lineId: string,
  date: string,
  workloadDays: number,
  source: string,
  lockId: string | null = null
): LiteAllocation {
  return {
    id: makeId('alloc'),
    orderId,
    lineId,
    date,
    workloadDays: round3(workloadDays),
    source,
    lockId
  }
}

function sortLocksByDateAndSeq(locks: LiteLock[]) {
  return (locks || []).slice().sort((a, b) => {
    const startCmp = compareDate(a.startDate, b.startDate)
    return startCmp !== 0 ? startCmp : (a.seq || 0) - (b.seq || 0)
  })
}

function buildScheduleSummary(params: {
  scenario: LiteScenario
  dates: string[]
  lineRows: LiteLineRow[]
  orderRows: LiteOrderRow[]
  totalOrders: number
  totalCapacity: number
  totalAssigned: number
  totalRemaining: number
}) {
  const bottleneck = params.lineRows.slice().sort((a, b) => b.utilization - a.utilization)[0]
  return {
    horizonStart: params.scenario.horizonStart,
    horizonEnd: params.dates[params.dates.length - 1],
    totalOrders: params.totalOrders,
    totalCapacity: params.totalCapacity,
    totalAssigned: params.totalAssigned,
    utilization: params.totalCapacity > EPSILON ? params.totalAssigned / params.totalCapacity : 0,
    delayedOrders: params.orderRows.filter((row) => Number(row.delayDays) > 0).length,
    totalRemaining: params.totalRemaining,
    bottleneckLineId: bottleneck?.lineId || null,
    bottleneckLineName: bottleneck?.lineName || null
  }
}

function buildLiteScheduleByQuantityCapacity(scenario: LiteScenario): LiteSchedulePlan {
  const dates = buildDateRange(
    scenario.horizonStart,
    scenario.horizonDays,
    scenario.skipStatutoryHolidays,
    scenario.weekendRestMode,
    scenario.dateWorkModeByDate
  )
  const lines = scenario.lines.filter((line) => line.enabled !== false)
  const orders = scenario.orders
  const lineSet = new Set(lines.map((line) => line.id))
  const warnings: string[] = []
  const allocations: LiteAllocation[] = []
  const lineCapByDate: Record<string, Record<string, number>> = {}
  const lineRemainingCap: Record<string, Record<string, number>> = {}
  const orderRemaining: Record<string, number> = {}
  const orderLineRemaining: Record<string, Record<string, number>> = {}

  lines.forEach((line) => {
    lineCapByDate[line.id] = {}
    lineRemainingCap[line.id] = {}
    dates.forEach((date) => {
      const cap = resolveLineCapacity(line, date)
      lineCapByDate[line.id][date] = cap
      lineRemainingCap[line.id][date] = cap
    })
  })

  orders.forEach((order) => {
    const remaining = Math.max(0, round3(order.workloadDays - order.completedDays))
    orderRemaining[order.id] = remaining
    const requested: Record<string, number> = {}
    Object.entries(order.lineWorkloads || {}).forEach(([lineId, qty]) => {
      if (!lineSet.has(lineId)) {
        warnings.push(`订单 ${order.orderNo} 指定产线 ${lineId} 不存在，已忽略。`)
        return
      }
      if (qty > EPSILON) {
        requested[lineId] = qty
      }
    })
    const requestedTotal = round3(Object.values(requested).reduce((sum, value) => sum + value, 0))
    const ratio = requestedTotal > EPSILON && remaining > EPSILON ? Math.min(1, remaining / requestedTotal) : 0
    const normalized: Record<string, number> = {}
    Object.entries(requested).forEach(([lineId, qty]) => {
      const value = round3(qty * ratio)
      if (value > EPSILON) {
        normalized[lineId] = value
      }
    })
    orderLineRemaining[order.id] = normalized
  })

  function reduceLineRequirement(orderId: string, lineId: string, qty: number) {
    const safeQty = positiveOr(qty, 0)
    const map = orderLineRemaining[orderId]
    if (safeQty <= EPSILON || !map || !Object.prototype.hasOwnProperty.call(map, lineId)) {
      return
    }
    map[lineId] = round3((map[lineId] || 0) - safeQty)
    if (map[lineId] <= EPSILON) {
      delete map[lineId]
    }
  }

  function allocateOne(orderId: string, lineId: string, date: string, wanted: number, source: string, lockId: string | null = null) {
    const qty = Math.min(
      positiveOr(wanted, 0),
      orderRemaining[orderId] ?? 0,
      lineRemainingCap[lineId]?.[date] ?? 0
    )
    if (qty <= EPSILON) {
      return 0
    }
    const safeQty = round3(qty)
    orderRemaining[orderId] = round3((orderRemaining[orderId] || 0) - safeQty)
    lineRemainingCap[lineId][date] = round3((lineRemainingCap[lineId][date] || 0) - safeQty)
    allocations.push(makeAllocation(orderId, lineId, date, safeQty, source, lockId))
    return safeQty
  }

  sortLocksByDateAndSeq(scenario.locks).forEach((lock) => {
    if (!Object.prototype.hasOwnProperty.call(orderRemaining, lock.orderId)) {
      warnings.push(`锁定片段 ${lock.id} 对应订单不存在，已跳过。`)
      return
    }
    if (!lineRemainingCap[lock.lineId]) {
      warnings.push(`锁定片段 ${lock.id} 对应产线不存在，已跳过。`)
      return
    }
    const lockDates = dates.filter(
      (date) => compareDate(date, lock.startDate) >= 0 && compareDate(date, lock.endDate) <= 0
    )
    if (lockDates.length === 0) {
      warnings.push(`锁定片段 ${lock.id} 不在当前周期内，已跳过。`)
      return
    }
    let left = Math.min(lock.workloadDays, orderRemaining[lock.orderId] || 0)
    lockDates.forEach((date) => {
      if (left <= EPSILON) {
        return
      }
      const done = allocateOne(lock.orderId, lock.lineId, date, left, 'LOCKED', lock.id)
      reduceLineRequirement(lock.orderId, lock.lineId, done)
      left = round3(left - done)
    })
  })

  dates.forEach((date) => {
    const activeLineIds = lines
      .map((line) => line.id)
      .filter((lineId) => (lineRemainingCap[lineId]?.[date] || 0) > EPSILON)
    if (activeLineIds.length === 0) {
      return
    }
    const candidates = orders
      .filter((order) => (orderRemaining[order.id] || 0) > EPSILON && compareDate(date, order.releaseDate) >= 0)
      .sort(orderSortForReplan)

    candidates.forEach((order) => {
      Object.entries(orderLineRemaining[order.id] || {})
        .filter(([, qty]) => qty > EPSILON)
        .sort((a, b) => b[1] - a[1])
        .forEach(([lineId, qty]) => {
          if (lineRemainingCap[lineId]) {
            const done = allocateOne(order.id, lineId, date, qty, 'AUTO_LINE')
            reduceLineRequirement(order.id, lineId, done)
          }
        })
    })

    candidates.forEach((order) => {
      let left = orderRemaining[order.id] || 0
      if (left <= EPSILON) {
        return
      }
      const dynamicLines = activeLineIds
        .slice()
        .sort((a, b) => (lineRemainingCap[b]?.[date] || 0) - (lineRemainingCap[a]?.[date] || 0))
      dynamicLines.forEach((lineId) => {
        if (left <= EPSILON) {
          return
        }
        const done = allocateOne(order.id, lineId, date, left, 'AUTO')
        left = round3(left - done)
      })
    })
  })

  const allocationMap: Record<string, LiteAllocation[]> = {}
  const orderAllocByDate: Record<string, Record<string, number>> = {}
  const orderAutoTotals: Record<string, number> = {}
  const orderLockedTotals: Record<string, number> = {}
  allocations.forEach((item) => {
    const lineDateKey = `${item.lineId}|${item.date}`
    allocationMap[lineDateKey] = allocationMap[lineDateKey] || []
    allocationMap[lineDateKey].push(item)
    const dateMap = (orderAllocByDate[item.orderId] = orderAllocByDate[item.orderId] || {})
    dateMap[item.date] = round3((dateMap[item.date] || 0) + item.workloadDays)
    if (item.source === 'LOCKED') {
      orderLockedTotals[item.orderId] = round3((orderLockedTotals[item.orderId] || 0) + item.workloadDays)
    } else {
      orderAutoTotals[item.orderId] = round3((orderAutoTotals[item.orderId] || 0) + item.workloadDays)
    }
  })

  const nominalDailyCapacity =
    dates.reduce(
      (sum, date) => sum + lines.reduce((lineSum, line) => lineSum + (lineCapByDate[line.id]?.[date] || 0), 0),
      0
    ) / Math.max(dates.length, 1)

  const orderRows = orders.map((order): LiteOrderRow => {
    const remaining = orderRemaining[order.id] || 0
    const dateMap = orderAllocByDate[order.id] || {}
    let completed = order.completedDays
    let completionDate: string | null = completed >= order.workloadDays ? addDays(scenario.horizonStart, -1) : null
    dates.forEach((date) => {
      if (completionDate) {
        return
      }
      completed = round3(completed + (dateMap[date] || 0))
      if (completed + EPSILON >= order.workloadDays) {
        completionDate = date
      }
    })
    if (!completionDate && remaining > EPSILON && nominalDailyCapacity > EPSILON) {
      completionDate = addDays(dates[dates.length - 1], Math.ceil(remaining / nominalDailyCapacity))
    }
    const hasLinePreference = Object.values(order.lineWorkloads || {}).reduce((sum, value) => sum + value, 0) > EPSILON
    const reasonParts: string[] = []
    if (hasLinePreference) {
      reasonParts.push('优先满足指定产线工作量')
    }
    if ((orderLockedTotals[order.id] || 0) > EPSILON) {
      reasonParts.push('手动锁定优先')
    }
    reasonParts.push('其余按订单顺序补齐')
    return {
      ...order,
      scheduledDays: round3(order.workloadDays - remaining - order.completedDays),
      autoScheduledDays: round3(orderAutoTotals[order.id] || 0),
      lockedScheduledDays: round3(orderLockedTotals[order.id] || 0),
      remainingDays: round3(remaining),
      completionDate,
      delayDays: completionDate ? Math.max(0, diffDays(completionDate, order.dueDate)) : null,
      reason: `${reasonParts.join('；')}。`
    }
  })

  const lineRows = lines.map((line): LiteLineRow => {
    let assignedTotal = 0
    let capacityTotal = 0
    const daily: LiteLineRow['daily'] = {}
    dates.forEach((date) => {
      const cap = lineCapByDate[line.id]?.[date] || 0
      const items = allocationMap[`${line.id}|${date}`] || []
      const assigned = round3(items.reduce((sum, item) => sum + item.workloadDays, 0))
      assignedTotal = round3(assignedTotal + assigned)
      capacityTotal = round3(capacityTotal + cap)
      daily[date] = { capacity: cap, assigned, utilization: cap > EPSILON ? assigned / cap : 0, items }
    })
    return {
      lineId: line.id,
      lineName: line.name,
      assignedTotal,
      capacityTotal,
      utilization: capacityTotal > EPSILON ? assignedTotal / capacityTotal : 0,
      daily
    }
  })

  const totalCapacity = round3(lineRows.reduce((sum, line) => sum + line.capacityTotal, 0))
  const totalAssigned = round3(allocations.reduce((sum, item) => sum + item.workloadDays, 0))
  const totalRemaining = round3(Object.values(orderRemaining).reduce((sum, value) => sum + value, 0))

  return {
    scenario,
    dates,
    allocations,
    warnings,
    lineRows,
    orderRows,
    summary: buildScheduleSummary({
      scenario,
      dates,
      lineRows,
      orderRows,
      totalOrders: orders.length,
      totalCapacity,
      totalAssigned,
      totalRemaining
    })
  }
}

export function alignToBusinessDate(dateText: string, scenario: LiteScenario) {
  let cursor = parseIsoDate(dateText) ? dateText : scenario.horizonStart
  let guard = 0
  while (
    isSkippedPlanningDate(
      cursor,
      scenario.skipStatutoryHolidays,
      scenario.weekendRestMode,
      scenario.dateWorkModeByDate
    ) &&
    guard < 400
  ) {
    const next = addDays(cursor, 1)
    if (next === cursor) {
      break
    }
    cursor = next
    guard += 1
  }
  return cursor
}

export function moveBusinessDays(startDate: string, offset: number, scenario: LiteScenario) {
  const steps = Math.max(0, Math.round(clampNumber(offset, 0)))
  let cursor = startDate
  for (let idx = 0; idx < steps; idx += 1) {
    cursor = nextBusinessDate(
      cursor,
      scenario.skipStatutoryHolidays,
      scenario.weekendRestMode,
      scenario.dateWorkModeByDate
    )
  }
  return cursor
}

export function countBusinessDaysInclusive(startDate: string, endDate: string, scenario: LiteScenario) {
  if (compareDate(endDate, startDate) < 0) {
    return 0
  }
  let days = 1
  let cursor = startDate
  let guard = 0
  while (compareDate(cursor, endDate) < 0 && guard < 5000) {
    const next = nextBusinessDate(
      cursor,
      scenario.skipStatutoryHolidays,
      scenario.weekendRestMode,
      scenario.dateWorkModeByDate
    )
    if (next === cursor) {
      break
    }
    cursor = next
    if (compareDate(cursor, endDate) <= 0) {
      days += 1
    }
    guard += 1
  }
  return days
}

function buildLiteScheduleByDurationManual(scenario: LiteScenario): LiteSchedulePlan {
  const dates = buildDateRange(
    scenario.horizonStart,
    scenario.horizonDays,
    scenario.skipStatutoryHolidays,
    scenario.weekendRestMode,
    scenario.dateWorkModeByDate
  )
  const lines = scenario.lines.filter((line) => line.enabled !== false)
  const orders = scenario.orders.slice().sort(orderSortForReplan)
  const warnings: string[] = []
  const allocations: LiteAllocation[] = []
  const orderSegments: Record<string, any[]> = {}
  const todayAnchor = alignToBusinessDate(scenario.horizonStart, scenario)

  if (scenario.locks.length > 0) {
    warnings.push('按天数模式下忽略锁定片段。')
  }

  lines.forEach((line) => {
    let cursor: string | null = null
    orders.forEach((order) => {
      const plannedDays = Math.max(0, Math.round(clampNumber(order.linePlanDays?.[line.id], 0)))
      if (plannedDays <= 0) {
        return
      }
      const releaseStart = alignToBusinessDate(order.releaseDate, scenario)
      if (!cursor) {
        cursor = releaseStart
      }
      const startDate = compareDate(cursor, releaseStart) >= 0 ? cursor : releaseStart
      const plannedEndDate = moveBusinessDays(startDate, plannedDays - 1, scenario)
      const key = makeLineOrderKey(line.id, order.id)
      const rawManualFinish = scenario.manualFinishByLineOrder?.[key]
      const manualFinishDate = parseIsoDate(rawManualFinish) ? rawManualFinish : null
      let actualEndDate = plannedEndDate
      let finishSource = 'PLANNED'
      if (manualFinishDate) {
        actualEndDate = compareDate(manualFinishDate, startDate) < 0 ? startDate : manualFinishDate
        finishSource = 'MANUAL'
      } else if (compareDate(todayAnchor, plannedEndDate) > 0) {
        actualEndDate = todayAnchor
        finishSource = 'EXTENDED'
      }
      const segment = {
        lineId: line.id,
        lineName: line.name,
        orderId: order.id,
        startDate,
        plannedEndDate,
        actualEndDate,
        plannedDays,
        manualFinishDate,
        finishSource
      }
      orderSegments[order.id] = orderSegments[order.id] || []
      orderSegments[order.id].push(segment)
      dates.forEach((date) => {
        if (compareDate(date, startDate) < 0 || compareDate(date, actualEndDate) > 0) {
          return
        }
        allocations.push({
          id: makeId('alloc'),
          orderId: order.id,
          lineId: line.id,
          date,
          workloadDays: 1,
          source: 'DURATION',
          lockId: null,
          segmentStartDate: startDate,
          plannedEndDate,
          actualEndDate,
          finishSource,
          manualFinishDate
        })
      })
      cursor = nextBusinessDate(
        actualEndDate,
        scenario.skipStatutoryHolidays,
        scenario.weekendRestMode,
        scenario.dateWorkModeByDate
      )
    })
  })

  orders.forEach((order) => {
    const plannedCount = Object.values(order.linePlanDays || {}).reduce(
      (sum, value) => sum + Math.max(0, Math.round(clampNumber(value, 0))),
      0
    )
    if (plannedCount <= 0) {
      warnings.push(`订单 ${order.orderNo} 未设置产线计划天数。`)
    }
  })

  const allocationMap: Record<string, LiteAllocation[]> = {}
  const orderAllocByDate: Record<string, Record<string, number>> = {}
  allocations.forEach((item) => {
    const key = `${item.lineId}|${item.date}`
    allocationMap[key] = allocationMap[key] || []
    allocationMap[key].push(item)
    const dateMap = (orderAllocByDate[item.orderId] = orderAllocByDate[item.orderId] || {})
    dateMap[item.date] = round3((dateMap[item.date] || 0) + item.workloadDays)
  })

  const lineRows = lines.map((line): LiteLineRow => {
    let assignedTotal = 0
    let capacityTotal = 0
    const daily: LiteLineRow['daily'] = {}
    dates.forEach((date) => {
      const items = allocationMap[`${line.id}|${date}`] || []
      const assigned = round3(items.reduce((sum, item) => sum + item.workloadDays, 0))
      const cap = 1
      assignedTotal = round3(assignedTotal + assigned)
      capacityTotal = round3(capacityTotal + cap)
      daily[date] = { capacity: cap, assigned, utilization: assigned / cap, items }
    })
    return {
      lineId: line.id,
      lineName: line.name,
      assignedTotal,
      capacityTotal,
      utilization: capacityTotal > EPSILON ? assignedTotal / capacityTotal : 0,
      daily
    }
  })

  const orderRows = orders.map((order): LiteOrderRow => {
    const segments = orderSegments[order.id] || []
    const plannedDays = round3(
      Object.values(order.linePlanDays || {}).reduce(
        (maxValue, value) => Math.max(maxValue, Math.max(0, Math.round(clampNumber(value, 0)))),
        0
      )
    )
    const dateMap = orderAllocByDate[order.id] || {}
    const scheduledDays = round3(Object.values(dateMap).reduce((sum, value) => sum + value, 0))
    let completedDays = 0
    let completionDate: string | null = null
    let hasExtendedSegment = false
    let manualFinishedCount = 0
    segments.forEach((segment) => {
      const endForCompleted = compareDate(segment.actualEndDate, todayAnchor) <= 0 ? segment.actualEndDate : todayAnchor
      const segmentCompletedDays = countBusinessDaysInclusive(segment.startDate, endForCompleted, scenario)
      completedDays = round3(Math.max(completedDays, segmentCompletedDays))
      if (segment.finishSource === 'EXTENDED') {
        hasExtendedSegment = true
      } else if (!completionDate || compareDate(segment.actualEndDate, completionDate) > 0) {
        completionDate = segment.actualEndDate
      }
      if (segment.manualFinishDate) {
        manualFinishedCount += 1
      }
    })
    if (hasExtendedSegment) {
      completionDate = null
    }
    const safeCompleted = Math.min(plannedDays, completedDays)
    const remainingDays = hasExtendedSegment
      ? round3(Math.max(1, plannedDays - safeCompleted))
      : round3(Math.max(0, plannedDays - safeCompleted))
    const allManualFinished = segments.length > 0 && manualFinishedCount === segments.length
    const hasManualFinished = manualFinishedCount > 0
    const finishStatus =
      segments.length === 0 ? '未配置' : allManualFinished ? '已结束' : hasManualFinished ? '部分报结束' : '未报结束'
    const actualFinishDate = allManualFinished ? completionDate : null
    const reason =
      segments.length === 0
        ? '未配置按天数产线计划。'
        : hasExtendedSegment
          ? '存在未报结束产线，后续订单顺延。'
          : '按计划天数排产，可手动报结束。'
    return {
      ...order,
      lineWorkloads: order.linePlanDays || {},
      workloadDays: plannedDays,
      completedDays: safeCompleted,
      scheduledDays,
      autoScheduledDays: scheduledDays,
      lockedScheduledDays: 0,
      remainingDays,
      completionDate,
      actualFinishDate,
      finishStatus,
      delayDays: completionDate
        ? Math.max(0, diffDays(completionDate, order.dueDate))
        : hasExtendedSegment
          ? Math.max(0, diffDays(todayAnchor, order.dueDate))
          : null,
      reason
    }
  })

  const totalCapacity = round3(lineRows.reduce((sum, line) => sum + line.capacityTotal, 0))
  const totalAssigned = round3(lineRows.reduce((sum, line) => sum + line.assignedTotal, 0))
  const totalRemaining = round3(orderRows.reduce((sum, row) => sum + Math.max(0, row.remainingDays || 0), 0))

  return {
    scenario,
    dates,
    allocations,
    warnings,
    lineRows,
    orderRows,
    summary: buildScheduleSummary({
      scenario,
      dates,
      lineRows,
      orderRows,
      totalOrders: orders.length,
      totalCapacity,
      totalAssigned,
      totalRemaining
    })
  }
}

export function buildLiteSchedule(inputScenario: any): LiteSchedulePlan {
  const scenario = normalizeLiteScenario(inputScenario)
  return scenario.planningMode === PLANNING_MODE.DURATION_MANUAL_FINISH
    ? buildLiteScheduleByDurationManual(scenario)
    : buildLiteScheduleByQuantityCapacity(scenario)
}

export function advanceLiteScenarioOneDay(inputScenario: any) {
  const scenario = normalizeLiteScenario(inputScenario)
  const plan = buildLiteSchedule(scenario)
  const today = scenario.horizonStart

  if (scenario.planningMode === PLANNING_MODE.DURATION_MANUAL_FINISH) {
    const completedToday = round3(
      plan.allocations
        .filter((item) => item.date === today)
        .reduce((sum, item) => sum + item.workloadDays, 0)
    )
    const newStart = nextBusinessDate(
      today,
      scenario.skipStatutoryHolidays,
      scenario.weekendRestMode,
      scenario.dateWorkModeByDate
    )
    const nextScenario = normalizeLiteScenario({
      ...scenario,
      horizonStart: newStart,
      simulationLogs: [
        { date: today, completedWorkload: completedToday, note: '按天数模式推进 1 天' },
        ...scenario.simulationLogs
      ].slice(0, 30)
    })
    const nextPlan = buildLiteSchedule(nextScenario)
    return {
      nextScenario,
      daySummary: {
        date: today,
        completedWorkload: completedToday,
        remainingWorkload: nextPlan.summary.totalRemaining,
        delayedOrders: nextPlan.summary.delayedOrders,
        message: `已推进至 ${newStart}，当日完成 ${completedToday} 天工作量。`
      }
    }
  }

  const completedByOrder: Record<string, number> = {}
  const consumedByLock: Record<string, number> = {}
  plan.allocations
    .filter((item) => item.date === today)
    .forEach((item) => {
      completedByOrder[item.orderId] = round3((completedByOrder[item.orderId] || 0) + item.workloadDays)
      if (item.lockId) {
        consumedByLock[item.lockId] = round3((consumedByLock[item.lockId] || 0) + item.workloadDays)
      }
    })

  const newStart = nextBusinessDate(
    today,
    scenario.skipStatutoryHolidays,
    scenario.weekendRestMode,
    scenario.dateWorkModeByDate
  )
  const nextOrders = scenario.orders.map((order) => {
    const completed = round3(order.completedDays + (completedByOrder[order.id] || 0))
    return { ...order, completedDays: Math.min(order.workloadDays, completed) }
  })
  const nextLocks = scenario.locks
    .map((lock) => {
      const left = round3(lock.workloadDays - (consumedByLock[lock.id] || 0))
      if (left <= EPSILON) {
        return null
      }
      const startDate = compareDate(lock.startDate, newStart) < 0 ? newStart : lock.startDate
      if (compareDate(startDate, lock.endDate) > 0) {
        return null
      }
      return { ...lock, startDate, workloadDays: left }
    })
    .filter(Boolean)
  const completedToday = round3(Object.values(completedByOrder).reduce((sum, value) => sum + value, 0))
  const nextScenario = normalizeLiteScenario({
    ...scenario,
    horizonStart: newStart,
    orders: nextOrders,
    locks: nextLocks,
    simulationLogs: [
      { date: today, completedWorkload: completedToday, note: '按当前方案推进 1 天' },
      ...scenario.simulationLogs
    ].slice(0, 30)
  })
  const nextPlan = buildLiteSchedule(nextScenario)
  return {
    nextScenario,
    daySummary: {
      date: today,
      completedWorkload: completedToday,
      remainingWorkload: nextPlan.summary.totalRemaining,
      delayedOrders: nextPlan.summary.delayedOrders,
      message: `已推进至 ${newStart}，当日完成 ${completedToday} 天工作量。`
    }
  }
}

export function toNumber(value: unknown, fallback = 0) {
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

export function formatNumber(value: unknown) {
  const n = Number(value)
  return Number.isFinite(n) ? String(Math.round(n)) : '-'
}

export function formatCapacityNumber(value: unknown) {
  const n = Number(value)
  return Number.isFinite(n) ? n.toLocaleString('zh-CN', { maximumFractionDigits: 6 }) : '-'
}

export function formatPercent(value: unknown) {
  const n = Number(value)
  return Number.isFinite(n) ? `${(n * 100).toFixed(1)}%` : '-'
}

export function formatAutoOrderNo(seqValue: unknown) {
  const seq = Math.max(1, Math.round(toNumber(seqValue, 1)))
  return `PO-${String(seq).padStart(4, '0')}`
}

export function formatSnapshotName(date = new Date()) {
  const year = date.getFullYear()
  const month = pad2(date.getMonth() + 1)
  const day = pad2(date.getDate())
  const hour = pad2(date.getHours())
  const minute = pad2(date.getMinutes())
  const second = pad2(date.getSeconds())
  return `${year}-${month}-${day} ${hour}-${minute}-${second}`
}

export function formatSnapshotDisplay(dateMs: unknown) {
  const n = Number(dateMs)
  return Number.isFinite(n) ? formatSnapshotName(new Date(n)) : '-'
}

export function buildModalLineTotals(scenario: LiteScenario, prevLineTotals: Record<string, unknown> = {}) {
  return Object.fromEntries(scenario.lines.map((line) => [line.id, String(prevLineTotals?.[line.id] ?? '0')]))
}

export function buildModalLinePlanDays(scenario: LiteScenario, prevLinePlanDays: Record<string, unknown> = {}) {
  return Object.fromEntries(scenario.lines.map((line) => [line.id, String(prevLinePlanDays?.[line.id] ?? '0')]))
}

export function buildModalLinePlanQuantities(
  scenario: LiteScenario,
  prevLinePlanQuantities: Record<string, unknown> = {}
) {
  return Object.fromEntries(scenario.lines.map((line) => [line.id, String(prevLinePlanQuantities?.[line.id] ?? '0')]))
}

export function createOrderModalForm(scenario: LiteScenario): OrderModalForm {
  return {
    orderNo: formatAutoOrderNo(scenario.nextOrderSeq || 1),
    productName: '',
    spec: '',
    batchNo: '',
    dueDate: addDays(scenario.horizonStart, 7),
    releaseDate: scenario.horizonStart,
    priority: 'NORMAL',
    lineTotals: buildModalLineTotals(scenario),
    linePlanDays: buildModalLinePlanDays(scenario),
    linePlanQuantities: buildModalLinePlanQuantities(scenario)
  }
}

export function createOrderModalFormFromOrder(scenario: LiteScenario, order: LiteOrder): OrderModalForm {
  return {
    orderNo: order.orderNo,
    productName: order.productName || '',
    spec: order.spec || '',
    batchNo: order.batchNo || '',
    dueDate: order.dueDate || addDays(scenario.horizonStart, 7),
    releaseDate: order.releaseDate || scenario.horizonStart,
    priority: 'NORMAL',
    lineTotals: buildModalLineTotals(scenario, order.lineWorkloads),
    linePlanDays: buildModalLinePlanDays(scenario, order.linePlanDays),
    linePlanQuantities: buildModalLinePlanQuantities(scenario, order.linePlanQuantities)
  }
}

export function buildOrderUpsertResult(params: {
  scenario: LiteScenario
  orderModalForm: OrderModalForm
  editingOrderId: string | null
  isDurationMode: boolean
  lineNameMap: Record<string, string>
}): { error?: string; message?: string; mutator?: (scenario: LiteScenario) => LiteScenario } {
  const { scenario, orderModalForm, editingOrderId, isDurationMode, lineNameMap } = params
  const inputOrderNo = String(orderModalForm.orderNo || '').trim()
  const productName = String(orderModalForm.productName || '').trim()
  const spec = String(orderModalForm.spec || '').trim()
  const batchNo = String(orderModalForm.batchNo || '').trim()
  const editingOrderNo = scenario.orders.find((order) => order.id === editingOrderId)?.orderNo || ''
  const orderNo = inputOrderNo || editingOrderNo || formatAutoOrderNo(scenario.nextOrderSeq || 1)
  const dueDate = orderModalForm.dueDate || addDays(scenario.horizonStart, 7)
  const releaseDate = orderModalForm.releaseDate || scenario.horizonStart

  if (isDurationMode) {
    const linePlanDays: Record<string, number> = {}
    Object.entries(orderModalForm.linePlanDays || {}).forEach(([lineIdRaw, value]) => {
      const lineId = String(lineIdRaw || '').trim()
      const days = Math.max(0, Math.round(toNumber(value, 0)))
      if (lineId && lineNameMap[lineId] && days > 0) {
        linePlanDays[lineId] = days
      }
    })
    const linePlanQuantities: Record<string, number> = {}
    Object.entries(orderModalForm.linePlanQuantities || {}).forEach(([lineIdRaw, value]) => {
      const lineId = String(lineIdRaw || '').trim()
      const qty = Math.max(0, Math.round(toNumber(value, 0)))
      if (lineId && lineNameMap[lineId] && qty > 0) {
        linePlanQuantities[lineId] = qty
      }
    })
    const maxPlanDays = Object.values(linePlanDays).reduce((maxValue, value) => Math.max(maxValue, value), 0)
    if (maxPlanDays <= 0) {
      return { error: '请至少填写一条产线的计划天数。' }
    }
    return {
      message: editingOrderId ? `订单已更新：${orderNo}` : `订单已新增：${orderNo}`,
      mutator: (prev) => ({
        ...prev,
        nextOrderSeq: editingOrderId ? prev.nextOrderSeq : Math.max(1, Math.round(toNumber(prev.nextOrderSeq, 1))) + 1,
        orders: editingOrderId
          ? prev.orders.map((order) =>
              order.id === editingOrderId
                ? {
                    ...order,
                    orderNo,
                    productName,
                    spec,
                    batchNo,
                    workloadDays: maxPlanDays,
                    completedDays: Math.min(toNumber(order.completedDays, 0), maxPlanDays),
                    dueDate,
                    releaseDate,
                    priority: 'NORMAL',
                    linePlanDays,
                    linePlanQuantities,
                    lineWorkloads: {}
                  }
                : order
            )
          : [
              ...prev.orders,
              {
                id: makeId('order'),
                orderNo,
                productName,
                spec,
                batchNo,
                orderSeq: Math.max(1, Math.round(toNumber(prev.nextOrderSeq, 1))),
                workloadDays: maxPlanDays,
                completedDays: 0,
                dueDate,
                releaseDate,
                priority: 'NORMAL',
                lineWorkloads: {},
                linePlanDays,
                linePlanQuantities
              }
            ]
      })
    }
  }

  const lineWorkloads: Record<string, number> = {}
  Object.entries(orderModalForm.lineTotals || {}).forEach(([lineIdRaw, value]) => {
    const lineId = String(lineIdRaw || '').trim()
    const workload = Math.max(0, toNumber(value, 0))
    if (lineId && lineNameMap[lineId] && workload > 0) {
      lineWorkloads[lineId] = (lineWorkloads[lineId] || 0) + workload
    }
  })
  const totalWorkload = Object.values(lineWorkloads).reduce((sum, value) => sum + value, 0)
  if (totalWorkload <= 0) {
    return { error: '请至少填写一条产线的工作量。' }
  }
  return {
    message: editingOrderId ? `订单已更新：${orderNo}` : `订单已新增：${orderNo}`,
    mutator: (prev) => ({
      ...prev,
      nextOrderSeq: editingOrderId ? prev.nextOrderSeq : Math.max(1, Math.round(toNumber(prev.nextOrderSeq, 1))) + 1,
      orders: editingOrderId
        ? prev.orders.map((order) =>
            order.id === editingOrderId
              ? {
                  ...order,
                  orderNo,
                  productName,
                  spec,
                  batchNo,
                  workloadDays: totalWorkload,
                  completedDays: Math.min(toNumber(order.completedDays, 0), totalWorkload),
                  dueDate,
                  releaseDate,
                  priority: 'NORMAL',
                  lineWorkloads,
                  linePlanDays: {},
                  linePlanQuantities: {}
                }
              : order
          )
        : [
            ...prev.orders,
            {
              id: makeId('order'),
              orderNo,
              productName,
              spec,
              batchNo,
              orderSeq: Math.max(1, Math.round(toNumber(prev.nextOrderSeq, 1))),
              workloadDays: totalWorkload,
              completedDays: 0,
              dueDate,
              releaseDate,
              priority: 'NORMAL',
              lineWorkloads,
              linePlanDays: {},
              linePlanQuantities: {}
            }
          ]
    })
  }
}

export function buildInsertOrderMutation({ orderId, insertDate }: { orderId: string; insertDate: string }) {
  return (prev: LiteScenario): LiteScenario => {
    const targetOrder = prev.orders.find((row) => row.id === orderId)
    if (!targetOrder) {
      return prev
    }
    const safeInsertDate = insertDate || prev.horizonStart
    const sortedIds = prev.orders
      .slice()
      .sort((a, b) => (toNumber(a.orderSeq, 0) || 0) - (toNumber(b.orderSeq, 0) || 0))
      .map((row) => row.id)
    const reordered = [orderId, ...sortedIds.filter((id) => id !== orderId)]
    const seqMap = Object.fromEntries(reordered.map((id, idx) => [id, idx + 1]))
    return {
      ...prev,
      orders: prev.orders.map((row) =>
        row.id === orderId
          ? { ...row, releaseDate: safeInsertDate, orderSeq: 1 }
          : { ...row, orderSeq: seqMap[row.id] || toNumber(row.orderSeq, 1) }
      ),
      locks: prev.locks.filter((lock) => Number(lock.seq) >= 0)
    }
  }
}

export function mapLiteSnapshotRow(row: any, now = Date.now()): LiteSnapshot {
  return {
    id: String(row?.id || makeId('snapshot')),
    name: String(row?.name || '').trim() || formatSnapshotName(new Date(now)),
    createdAt: Number(row?.createdAt) || now,
    updatedAt: Number(row?.updatedAt) || Number(row?.createdAt) || now,
    scenario: normalizeLiteScenario(row?.scenario)
  }
}

export function sortSnapshotsByUpdatedAtDesc(rows: LiteSnapshot[]) {
  return [...rows].sort((a, b) => b.updatedAt - a.updatedAt)
}

export function readLiteScenarioFromStorage(): { value: LiteScenario | null; error: string } {
  if (typeof window === 'undefined') {
    return { value: createDefaultLiteScenario(), error: '' }
  }
  const raw = window.localStorage.getItem(LITE_SCENARIO_STORAGE_KEY)
  if (!raw) {
    return { value: createDefaultLiteScenario(), error: '' }
  }
  try {
    return { value: normalizeLiteScenario(JSON.parse(raw)), error: '' }
  } catch (error) {
    return {
      value: null,
      error: `本地排产场景数据已损坏，无法继续读写：${error instanceof Error ? error.message : String(error)}`
    }
  }
}

export function writeLiteScenarioToStorage(scenario: LiteScenario) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(LITE_SCENARIO_STORAGE_KEY, JSON.stringify(normalizeLiteScenario(scenario)))
}

export function readLiteSnapshotsFromStorage(): { value: LiteSnapshot[] | null; error: string } {
  if (typeof window === 'undefined') {
    return { value: [], error: '' }
  }
  const raw = window.localStorage.getItem(LITE_SNAPSHOT_STORAGE_KEY)
  if (!raw) {
    return { value: [], error: '' }
  }
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return { value: null, error: '本地场景快照数据不是数组，无法继续读写。' }
    }
    return { value: sortSnapshotsByUpdatedAtDesc(parsed.map((row) => mapLiteSnapshotRow(row))), error: '' }
  } catch (error) {
    return {
      value: null,
      error: `本地场景快照数据已损坏，无法继续读写：${error instanceof Error ? error.message : String(error)}`
    }
  }
}

export function writeLiteSnapshotsToStorage(rows: LiteSnapshot[]) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(LITE_SNAPSHOT_STORAGE_KEY, JSON.stringify(Array.isArray(rows) ? rows : []))
}

export function collectScheduledRows(params: {
  allocations: LiteAllocation[]
  compareDate: (a: string, b: string) => number
  lineNameMap: Record<string, string>
  orderMetaMap: Record<string, any>
}) {
  const { allocations, lineNameMap, orderMetaMap } = params
  return (allocations || [])
    .filter((item) => Number(item.workloadDays || 0) > 0)
    .slice()
    .sort((a, b) => {
      const dateCmp = params.compareDate(a.date, b.date)
      if (dateCmp !== 0) {
        return dateCmp
      }
      const lineCmp = String(lineNameMap[a.lineId] || a.lineId).localeCompare(
        String(lineNameMap[b.lineId] || b.lineId),
        'zh-Hans-CN'
      )
      if (lineCmp !== 0) {
        return lineCmp
      }
      return String(orderMetaMap[a.orderId]?.orderNo || a.orderId).localeCompare(
        String(orderMetaMap[b.orderId]?.orderNo || b.orderId),
        'zh-Hans-CN'
      )
    })
}

function formatExportWorkload(value: unknown) {
  const n = Number(value)
  if (!Number.isFinite(n)) {
    return ''
  }
  return String(Math.max(0, Math.round(n)))
}

export function buildScheduledOrdersExportPayload(params: {
  scheduledRows: LiteAllocation[]
  orderMetaMap: Record<string, any>
  lineNameMap: Record<string, string>
  manualFinishByLineOrder?: Record<string, string>
  makeLineOrderKey: (lineId: string, orderId: string) => string
  isDurationMode: boolean
  stamp: string
}) {
  const {
    scheduledRows,
    orderMetaMap,
    lineNameMap,
    manualFinishByLineOrder = {},
    isDurationMode,
    stamp
  } = params
  if (scheduledRows.length === 0) {
    return { error: '当前没有可导出的已排产订单。', headers: [], rows: [], fileName: '' }
  }
  const sourceTextMap: Record<string, string> = {
    DURATION: '按天数排产',
    LOCKED: '锁定片段',
    LOCK: '锁定片段',
    AUTO_LINE: '指定产线排产',
    AUTO: '自动排产'
  }
  const unitLabel = isDurationMode ? '天' : '个'
  const headers = ['序号', '排产日期', '产线', '订单号', '产品名称', '规格', '批号', '排产量', '单位', '来源', '手动结束日期']
  const rows = scheduledRows.map((item, idx) => {
    const orderMeta = orderMetaMap[item.orderId] || {}
    const lineName = lineNameMap[item.lineId] || item.lineId
    const manualFinishDate = manualFinishByLineOrder?.[params.makeLineOrderKey(item.lineId, item.orderId)] || ''
    return [
      String(idx + 1),
      String(item.date || ''),
      String(lineName || ''),
      String(orderMeta.orderNo || item.orderId || ''),
      String(orderMeta.productName || ''),
      String(orderMeta.spec || ''),
      String(orderMeta.batchNo || ''),
      formatExportWorkload(item.workloadDays),
      unitLabel,
      sourceTextMap[item.source] || String(item.source || '-'),
      String(manualFinishDate || '-')
    ]
  })
  return { headers, rows, fileName: `lite排产订单_${stamp}.xls` }
}

function escapeExcelCell(value: unknown) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

export function buildExcelTableHtml(headers: string[], rows: string[][]) {
  const headHtml = headers.map((header) => `<th>${escapeExcelCell(header)}</th>`).join('')
  const bodyHtml = rows
    .map((row) => `<tr>${row.map((cell) => `<td>${escapeExcelCell(cell)}</td>`).join('')}</tr>`)
    .join('')
  return `<!DOCTYPE html>
<html>
  <head><meta charset="UTF-8" /></head>
  <body>
    <table border="1">
      <thead><tr>${headHtml}</tr></thead>
      <tbody>${bodyHtml}</tbody>
    </table>
  </body>
</html>`
}

export function downloadTextFile(text: string, fileName: string, mimeType: string) {
  const blob = new Blob([text], { type: mimeType })
  const urlApi = typeof window !== 'undefined' && window.URL ? window.URL : URL
  const link = document.createElement('a')
  link.download = fileName
  if (urlApi && typeof urlApi.createObjectURL === 'function') {
    const blobUrl = urlApi.createObjectURL(blob)
    link.href = blobUrl
    document.body.appendChild(link)
    link.click()
    link.remove()
    if (typeof urlApi.revokeObjectURL === 'function') {
      urlApi.revokeObjectURL(blobUrl)
    }
    return
  }
  link.href = `data:${mimeType};charset=utf-8,${encodeURIComponent(text)}`
  document.body.appendChild(link)
  link.click()
  link.remove()
}

export function parseMonthText(monthText: string) {
  const text = String(monthText || '').trim()
  const match = text.match(/^(\d{4})-(\d{2})$/)
  if (!match) {
    return null
  }
  const year = Number(match[1])
  const month = Number(match[2])
  return Number.isInteger(year) && Number.isInteger(month) && month >= 1 && month <= 12 ? { year, month } : null
}

export function formatMonthText(year: number, month: number) {
  return `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}`
}

export function getMonthDayCount(year: number, month: number) {
  return new Date(Date.UTC(year, month, 0)).getUTCDate()
}

export function monthTextFromDate(dateText: string) {
  const parsedDate = parseIsoDate(dateText)
  return parsedDate ? formatMonthText(parsedDate.year, parsedDate.month) : null
}

export function buildCalendarWeeksByMonth(monthText: string) {
  const parsed = parseMonthText(monthText)
  if (!parsed) {
    return []
  }
  const daysInMonth = getMonthDayCount(parsed.year, parsed.month)
  const monthStart = new Date(Date.UTC(parsed.year, parsed.month - 1, 1))
  const leadingEmptyCount = (monthStart.getUTCDay() + 6) % 7
  const cells: Array<string | null> = []
  for (let i = 0; i < leadingEmptyCount; i += 1) {
    cells.push(null)
  }
  for (let day = 1; day <= daysInMonth; day += 1) {
    cells.push(toIsoDateFromUtc(new Date(Date.UTC(parsed.year, parsed.month - 1, day))))
  }
  const trailingEmptyCount = (7 - (cells.length % 7)) % 7
  for (let i = 0; i < trailingEmptyCount; i += 1) {
    cells.push(null)
  }
  const weeks: Array<Array<string | null>> = []
  for (let idx = 0; idx < cells.length; idx += 7) {
    weeks.push(cells.slice(idx, idx + 7))
  }
  return weeks
}
