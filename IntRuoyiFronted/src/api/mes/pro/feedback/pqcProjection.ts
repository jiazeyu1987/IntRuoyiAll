import type {
  FrontlinePqcBusinessDateResponse,
  FrontlinePqcProcessResponseVO,
  FrontlinePqcProcessVO,
  FrontlinePqcTaskOptionResponseVO,
  FrontlinePqcTaskOptionVO
} from './index'

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

export const projectFrontlinePqcProcesses = (
  processes: FrontlinePqcProcessResponseVO[]
): FrontlinePqcProcessVO[] =>
  processes
    .map((process) => ({
      ...process,
      pqcTaskOptions: process.pqcTaskOptions.map(normalizePqcTaskOption).sort(comparePqcTaskOptions)
    }))
    .sort(
      (left, right) =>
        left.qaProcessSort - right.qaProcessSort || left.qaProcessId - right.qaProcessId
    )
