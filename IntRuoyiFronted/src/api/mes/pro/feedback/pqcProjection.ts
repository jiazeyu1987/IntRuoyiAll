import type {
  FrontlinePqcProcessVO,
  FrontlinePqcTaskOptionVO
} from './index'

export type FrontlinePqcBusinessDateResponse = string | [number, number, number] | number[]

export type FrontlinePqcTaskOptionResponseVO = Omit<FrontlinePqcTaskOptionVO, 'businessDate'> & {
  businessDate: FrontlinePqcBusinessDateResponse
}

export type FrontlinePqcProcessResponseVO = Omit<FrontlinePqcProcessVO, 'pqcTaskOptions'> & {
  pqcTaskOptions: FrontlinePqcTaskOptionResponseVO[]
}

const BUSINESS_DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/

const isValidBusinessDate = (year: number, month: number, day: number) => {
  if (
    ![year, month, day].every(Number.isInteger) ||
    year < 1 ||
    month < 1 ||
    month > 12 ||
    day < 1
  ) {
    return false
  }
  const date = new Date(0)
  date.setUTCHours(0, 0, 0, 0)
  date.setUTCFullYear(year, month - 1, day)
  return (
    date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day
  )
}

const formatBusinessDate = (year: number, month: number, day: number) =>
  `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`

const normalizePqcBusinessDate = (value: FrontlinePqcBusinessDateResponse, pqcTaskId: number) => {
  const parts =
    typeof value === 'string'
      ? BUSINESS_DATE_PATTERN.exec(value)?.slice(1).map(Number)
      : Array.isArray(value) && value.length === 3
        ? value
        : undefined
  if (!parts || !isValidBusinessDate(parts[0], parts[1], parts[2])) {
    throw new Error(`PQC任务业务日期无效：pqcTaskId=${pqcTaskId}`)
  }
  return formatBusinessDate(parts[0], parts[1], parts[2])
}

const normalizePqcTaskOption = (
  option: FrontlinePqcTaskOptionResponseVO
): FrontlinePqcTaskOptionVO => ({
  ...option,
  businessDate: normalizePqcBusinessDate(option.businessDate, option.pqcTaskId)
})

const comparePqcTaskOptions = (left: FrontlinePqcTaskOptionVO, right: FrontlinePqcTaskOptionVO) =>
  left.businessDate.localeCompare(right.businessDate) ||
  left.ruleSort - right.ruleSort ||
  left.roundNo - right.roundNo ||
  left.pqcTaskId - right.pqcTaskId

const normalizePqcProcessDisplayName = (process: FrontlinePqcProcessResponseVO) => {
  if (process.regulationSourceType !== 'COMMON_PACKAGING') {
    return process.qaProcessName
  }
  const normalized = process.qaProcessName.trim()
  if (normalized === '初包装' || normalized === '初包装过程检验规程') {
    return '小包装'
  }
  if (normalized === '大中包装' || normalized === '大中包装过程检验规程') {
    return '中大包装'
  }
  return process.qaProcessName
}

const resolvePqcProcessSourceSort = (process: FrontlinePqcProcessVO) =>
  process.regulationSourceType === 'COMMON_PACKAGING' ? 1 : 0

const resolveCommonPackagingProcessSort = (process: FrontlinePqcProcessVO) => {
  if (process.qaProcessName === '小包装') {
    return 1
  }
  if (process.qaProcessName === '中大包装') {
    return 2
  }
  return process.qaProcessSort
}

const comparePqcProcesses = (left: FrontlinePqcProcessVO, right: FrontlinePqcProcessVO) => {
  const sourceSort = resolvePqcProcessSourceSort(left) - resolvePqcProcessSourceSort(right)
  if (sourceSort !== 0) {
    return sourceSort
  }
  if (left.regulationSourceType === 'COMMON_PACKAGING') {
    return (
      resolveCommonPackagingProcessSort(left) - resolveCommonPackagingProcessSort(right) ||
      left.qaProcessId - right.qaProcessId
    )
  }
  return left.qaProcessSort - right.qaProcessSort || left.qaProcessId - right.qaProcessId
}

export const projectFrontlinePqcProcesses = (
  processes: FrontlinePqcProcessResponseVO[]
): FrontlinePqcProcessVO[] => {
  const orderedProcesses = processes
    .map<FrontlinePqcProcessVO>((process) => ({
      ...process,
      qaProcessName: normalizePqcProcessDisplayName(process),
      pqcTaskOptions: process.pqcTaskOptions.map(normalizePqcTaskOption).sort(comparePqcTaskOptions)
    }))
    .sort(comparePqcProcesses)
  let nextCommonPackagingSort = Math.max(
    0,
    ...orderedProcesses
      .filter((process) => process.regulationSourceType !== 'COMMON_PACKAGING')
      .map((process) => process.qaProcessSort)
  )
  return orderedProcesses.map((process) =>
    process.regulationSourceType === 'COMMON_PACKAGING'
      ? { ...process, qaProcessSort: ++nextCommonPackagingSort }
      : process
  )
}
