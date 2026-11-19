const businessTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  timeZone: 'Asia/Shanghai',
  year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23'
})

// Backend date-time fields are epoch milliseconds; calendar dates use separate formatters.
export const formatBusinessDateTime = (
  value?: number | string | Date | null,
  emptyText = '--',
  invalidText = '时间格式异常'
) => {
  if (value === undefined || value === null || value === '') return emptyText
  if (!(value instanceof Date) && typeof value !== 'number' &&
    (typeof value !== 'string' || !/^\d+$/.test(value))) return invalidText
  const date = value instanceof Date ? value : new Date(Number(value))
  if (!Number.isFinite(date.getTime())) return invalidText
  const parts = Object.fromEntries(businessTimeFormatter.formatToParts(date).map(part => [part.type, part.value]))
  return `${parts.year}-${parts.month}-${parts.day} ${parts.hour}:${parts.minute}:${parts.second}`
}

export const businessDateTimeFormatter = (
  _row: unknown,
  _column: unknown,
  value?: number | string | Date | null
) => formatBusinessDateTime(value, '')
