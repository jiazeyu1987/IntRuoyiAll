export const WORKDAY_HOURS = 8
export const READABLE_SHORT_TASK_HOURS = 12
export const READABLE_DAY_COLUMN_WIDTH = 96
export const READABLE_SHIFT_COLUMN_WIDTH = 72

type GanttDateValue = string | number | Date | null | undefined

export interface ReadableGanttTask {
  id?: string | number
  originalId?: string | number
  type?: string
  text?: string
  workOrderCode?: string
  parent?: string | number | null
  startDate?: GanttDateValue
  endDate?: GanttDateValue
  start_date?: GanttDateValue
  end_date?: GanttDateValue
  duration?: string | number | null
  readabilityCompact?: boolean
  readabilityDurationHours?: number
  [key: string]: any
}

export interface NormalizedReadableGanttTask extends ReadableGanttTask {
  start_date?: Date
  end_date?: Date
  unscheduled?: boolean
  readabilityMissingScheduleReason?: string
  readabilityCompact: boolean
  readabilityDurationHours: number
}

interface TaskRange {
  startDate?: Date
  endDate?: Date
  durationHours: number
  unscheduled: boolean
  missingScheduleReason?: string
}

const DATE_TIME_PATTERN =
  /^(\d{4})-(\d{1,2})-(\d{1,2})(?:[ T](\d{1,2}):(\d{1,2})(?::(\d{1,2})(?:\.\d{1,3})?)?)?$/

const cloneDate = (value: Date) => new Date(value.getTime())

const addHours = (value: Date, hours: number) => {
  const next = cloneDate(value)
  next.setHours(next.getHours() + hours)
  return next
}

const isPresent = (value: GanttDateValue) => value !== null && value !== undefined && value !== ''

const getTaskLabel = (task: ReadableGanttTask) =>
  String(task.id ?? task.originalId ?? task.text ?? 'unknown')

const isProjectTask = (task: ReadableGanttTask) => task.type === 'project'

const toTrimmedText = (value: unknown) => String(value ?? '').trim()

export const getGanttWorkOrderProcessLabel = (task: ReadableGanttTask) => {
  const workOrderCode = toTrimmedText(task.workOrderCode)
  if (!workOrderCode) {
    throw new Error(`production gantt task missing workOrderCode: ${getTaskLabel(task)}`)
  }
  if (isProjectTask(task)) {
    return workOrderCode
  }
  const process = toTrimmedText(task.process)
  if (!process) {
    throw new Error(`production gantt task missing process: ${getTaskLabel(task)}`)
  }
  return `${workOrderCode} / ${process}`
}

const isMidnight = (date: Date) =>
  date.getHours() === 0 &&
  date.getMinutes() === 0 &&
  date.getSeconds() === 0 &&
  date.getMilliseconds() === 0

const parseLocalDateParts = (value: string) => {
  const match = value.trim().match(DATE_TIME_PATTERN)
  if (!match) {
    return null
  }
  const [, year, month, day, hour = '0', minute = '0', second = '0'] = match
  return new Date(
    Number(year),
    Number(month) - 1,
    Number(day),
    Number(hour),
    Number(minute),
    Number(second),
    0
  )
}

const toReadableError = (fieldName: string, task: ReadableGanttTask) =>
  `production gantt task missing ${fieldName}: ${getTaskLabel(task)}`

const toMissingScheduleReason = (task: ReadableGanttTask) => {
  const hasStart = isPresent(resolveDateValue(task, 'startDate'))
  const hasEnd = isPresent(resolveDateValue(task, 'endDate'))
  if (!hasStart && !hasEnd) {
    return '缺少开始和结束时间'
  }
  if (!hasStart) {
    return '缺少开始时间'
  }
  return '缺少结束时间'
}

export const parseGanttDate = (
  value: GanttDateValue,
  fieldName: 'startDate' | 'endDate',
  task: ReadableGanttTask
) => {
  if (!isPresent(value)) {
    throw new Error(toReadableError(fieldName, task))
  }

  if (value instanceof Date) {
    if (Number.isNaN(value.getTime())) {
      throw new Error(`production gantt task invalid ${fieldName}: ${getTaskLabel(task)}`)
    }
    return cloneDate(value)
  }

  if (typeof value === 'number') {
    const parsed = new Date(value)
    if (Number.isNaN(parsed.getTime())) {
      throw new Error(`production gantt task invalid ${fieldName}: ${getTaskLabel(task)}`)
    }
    return parsed
  }

  const parsedByParts = parseLocalDateParts(String(value))
  const parsed = parsedByParts ?? new Date(String(value))
  if (Number.isNaN(parsed.getTime())) {
    throw new Error(`production gantt task invalid ${fieldName}: ${getTaskLabel(task)}`)
  }
  return parsed
}

const resolveDateValue = (task: ReadableGanttTask, camelName: 'startDate' | 'endDate') => {
  if (camelName === 'startDate') {
    return task.startDate ?? task.start_date
  }
  return task.endDate ?? task.end_date
}

const resolveDurationHours = (duration: ReadableGanttTask['duration']) => {
  if (duration === null || duration === undefined || duration === '') {
    return null
  }
  const parsed = Number(duration)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return null
  }
  return parsed * WORKDAY_HOURS
}

const resolveOwnTaskRange = (task: ReadableGanttTask): TaskRange => {
  const rawStart = resolveDateValue(task, 'startDate')
  const rawEnd = resolveDateValue(task, 'endDate')
  if (!isPresent(rawStart) || !isPresent(rawEnd)) {
    return {
      durationHours: 0,
      unscheduled: true,
      missingScheduleReason: toMissingScheduleReason(task)
    }
  }
  const startDate = parseGanttDate(rawStart, 'startDate', task)
  let endDate = parseGanttDate(rawEnd, 'endDate', task)

  if (endDate.getTime() < startDate.getTime()) {
    throw new Error(`production gantt task endDate before startDate: ${getTaskLabel(task)}`)
  }

  if (endDate.getTime() === startDate.getTime()) {
    const durationHours = resolveDurationHours(task.duration)
    if (durationHours !== null) {
      endDate = addHours(startDate, durationHours)
    } else if (isMidnight(startDate) && isMidnight(endDate)) {
      endDate = addHours(startDate, WORKDAY_HOURS)
    } else {
      throw new Error(`production gantt task zero duration: ${getTaskLabel(task)}`)
    }
  }

  return {
    startDate,
    endDate,
    durationHours: getDateRangeHours(startDate, endDate),
    unscheduled: false
  }
}

const getDateRangeHours = (startDate: Date, endDate: Date) =>
  Math.max(0, (endDate.getTime() - startDate.getTime()) / 36e5)

export const getGanttTaskDurationHours = (task: Pick<ReadableGanttTask, 'start_date' | 'end_date'>) => {
  if (!(task.start_date instanceof Date) || !(task.end_date instanceof Date)) {
    return 0
  }
  return getDateRangeHours(task.start_date, task.end_date)
}

export const normalizeGanttTasksForReadability = (
  tasks: ReadableGanttTask[]
): NormalizedReadableGanttTask[] => {
  const items = (tasks || []).map((task) => ({ ...task }))
  const childrenByParent = new Map<string, ReadableGanttTask[]>()

  for (const task of items) {
    if (task.id === null || task.id === undefined || task.id === '') {
      throw new Error('production gantt task missing id')
    }
  }

  for (const task of items) {
    if (task.parent === null || task.parent === undefined || task.parent === '') {
      continue
    }
    const parentId = String(task.parent)
    const siblings = childrenByParent.get(parentId) ?? []
    siblings.push(task)
    childrenByParent.set(parentId, siblings)
  }

  const rangeById = new Map<string, TaskRange>()
  const resolving = new Set<string>()

  const resolveRange = (task: ReadableGanttTask): TaskRange => {
    const taskId = String(task.id)
    const cached = rangeById.get(taskId)
    if (cached) {
      return cached
    }
    if (resolving.has(taskId)) {
      throw new Error(`production gantt task parent cycle: ${getTaskLabel(task)}`)
    }
    resolving.add(taskId)

    const hasStart = isPresent(resolveDateValue(task, 'startDate'))
    const hasEnd = isPresent(resolveDateValue(task, 'endDate'))
    let range: TaskRange

    if (hasStart || hasEnd) {
      range = resolveOwnTaskRange(task)
    } else if (isProjectTask(task)) {
      const children = childrenByParent.get(taskId) ?? []
      if (!children.length) {
        range = {
          durationHours: 0,
          unscheduled: true,
          missingScheduleReason: '无已排产子任务'
        }
      } else {
        const childRanges = children.map(resolveRange).filter((child) => !child.unscheduled)
        if (!childRanges.length) {
          range = {
            durationHours: 0,
            unscheduled: true,
            missingScheduleReason: '无已排产子任务'
          }
        } else {
          const startDate = childRanges.reduce(
            (min, child) =>
              child.startDate && child.startDate.getTime() < min.getTime() ? child.startDate : min,
            childRanges[0].startDate as Date
          )
          const endDate = childRanges.reduce(
            (max, child) =>
              child.endDate && child.endDate.getTime() > max.getTime() ? child.endDate : max,
            childRanges[0].endDate as Date
          )
          range = {
            startDate: cloneDate(startDate),
            endDate: cloneDate(endDate),
            durationHours: getDateRangeHours(startDate, endDate),
            unscheduled: false
          }
        }
      }
    } else {
      range = resolveOwnTaskRange(task)
    }

    resolving.delete(taskId)
    rangeById.set(taskId, range)
    return range
  }

  return items.map((task) => {
    const { start_date: _rawStartDate, end_date: _rawEndDate, ...taskWithoutGanttDates } = task
    const range = resolveRange(task)
    const readabilityCompact =
      !range.unscheduled &&
      !isProjectTask(task) &&
      range.durationHours > 0 &&
      range.durationHours <= READABLE_SHORT_TASK_HOURS
    return {
      ...taskWithoutGanttDates,
      ...(range.startDate ? { start_date: cloneDate(range.startDate) } : {}),
      ...(range.endDate ? { end_date: cloneDate(range.endDate) } : {}),
      unscheduled: range.unscheduled,
      readabilityMissingScheduleReason: range.missingScheduleReason,
      readabilityCompact,
      readabilityDurationHours: range.durationHours
    }
  })
}

const pad = (value: number) => String(value).padStart(2, '0')

export const formatGanttDateTime = (date: Date) => {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) {
    return ''
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}`
}

export const formatGanttGridDate = (date: Date) => {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) {
    return ''
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export const escapeGanttHtml = (value: unknown) =>
  String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
