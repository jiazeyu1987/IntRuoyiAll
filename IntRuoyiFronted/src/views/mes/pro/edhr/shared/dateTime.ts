import { formatDate } from '@/utils/formatTime'

export type EdhrDateTimeValue = string | number | Date | null | undefined

export const toEdhrDateTime = (value: EdhrDateTimeValue): Date | undefined => {
  if (value === null || value === undefined || value === '') return undefined
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? undefined : value

  if (typeof value === 'number') {
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? undefined : date
  }

  const trimmedValue = value.trim()
  const date = /^\d+$/.test(trimmedValue)
    ? new Date(Number(trimmedValue))
    : new Date(trimmedValue)

  return Number.isNaN(date.getTime()) ? undefined : date
}

export const formatEdhrDateTime = (
  value: EdhrDateTimeValue,
  emptyText = '--',
  invalidText = '时间格式异常'
): string => {
  if (value === null || value === undefined || value === '') return emptyText
  if (typeof value === 'string' && value.trim() === '') return emptyText

  const date = toEdhrDateTime(value)
  return date ? formatDate(date, 'YYYY-MM-DD HH:mm:ss') : invalidText
}

export const edhrDateTimeFormatter = (
  _row: unknown,
  _column: unknown,
  cellValue: EdhrDateTimeValue
): string => formatEdhrDateTime(cellValue, '')
